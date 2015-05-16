package br.com.estudio89.syncing;

import android.content.Context;
import br.com.estudio89.syncing.bus.AsyncBus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by luccascorrea on 11/30/14.
 */
public abstract class AbstractSyncManager<T> implements SyncManager<T>{
    @Override
    public abstract String getIdentifier();

    @Override
    public abstract String getResponseIdentifier();

    @Override
    public abstract boolean shouldSendSingleObject();

    @Override
    public abstract JSONArray getModifiedData();

    @Override
    public abstract boolean hasModifiedData();

    @Override
    public abstract List<String> getModifiedFiles();

    @Override
    public abstract List<String> getModifiedFilesForObject(JSONObject object);

    @Override
    public List<T> saveNewData(JSONArray jsonObjects, String deviceId, JSONObject params) {
        List<T> newObjects = new ArrayList<T>();
        JSONObject objectJSON;
        try {
            for (int i = 0; i < jsonObjects.length(); i++) {
                objectJSON = jsonObjects.getJSONObject(i);
                T object = this.saveObject(objectJSON, deviceId);
                newObjects.add(object);
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return newObjects;
    }

    @Override
    public abstract void processSendResponse(JSONArray jsonResponse);

    @Override
    public abstract JSONObject serializeObject(T object);

    @Override
    public abstract T saveObject(JSONObject object, String deviceId);

    @Override
    public abstract void postEvent(List<T> objects, AsyncBus bus, Context context);
}
