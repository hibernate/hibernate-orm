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
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Describe aliases for filters.
 *
 * @author Rob Worsnop
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface SqlFragmentAlias {
	/**
	 * The alias within the fragment.
	 */
	String alias();

	/**
	 * The table corresponding to the alias.
	 */
	String table() default "";

	/**
	 * The entity class associated with the alias.
	 */
	Class entity() default void.class;
}
