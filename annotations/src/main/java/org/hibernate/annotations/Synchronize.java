package org.hibernate.annotations;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Ensures that auto-flush happens correctly and that queries against the derived 
 * entity do not return stale data.
 * 
 * Mostly used with Subselect.
 * 
 * @author Sharath Reddy
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface Synchronize {
	String [] value();
}
