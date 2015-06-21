package br.com.estudio89.syncing.serialization;

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
        if (annotation != null) {
            String name = annotation.name();
            if (!"".equals(name)) {
                return name;
            } else {
                return field.getName();
            }
        }

        return field.getName();
    }

    protected boolean isIgnored() {
        if (annotation == null) {
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
        return (FieldClass) value;
    }

    public boolean updateJSON() throws IllegalAccessException, JSONException {
        if (isIgnored() || !isWritable()) {
            return false;
        }
        field.setAccessible(true);
        FieldClass value = (FieldClass) field.get(object);
        String name = getFieldName();
        jsonObject.put(name, format(value));

        return true;
    }

    public boolean updateField() throws JSONException, IllegalAccessException {
        if (isIgnored() || !isReadable()) {
            return false;
        }

        field.setAccessible(true);
        String name = getFieldName();
        Object value = jsonObject.get(name);
        field.set(object, parse(value));

        return true;
    }
}
