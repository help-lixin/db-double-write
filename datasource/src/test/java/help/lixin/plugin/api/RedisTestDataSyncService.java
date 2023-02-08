package help.lixin.plugin.api;

import help.lixin.plugin.api.IDataSyncService;
import io.seata.common.loader.LoadLevel;
import io.seata.rm.datasource.undo.SQLUndoLog;

import java.util.List;

@LoadLevel(name = "redis", order = -1)
public class RedisTestDataSyncService implements IDataSyncService {
    @Override
    public void sync(List<SQLUndoLog> items) throws Exception {
        throw new Exception("redis execute sync fail...");
    }
}
