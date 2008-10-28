package org.hibernate.annotations;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

/**
 * Extends {@link javax.persistence.NamedNativeQueries} to hold hibernate NamedNativeQuery
 * objects
 *
 * @author Emmanuel Bernard
 */
@Target({TYPE, PACKAGE})
@Retention(RUNTIME)
public @interface NamedNativeQueries {
	NamedNativeQuery[] value();
}