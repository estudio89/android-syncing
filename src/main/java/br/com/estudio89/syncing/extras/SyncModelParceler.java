package br.com.estudio89.syncing.extras;

import android.os.Parcel;
import android.os.Parcelable;
import br.com.estudio89.syncing.models.SyncModel;

import java.lang.reflect.ParameterizedType;

/**
 * Created by luccascorrea on 7/13/17.
 *
 */
public abstract class SyncModelParceler<X extends SyncModel> implements Parcelable {
    protected X item;

    public SyncModelParceler(X item) {
        this.item = item;
    }

    protected SyncModelParceler(Parcel in) {
        try {
            Class klass = ((Class) ((ParameterizedType) getClass()
                    .getGenericSuperclass()).getActualTypeArguments()[0]);
            item = (X) klass.newInstance();
        } catch(ClassCastException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        item.setId((Long) in.readValue(Long.class.getClassLoader()));
        item.setIsNew(in.readByte() != 0);
        item.setModified(in.readByte() != 0);
        item.setIdServer(in.readLong());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(item.getId());
        dest.writeByte((byte) (item.isNew() ? 1 : 0));
        dest.writeByte((byte) (item.isModified() ? 1 : 0));
        dest.writeLong(item.getIdServer());
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
