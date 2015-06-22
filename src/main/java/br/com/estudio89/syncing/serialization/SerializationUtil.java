package br.com.estudio89.syncing.serialization;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.lang.reflect.Field;
import java.util.Date;

/**
 * Created by luccascorrea on 6/21/15.
 */
public class SerializationUtil {

    public static String getFieldName(Field field) {
        JSON annotation = field.getAnnotation(JSON.class);
        return getFieldName(field, annotation);
    }

    public static String getFieldName(Field field, JSON annotation) {
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

    public static Date parseServerDate(String strDate){
        DateTimeFormatter parser = ISODateTimeFormat.dateTimeParser();
        DateTime date = parser.parseDateTime(strDate);
        return date.toDate();
    }
}
