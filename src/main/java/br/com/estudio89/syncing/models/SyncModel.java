package br.com.estudio89.syncing.models;


import br.com.estudio89.syncing.serialization.annotations.JSON;
import com.orm.StringUtil;
import com.orm.SugarRecord;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.Iterator;

/**
 * Created by luccascorrea on 6/20/15.
 */
public abstract class SyncModel<T extends SyncModel<?>> extends SugarRecord<T> {

    @JSON(name="id", ignoreIf = "0")
    protected long idServer = 0;

    @JSON(ignore=true)
    protected boolean modified = false;

    @JSON(ignore=true)
    protected boolean _isNew = false;


    public long getIdServer() {
        return idServer;
    }

    public void setIdServer(long idServer) {
        this.idServer = idServer;
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    public boolean isNew() {
        return _isNew;
    }

    public void setIsNew(boolean isNew) {
        this._isNew = isNew;
    }

    public static <Model extends SyncModel> long getNewObjectsCount(Class <Model> type) {

        return Model.count(type,"_is_new = ?",new String[]{1 + ""});
    }

    public static <Model extends SyncModel> void setObjectsOld(Class <Model> type) {
        String table = Model.getTableName(type);
        Model.executeQuery("UPDATE " + table + " SET _is_new = ? WHERE _is_new = ?",new String[]{0 + "", 1 + ""});
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SyncModel<?>)) {
            return false;
        }

        return ((SyncModel<?>) o).getId() == this.getId();
    }

    private static <Model extends  SyncModel> Field getDateField(Class<Model> type) {
        Field[] fieldList = type.getDeclaredFields();
        for (Field f:fieldList) {
            if (f.getType() == Date.class) {
                return f;
            }
        }

        return null;
    }

    /**
     * Returns the oldest object in cache.
     * This method should only be used if the model has a single date field.
     * If the method has more date fields, use the more explicit getOldest(type, dateColumn).
     *
     * @param type
     * @param <Model>
     * @return
     */
    public static <Model extends  SyncModel> SyncModel<?> getOldest(Class<Model> type) {
        Field dateField= getDateField(type);
        if (dateField == null) {
            return null;
        }
        return getOldest(type, dateField);

    }

    public static <Model extends  SyncModel> SyncModel<?> getOldest(Class<Model> type, Field dateField) {
        if (dateField.getType() != Date.class) {
            throw new IllegalArgumentException("The field must be of type Date.class but was " + dateField.getType().getSimpleName());
        }
        String dateColumn = StringUtil.toSQLName(dateField.getName());
        return getOldest(type, dateColumn);
    }

    public static <Model extends  SyncModel> SyncModel<?> getOldest(Class<Model> type, String dateColumn) {
        Iterator<Model> iterator = Model.findAsIterator(type, "", new String[]{}, "", dateColumn + " ASC", "1");
        if (iterator.hasNext()) {
            return iterator.next();
        }

        return null;
    }


}
