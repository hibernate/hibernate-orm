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
	 * A cascade point that occurs just afterQuery the insertion of the parent entity and
	 * just beforeQuery deletion
	 */
	AFTER_INSERT_BEFORE_DELETE,

	/**
	 * A cascade point that occurs just beforeQuery the insertion of the parent entity and
	 * just afterQuery deletion
	 */
	BEFORE_INSERT_AFTER_DELETE,

	/**
	 * A cascade point that occurs just afterQuery the insertion of the parent entity and
	 * just beforeQuery deletion, inside a collection
	 */
	AFTER_INSERT_BEFORE_DELETE_VIA_COLLECTION,

	/**
	 * A cascade point that occurs just afterQuery update of the parent entity
	 */
	AFTER_UPDATE,

	/**
	 * A cascade point that occurs just beforeQuery the session is flushed
	 */
	BEFORE_FLUSH,

	/**
	 * A cascade point that occurs just afterQuery eviction of the parent entity from the
	 * session cache
	 */
	AFTER_EVICT,

	/**
	 * A cascade point that occurs just afterQuery locking a transient parent entity into the
	 * session cache
	 */
	BEFORE_REFRESH,

	/**
	 * A cascade point that occurs just afterQuery refreshing a parent entity
	 */
	AFTER_LOCK,

	/**
	 * A cascade point that occurs just beforeQuery merging from a transient parent entity into
	 * the object in the session cache
	 */
	BEFORE_MERGE
}
