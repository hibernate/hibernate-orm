/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
