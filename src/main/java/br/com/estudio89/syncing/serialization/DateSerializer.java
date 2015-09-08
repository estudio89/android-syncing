package br.com.estudio89.syncing.serialization;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.Date;

/**
 * Created by luccascorrea on 6/20/15.
 */
public class DateSerializer extends FieldSerializer<Date> {

    public DateSerializer(Field field, Object object, JSONObject jsonObject) {
        super(field, object, jsonObject);
    }

    protected String format(Date date) {
        if (date == null) {
            return null;
        }
        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
        return fmt.print(new DateTime(date));
    }

    protected Date parse(Object value) {
        if (JSONObject.NULL.equals(value)) {
            return new Date();
        }
        String strDate = (String) value;
        return SerializationUtil.parseServerDate(strDate);
    }

}
