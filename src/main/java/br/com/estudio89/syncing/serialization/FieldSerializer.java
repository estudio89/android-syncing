package br.com.estudio89.syncing.serialization;

import br.com.estudio89.syncing.serialization.annotations.JSON;
import com.orm.dsl.Ignore;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;

/**
 * Created by luccascorrea on 6/20/15.
 */
public class FieldSerializer<FieldClass> {
    protected Field field;
    protected Object object;
    protected JSONObject jsonObject;
    protected JSON annotation;

    public FieldSerializer(Field field, Object object, JSONObject jsonObject) {
        this.field = field;
        this.object = object;
        this.jsonObject = jsonObject;
        if (field.isAnnotationPresent(JSON.class)) {
            annotation = field.getAnnotation(JSON.class);
        }
    }

    protected String getFieldName() {
        return SerializationUtil.getFieldName(field, annotation);
    }

    protected boolean isIgnored() {
        if (field.isAnnotationPresent(Ignore.class)) {
            return true;
        } else if (annotation == null) {
            return false;
        } else {
            return annotation.ignore();
        }
    }

    protected boolean isWritable() {
        if (annotation == null) {
            return true;
        } else {
            return annotation.writable();
        }
    }

    protected boolean isReadable() {
        if (annotation == null) {
            return true;
        } else {
            return annotation.readable();
        }
    }

    protected Object format(FieldClass value) {
        return value;
    }

    protected FieldClass parse(Object value) {
        if (JSONObject.NULL.equals(value)) {
            return null;
        }
        return (FieldClass) value;
    }

    public boolean updateJSON() throws IllegalAccessException, JSONException {
        if (isIgnored() || !isWritable()) {
            return false;
        }
        field.setAccessible(true);
        FieldClass value = (FieldClass) field.get(object);
        if (annotation != null && !JSON.noValue.equals(annotation.ignoreIf())) {
            if (String.valueOf(format(value)).equals(annotation.ignoreIf())) {
                return false;
            }
        }
        Object formatted = format(value);

        String name = getFieldName();
        String[] nameTree = name.split("\\.");
        JSONObject curJSONObj = jsonObject;
        for (int idx=0; idx < nameTree.length; idx++) {
            String part = nameTree[idx];
            if (idx == nameTree.length - 1) {
                if (formatted != null) {
                    curJSONObj.put(part, formatted);
                } else {
                    curJSONObj.put(part, JSONObject.NULL);
                }
            } else {
                if (curJSONObj.has(part)) {
                    curJSONObj = curJSONObj.getJSONObject(part);
                } else {
                    JSONObject aux = new JSONObject();
                    curJSONObj.put(part, aux);
                    curJSONObj = aux;
                }

            }
        }


        return true;
    }

    public boolean updateField() throws JSONException, IllegalAccessException {
        if (isIgnored() || !isReadable()) {
            return false;
        }

        field.setAccessible(true);
        String name = getFieldName();
        String[] nameTree = name.split("\\.");
        Object value = null;
        JSONObject curJSONObj = jsonObject;
        for(int idx = 0; idx < nameTree.length; idx++) {
            String part = nameTree[idx];
            if (idx == nameTree.length - 1) {
                value = curJSONObj.get(part);
            } else {
                curJSONObj = curJSONObj.getJSONObject(part);
            }
        }
        try {
            field.set(object, parse(value));
        } catch(IllegalArgumentException e) {
            throw new IllegalArgumentException("invalid value for field " + name + ". Type should be " +field.getType().getSimpleName() + " but was " + value.getClass().getSimpleName());
        }

        return true;
    }

}