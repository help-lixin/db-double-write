package help.lixin.plugin.api;

import help.lixin.plugin.ctx.SyncContext;

public interface IDataSyncService {
    void sync(SyncContext ctx) throws Exception;
}
