package help.lixin.plugin.api;

import help.lixin.plugin.api.IDataSyncService;
import io.seata.common.loader.LoadLevel;
import io.seata.rm.datasource.undo.SQLUndoLog;

import java.util.List;

@LoadLevel(name = "es", order = -2)
public class EsDataSyncService implements IDataSyncService {
    @Override
    public void sync(List<SQLUndoLog> items) throws Exception {
        System.out.println("es execute success...");
    }
}
