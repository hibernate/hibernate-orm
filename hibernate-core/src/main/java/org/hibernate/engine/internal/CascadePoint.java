/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.internal;

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
	 * A cascade point that occurs just after locking a transient parent entity into the
	 * session cache
	 */
	BEFORE_REFRESH,

	/**
	 * A cascade point that occurs just after refreshing a parent entity
	 */
	AFTER_LOCK,

	/**
	 * A cascade point that occurs just before merging from a transient parent entity into
	 * the object in the session cache
	 */
	BEFORE_MERGE
}
