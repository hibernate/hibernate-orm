package org.hibernate.annotations;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

/**
 * Extends {@link javax.persistence.NamedQueries} to hold hibernate NamedQuery
 * objects
 *
 * @author Emmanuel Bernard
 * @author Carlos González-Cadenas
 */
@Target({TYPE, PACKAGE})
@Retention(RUNTIME)
public @interface NamedQueries {
	NamedQuery[] value();
}