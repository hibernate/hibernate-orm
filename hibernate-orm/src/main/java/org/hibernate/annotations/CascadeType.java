/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

/**
 * Cascade types (can override default JPA cascades).
 */
public enum CascadeType {
	/**
	 * Includes all types listed here.
	 */
	ALL,
	/**
	 * Corresponds to {@link javax.persistence.CascadeType#PERSIST}.
	 */
	PERSIST,
	/**
	 * Corresponds to {@link javax.persistence.CascadeType#MERGE}.
	 */
	MERGE,
	/**
	 * Corresponds to {@link javax.persistence.CascadeType#REMOVE}.
	 */
	REMOVE,
	/**
	 * Corresponds to {@link javax.persistence.CascadeType#REFRESH}.
	 */
	REFRESH,
	/**
	 * Corresponds to the Hibernate native DELETE action.
	 */
	DELETE,
	/**
	 * Corresponds to the Hibernate native SAVE_UPDATE (direct reattachment) action.
	 */
	SAVE_UPDATE,
	/**
	 * Corresponds to the Hibernate native REPLICATE action.
	 */
	REPLICATE,
	/**
	 * Hibernate originally handled orphan removal as a specialized cascade.
	 *
	 * @deprecated use @OneToOne(orphanRemoval=true) or @OneToMany(orphanRemoval=true)
	 */
	@Deprecated
	DELETE_ORPHAN,
	/**
	 * Corresponds to the Hibernate native LOCK action.
	 */
	LOCK,
	/**
	 * JPA originally planned on calling DETACH EVICT.
	 *
	 * @deprecated use {@link javax.persistence.CascadeType#DETACH}
	 */
	@Deprecated
	EVICT,
	/**
	 * Corresponds to {@link javax.persistence.CascadeType#DETACH}.
	 */
	DETACH
}
