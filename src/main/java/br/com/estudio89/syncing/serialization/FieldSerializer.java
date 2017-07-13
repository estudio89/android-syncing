package br.com.estudio89.syncing.serialization;

import br.com.estudio89.syncing.models.SyncModel;
import br.com.estudio89.syncing.serialization.annotations.JSON;
import com.orm.dsl.Ignore;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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

    protected boolean isNullable() {
        if (annotation == null) {
            return false;
        } else {
            return annotation.nullable();
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

    protected boolean allowOverwrite() {
        if (annotation == null) {
            return true;
        } else {
            try {
                SyncModel sm = (SyncModel) object;
                if (!annotation.allowOverwrite()) {
                    return !sm.isModified();
                } else {
                    return annotation.allowOverwrite();
                }
            } catch (ClassCastException e) {
                return annotation.allowOverwrite();
            }

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
        SerializationUtil.setJSONValue(jsonObject, name, formatted);


        return true;
    }

    public boolean updateField() throws JSONException, IllegalAccessException {
        if (isIgnored() || !isReadable() || !allowOverwrite()) {
            return false;
        }

        field.setAccessible(true);
        String name = getFieldName();
        Object value;
        try {

            value = SerializationUtil.getJSONValue(jsonObject, name);
        } catch (JSONException e) {
            if (!isNullable()) {
                throw e;
            } else {
                return false;
            }
        }

        try {
            String fieldName = field.getName();
            fieldName = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
            Method setter;
            boolean wasSet = false;
            try {
                setter = object.getClass().getMethod("set" + fieldName, field.getType());
                setter.invoke(object, parse(value));
                wasSet = true;
            } catch (NoSuchMethodException e) {

            } catch (InvocationTargetException e) {
            }
            if (!wasSet) {
                field.set(object, parse(value));
            }

        } catch(IllegalArgumentException e) {
            throw new IllegalArgumentException("invalid value for field " + name + ". Type should be " +field.getType().getSimpleName() + " but was " + value.getClass().getSimpleName());
        }

        return true;
    }

}
