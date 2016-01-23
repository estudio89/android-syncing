package br.com.estudio89.syncing.extras;

import android.content.Context;
import br.com.estudio89.syncing.ReadOnlyAbstractSyncManager;
import br.com.estudio89.syncing.SyncConfig;
import br.com.estudio89.syncing.SyncManager;
import br.com.estudio89.syncing.models.SyncModel;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by luccascorrea on 1/6/16.
 *
 * This {@link SyncManager} is loaded automatically. It deals with
 * the response sent by the server indicating that the user's token expired.
 */
public class SyncManagerExpiredToken extends ReadOnlyAbstractSyncManager {

    @Override
    public String getIdentifier() {
        return "expiredToken";
    }

    @Override
    public boolean hasTimestamp() {
        return false;
    }

    @Override
    public List saveNewData(JSONArray jsonObjects, String deviceId, JSONObject params, Context context) {
        boolean expired = params.optBoolean("expiredToken");
        if (expired) {
            SyncConfig syncConfig = SyncConfig.getInstance();
            if (syncConfig.userIsLoggedIn()) {
                syncConfig.logout(true, true);
            }
        }
        return new ArrayList();
    }

    @Override
    public SyncModel saveObject(JSONObject object, String deviceId, Context context) {
        return null;
    }
}
