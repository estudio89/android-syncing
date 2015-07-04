package br.com.estudio89.syncing.serialization.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by luccascorrea on 6/21/15.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface NestedManager {
    Class manager();
    boolean writable() default false;
    String accessorMethod() default ""; // only necessary if writable is true
}
