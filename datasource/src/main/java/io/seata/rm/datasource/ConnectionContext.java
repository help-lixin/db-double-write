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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.rm.datasource.undo.SQLUndoLog;

/**
 * The type Connection context.
 *
 * @author sharajava
 */
public class ConnectionContext {
    /**
     * Table and primary key should not be duplicated.
     */
    private Set<String> lockKeysBuffer = new HashSet<>();
    private List<SQLUndoLog> sqlUndoItemsBuffer = new ArrayList<>();

    public Set<String> getLockKeysBuffer() {
        return lockKeysBuffer;
    }

    public void setLockKeysBuffer(Set<String> lockKeysBuffer) {
        this.lockKeysBuffer = lockKeysBuffer;
    }

    public List<SQLUndoLog> getSqlUndoItemsBuffer() {
        return sqlUndoItemsBuffer;
    }

    public List<SQLUndoLog> getUndoItems() {
        return sqlUndoItemsBuffer;
    }

    public void setSqlUndoItemsBuffer(List<SQLUndoLog> sqlUndoItemsBuffer) {
        this.sqlUndoItemsBuffer = sqlUndoItemsBuffer;
    }

    void appendUndoItem(SQLUndoLog sqlUndoLog) {
        sqlUndoItemsBuffer.add(sqlUndoLog);
    }

    /**
     * Reset.
     */
    public void reset() {
        lockKeysBuffer.clear();
        sqlUndoItemsBuffer.clear();
    }
}
