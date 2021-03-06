package br.com.estudio89.syncing;

import android.content.Context;
import android.content.SharedPreferences;
import br.com.estudio89.syncing.bus.AsyncBus;
import br.com.estudio89.syncing.models.SyncModel;
import br.com.estudio89.syncing.serialization.SerializationUtil;
import br.com.estudio89.syncing.serialization.SyncModelSerializer;
import br.com.estudio89.syncing.serialization.annotations.NestedManager;
import br.com.estudio89.syncing.serialization.annotations.Paginate;
import com.orm.dsl.Ignore;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by luccascorrea on 11/30/14.
 *
 */
@SuppressWarnings({"unused", "unchecked"})
public abstract class AbstractSyncManager<Model extends SyncModel<?>> implements SyncManager<Model>{
    Class modelClass;
    protected Model oldestInCache;
    protected Field dateField;
    protected HashMap<Field,String> parentFields = new HashMap<>();
    protected boolean shouldPaginate;
    protected String paginationIdentifier;
    protected HashMap<Field, SyncManager> childrenFields = new HashMap<>();
    protected DataSyncHelper dataSyncHelper;

    public AbstractSyncManager() {
        shouldPaginate = this.getClass().isAnnotationPresent(Paginate.class);
        if (shouldPaginate && this instanceof ReadOnlyAbstractSyncManager) {
            throw new IllegalArgumentException("ReadOnlyAbstractSyncManager classes cannot paginate. Remove the @Paginate annotation from your class definition.");
        }
        try {
            this.modelClass = ((Class) ((ParameterizedType) getClass()
                    .getGenericSuperclass()).getActualTypeArguments()[0]);
            verifyFields();
        } catch(ClassCastException e) {
            if (!(this instanceof ReadOnlyAbstractSyncManager)) {
                throw new IllegalArgumentException("The model class for this SyncManager was not specified and it does not inherit from the class ReadOnlyAbstractSyncManager. " +
                        "You probably forgot to extend your class from AbstractSyncManager<Model>.");
            }
        }
    }

    @Override
    public void setDataSyncHelper(DataSyncHelper dataSyncHelper) {
        this.dataSyncHelper = dataSyncHelper;
    }

    /**
     * Loops through the model class's fields in order to identify
     * nested managers, parent models and the datefield used for paginating.
     */
    protected void verifyFields() {
        Field[] fieldList = modelClass.getDeclaredFields();
        String paginateField = "";
        if (shouldPaginate) {
            Paginate annotation = getClass().getAnnotation(Paginate.class);
            paginateField = annotation.byField();
            paginationIdentifier = annotation.extraIdentifier();
        }
        for (Field f:fieldList) {
            Class type = f.getType();
            if (shouldPaginate && type == Date.class) {
                if ("".equals(paginateField) || paginateField.equals(f.getName())) {
                    f.setAccessible(true);
                    dateField = f;
                }
            } else if (SyncModel.class.isAssignableFrom(type) && !f.isAnnotationPresent(Ignore.class)) {
                f.setAccessible(true);
                String parentFieldName = SerializationUtil.getFieldName(f);
                parentFields.put(f, parentFieldName);
            } else if (f.isAnnotationPresent(NestedManager.class)) {
                f.setAccessible(true);
                NestedManager annotation = f.getAnnotation(NestedManager.class);
                childrenFields.put(f, getNestedSyncManager(annotation.manager()));
            }
        }
    }

    /**
     * Instantiates the nested {@link SyncManager}.
     * @param klass the class
     * @return new instance
     */
    protected SyncManager getNestedSyncManager(Class klass) {
        try {
            return (SyncManager) klass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throwException(e);
        }
        return null;
    }

    /**
     * Returns the date value for an object. This date is used when paginating.
     * @param object a {@link SyncModel} instance.
     * @return the date
     */
    protected Date getDate(Model object) {
        if (dateField != null) {
            try {
                return (Date) dateField.get(object);
            } catch (IllegalAccessException e) {
                throwException(e);
            }
        }
        return null;
    }

    /**
     * This method must be implemented if the Model supports pagination.
     * It is used for sending a delete event when the server asks the cache
     * to be cleaned while fetching new data.
     *
     * @return should return an instance of the deleted sync manager. Default implementation
     * always returns null.
     */
    @SuppressWarnings("SameReturnValue")
    public SyncManager getSyncManagerDeleted() {
        return null;
    }

    @Override
    public abstract String getIdentifier();

    @Override
    public abstract String getResponseIdentifier();

    @Override
    public boolean shouldSendSingleObject(){
        return false;
    }

    @Override
    public boolean shouldRetainSavedObjects() {
        return true;
    }

    @Override
    public boolean hasTimestamp() {
        return true;
    }

    @Override
    public JSONArray getModifiedData() {
        List<Model> objectList = Model.find(modelClass, "modified = ?", "1");
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
    public List<String> getModifiedFiles(){
        return new ArrayList<>();
    }

    @Override
    public List<String> getModifiedFilesForObject(JSONObject object) {
        return new ArrayList<>();
    }

    /**
     * This method is necessary for unit testing this class.
     * @return the oldest item
     */
    protected Model getOldest() {
        if (dateField == null) {
            return null;
        }
        return (Model) SyncModel.getOldest(modelClass, dateField);
    }

    /**
     * This method is necessary for unit testing this class.
     * @return list with all the items.
     */
    protected List<? extends SyncModel> listAll() {
        return SyncModel.listAll(modelClass);
    }

    /**
     * This method is necessary for unit testing this class.
     */
    protected void deleteAll() {
        SyncModel.deleteAll(this.modelClass);
    }

    protected void deleteMissingChildren(Class childClass, String parentColumn, long parentId, List<? extends SyncModel> newItems) {
        List<Long> remainingIds = new ArrayList<>();
        for (SyncModel sm:newItems) {
            if (sm != null) {
                remainingIds.add(sm.getId());
            }
        }

        if (remainingIds.size() > 0) {
            SyncModel.deleteAll(childClass, parentColumn + " = " + parentId + " AND id not in (" + StringUtil.join(remainingIds, ",") + ")");
        }

    }

    protected String getPaginationIdentifier(JSONObject params) {
        String identifier = "";
        if (!"".equals(paginationIdentifier)) {
            try {
                identifier = "." + params.getString(paginationIdentifier);
            } catch (JSONException e) {
                throwException(e);
            }
        }
        return identifier;
    }
    public List<Model> saveNewData(JSONArray jsonObjects, String deviceId, JSONObject params, Context context) throws IOException {

        if(shouldPaginate && params.has("more")) {
            // The user is paginating
            boolean more = params.optBoolean("more");
            saveBooleanPref("more" + getPaginationIdentifier(params), more, context);
        }


        List<? extends SyncModel> deletedObjects = null;
        boolean isSyncing = false;
        if(shouldPaginate && params.has("deleteCache")) {
            // The user is fetching new objects
            isSyncing = true;
            boolean deleteCache = params.optBoolean("deleteCache");
            if (deleteCache) {
                // The server asked that the cache is cleared
                deletedObjects = listAll();
                deleteAll();
                saveBooleanPref("more" + getPaginationIdentifier(params),true, context);
            }
        }

        oldestInCache = getOldest();
        List<Model> newObjects = new ArrayList<>();
        JSONObject objectJSON;
        try {
            for (int i = 0; i < jsonObjects.length(); i++) {
                objectJSON = jsonObjects.getJSONObject(i);

                if (shouldPaginate && isSyncing && dateField != null && oldestInCache != null) {
                    String jsonField = SerializationUtil.getFieldName(dateField);
                    String strDate = objectJSON.getString(jsonField);
                    Date pubDate = SerializationUtil.parseServerDate(strDate);
                    if (pubDate.getTime() < getDate(oldestInCache).getTime()) {
                        // The user is syncing and received an old item that is not in cache
                        // This means its an old item beyond what is in cache and so it should be discarded.
                        saveBooleanPref("more" + getPaginationIdentifier(params), true, context);
                        continue;
                    }
                }
                Model object = this.saveObject(objectJSON, deviceId, context);
                if (shouldRetainSavedObjects()) {
                    newObjects.add(object);
                }

            }
        } catch (JSONException e) {
            throwException(e);
        }

        if (deletedObjects != null) {
            // The cache was cleared. Sending delete event.
            SyncManager syncManagerDeleted = getSyncManagerDeleted();
            if (syncManagerDeleted != null) {
                this.dataSyncHelper.addToEventQueue(syncManagerDeleted, deletedObjects);
            }
        }
        return newObjects;

    }

    @Override
    public List<Model> processSendResponse(JSONArray jsonArray) {
        List<Model> objects = new ArrayList<>();
        try {
            for (int i=0; i<jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                Model object = null;
                try {
                    object = processResponseForObject(obj);
                } catch (IOException e) {
                    throwException(e);
                }
                objects.add(object);
            }
        } catch (JSONException e) {
            throwException(e);
        }
        return objects;
    }

    public Model processResponseForObject(JSONObject obj) throws JSONException, IOException {
        long idServer = obj.getLong("id");
        String idClient = getStringOrNull(obj, "idClient");

        Model object = findItem(idServer, idClient, "", null, true);

        if (object != null) {
            object.setModified(false);
            object.setIdServer(idServer);
            object.save();
        }
        return object;
    }

    protected void throwException(Throwable e) {
        String msg = "While processing SyncManager with identifier " + this.getIdentifier() + ".";
        throw new RuntimeException(msg, e);
    }
    @Override
    public JSONObject serializeObject(Model object) {

        JSONObject jsonObject = new JSONObject();
        SyncModelSerializer<Model> serializer = new SyncModelSerializer<>(this.modelClass);
        try {
            serializer.toJSON(object, jsonObject);
        } catch (JSONException | IllegalAccessException e) {
            throwException(e);
        }

        // Adding parent id
        if (parentFields.size() > 0) {
            for (Field parentField:parentFields.keySet()) {
                String fieldName = parentFields.get(parentField);
                try {
                    SyncModel parent = (SyncModel) parentField.get(object);
                    if (parent != null) {
                        jsonObject.put(fieldName, parent.getIdServer());
                    } else {
                        jsonObject.put(fieldName, JSONObject.NULL);
                    }
                } catch (IllegalAccessException | JSONException e) {
                    throwException(e);
                }
            }
        }

        // Adding writable children fields
        for (Field childField:childrenFields.keySet()) {
            NestedManager annotation = childField.getAnnotation(NestedManager.class);
            if (annotation.writable()) {
                String accessorMethod = annotation.accessorMethod();
                Method method = null;
                if (!"".equals(accessorMethod)) {
                    try {
                        method = this.modelClass.getMethod(accessorMethod);
                    } catch (NoSuchMethodException e) {
                        throwException(e);
                    }
                } else {
                    String name = childField.getName();
                    name = name.substring(0, 1).toUpperCase() + name.substring(1);
                    try {
                        method = this.modelClass.getMethod("get" + name);
                    } catch (NoSuchMethodException e) {
                        throwException(e);
                    }
                }
                String fieldName = SerializationUtil.getFieldName(childField);
                SyncManager childSyncManager = childrenFields.get(childField);
                List<SyncModel> children = null;
                try {
                    assert method != null;
                    children = (List<SyncModel>) method.invoke(object);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throwException(e);
                }
                JSONArray serializedChildren = new JSONArray();
                assert children != null;
                for (SyncModel child:children) {
                    serializedChildren.put(childSyncManager.serializeObject(child));
                }
                try {
                    jsonObject.put(fieldName, serializedChildren);
                } catch (JSONException e) {
                    throwException(e);
                }
            }
        }

        return jsonObject;
    }
    protected Model findItem(long idServer, String idClient, String deviceId, String itemDeviceId) throws IOException {
        return findItem(idServer, idClient, deviceId, itemDeviceId, false, null);
    }

    protected Model findItem(long idServer, String idClient, String deviceId, String itemDeviceId, boolean ignoreDeviceId) throws IOException {
        return findItem(idServer, idClient, deviceId, itemDeviceId, ignoreDeviceId, null);
    }

    protected Model findItem(long idServer, String idClient, String deviceId, String itemDeviceId, JSONObject object) throws IOException {
        return findItem(idServer, idClient, deviceId, itemDeviceId, false, object);
    }
    /**
     * Given a server id and a client id, checks if there is an item in the database that matches.
     * If there is one, returns it, if there isn't, returns null.
     *
     * @param idServer id in the server
     * @param idClient id in the device
     * @return the item
     */
    protected Model findItem(long idServer, String idClient, String deviceId, String itemDeviceId, boolean ignoreDeviceId, JSONObject object) throws IOException {
        List<Model> objectList;
        if ((ignoreDeviceId || deviceId.equals(itemDeviceId)) && idClient != null) {
            if (idServer != 0) {
                objectList = SyncModel.find(this.modelClass, "id_server = ? or id = ?", idServer + "", idClient);
            } else {
                objectList = SyncModel.find(this.modelClass, "id = ?", idClient);
            }

        } else {
            objectList = SyncModel.find(this.modelClass, "id_server = ?", idServer + "");
        }
        if (objectList.size() > 0) {
            return objectList.get(0);
        } else {
            return null;
        }
    }

    /**
     * Given the parent's class and id, fetches it from the database or returns null
     * if it does not exist.
     *
     * @param parentClass the parent class
     * @param parentId the parent item id (in the server)
     * @return the parent item or null if not found
     */
    protected SyncModel findParent(Class parentClass, String parentId) throws IOException {
        if ("null".equals(parentId)) {
            return null;
        }

        List<SyncModel> results = SyncModel.find(parentClass, "id_server = ?", parentId);
        if (results.size() > 0) {
            return results.get(0);
        } else {
            return null;
        }
    }

    protected String getStringOrNull(JSONObject object, String key) throws JSONException {
        return object.has(key) && !"null".equals(object.getString(key)) ? object.getString(key) : null;
    }
    @Override
    public Model saveObject(JSONObject object, String deviceId, Context context) throws IOException {
        if (this.modelClass == null) {
            throw new UnsupportedOperationException("Classes that extend ReadOnlyAbstractSyncManager and don't specify a model class must implement their own saveObject method");
        }

        // Getting server id, clientId and deviceId
        long idServer = 0;
        try {
            idServer = object.getLong("id");
        } catch (JSONException e) {
            throwException(e);
        }

        String idClient = null;
        try {
            idClient = getStringOrNull(object, "idClient");
        } catch (JSONException e) {
            throwException(e);
        }

        String itemDeviceId = null;
        try {
            itemDeviceId = getStringOrNull(object, "deviceId");
        } catch (JSONException e) {
            throwException(e);
        }

        // Finding object if it exists
        Model newItem = findItem(idServer, idClient, deviceId, itemDeviceId, object);
        boolean checkIsNew = false;
        if (newItem == null) {
            try {
                newItem = (Model) this.modelClass.newInstance();
                checkIsNew = true;
            } catch (InstantiationException | IllegalAccessException e) {
                throwException(e);
            }
        }

        // Updating attributes from JSON
        SyncModelSerializer<Model> serializer = new SyncModelSerializer<>(this.modelClass);
        try {
            serializer.updateFromJSON(object, newItem);
        } catch (JSONException | IllegalAccessException | IllegalArgumentException e) {
            throwException(e);
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
        if (parentFields.size() > 0) {
            for (Field parentField:parentFields.keySet()) {
                String parentFieldName = parentFields.get(parentField);
                try {
                    String parentId = SerializationUtil.getJSONString(object, parentFieldName);
                    Class<SyncModel> parentClass = (Class<SyncModel>) parentField.getType();

                    SyncModel parent = findParent(parentClass, parentId);
                    if (parent == null && !"null".equals(parentId)) {
                        assert newItem != null;
                        throw new RuntimeException("An item of class " + parentClass.getSimpleName() + " with id server " + parentId + " was not found for item of class " + this.modelClass.getSimpleName() +
                        " with id_server " + newItem.getIdServer());
                    }

                    try {
                        parentField.set(newItem, parent);
                    } catch (IllegalAccessException e) {
                        throwException(e);
                    }
                } catch (JSONException e) {
                    throw new ParentNotFoundException("The field \"" + parentFieldName + "\" was not found in json object. " +
                            "Maybe you forgot to specify which field to look for by using the annotation JSON(name=<field_name>).");
                }
            }
        }

        performSave(newItem);

        // Checking if this object has children fields
        if (childrenFields.size() > 0) {
            for (Field f: childrenFields.keySet()) {
                String jsonName = SerializationUtil.getFieldName(f);
                JSONArray children = new JSONArray();
                try {
                    children = object.getJSONArray(jsonName);
                } catch (JSONException e) {
                    throwException(e);
                }
                NestedManager annotation = f.getAnnotation(NestedManager.class);
                SyncManager nestedSyncManager = childrenFields.get(f);
                nestedSyncManager.setDataSyncHelper(this.dataSyncHelper);

                JSONObject childParams = null;
                if (!"".equals(annotation.paginationParams())) {
                    try {
                        childParams = object.getJSONObject(annotation.paginationParams());
                    } catch (JSONException e) {
                        throwException(e);
                    }
                } else {
                    childParams = new JSONObject();
                }

                List<? extends SyncModel> newChildren = nestedSyncManager.saveNewData(children, deviceId, childParams, context);

                assert newItem != null;
                if (annotation.discardOnSave() && newItem.getId() != null) {
                    Type type = f.getGenericType();
                    if (type instanceof ParameterizedType) {
                        ParameterizedType pType = (ParameterizedType) type;
                        Class<SyncModel> childClass = (Class<SyncModel>) ((ParameterizedType) type).getActualTypeArguments()[0];
                        deleteMissingChildren(childClass, newItem.getSqlName(), newItem.getId(), newChildren);
                    }
                }
                this.dataSyncHelper.addToEventQueue(nestedSyncManager, newChildren);

            }
        }

        return newItem;

    }

    /**
     * This method is necessary for unit testing this class.
     */
    protected void performSave(Model item) {
        item.save();
    }
    /**
     * Saves a boolean preference to the preferences file. Used when paginating.
     */
    protected void saveBooleanPref(String key, boolean value, Context context) {
        SyncConfig syncConfig = SyncConfig.getInstance();
        SharedPreferences sharedPref = syncConfig.getPreferences();
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(this.modelClass.getSimpleName() + "." + key, value);
        editor.commit();
    }

    /**
     * Helper method. Not used here explicitly, available for use by child classes.
     * @param key key
     * @param defaultValue default value
     * @return the value stored in the preferences.
     */
    protected boolean getBooleanPref(String key, boolean defaultValue) {
        SyncConfig syncConfig = SyncConfig.getInstance();
        SharedPreferences sharedPref = syncConfig.getPreferences();
        return sharedPref.getBoolean(this.modelClass.getSimpleName() + "." + key, defaultValue);
    }

    /**
     * Indicates if there are more items to be fetched from the server. Used when paginating.
     * @param context context
     * @return boolean indicating if there are more items.
     */
    public boolean moreOnServer(Context context) {
        return moreOnServer(context, null);
    }

    /**
     * Indicates if there are more items to be fetched from the server. Used when paginating.
     *
     * @param context context
     * @param paginationIdentifier an identifier that distinguished multiple
     *                             paginations of the same model class.
     * @return boolean indicating if there are more items.
     */
    public boolean moreOnServer(Context context, @SuppressWarnings("SameParameterValue") String paginationIdentifier) {
        if (paginationIdentifier == null || "".equals(paginationIdentifier)) {
            paginationIdentifier = "";
        } else if (!paginationIdentifier.startsWith(".")) {
            paginationIdentifier = "." + paginationIdentifier;
        }
        SyncConfig syncConfig = SyncConfig.getInstance();
        SharedPreferences sharedPref = syncConfig.getPreferences();
        return sharedPref.getBoolean(this.modelClass.getSimpleName() + "." + "more" + paginationIdentifier, false);
    }

    @Override
    public abstract void postEvent(List<Model> objects, AsyncBus bus, Context context);

    @Override
    public int getDelay() {
        return 0;
    }

    public static class ParentNotFoundException extends RuntimeException {
        public ParentNotFoundException(String message) {
            super(message);
        }
    }
}
