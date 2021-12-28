/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import org.hibernate.tuple.DefaultValueGeneration;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies that a column has a {@code DEFAULT} value specified in DDL.
 *
 * @author Steve Ebersole
 *
 * @see ColumnGeneratedAlways
 */
@Target( {FIELD, METHOD} )
@Retention( RUNTIME )
@ValueGenerationType(generatedBy = DefaultValueGeneration.class)
public @interface ColumnDefault {
	/**
	 * The {@code DEFAULT} value to use in generated DDL.
	 *
	 * @return a SQL expression that evaluates to the default column value
	 */
	String value();

	/**
	 * Determines if the defaulted value is selected after every SQL {@code INSERT}.
	 *
	 * @return {@code true} if a {@code SELECT} should be executed to read the defaulted value
	 */
	boolean selectDefaulted() default true;
}
