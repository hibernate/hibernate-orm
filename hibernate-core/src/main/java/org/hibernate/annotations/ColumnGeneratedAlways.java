/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import org.hibernate.tuple.GeneratedAlwaysValueGeneration;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies that a is defined using a DDL {@code generated always as} clause,
 * or equivalent.
 *
 * @author Gavin King
 *
 * @see ColumnDefault
 */
@Target( {FIELD, METHOD} )
@Retention( RUNTIME )
@ValueGenerationType(generatedBy = GeneratedAlwaysValueGeneration.class)
public @interface ColumnGeneratedAlways {
	/**
	 * The expression to include in the generated DDL.
	 *
	 * @return the SQL expression that is evaluated to generate the column value.
	 */
	String value();

	/**
	 * Determines if the generated value is selected after every SQL {@code INSERT} or {@code UPDATE}.
	 *
	 * @return {@code true} if a {@code SELECT} should be executed to read the generated value
	 */
	boolean selectGenerated() default true;
}
