/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.binding;

import java.util.HashMap;
import java.util.Map;


/**
 * @author Hardy Ferentschik
 */
public enum CascadeType {
	/**
	 * Cascades save, delete, update, evict, lock, replicate, merge, persist
	 */
	ALL,

	/**
	 * Cascades save, delete, update, evict, lock, replicate, merge, persist + delete orphans
	 */
	ALL_DELETE_ORPHAN,

	/**
	 * Cascades save and update
	 */
	UPDATE,

	/**
	 * Cascades persist
	 */
	PERSIST,

	/**
	 * Cascades merge
	 */
	MERGE,

	/**
	 * Cascades lock
	 */
	LOCK,

	/**
	 * Cascades refresh
	 */
	REFRESH,

	/**
	 * Cascades replicate
	 */
	REPLICATE,

	/**
	 * Cascades evict
	 */
	EVICT,

	/**
	 * Cascade delete
	 */
	DELETE,

	/**
	 * Cascade delete + delete orphans
	 */
	DELETE_ORPHAN,

	/**
	 * No cascading
	 */
	NONE;

	private static final Map<String, CascadeType> hbmOptionToEnum = new HashMap<String, CascadeType>();

	static {
		hbmOptionToEnum.put( "all", ALL );
		hbmOptionToEnum.put( "all-delete-orphan", ALL_DELETE_ORPHAN );
		hbmOptionToEnum.put( "save-update", UPDATE );
		hbmOptionToEnum.put( "persist", PERSIST );
		hbmOptionToEnum.put( "merge", MERGE );
		hbmOptionToEnum.put( "lock", LOCK );
		hbmOptionToEnum.put( "refresh", REFRESH );
		hbmOptionToEnum.put( "replicate", REPLICATE );
		hbmOptionToEnum.put( "evict", EVICT );
		hbmOptionToEnum.put( "delete", DELETE );
		hbmOptionToEnum.put( "remove", DELETE ); // adds remove as a sort-of alias for delete...
		hbmOptionToEnum.put( "delete-orphan", DELETE_ORPHAN );
		hbmOptionToEnum.put( "none", NONE );
	}

	/**
	 * @param hbmOptionName the cascading option as specified in the hbm mapping file
	 *
	 * @return Returns the {@code CascadeType} for a given hbm cascading option
	 */
	public static CascadeType getCascadeType(String hbmOptionName) {
		return hbmOptionToEnum.get( hbmOptionName );
	}
}
