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
 * Specifes that the mapped column is defined using a DDL
 * {@code generated always as} clause, or equivalent.
 *
 * @author Gavin King
 */
@Target( {FIELD, METHOD} )
@Retention( RUNTIME )
public @interface ColumnGeneratedAlways {
	/**
	 * The SQL expression used to generate the column value.
	 */
	String value();
}
