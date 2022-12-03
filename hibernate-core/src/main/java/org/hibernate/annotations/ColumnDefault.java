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
 * Specifies that a column has a {@code default} value specified in DDL.
 * <p>
 * {@code @ColumnDefault} may be used in combination with:
 * <ul>
 *     <li>{@code DynamicInsert}, to let the database fill in the value of
 *         a null entity attribute, or
 *     <li>{@code @Generated(event=INSERT)}, to populate an entity attribute
 *         with the defaulted value of a database column.
 * </ul>
 * If {@link Generated} is not used, a {@code default} value can state held
 * in memory to lose synchronization with the database.
 *
 * @author Steve Ebersole
 *
 * @see GeneratedColumn
 * @see DialectOverride.ColumnDefault
 */
@Target( {FIELD, METHOD} )
@Retention( RUNTIME )
public @interface ColumnDefault {
	/**
	 * The {@code default} value to use in generated DDL.
	 *
	 * @return a SQL expression that evaluates to the default column value
	 */
	String value();
}
