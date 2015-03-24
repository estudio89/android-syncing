package br.com.estudio89.syncing;

import br.com.estudio89.syncing.bus.AsyncBus;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by luccascorrea on 11/28/14.
 */
public class TestSyncManager implements SyncManager {
    @Override
    public String getIdentifier() {
        return "test";
    }

    @Override
    public String getResponseIdentifier() {
        return "test_id";
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
        return new ArrayList<String>();
    }

    @Override
    public List<String> getModifiedFilesForObject(JSONObject object) {
        return new ArrayList<String>();
    }

    @Override
    public List saveNewData(JSONArray jsonObjects, String deviceId, JSONObject params) {
        return new ArrayList();
    }

    @Override
    public void processSendResponse(JSONArray jsonResponse) {

    }

    @Override
    public JSONObject serializeObject(Object object) {
        return new JSONObject();
    }

    @Override
    public Object saveObject(JSONObject object, String deviceId) {
        return new JSONObject();
    }

    @Override
    public void postEvent(List objects, AsyncBus bus) {

    }
}
