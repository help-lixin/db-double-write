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
package io.seata.rm.datasource;

import help.lixin.plugin.api.IDataSyncService;
import help.lixin.plugin.ctx.SyncContext;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.rm.datasource.undo.SQLUndoLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * The type Connection proxy.
 *
 * @author sharajava
 */
public class ConnectionProxy extends AbstractConnectionProxy {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionProxy.class);

    private ConnectionContext context = new ConnectionContext();

    /**
     * Instantiates a new Connection proxy.
     *
     * @param dataSourceProxy  the data source proxy
     * @param targetConnection the target connection
     */
    public ConnectionProxy(DataSourceProxy dataSourceProxy, Connection targetConnection) {
        super(dataSourceProxy, targetConnection);
    }

    /**
     * Gets context.
     *
     * @return the context
     */
    public ConnectionContext getContext() {
        return context;
    }

    /**
     * append sqlUndoLog
     *
     * @param sqlUndoLog the sql undo log
     */
    public void appendUndoLog(SQLUndoLog sqlUndoLog) {
        context.appendUndoItem(sqlUndoLog);
    }


    @Override
    public void commit() throws SQLException {
        try {
            // 执行拦截器
            doInterceptor();
            // 拦截器没有抛出错误的情况下,再进行commit提交
            targetConnection.commit();
        } catch (SQLException e) {
            if (targetConnection != null && !getAutoCommit()) {
                rollback();
            }
            throw e;
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }

    private void doInterceptor() throws SQLException {
        try {
            List<IDataSyncService> dataSyncServices = EnhancedServiceLoader.loadAll(IDataSyncService.class);
            if (null != dataSyncServices) {
                for (IDataSyncService dataSyncService : dataSyncServices) {
                    SyncContext ctx = new SyncContext();
                    ctx.setItems(context.getUndoItems());
                    ctx.getOthers().put("__connection", this);
                    dataSyncService.sync(ctx);
                }
            }

            // UndoLogManagerFactory.getUndoLogManager(this.getDbType()).flushUndoLogs(this);
            // 模拟拦截器出现故障
            // int i = 1 / 0;
        } catch (Throwable ex) {
            LOGGER.error("process connectionProxy commit error: {}", ex.getMessage(), ex);
            throw new SQLException(ex);
        }
        context.reset();
    }

    @Override
    public void rollback() throws SQLException {
        targetConnection.rollback();
        context.reset();
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        if (autoCommit && !getAutoCommit()) {
            // change autocommit from false to true, we should commit() first according to JDBC spec.
            commit();
        }
        targetConnection.setAutoCommit(autoCommit);
    }
}
