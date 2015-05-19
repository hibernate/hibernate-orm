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
 * Describe an index column of a List.
 *
 * @author Matthew Inger
 *
 * @deprecated Prefer the standard JPA {@link javax.persistence.OrderColumn} annotation and the Hibernate specific
 * {@link ListIndexBase} (for replacing {@link #base()}).
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
@Deprecated
public @interface IndexColumn {
	/**
	 * The column name.
	 */
	String name();

	/**
	 * The starting index value.  Zero (0) by default, since Lists indexes start at zero (0).
	 */
	int base() default 0;

	/**
	 * Is the column nullable?
	 */
	boolean nullable() default true;

	/**
	 * An explicit column definition.
	 */
	String columnDefinition() default "";
}
