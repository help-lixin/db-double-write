package help.lixin.plugin.api;

import io.seata.rm.datasource.undo.SQLUndoLog;

import java.util.List;

public interface IDataSyncService {
    void sync(List<SQLUndoLog> items) throws Exception;
}
