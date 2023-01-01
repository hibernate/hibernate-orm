/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
 *
 * @author Emmanuel Bernard
 *
 * @see DialectOverride.Check
 */
@Target({TYPE, METHOD, FIELD})
@Retention(RUNTIME)
@Repeatable(Checks.class)
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
