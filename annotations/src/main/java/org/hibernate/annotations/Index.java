package org.hibernate.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

/**
 * Define a DB index
 *
 * @author Emmanuel Bernard
 */
@Target({FIELD, METHOD})
@Retention(RUNTIME)
public @interface Index {
	String name();

	String[] columnNames() default {};
}
