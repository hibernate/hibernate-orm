/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies an {@code on delete} action for a foreign key constraint.
 * The most common usage is {@code @OnDelete(action = CASCADE)}.
 * Note that this results in an {@code on delete cascade} clause in
 * the DDL definition of the foreign key. It's completely different
 * to {@link jakarta.persistence.CascadeType#REMOVE}.
 * <p>
 * Like database triggers, {@code on delete} actions can cause state
 * held in memory to lose synchronization with the database.
 *
 * @author Emmanuel Bernard
 */
@Target({METHOD, FIELD, TYPE})
@Retention(RUNTIME)
public @interface OnDelete {
	/**
	 * The action to taken by the database when deletion of a row
	 * would cause the constraint to be violated.
	 */
	OnDeleteAction action();
}
