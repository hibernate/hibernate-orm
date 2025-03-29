/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.Incubating;
import org.hibernate.dialect.Dialect;

import jakarta.persistence.AttributeConverter;
import org.hibernate.metamodel.UnsupportedMappingException;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Describes a soft-delete indicator mapping.
 * <p/>
 * Soft deletes handle "deletions" from a database table by setting a column in
 * the table to indicate deletion.
 * <p/>
 * May be defined at various levels<ul>
 *     <li>
 *         {@linkplain ElementType#PACKAGE PACKAGE}, where it applies to all
 *         mappings defined in the package, unless defined more specifically.
 *     </li>
 *     <li>
 *         {@linkplain ElementType#TYPE TYPE}, where it applies to an entity hierarchy.
 *         The annotation must be defined on the root of the hierarchy and affects to the
 *         hierarchy as a whole. The soft-delete column is assumed to be on the hierarchy's
 *         root table.
 *     </li>
 *     <li>
 *         {@linkplain ElementType#FIELD FIELD} / {@linkplain ElementType#METHOD METHOD}, where
 *         it applies to the rows of an {@link jakarta.persistence.ElementCollection} or
 *         {@link jakarta.persistence.ManyToMany} table.
 *     </li>
 *     <li>
 *         {@linkplain ElementType#ANNOTATION_TYPE ANNOTATION_TYPE} on another annotation
 *         defined according to the previous targets.
 *     </li>
 * </ul>
 *
 * @since 6.4
 * @author Steve Ebersole
 */
@Target({PACKAGE, TYPE, FIELD, METHOD, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Documented
@Incubating
public @interface SoftDelete {
	/**
	 * (Optional) The name of the column.
	 * <p/>
	 * Default depends on the {@linkplain #strategy() strategy} being used.
	 *
	 * @see SoftDeleteType#getDefaultColumnName()
	 */
	String columnName() default "";

	/**
	 * (Optional) The SQL fragment that is used when
	 * 	generating the DDL for the column.
	 * <p>
	 * The DDL must be written in the native SQL dialect
	 * of the target database (it is not portable across databases).
	 *
	 * @since 7.0
	 */
	String options() default "";

	/**
	 * (Optional) A comment to be applied to the column.
	 *
	 * @since 7.0
	 */
	String comment() default "";

	/**
	 * The strategy to use for storing/reading values to/from the database.
	 * <p/>
	 * The strategy also affects the default {@linkplain #columnName() column name} - see
	 * {@linkplain SoftDeleteType#getDefaultColumnName}.
	 */
	SoftDeleteType strategy() default SoftDeleteType.DELETED;

	/**
	 * (Optional) Conversion to apply to determine the appropriate value to
	 * store in the database.  The "domain representation" can be: <dl>
	 *     <dt>{@code true}</dt>
	 *     <dd>Indicates that the row is considered deleted</dd>
	 *
	 *     <dt>{@code false}</dt>
	 *     <dd>Indicates that the row is considered NOT deleted</dd>
	 * </dl>
	 * <p/>
	 * By default, values are stored as booleans in the database according to
	 * the {@linkplain Dialect#getPreferredSqlTypeCodeForBoolean() dialect}
	 * and {@linkplain org.hibernate.cfg.MappingSettings#PREFERRED_BOOLEAN_JDBC_TYPE settings}
	 *
	 * @apiNote Only valid when {@linkplain #strategy} is {@linkplain SoftDeleteType#DELETED}
	 * or {@linkplain SoftDeleteType#ACTIVE}.  Will lead to a {@linkplain UnsupportedMappingException}
	 * when combined with {@linkplain SoftDeleteType#TIMESTAMP}.
	 *
	 * @implSpec The specified converter should never return {@code null}
	 */
	Class<? extends AttributeConverter<Boolean,?>> converter() default UnspecifiedConversion.class;

	/**
	 * Used as the default for {@linkplain SoftDelete#converter()}, indicating that
	 * {@linkplain Dialect#getPreferredSqlTypeCodeForBoolean() dialect} and
	 * {@linkplain org.hibernate.cfg.MappingSettings#PREFERRED_BOOLEAN_JDBC_TYPE settings}
	 * resolution should be used.
	 */
	interface UnspecifiedConversion extends AttributeConverter<Boolean,Object> {}
}
