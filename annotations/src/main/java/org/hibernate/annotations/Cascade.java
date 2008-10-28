package org.hibernate.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

/**
 * Apply a cascade strategy on an association
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface Cascade {
	CascadeType[] value();
}
