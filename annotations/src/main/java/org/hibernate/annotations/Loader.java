package org.hibernate.annotations;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

/**
 * Loader Annotation for overwriting Hibernate default FIND method
 *
 * @author László Benke
 */
@Target( {TYPE, FIELD, METHOD} )
@Retention( RUNTIME )
public @interface Loader {
	/**
	 * namedQuery to use for loading
	 */
	String namedQuery() default "";
}
