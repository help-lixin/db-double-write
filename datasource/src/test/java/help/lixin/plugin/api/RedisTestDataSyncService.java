package help.lixin.plugin.api;

import help.lixin.plugin.ctx.SyncContext;
import io.seata.common.loader.LoadLevel;

@LoadLevel(name = "redis", order = -1)
public class RedisTestDataSyncService implements IDataSyncService {
    @Override
    public void sync(SyncContext ctx) throws Exception {
        throw new Exception("redis execute sync fail...");
    }
}
