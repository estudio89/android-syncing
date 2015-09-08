package br.com.estudio89.syncing.serialization;

import com.orm.SugarRecord;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by luccascorrea on 6/20/15.
 */
public class JSONSerializer<Model> {

    private Class modelClass;

    public JSONSerializer(Class<Model> type) {
        this.modelClass = type;
    }

    public List<Field> toJSON(Model object, JSONObject jsonObject) throws JSONException, IllegalAccessException {
        Class superClass = modelClass;
        List<Field> unusedFields = new ArrayList<Field>();

        while (superClass != null) {
            Field[] fields = superClass.getDeclaredFields();
            for (Field field:fields) {
                FieldSerializer fieldSerializer = getFieldSerializer(field, object, jsonObject);
                if (fieldSerializer == null || !fieldSerializer.updateJSON()) {
                    unusedFields.add(field);
                }

            }
            superClass = superClass.getSuperclass();

            if (superClass == SugarRecord.class) {
                break;
            }
        }

        return unusedFields;
    }

    public List<Field> updateFromJSON(JSONObject jsonObject, Model object) throws JSONException, IllegalAccessException {

        Class superClass = modelClass;
        List<Field> unusedFields = new ArrayList<Field>();

        while (superClass != null) {
            Field[] fields = superClass.getDeclaredFields();
            for (Field field:fields) {
                FieldSerializer fieldSerializer = getFieldSerializer(field, object, jsonObject);
                if (fieldSerializer == null || !fieldSerializer.updateField()) {
                    unusedFields.add(field);
                }
            }

            superClass = superClass.getSuperclass();
            if (superClass == SugarRecord.class) {
                break;
            }
        }

        return unusedFields;
    }

    private FieldSerializer getFieldSerializer(Field field, Object object, JSONObject jsonObject) {
        Class type = field.getType();
        if (type == Date.class) {
            return new DateSerializer(field, object, jsonObject);
        } else if (type == Long.TYPE || type == Long.class) {
            return new FieldSerializer<Long>(field, object, jsonObject);
        } else if (type == Integer.TYPE || type == Integer.class) {
            return new FieldSerializer<Integer>(field, object, jsonObject);
        } else if (type == Boolean.TYPE || type == Boolean.class) {
            return new FieldSerializer<Boolean>(field, object, jsonObject);
        } else if (type == String.class) {
            return new FieldSerializer<Boolean>(field, object, jsonObject);
        }

        return null;


    }

}
