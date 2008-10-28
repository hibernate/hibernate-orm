package org.hibernate.annotations;

import static java.lang.annotation.ElementType.*;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

/**
 * Where clause to add to the element Entity or target entity of a collection
 * The clause is written in SQL
 *
 * @author Emmanuel Bernard
 */
@Target({TYPE, METHOD, FIELD})
@Retention(RUNTIME)
public @interface Where {
	String clause();
}
