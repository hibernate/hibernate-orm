/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.Incubating;
import org.hibernate.dialect.Dialect;

import jakarta.persistence.AttributeConverter;

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
	 * Default depends on {@linkplain #trackActive()} - {@code deleted} if {@code false} and
	 * {@code active} if {@code true}.
	 */
	String columnName() default "";

	/**
	 * Whether the database value indicates active/inactive, as opposed to the
	 * default of tracking deleted/not-deleted
	 * <p/>
	 * By default, the database values are interpreted as <ul>
	 *     <li>{@code true} means the row is considered deleted</li>
	 *     <li>{@code false} means the row is considered NOT deleted</li>
	 * </ul>
	 * <p/>
	 * Setting this {@code true} reverses the interpretation of the database value <ul>
	 *     <li>{@code true} means the row is active (NOT deleted)</li>
	 *     <li>{@code false} means the row is inactive (deleted)</li>
	 * </ul>
	 *
	 * @implNote Causes the {@linkplain #converter() conversion} to be wrapped in
	 * a negated conversion.
	 */
	boolean trackActive() default false;

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
	 * @apiNote The converter should never return {@code null}
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
