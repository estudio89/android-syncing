package br.com.estudio89.syncing.serialization;

import br.com.estudio89.syncing.serialization.annotations.JSON;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by luccascorrea on 6/21/15.
 *
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

    /** Transform Calendar to ISO 8601 string. */
    public static String formatServerDate(final Date date) {
        String formatted = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSZ")
                .format(date);
        return formatted.substring(0, 28) + ":" + formatted.substring(28);
    }

    /** Transform ISO 8601 string to Calendar. */
    public static Date parseServerDate(final String iso8601string) {
        String s = iso8601string.replace("Z", "+00:00");
        String format = "yyyy-MM-dd'T'HH:mm:ssZ";
        if (!s.contains(".")) {
            // Date only, no microseconds

            try {
                s = s.substring(0, 22) + s.substring(23);  // to get rid of the ":"
            } catch (IndexOutOfBoundsException e) {
                throw new RuntimeException(e);
            }
        } else {
            // Date and time

            try {
                s = s.substring(0, 29) + s.substring(30);  // to get rid of the ":"
                s = s.substring(0, 23) + s.substring(26);  // to get rid of microseconds
                format = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
            } catch (IndexOutOfBoundsException e) {
                throw new RuntimeException(e);
            }
        }
        Date date = null;
        try {
            date = new SimpleDateFormat(format).parse(s);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return date;
    }
}
