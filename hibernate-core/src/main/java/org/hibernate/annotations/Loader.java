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
 * Specifies that a named query should be used to load an entity or
 * collection, overriding the SQL that Hibernate generates by default.
 *
 * @author László Benke
 *
 * @deprecated Use {@link SQLSelect} or {@link HQLSelect}.
 */
@Target( {TYPE, FIELD, METHOD} )
@Retention( RUNTIME )
@Deprecated(since = "6.2")
public @interface Loader {
	/**
	 * The named query to use for loading the entity or collection.
	 */
	String namedQuery() default "";
}
