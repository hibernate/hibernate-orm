package org.hibernate.annotations;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Map an immutable and read-only entity to a given SQL subselect expression:
 * @author Sharath Reddy
 *
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface Subselect {
	String value();
}
