package br.com.estudio89.syncing.manager;

import br.com.estudio89.syncing.ReadOnlyAbstractSyncManager;
import org.json.JSONObject;

/**
 * Created by luccascorrea on 6/24/15.
 */
public class TestReadOnlySyncManager extends ReadOnlyAbstractSyncManager {
    @Override
    public String getIdentifier() {
        return "test";
    }

    @Override
    public JSONObject serializeObject(Object object) {
        return new JSONObject();
    }
}
