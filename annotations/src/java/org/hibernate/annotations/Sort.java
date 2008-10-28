//$Id$
package org.hibernate.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

/**
 * Collection sort
 * (Java level sorting)
 *
 * @author Emmanuel Bernard
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface Sort {
	/**
	 * sort type
	 */
	SortType type() default SortType.UNSORTED;
	/**
	 * Sort comparator implementation
	 */
	//TODO find a way to use Class<Comparator>

	Class comparator() default void.class;
}
