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
 * Custom SQL statement for delete of an entity/collection.
 *
 * @author L�szl� Benke
 */
@Target( {TYPE, FIELD, METHOD} )
@Retention( RUNTIME )
public @interface SQLDelete {
	/**
	 * Procedure name or SQL DELETE statement.
	 */
	String sql();

	/**
	 * Is the statement callable (aka a {@link java.sql.CallableStatement})?
	 */
	boolean callable() default false;

	/**
	 * For persistence operation what style of determining results (success/failure) is to be used.
	 */
	ResultCheckStyle check() default ResultCheckStyle.NONE;
}
