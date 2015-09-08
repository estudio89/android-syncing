package br.com.estudio89.syncing.serialization;

import br.com.estudio89.syncing.models.SyncModel;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Created by luccascorrea on 6/21/15.
 */
public class SyncModelSerializer<Model extends SyncModel<?>> extends JSONSerializer<Model> {

    public SyncModelSerializer(Class<Model> type) {
        super(type);
    }

    @Override
    public List<Field> toJSON(Model object, JSONObject jsonObject) throws JSONException, IllegalAccessException {
        List<Field> unusedFields = super.toJSON(object, jsonObject);
        jsonObject.put("idClient", object.getId());
        return unusedFields;
    }
}
