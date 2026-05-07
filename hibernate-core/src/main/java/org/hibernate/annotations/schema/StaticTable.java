package org.hibernate.annotations.schema;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotates a generated annotation type
 * with the same name as a table.
 */
@Retention(RUNTIME)
@Target(ANNOTATION_TYPE)
@interface StaticTable {
	String name();
}
