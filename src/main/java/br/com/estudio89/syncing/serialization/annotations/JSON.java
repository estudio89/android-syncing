package br.com.estudio89.syncing.serialization.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by luccascorrea on 6/20/15.
 *
 * The attribute writable, if false, means the attribute will not be written to the json object
 * when serializing the Model object.
 *
 * The attribute readable, if false, means the attribute will not be read from the json object
 * when deserializing the object.
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JSON {
    String ignoreIf() default noValue;
    boolean ignore() default false;
    boolean writable() default true;
    boolean readable() default true;
    boolean allowOverwrite() default true;
    String name() default "";

    public static final String noValue = "[no_value]";

}
