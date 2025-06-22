/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

/**
 * Enumerates the possible optimistic lock checking strategies.
 *
 * @see OptimisticLocking
 *
 * @author Emmanuel Bernard
 */
public enum OptimisticLockType {
	/**
	 * No optimistic locking.
	 */
	NONE,
	/**
	 * Optimistic locking using a dedicated timestamp column or
	 * {@linkplain jakarta.persistence.Version version column}.
	 * This is the usual strategy.
	 * <p>
	 * Any SQL {@code update} or {@code delete} statement will
	 * have a {@code where} clause restriction which specifies
	 * the primary key and current version. If no rows are
	 * updated, this is interpreted as a lock checking failure.
	 *
	 * @see jakarta.persistence.Version
	 */
	VERSION,
	/**
	 * Optimistic locking based on <em>dirty</em> fields of the
	 * entity.
	 * <p>
	 * A SQL {@code update} or {@code delete} statement will
	 * have every dirty field of the entity instance listed in
	 * the {@code where} clause restriction.
	 */
	DIRTY,
	/**
	 * Optimistic locking based on <em>all</em> fields of the
	 * entity.
	 * <p>
	 * A SQL {@code update} or {@code delete} statement will
	 * have every field of the entity listed in the {@code where}
	 * clause restriction.
	 */
	ALL
}
