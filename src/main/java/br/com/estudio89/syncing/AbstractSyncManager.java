package br.com.estudio89.syncing;

import android.content.Context;
import android.content.SharedPreferences;
import br.com.estudio89.syncing.bus.AsyncBus;
import br.com.estudio89.syncing.bus.EventBusManager;
import br.com.estudio89.syncing.models.SyncModel;
import br.com.estudio89.syncing.serialization.NestedManager;
import br.com.estudio89.syncing.serialization.SerializationUtil;
import br.com.estudio89.syncing.serialization.SyncModelSerializer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by luccascorrea on 11/30/14.
 */
public abstract class AbstractSyncManager<Model extends SyncModel<?>> implements SyncManager<Model>{
    Class modelClass;
    protected Model oldestInCache;
    protected Field dateField;
    protected Field parentField;
    protected String parentFieldName;
    protected HashMap<Field, SyncManager> childrenFields = new HashMap<Field, SyncManager>();

    public AbstractSyncManager() {
        this.modelClass = ((Class) ((ParameterizedType) getClass()
                .getGenericSuperclass()).getActualTypeArguments()[0]);
        verifyFields();
    }

    protected void verifyFields() {
        Field[] fieldList = modelClass.getDeclaredFields();
        for (Field f:fieldList) {
            Class type = f.getType();
            if (type == Date.class) {
                f.setAccessible(true);
                dateField = f;
            } else if (SyncModel.class.isAssignableFrom(type)) {
                f.setAccessible(true);
                parentField = f;
                parentFieldName = SerializationUtil.getFieldName(f);
            } else if (f.isAnnotationPresent(NestedManager.class)) {
                f.setAccessible(true);
                NestedManager annotation = f.getAnnotation(NestedManager.class);
                childrenFields.put(f, getNestedSyncManager(annotation.manager()));
            }
        }
    }

    protected Date getDate(Model object) {
        if (dateField != null) {
            try {
                return (Date) dateField.get(object);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    public SyncManager getSyncManagerDeleted() {
        return null;
    }

    @Override
    public abstract String getIdentifier();

    @Override
    public abstract String getResponseIdentifier();

    @Override
    public abstract boolean shouldSendSingleObject();

    @Override
    public JSONArray getModifiedData() {
        List<Model> objectList = Model.find(modelClass, "modified = ?", new String[]{"1"});
        JSONArray array = new JSONArray();
        for (Model object : objectList) {
            array.put(serializeObject(object));
        }
        return array;
    }

    @Override
    public boolean hasModifiedData() {
        return Model.count(modelClass, "modified = ?", new String[]{"1"}) > 0;
    }

    @Override
    public abstract List<String> getModifiedFiles();

    @Override
    public abstract List<String> getModifiedFilesForObject(JSONObject object);

    public List saveNewData(JSONArray jsonObjects, String deviceId, JSONObject params, Context context) {


        if(params.has("more")) {
            // Pagination
            boolean more = params.optBoolean("more");
            saveBooleanPref("more",more, context);
        }

        List<Model> deletedObjects = null;
        boolean isSyncing = false;
        if(params.has("deleteCache")) {
            // Syncing
            isSyncing = true;
            boolean deleteCache = params.optBoolean("deleteCache");
            if (deleteCache) {
                deletedObjects = Model.listAll(this.modelClass);
                Model.deleteAll(this.modelClass);
                saveBooleanPref("more",true, context);
            }
        }

        oldestInCache = (Model) Model.getOldest(modelClass);
        List<Object> newObjects = new ArrayList<Object>();
        JSONObject objectJSON;
        try {
            for (int i = 0; i < jsonObjects.length(); i++) {
                objectJSON = jsonObjects.getJSONObject(i);

                if (isSyncing && dateField != null) {
                    String jsonField = SerializationUtil.getFieldName(dateField);
                    String strDate = objectJSON.getString(jsonField);
                    Date pubDate = SerializationUtil.parseServerDate(strDate);
                    if (pubDate.getTime() < getDate(oldestInCache).getTime()) {
                        // The user is syncing and received an old item that is not in cache
                        // This means its an old item beyond what is in cache and so it should be discarded.
                        saveBooleanPref("more",true, context);
                        continue;
                    }
                }
                Object object = this.saveObject(objectJSON, deviceId, context);
                newObjects.add(object);
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        if (deletedObjects != null) {
            SyncManager syncManagerDeleted = getSyncManagerDeleted();
            if (syncManagerDeleted != null) {
                syncManagerDeleted.postEvent(deletedObjects, EventBusManager.getBus(), null);

            }
        }
        return newObjects;

    }

    @Override
    public abstract void processSendResponse(JSONArray jsonResponse);

    @Override
    public JSONObject serializeObject(Model object) {
        JSONObject jsonObject = new JSONObject();
        SyncModelSerializer<Model> serializer = new SyncModelSerializer<Model>(this.modelClass);
        try {
            serializer.toJSON(object, jsonObject);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        // Adding parent id
        if (parentField != null) {
            String fieldName = SerializationUtil.getFieldName(parentField);
            try {
                SyncModel parent = (SyncModel) parentField.get(object);
                jsonObject.put(fieldName, parent.getIdServer());
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }

        return jsonObject;
    }

    protected Model findItem(long idServer, String idClient) {
        List<Model> objectList;
        if (idClient != null) {
            objectList = Model.find(this.modelClass, "id_server = ? or id = ?", new String[]{idServer + "", idClient});
        } else {
            objectList = Model.find(this.modelClass, "id_server = ?", new String[]{idServer + ""});
        }
        if (objectList.size() > 0) {
            return objectList.get(0);
        } else {
            return null;
        }
    }

    protected SyncManager getNestedSyncManager(Class klass) {
        try {
            return (SyncManager) klass.newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    protected SyncModel findParent(Class parentClass, String parentId) {
        List<SyncModel> results = SyncModel.find(parentClass, "id_server = ", new String[]{parentId});
        if (results.size() > 0) {
            return results.get(0);
        } else {
            return null;
        }
    }
    @Override
    public Model saveObject(JSONObject object, String deviceId, Context context) {

        // Getting server id and client id
        long idServer = 0;
        try {
            idServer = object.getLong("id");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        String idClient = null;
        try {
            idClient = object.has("idClient") && object.getString("idClient") != "null" ? object.getString("idClient") : null;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        // Finding object if it exists
        Model newItem = findItem(idServer, idClient);
        boolean checkIsNew = false;
        if (newItem == null) {
            try {
                newItem = (Model) this.modelClass.newInstance();
                checkIsNew = true;
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        // Updating attributes from JSON
        SyncModelSerializer<Model> serializer = new SyncModelSerializer<Model>(this.modelClass);
        try {
            serializer.updateFromJSON(object, newItem);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        // Checking if the item is new
        if (checkIsNew) {
            if (dateField != null) {
                if (oldestInCache == null || getDate(newItem).getTime() > getDate(oldestInCache).getTime()) {
                    // If the condition above is false, the user is paginating, so the item is not new.
                    newItem.setIsNew(true);
                }
            } else {
                newItem.setIsNew(true);
            }
        }

        // Checking if this object has parent fields
        if (parentField != null) {
            try {
                String parentId = object.getString(parentFieldName);
                Class<SyncModel> parentClass = (Class<SyncModel>) parentField.getType();

                SyncModel parent = findParent(parentClass, parentId);
                if (parent == null) {
                    throw new RuntimeException("An item of class " + parentClass.getSimpleName() + " with id server " + parentId + " was not found for item of class " + this.modelClass.getSimpleName() +
                    " with id_server " + newItem.getIdServer());
                }

                try {
                    parentField.set(newItem, parent);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            } catch (JSONException e) {
                throw new IllegalArgumentException("The field \"" + parentFieldName + "\" was not found in json object. " +
                        "Maybe you forgot to specify which field to look for by using the annotation JSON(name=<field_name>).");
            }
        }

        // Checking if this object has children fields
        if (childrenFields.size() > 0) {
            for (Field f: childrenFields.keySet()) {
                String jsonName = SerializationUtil.getFieldName(f);
                JSONArray children;
                try {
                    children = object.getJSONArray(jsonName);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                SyncManager nestedSyncManager = childrenFields.get(f);
                List<SyncModel> newChildren = nestedSyncManager.saveNewData(children,deviceId, new JSONObject(), context);
                nestedSyncManager.postEvent(newChildren, EventBusManager.getBus(), context);
            }
        }

        performSave(newItem);

        return newItem;

    }

    protected void performSave(Model item) {
        item.save();
    }

    protected void saveBooleanPref(String key, boolean value, Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(
                this.modelClass.getCanonicalName(), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    public boolean moreOnServer(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(
                this.modelClass.getCanonicalName(), Context.MODE_PRIVATE);

        return sharedPref.getBoolean("more", true);
    }

    @Override
    public abstract void postEvent(List<Model> objects, AsyncBus bus, Context context);
}
