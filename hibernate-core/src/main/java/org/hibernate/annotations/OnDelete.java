/*
 * SPDX-License-Identifier: Apache-2.0
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
 * <pre>
 * &#064;ManyToOne
 * &#064;OnDelete(action = CASCADE)
 * Parent parent;
 * </pre>
 * Note that this results in an {@code on delete cascade} clause in
 * the DDL definition of the foreign key. It's completely different
 * to {@link jakarta.persistence.CascadeType#REMOVE}.
 * <p>
 * In fact, {@code @OnDelete} may be combined with {@code cascade=REMOVE}.
 * <pre>
 * &#064;ManyToOne(cascade = REMOVE)
 * &#064;OnDelete(action = CASCADE)
 * Parent parent;
 * </pre>
 * <ul>
 * <li>If {@code @OnDelete(action = CASCADE)} is used in conjunction
 *     with {@code cascade=REMOVE}, then associated entities are fetched
 *     from the database, marked deleted in the persistence context,
 *     and evicted from the second-level cache.
 * <li>If {@code @OnDelete(action = CASCADE)} is used on its own,
 *     <em>without</em> {@code cascade=REMOVE}, then associated
 *     entities are not fetched from the database, are not marked
 *     deleted in the persistence context, and are not automatically
 *     evicted from the second-level cache.
 * </ul>
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
