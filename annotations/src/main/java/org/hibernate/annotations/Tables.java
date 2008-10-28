package org.hibernate.annotations;

import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

/**
 * Plural of Table
 *
 * @author Emmanuel Bernard
 * @see Table
 */
@Target({TYPE})
@Retention(RUNTIME)
public @interface Tables {
	Table[] value();
}
