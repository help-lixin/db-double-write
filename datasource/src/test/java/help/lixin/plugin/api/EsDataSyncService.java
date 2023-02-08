package help.lixin.plugin.api;

import help.lixin.plugin.ctx.SyncContext;
import io.seata.common.loader.LoadLevel;

@LoadLevel(name = "es", order = -2)
public class EsDataSyncService implements IDataSyncService {
    @Override
    public void sync(SyncContext ctx) throws Exception {
        System.out.println("es execute success...");
    }
}
