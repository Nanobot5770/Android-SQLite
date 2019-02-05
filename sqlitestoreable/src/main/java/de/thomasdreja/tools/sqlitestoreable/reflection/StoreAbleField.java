package de.thomasdreja.tools.sqlitestoreable.reflection;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface StoreAbleField {
    String fieldName() default "";
}
