package org.hibernate.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

/**
 * Describe an index column of a List
 *
 * @author Matthew Inger
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface IndexColumn {
	/** column name */
	String name();
	/** index in DB start from base */
	int base() default 0;
	/** is the index nullable */
	boolean nullable() default true;
	/** column definition, default to an appropriate integer */
	String columnDefinition() default "";
}
