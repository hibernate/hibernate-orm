/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
 *     <li>{@link DynamicInsert @DynamicInsert}, to let the database fill in
 *         the value of a null entity attribute, or
 *     <li>{@link Generated @Generated}, to populate an entity attribute with
 *         the defaulted value of a database column.
 * </ul>
 * <p>
 * For example, this field will be populated with the value {@code "new"} by
 * the database when the entity is inserted:
 * <pre>
 * &#064;Generated &#064;ColumnDefault(value = "'new'")
 * private String status;
 * </pre>
 * <p>
 * If {@code @Generated} is not used, a {@code default} value can cause state
 * held in memory to lose synchronization with the database.
 *
 * @author Steve Ebersole
 *
 * @see Generated
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
