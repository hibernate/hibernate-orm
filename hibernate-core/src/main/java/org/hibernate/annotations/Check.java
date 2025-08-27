/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies a {@code check} constraint to be included in the generated DDL.
 * <ul>
 * <li>When a basic-typed field or property is annotated, the check constraint is
 *     added to the {@linkplain jakarta.persistence.Column column} definition.
 * <li>When a {@link jakarta.persistence.ManyToOne} association is annotated, the
 *     check constraint is added to the {@linkplain jakarta.persistence.JoinColumn
 *     join column} definition.
 * <li>When an owned collection is annotated, the check constraint is added to the
 *     {@linkplain jakarta.persistence.CollectionTable collection table} or
 *     {@linkplain jakarta.persistence.JoinTable association join table}.
 * <li>When an entity class is annotated, the check constraint is added to either
 *     the {@linkplain jakarta.persistence.Table primary table} or to a
 *     {@linkplain jakarta.persistence.SecondaryTable secondary table}, depending
 *     on which columns are involved in the constraint expression specified by
 *     {@link #constraints()}.
 * </ul>
 * <p>
 * For an entity with {@linkplain jakarta.persistence.SecondaryTable secondary tables},
 * a check constraint may involve columns of the primary table, or columns of any one
 * of the secondary tables. But it may not involve columns of more than one table.
 * <p>
 * An entity may have multiple {@code @Check} annotations, each defining a different
 * constraint.
 *
 * @author Emmanuel Bernard
 * @author Gavin King
 *
 * @see DialectOverride.Check
 *
 * @deprecated Prefer {@link jakarta.persistence.Table#check},
 *             {@link jakarta.persistence.Column#check}, etc.,
 *             with {@link jakarta.persistence.CheckConstraint @CheckConstraint}.
 */
@Target({TYPE, METHOD, FIELD})
@Retention(RUNTIME)
@Repeatable(Checks.class)
@Deprecated(since = "7")
public @interface Check {
	/**
	 * The optional name of the check constraint.
	 */
	String name() default "";
	/**
	 * The check constraint, written in native SQL.
	 */
	String constraints();
}
