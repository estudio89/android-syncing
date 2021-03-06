package br.com.estudio89.syncing;

import android.content.Context;
import br.com.estudio89.syncing.bus.AsyncBus;
import br.com.estudio89.syncing.models.SyncModel;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by luccascorrea on 11/30/14.
 *
 * A {@link SyncManager} that only reads data from the server and never modifies anything.
 */
public abstract class ReadOnlyAbstractSyncManager<T extends SyncModel<?>> extends AbstractSyncManager<T> {

    @Override
    public String getResponseIdentifier() {
        return "" + this.hashCode();
    }

    @Override
    public boolean shouldSendSingleObject() {
        return false;
    }

    @Override
    public JSONArray getModifiedData() {
        return new JSONArray();
    }

    @Override
    public boolean hasModifiedData() {
        return false;
    }

    @Override
    public List<String> getModifiedFiles() {
        return new ArrayList<>();
    }

    @Override
    public List<String> getModifiedFilesForObject(JSONObject object) {
        return new ArrayList<>();
    }

    @Override
    public void postEvent(List<T> objects, AsyncBus bus, Context context) {

    }

    @Override
    public List<T> processSendResponse(JSONArray jsonArray) {
        return new ArrayList<>();
    }

    @Override
    public JSONObject serializeObject(T object) {
        return null;
    }
}
