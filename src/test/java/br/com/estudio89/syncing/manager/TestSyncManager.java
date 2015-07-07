package br.com.estudio89.syncing.manager;

import android.content.Context;
import br.com.estudio89.syncing.AbstractSyncManager;
import br.com.estudio89.syncing.SyncManager;
import br.com.estudio89.syncing.bus.AsyncBus;
import br.com.estudio89.syncing.models.SyncModel;
import br.com.estudio89.syncing.serialization.annotations.Paginate;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by luccascorrea on 11/28/14.
 */
@Paginate(byField="pubDate")
public class TestSyncManager extends AbstractSyncManager<TestSyncModel> {

    public void setOldestInCache(TestSyncModel old) {
        oldestInCache = old;
    }

    public void setNestedManager(SyncManager nested) {
        for (Field f:childrenFields.keySet()) {
            if (f.getType() == List.class){
                childrenFields.put(f,nested);
                return;
            }
        }
    }

    public Field getDateField() {
        return dateField;
    }

    public HashMap<Field,String> getParentFields() {
        return parentFields;
    }


    public HashMap<Field, SyncManager> getChildrenFields() {
        return childrenFields;
    }

    @Override
    public void saveBooleanPref(String key, boolean value, Context context) {
        super.saveBooleanPref(key, value, context);
    }

    @Override
    public TestSyncModel getOldest() {
        return super.getOldest();
    }

    @Override
    public List<TestSyncModel> listAll() {
        return super.listAll();
    }

    @Override
    public void deleteAllChildren(Class childClass, String parentColumn, long parentId) {
        super.deleteAllChildren(childClass, parentColumn, parentId);
    }

    @Override
    public void deleteAll() {
        super.deleteAll();
    }

    @Override
    public TestSyncModel findItem(long idServer, String idClient, String deviceId, String itemDeviceId, boolean ignoreDeviceId, JSONObject object) {
        return super.findItem(idServer, idClient, deviceId, itemDeviceId, ignoreDeviceId, object);
    }

    @Override
    public TestSyncModel findItem(long idServer, String idClient, String deviceId, String itemDeviceId, boolean ignoreDeviceId) {
        return super.findItem(idServer, idClient, deviceId, itemDeviceId, ignoreDeviceId);
    }

    @Override
    public SyncModel findParent(Class parentClass, String parentId) {
        return super.findParent(parentClass, parentId);
    }

    @Override
    public void performSave(TestSyncModel item) {
        super.performSave(item);
    }

    @Override
    public SyncManager getNestedSyncManager(Class klass) {
        return super.getNestedSyncManager(klass);
    }

    @Override
    public void verifyFields() {
        super.verifyFields();
    }

    @Override
    public Date getDate(TestSyncModel object) {
        return super.getDate(object);
    }

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
    public List<String> getModifiedFiles() {
        return new ArrayList<String>();
    }

    @Override
    public List<String> getModifiedFilesForObject(JSONObject object) {
        return new ArrayList<String>();
    }

    @Override
    public void postEvent(List objects, AsyncBus bus, Context context) {

    }
}
