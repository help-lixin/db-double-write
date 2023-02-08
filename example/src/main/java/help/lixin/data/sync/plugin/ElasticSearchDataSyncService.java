package help.lixin.data.sync.plugin;

import help.lixin.plugin.api.IDataSyncService;
import help.lixin.plugin.ctx.SyncContext;
import io.seata.rm.datasource.sql.struct.Field;
import io.seata.rm.datasource.sql.struct.Row;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.rm.datasource.undo.SQLUndoLog;
import io.seata.sqlparser.SQLType;

import java.util.List;

public class ElasticSearchDataSyncService implements IDataSyncService {
    @Override
    public void sync(SyncContext ctx) throws Exception {
        List<SQLUndoLog> items = ctx.getItems();
        for (SQLUndoLog log : items) {
            SQLType sqlType = log.getSqlType();
            TableRecords beforeImage = log.getBeforeImage();
            TableRecords afterImage = log.getAfterImage();
            System.out.println("\n\n");
            System.out.println("sqlType: " + sqlType);
            if (null != afterImage) {
                List<Row> rows = afterImage.getRows();
                for (Row row : rows) {
                    System.out.println("tableName:" + afterImage.getTableName());
                    List<Field> fields = row.getFields();
                    for (Field field : fields) {
                        System.out.println(field.getName() + "--------------" + field.getValue());
                    }
                }
            }
        }
    }
}
