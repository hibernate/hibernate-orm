/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import org.hibernate.Internal;
import org.hibernate.LockMode;
import org.hibernate.ReplicationMode;

/**
 * Enumerates the persistence operations which may be cascaded from
 * one entity instance to associated entity instances.
 * <p>
 * This enumeration of cascade types competes with the JPA-defined
 * enumeration {@link jakarta.persistence.CascadeType}, but offers
 * additional options, including {@link #LOCK}.
 * <p>
 * To enable cascade {@code LOCK}, use {@link Cascade @Cascade}, for
 * example:
 * <pre>
 * &#64;OneToMany(mappedBy="parent")
 * &#64;Cascade({PERSIST,REFRESH,REMOVE,LOCK})
 * Set&lt;Child&gt; children;
 * </pre>
 *
 * @see Cascade
 *
 * @deprecated Use the JPA-defined
 *             {@link jakarta.persistence.CascadeType}.
 *             This enumeration will be removed to alleviate the
 *             duplication in naming.
 */
@Deprecated(since = "7", forRemoval = true)
public enum CascadeType {
	/**
	 * Includes all types listed here.
	 * Equivalent to {@link jakarta.persistence.CascadeType#ALL}.
	 */
	ALL,

	/**
	 * Equivalent to {@link jakarta.persistence.CascadeType#PERSIST}.
	 *
	 * @see jakarta.persistence.EntityManager#persist(Object)
	 */
	PERSIST,

	/**
	 * Equivalent to {@link jakarta.persistence.CascadeType#MERGE}.
	 *
	 * @see jakarta.persistence.EntityManager#merge(Object)
	 */
	MERGE,

	/**
	 * Equivalent to {@link jakarta.persistence.CascadeType#REMOVE}.
	 *
	 * @see jakarta.persistence.EntityManager#remove(Object)
	 */
	REMOVE,

	/**
	 * Equivalent to {@link jakarta.persistence.CascadeType#REFRESH}.
	 *
	 * @see jakarta.persistence.EntityManager#refresh(Object)
	 */
	REFRESH,

	/**
	 * Equivalent to {@link jakarta.persistence.CascadeType#DETACH}.
	 *
	 * @see jakarta.persistence.EntityManager#detach(Object)
	 */
	DETACH,

	/**
	 * A cascade type for the {@code lock()} operation.
	 * <p>
	 * This cascade type has no equivalent in JPA.
	 *
	 * @see org.hibernate.Session#lock(Object, LockMode)
	 */
	LOCK,

	/**
	 * A cascade type for the {@code replicate()} operation.
	 *
	 * @see org.hibernate.Session#replicate(Object, ReplicationMode)
	 *
	 * @deprecated since {@link org.hibernate.Session#replicate(Object, ReplicationMode)}
	 *             is deprecated
	 */
	@Deprecated
	REPLICATE,

	/**
	 * Ancient versions of Hibernate treated orphan removal as a
	 * specialized type of cascade. But since JPA 1.0, orphan removal is
	 * considered a completely separate setting, and may be enabled by
	 * annotating a one-to-one or one-to-many association
	 * {@link OneToOne#orphanRemoval() @OneToOne(orphanRemoval=true)} or
	 * {@link OneToMany#orphanRemoval() @OneToMany(orphanRemoval=true)}.
	 *
	 * @apiNote This is now valid only for internal usage.
	 */
	@Internal
	DELETE_ORPHAN
}
