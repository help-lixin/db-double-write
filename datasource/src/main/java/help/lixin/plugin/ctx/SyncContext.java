package help.lixin.plugin.ctx;

import io.seata.rm.datasource.undo.SQLUndoLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SyncContext {

    private List<SQLUndoLog> items = new ArrayList<>(0);

    private Map<String, Object> others = new HashMap<>();

    public List<SQLUndoLog> getItems() {
        return items;
    }

    public void setItems(List<SQLUndoLog> items) {
        if (null != items) {
            this.items = items;
        }
    }

    public Map<String, Object> getOthers() {
        return others;
    }

    public void setOthers(Map<String, Object> others) {
        if (null != others) {
            this.others = others;
        }
    }
}
