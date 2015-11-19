package br.com.estudio89.syncing.manager;

import android.content.Context;
import br.com.estudio89.syncing.AbstractSyncManager;
import br.com.estudio89.syncing.bus.AsyncBus;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by luccascorrea on 6/21/15.
 */
public class ChildSyncManager extends AbstractSyncManager<ChildSyncModel>{
    @Override
    public String getIdentifier() {
        return null;
    }

    @Override
    public String getResponseIdentifier() {
        return null;
    }

    @Override
    public boolean shouldSendSingleObject() {
        return false;
    }

    @Override
    public List<String> getModifiedFiles() {
        return null;
    }

    @Override
    public List<String> getModifiedFilesForObject(JSONObject object) {
        return null;
    }

    @Override
    public List<ChildSyncModel> processSendResponse(JSONArray jsonResponse) {
        return new ArrayList<ChildSyncModel>();
    }

    @Override
    public void postEvent(List<ChildSyncModel> objects, AsyncBus bus, Context context) {

    }
}