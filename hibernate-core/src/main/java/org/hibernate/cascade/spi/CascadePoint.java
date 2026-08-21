/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cascade.spi;

/**
 * Describes the point at which a cascade is occurring
 *
 * @author Steve Ebersole
 */
public enum CascadePoint {
	/**
	 * A cascade point that occurs just after the insertion of the parent entity and
	 * just before deletion
	 */
	AFTER_INSERT_BEFORE_DELETE,

	/**
	 * A cascade point that occurs just before the insertion of the parent entity and
	 * just after deletion
	 */
	BEFORE_INSERT_AFTER_DELETE,

	/**
	 * A cascade point that occurs just after the insertion of the parent entity and
	 * just before deletion, inside a collection
	 */
	AFTER_INSERT_BEFORE_DELETE_VIA_COLLECTION,

	/**
	 * A cascade point that occurs just after update of the parent entity
	 */
	AFTER_UPDATE,

	/**
	 * A cascade point that occurs just before the session is flushed
	 */
	BEFORE_FLUSH,

	/**
	 * A cascade point that occurs just after eviction of the parent entity from the
	 * session cache
	 */
	AFTER_EVICT,

	/**
	 * A cascade point that occurs just before refreshing a parent entity
	 */
	BEFORE_REFRESH,

	/**
	 * A cascade point that occurs just before merging from a transient parent entity into
	 * the object in the session cache
	 */
	BEFORE_MERGE
}
