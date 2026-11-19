package org.hibernate.processor.test.compositevalue;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.annotations.CompositeType;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target({FIELD, METHOD})
@CompositeType(DateTimeAndZoneType.class)
public @interface ZonedDateTimeType {
}
