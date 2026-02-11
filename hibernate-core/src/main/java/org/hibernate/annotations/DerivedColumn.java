/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import org.hibernate.Incubating;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Adds a derived column to the generated DDL for the
 * primary table of the annotated entity, or to the
 * join or collection table of the annotated association
 * or collection.
 * <p>
 * Unlike {@link GeneratedColumn}, this annotation only
 * affects DDL generation, and does not imply a mapping
 * of any entity attribute to the derived column. If an
 * attribute of an entity maps to a derived column, its
 * {@linkplain jakarta.persistence.Column column mapping}
 * should explicitly specify {@code insertable=false
 * updatable=false}, or {@link GeneratedColumn} should be
 * used instead.
 *
 * @see GeneratedColumn
 *
 * @since 7.4
 * @author Gavin King
 */
@Target( {TYPE, FIELD, METHOD} )
@Retention( RUNTIME )
@Incubating
public @interface DerivedColumn {
	/**
	 * The name of the derived column.
	 */
	String name();

	/**
	 * The {@linkplain org.hibernate.type.SqlTypes SQL
	 * type code} of the type of the derived column.
	 */
	int sqlType();

	/**
	 * The expression to include in the generated DDL.
	 *
	 * @return the SQL expression that is evaluated to
	 *         generate the column value.
	 */
	String value();

	/**
	 * Is this derived column stored? Derived columns
	 * are stored columns by default. {@code stored=false}
	 * specifies that the column is virtual when the
	 * database supports virtual columns.
	 */
	boolean stored() default true;

	/**
	 * Is this derived column hidden? Derived columns
	 * are unhidden by default. {@code hidden=true}
	 * specifies that the column is hidden when the
	 * database supports hidden columns.
	 */
	boolean hidden() default false;
}
