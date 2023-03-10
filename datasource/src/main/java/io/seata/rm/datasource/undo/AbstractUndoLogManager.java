/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.rm.datasource.undo;

import io.seata.common.Constants;
import io.seata.common.util.CollectionUtils;
import io.seata.core.constants.ClientTableColumnsName;
import io.seata.core.exception.TransactionException;
import io.seata.rm.datasource.ConnectionContext;
import io.seata.rm.datasource.ConnectionProxy;
import io.seata.rm.datasource.DataSourceProxy;
import io.seata.rm.datasource.sql.struct.TableMeta;
import io.seata.rm.datasource.sql.struct.TableMetaCacheFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

import static io.seata.common.DefaultValues.DEFAULT_TRANSACTION_UNDO_LOG_TABLE;

/**
 * @author jsbxyyx
 */
public abstract class AbstractUndoLogManager implements UndoLogManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractUndoLogManager.class);

    protected enum State {
        /**
         * This state can be properly rolled back by services
         */
        Normal(0),
        /**
         * This state prevents the branch transaction from inserting undo_log after the global transaction is rolled
         * back.
         */
        GlobalFinished(1);

        private int value;

        State(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    protected static final String UNDO_LOG_TABLE_NAME = DEFAULT_TRANSACTION_UNDO_LOG_TABLE;

    protected static final String SELECT_UNDO_LOG_SQL = "SELECT * FROM " + UNDO_LOG_TABLE_NAME + " WHERE "
            + ClientTableColumnsName.UNDO_LOG_ID
            + " = ? FOR UPDATE";

    protected static final String DELETE_UNDO_LOG_SQL = "DELETE FROM " + UNDO_LOG_TABLE_NAME + " WHERE "
            + ClientTableColumnsName.UNDO_LOG_ID + " = ?";

    private static final ThreadLocal<String> SERIALIZER_LOCAL = new ThreadLocal<>();

    public static String getCurrentSerializer() {
        return SERIALIZER_LOCAL.get();
    }

    public static void setCurrentSerializer(String serializer) {
        SERIALIZER_LOCAL.set(serializer);
    }

    public static void removeCurrentSerializer() {
        SERIALIZER_LOCAL.remove();
    }

    /**
     * Delete undo log.
     *
     * @param id      the id
     * @param conn     the conn
     * @throws SQLException the sql exception
     */
    @Override
    public void deleteUndoLog(String id, Connection conn) throws SQLException {
        try (PreparedStatement deletePST = conn.prepareStatement(DELETE_UNDO_LOG_SQL)) {
            deletePST.setString(1, id);
            deletePST.executeUpdate();
        } catch (Exception e) {
            if (!(e instanceof SQLException)) {
                e = new SQLException(e);
            }
            throw (SQLException) e;
        }
    }

    /**
     * batch Delete undo log.
     *
     * @param ids
     * @param conn
     */
    @Override
    public void batchDeleteUndoLog(Set<String> ids, Connection conn) throws SQLException {
        if (CollectionUtils.isEmpty(ids) ) {
            return;
        }
        int idSize = ids.size();
        String batchDeleteSql = toBatchDeleteUndoLogSql(idSize);
        try (PreparedStatement deletePST = conn.prepareStatement(batchDeleteSql)) {
            int paramsIndex = 1;
            for (String id : ids) {
                deletePST.setString(paramsIndex++, id);
            }
            int deleteRows = deletePST.executeUpdate();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("batch delete undo log size {}", deleteRows);
            }
        } catch (Exception e) {
            if (!(e instanceof SQLException)) {
                e = new SQLException(e);
            }
            throw (SQLException) e;
        }
    }

    protected static String toBatchDeleteUndoLogSql(int xidSize) {
        StringBuilder sqlBuilder = new StringBuilder(64);
        sqlBuilder.append("DELETE FROM ").append(UNDO_LOG_TABLE_NAME).append(" WHERE  ")
                .append(ClientTableColumnsName.UNDO_LOG_ID).append(" IN ");
        appendInParam(xidSize, sqlBuilder);
        return sqlBuilder.toString();
    }

    protected static void appendInParam(int size, StringBuilder sqlBuilder) {
        sqlBuilder.append(" (");
        for (int i = 0; i < size; i++) {
            sqlBuilder.append("?");
            if (i < (size - 1)) {
                sqlBuilder.append(",");
            }
        }
        sqlBuilder.append(") ");
    }

    protected static boolean canUndo(int state) {
        return state == State.Normal.getValue();
    }

    protected String buildContext(String serializer) {
        Map<String, String> map = new HashMap<>();
        map.put(UndoLogConstants.SERIALIZER_KEY, serializer);
        return CollectionUtils.encodeMap(map);
    }

    protected Map<String, String> parseContext(String data) {
        return CollectionUtils.decodeMap(data);
    }

    /**
     * Flush undo logs.
     *
     * @param cp the cp
     * @throws SQLException the sql exception
     */
    @Override
    public void flushUndoLogs(ConnectionProxy cp) throws SQLException {
        ConnectionContext connectionContext = cp.getContext();

        String id = UUID.randomUUID().toString().replace("-","");
        BranchUndoLog branchUndoLog = new BranchUndoLog();
        branchUndoLog.setId(id);
        branchUndoLog.setSqlUndoLogs(connectionContext.getUndoItems());

        UndoLogParser parser = UndoLogParserFactory.getInstance();
        byte[] undoLogContent = parser.encode(branchUndoLog);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Flushing UNDO LOG: {}", new String(undoLogContent, Constants.DEFAULT_CHARSET));
        }

        insertUndoLogWithNormal(id, buildContext(parser.getName()), undoLogContent,
                cp.getTargetConnection());
    }

    /**
     * Undo.
     *
     * @param dataSourceProxy the data source proxy
     * @param id             the id
     * @throws TransactionException the transaction exception
     */
    @Override
    public void undo(DataSourceProxy dataSourceProxy, String id) throws TransactionException {
        Connection conn = null;
        ResultSet rs = null;
        PreparedStatement selectPST = null;
        boolean originalAutoCommit = true;

        for (; ; ) {
            try {
                conn = dataSourceProxy.getPlainConnection();

                // The entire undo process should run in a local transaction.
                if (originalAutoCommit = conn.getAutoCommit()) {
                    conn.setAutoCommit(false);
                }

                // Find UNDO LOG
                selectPST = conn.prepareStatement(SELECT_UNDO_LOG_SQL);
                selectPST.setString(1, id);
                rs = selectPST.executeQuery();

                boolean exists = false;
                while (rs.next()) {
                    exists = true;

                    // It is possible that the server repeatedly sends a rollback request to roll back
                    // the same branch transaction to multiple processes,
                    // ensuring that only the undo_log in the normal state is processed.
                    int state = rs.getInt(ClientTableColumnsName.UNDO_LOG_LOG_STATUS);
                    if (!canUndo(state)) {
                        if (LOGGER.isInfoEnabled()) {
                            LOGGER.info("xid {} ,ignore {} undo_log", id,  state);
                        }
                        return;
                    }

                    String contextString = rs.getString(ClientTableColumnsName.UNDO_LOG_CONTEXT);
                    Map<String, String> context = parseContext(contextString);
                    byte[] rollbackInfo = getRollbackInfo(rs);

                    String serializer = context == null ? null : context.get(UndoLogConstants.SERIALIZER_KEY);
                    UndoLogParser parser = serializer == null ? UndoLogParserFactory.getInstance()
                            : UndoLogParserFactory.getInstance(serializer);
                    BranchUndoLog branchUndoLog = parser.decode(rollbackInfo);

                    try {
                        // put serializer name to local
                        setCurrentSerializer(parser.getName());
                        List<SQLUndoLog> sqlUndoLogs = branchUndoLog.getSqlUndoLogs();
                        if (sqlUndoLogs.size() > 1) {
                            Collections.reverse(sqlUndoLogs);
                        }
                        for (SQLUndoLog sqlUndoLog : sqlUndoLogs) {
                            TableMeta tableMeta = TableMetaCacheFactory.getTableMetaCache(dataSourceProxy.getDbType()).getTableMeta(
                                    conn, sqlUndoLog.getTableName(), dataSourceProxy.getResourceId());
                            sqlUndoLog.setTableMeta(tableMeta);
                            AbstractUndoExecutor undoExecutor = UndoExecutorFactory.getUndoExecutor(
                                    dataSourceProxy.getDbType(), sqlUndoLog);
                            undoExecutor.executeOn(conn);
                        }
                    } finally {
                        // remove serializer name
                        removeCurrentSerializer();
                    }
                }

                // If undo_log exists, it means that the branch transaction has completed the first phase,
                // we can directly roll back and clean the undo_log
                // Otherwise, it indicates that there is an exception in the branch transaction,
                // causing undo_log not to be written to the database.
                // For example, the business processing timeout, the global transaction is the initiator rolls back.
                // To ensure data consistency, we can insert an undo_log with GlobalFinished state
                // to prevent the local transaction of the first phase of other programs from being correctly submitted.
                // See https://github.com/seata/seata/issues/489

                if (exists) {
                    deleteUndoLog(id, conn);
                    conn.commit();
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("xid {} undo_log deleted with {}", id,
                                State.GlobalFinished.name());
                    }
                } else {
                    insertUndoLogWithGlobalFinished(id, UndoLogParserFactory.getInstance(), conn);
                    conn.commit();
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("xid {}  , undo_log added with {}", id,
                                State.GlobalFinished.name());
                    }
                }

                return;
            } catch (SQLIntegrityConstraintViolationException e) {
                // Possible undo_log has been inserted into the database by other processes, retrying rollback undo_log
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("xid {}   undo_log inserted, retry rollback", id );
                }
            } catch (Throwable e) {
                if (conn != null) {
                    try {
                        conn.rollback();
                    } catch (SQLException rollbackEx) {
                        LOGGER.warn("Failed to close JDBC resource while undo ... ", rollbackEx);
                    }
                }
                // TOOD lixin
//                throw new BranchTransactionException(BranchRollbackFailed_Retriable, String
//                        .format("Branch session rollback failed and try again later xid = %s branchId = %s %s", xid,
//                                branchId, e.getMessage()), e);

            } finally {
                try {
                    if (rs != null) {
                        rs.close();
                    }
                    if (selectPST != null) {
                        selectPST.close();
                    }
                    if (conn != null) {
                        if (originalAutoCommit) {
                            conn.setAutoCommit(true);
                        }
                        conn.close();
                    }
                } catch (SQLException closeEx) {
                    LOGGER.warn("Failed to close JDBC resource while undo ... ", closeEx);
                }
            }
        }
    }

    /**
     * insert uodo log when global finished
     *
     * @param id           the id
     * @param undoLogParser the undoLogParse
     * @param conn          sql connection
     * @throws SQLException
     */
    protected abstract void insertUndoLogWithGlobalFinished(String id,  UndoLogParser undoLogParser,
                                                            Connection conn) throws SQLException;

    /**
     * insert uodo log when normal
     *
     * @param id            the id
     * @param rollbackCtx    the rollbackContext
     * @param undoLogContent the undoLogContent
     * @param conn           sql connection
     * @throws SQLException
     */
    protected abstract void insertUndoLogWithNormal(String id, String rollbackCtx,
                                                    byte[] undoLogContent, Connection conn) throws SQLException;

    /**
     * RollbackInfo to bytes
     *
     * @param rs
     * @return
     * @throws SQLException
     */
    protected abstract byte[] getRollbackInfo(ResultSet rs) throws SQLException;
}
