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
package io.seata.rm.datasource.undo.oracle;

import io.seata.common.loader.LoadLevel;
import io.seata.common.util.BlobUtils;
import io.seata.core.constants.ClientTableColumnsName;
import io.seata.rm.datasource.undo.AbstractUndoLogManager;
import io.seata.rm.datasource.undo.UndoLogParser;
import io.seata.sqlparser.util.JdbcConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

/**
 * @author jsbxyyx
 */
@LoadLevel(name = JdbcConstants.ORACLE)
public class OracleUndoLogManager extends AbstractUndoLogManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(OracleUndoLogManager.class);


    private static final String INSERT_UNDO_LOG_SQL = "INSERT INTO " + UNDO_LOG_TABLE_NAME + "\n" +
            "\t(id,context, rollback_info, log_status, log_created, log_modified)\n" +
            "VALUES (UNDO_LOG_SEQ.nextval, ?, ?, ?, sysdate, sysdate)";

    private static final String DELETE_UNDO_LOG_BY_CREATE_SQL = "DELETE FROM " + UNDO_LOG_TABLE_NAME +
            " WHERE log_created <= ? and ROWNUM <= ?";

    @Override
    public int deleteUndoLogByLogCreated(Date logCreated, int limitRows, Connection conn) throws SQLException {
        try (PreparedStatement deletePST = conn.prepareStatement(DELETE_UNDO_LOG_BY_CREATE_SQL)) {
            deletePST.setDate(1, new java.sql.Date(logCreated.getTime()));
            deletePST.setInt(2, limitRows);
            int deleteRows = deletePST.executeUpdate();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("batch delete undo log size {}", deleteRows);
            }
            return deleteRows;
        } catch (Exception e) {
            if (!(e instanceof SQLException)) {
                e = new SQLException(e);
            }
            throw (SQLException) e;
        }
    }

    @Override
    protected void insertUndoLogWithNormal(String id, String rollbackCtx,
                                           byte[] undoLogContent, Connection conn) throws SQLException {
        insertUndoLog(id, rollbackCtx, undoLogContent, State.Normal, conn);
    }

    @Override
    protected byte[] getRollbackInfo(ResultSet rs) throws SQLException {
        Blob b = rs.getBlob(ClientTableColumnsName.UNDO_LOG_ROLLBACK_INFO);
        byte[] rollbackInfo = BlobUtils.blob2Bytes(b);
        return rollbackInfo;
    }

    @Override
    protected void insertUndoLogWithGlobalFinished(String id,UndoLogParser parser, Connection conn) throws SQLException {
        insertUndoLog(id, buildContext(parser.getName()),
                parser.getDefaultContent(), State.GlobalFinished, conn);
    }


    private void insertUndoLog(String id, String rollbackCtx,
                               byte[] undoLogContent, State state, Connection conn) throws SQLException {
        try (PreparedStatement pst = conn.prepareStatement(INSERT_UNDO_LOG_SQL)) {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(undoLogContent);
            pst.setBlob(1, inputStream);
            pst.setString(2, rollbackCtx);
            pst.setInt(3, state.getValue());
            pst.executeUpdate();
        } catch (Exception e) {
            if (!(e instanceof SQLException)) {
                e = new SQLException(e);
            }
            throw (SQLException) e;
        }
    }

}