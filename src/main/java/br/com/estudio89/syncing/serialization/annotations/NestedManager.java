package br.com.estudio89.syncing.serialization.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by luccascorrea on 6/21/15.
 *
 * manager: SyncManager class for this child field.
 * writable: boolean indicating if this field should be written to json when serializing its parent.
 * accessorMethod: optional String indicating which method used to retrieve the child items. If not given, the accessor
 *                 method used will be "get" + childFieldName. You should only worry about this if writable is true.
 * discardOnSave: boolean indicating if the child items should all be removed before saving new items.
 * paginationParams: when the nested manager paginates, this should be the name of the field in the json object
 *                 that represents the pagination parameter for the child.
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface NestedManager {
    Class manager();
    boolean writable() default false;
    String accessorMethod() default ""; // only used if writable is true. Optional.
    boolean discardOnSave() default false;
    String paginationParams() default "";
}
