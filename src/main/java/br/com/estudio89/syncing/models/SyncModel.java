package br.com.estudio89.syncing.models;


import br.com.estudio89.syncing.serialization.JSON;
import com.orm.StringUtil;
import com.orm.SugarRecord;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.Iterator;

/**
 * Created by luccascorrea on 6/20/15.
 */
public abstract class SyncModel<T extends SyncModel<?>> extends SugarRecord<T> {

    @JSON(name="idClient", readable=false)
    Long id;

    @JSON(name="id")
    long idServer;

    @JSON(ignore=true)
    boolean modified = false;

    @JSON(ignore=true)
    boolean _isNew = false;


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

    private static <Model extends  SyncModel> String getDateColumn(Class<Model> type) {
        Field[] fieldList = type.getDeclaredFields();
        for (Field f:fieldList) {
            if (f.getType() == Date.class) {
                return StringUtil.toSQLName(f.getName());
            }
        }

        return null;
    }

    public static <Model extends  SyncModel> SyncModel<?> getOldest(Class<Model> type) {
        String dateColumn = getDateColumn(type);
        if (dateColumn == null) {
            return null;
        }

        Iterator<Model> iterator = Model.findAsIterator(type, "", new String[]{}, "", dateColumn + " ASC", "1");
        if (iterator.hasNext()) {
            return iterator.next();
        }

        return null;
    }
}
