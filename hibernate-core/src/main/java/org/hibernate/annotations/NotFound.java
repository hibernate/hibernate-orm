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
 * Indicates that a {@linkplain jakarta.persistence.ManyToOne many to one},
 * {@linkplain jakarta.persistence.OneToOne one to one}, or
 * {@linkplain jakarta.persistence.ManyToMany many to many} association
 * maps to a column holding foreign keys, but without a foreign key
 * constraint, and which may therefore violate referential integrity.
 * <p>
 * The {@link #action} specifies how Hibernate should handle violations of
 * referential integrity, that is, the case of an orphaned foreign key with
 * no associated row in the referenced table:
 * <ul>
 * <li>{@link NotFoundAction#EXCEPTION} specifies that this situation
 *     should be treated as an error, causing a
 *     {@link org.hibernate.exception.ConstraintViolationException}, and
 * <li>{@link NotFoundAction#IGNORE} specifies that this situation should
 *     be tolerated and treated as if the foreign key were null.
 * </ul>
 * <p>
 * Note that this annotation has the side effect of making a to-one
 * association non-lazy. It does not affect the laziness of a many-to-many
 * association.
 * <p>
 * This annotation implies
 * {@link jakarta.persistence.ConstraintMode#NO_CONSTRAINT} for the purposes
 * of DDL generation. That is, no foreign key constraint will be included
 * in the generated DDL.
 *
 * @author Emmanuel Bernard
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface NotFound {
	/**
	 * Specifies how to handle the case of an orphaned foreign key.
	 * <p>
	 * By default, an {@linkplain NotFoundAction#EXCEPTION exception is thrown}.
	 */
	NotFoundAction action() default NotFoundAction.EXCEPTION;
}
