// $Id:$
package org.hibernate.test.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.dialect.Dialect;

/**
 * Annotations used to mark a test to be specific to a given dialect.
 * 
 * @author Hardy Ferentschik
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresDialect {
	Class<? extends Dialect>[] value();
}
