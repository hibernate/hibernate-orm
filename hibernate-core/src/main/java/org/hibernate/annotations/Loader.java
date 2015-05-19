/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Used to override how Hibernate performs load operations. naming a named query to use instead of
 * its generated SELECT SQL.
 *
 * @author L�szl� Benke
 */
@Target( {TYPE, FIELD, METHOD} )
@Retention( RUNTIME )
public @interface Loader {
	/**
	 * THe namedQuery to use for loading.
	 */
	String namedQuery() default "";
}
