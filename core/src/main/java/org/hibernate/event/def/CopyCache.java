//$Id: $
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.event.def;

import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.Collection;

import org.hibernate.util.IdentityMap;
import org.hibernate.AssertionFailure;

/**
 * CopyCache is intended to be the Map implementation used by
 * {@link DefaultMergeEventListener} to keep track of entities and their copies
 * being merged into the session. This implementation also tracks whether a
 * an entity in the CopyCache is included in the merge. This allows a
 * an entity and its copy to be added to a CopyCache before merge has cascaded
 * to that entity.
 *
 * @author Gail Badner
 */
class CopyCache implements Map {
	private Map entityToCopyMap = IdentityMap.instantiate(10);
		// key is an entity involved with the merge;
		// value can be either a copy of the entity or the entity itself

	private Map entityToIncludeInMergeFlagMap = IdentityMap.instantiate(10);
	    // key is an entity involved with the merge;
	    // value is a flag indicating if the entity is included in the merge

	/**
	 * Clears the CopyCache.
	 */
	public void clear() {
		entityToCopyMap.clear();
		entityToIncludeInMergeFlagMap.clear();
	}

	/**
	 * Returns true if this CopyCache contains a mapping for the specified entity.
	 * @param entity must be non-null
	 * @return true if this CopyCache contains a mapping for the specified entity
	 * @throws NullPointerException if entity is null
	 */
	public boolean containsKey(Object entity) {
		if ( entity == null ) {
			throw new NullPointerException( "null entities are not supported by " + getClass().getName() );
		}
		return entityToCopyMap.containsKey( entity );
	}

	/**
	 * Returns true if this CopyCache maps one or more entities to the specified copy.
	 * @param copy must be non-null
	 * @return true if this CopyCache maps one or more entities to the specified copy
	 * @throws NullPointerException if copy is null
	 */
	public boolean containsValue(Object copy) {
		if ( copy == null ) {
			throw new NullPointerException( "null copies are not supported by " + getClass().getName() );
		}
		return entityToCopyMap.containsValue( copy );
	}

	/**
	 * Returns a set view of the entity-to-copy mappings contained in this CopyCache.
	 * @return set view of the entity-to-copy mappings contained in this CopyCache
	 */
	public Set entrySet() {
		return entityToCopyMap.entrySet();
	}

	/**
	 * Returns the copy to which this CopyCache maps the specified entity.
	 * @param entity must be non-null
	 * @return the copy to which this CopyCache maps the specified entity
	 * @throws NullPointerException if entity is null
	 */
	public Object get(Object entity) {
		if ( entity == null ) {
			throw new NullPointerException( "null entities are not supported by " + getClass().getName() );
		}
		return entityToCopyMap.get( entity );
	}

	/**
	 * Returns true if this CopyCache contains no entity-copy mappings.
	 * @return true if this CopyCache contains no entity-copy mappings
	 */
	public boolean isEmpty() {
		return entityToCopyMap.isEmpty();
	}

	/**
	 * Returns a set view of the entities contained in this CopyCache
	 * @return a set view of the entities contained in this CopyCache
	 */
	public Set keySet() {
		return entityToCopyMap.keySet();
	}

	/**
	 * Associates the specified entity with the specified copy in this CopyCache;
	 * @param entity must be non-null
	 * @param copy must be non- null
	 * @return previous copy associated with specified entity, or null if
	 * there was no mapping for entity.
	 * @throws NullPointerException if entity or copy is null
	 */
	public Object put(Object entity, Object copy) {
		if ( entity == null || copy == null ) {
			throw new NullPointerException( "null entities and copies are not supported by " + getClass().getName() );
		}
		entityToIncludeInMergeFlagMap.put( entity, Boolean.FALSE );
		return entityToCopyMap.put( entity, copy );
	}

	/**
	 * Associates the specified entity with the specified copy in this CopyCache;
	 * @param entity must be non-null
	 * @param copy must be non- null
	 * @param isIncludedInMerge indicates if the entity is included in merge
	 *
	 * @return previous copy associated with specified entity, or null if
	 * there was no mapping for entity.
	 * @throws NullPointerException if entity or copy is null
	 */
	/* package-private */ Object put(Object entity, Object copy, boolean isIncludedInMerge) {
		if ( entity == null || copy == null ) {
			throw new NullPointerException( "null entities and copies are not supported by " + getClass().getName() );
		}
		entityToIncludeInMergeFlagMap.put( entity, Boolean.valueOf( isIncludedInMerge ) );
		return entityToCopyMap.put( entity, copy );
	}

	/**
	 * Copies all of the mappings from the specified map to this CopyCache
	 * @param map keys and values must be non-null
	 * @throws NullPointerException if any map keys or values are null
	 */
	public void putAll(Map map) {
		for ( Iterator it=map.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry entry = ( Map.Entry ) it.next();
			if ( entry.getKey() == null || entry.getValue() == null ) {
				throw new NullPointerException( "null entities and copies are not supported by " + getClass().getName() );
			}
			entityToCopyMap.put( entry.getKey(), entry.getValue() );
			entityToIncludeInMergeFlagMap.put( entry.getKey(), Boolean.FALSE );
		}
	}

	/**
	 * Removes the mapping for this entity from this CopyCache if it is present
	 * @param entity must be non-null
	 * @return previous value associated with specified entity, or null if there was no mapping for entity.
	 * @throws NullPointerException if entity is null
	 */
	public Object remove(Object entity) {
		if ( entity == null ) {
			throw new NullPointerException( "null entities are not supported by " + getClass().getName() );
		}
		entityToIncludeInMergeFlagMap.remove( entity );
		return entityToCopyMap.remove( entity );
	}

	/**
	 * Returns the number of entity-copy mappings in this CopyCache
	 * @return the number of entity-copy mappings in this CopyCache
	 */
	public int size() {
		return entityToCopyMap.size();
	}

	/**
	 * Returns a collection view of the entity copies contained in this CopyCache.
	 * @return a collection view of the entity copies contained in this CopyCache
	 */
	public Collection values() {
		return entityToCopyMap.values();
	}

	/**
	 * Returns true if the specified entity is included in the merge.
	 * @param entity must be non-null
	 * @return true if the specified entity is included in the merge.
	 * @throws NullPointerException if entity is null
	 */
	public boolean isIncludedInMerge(Object entity) {
		if ( entity == null ) {
			throw new NullPointerException( "null entities are not supported by " + getClass().getName() );
		}
		return ( ( Boolean ) entityToIncludeInMergeFlagMap.get( entity ) ).booleanValue();
	}

	/**
	 * Set flag to indicate if an entity is included in the merge.
	 * @param entity must be non-null and this CopyCache must contain a mapping for this entity
	 * @return true if the specified entity is included in the merge
	 * @throws NullPointerException if entity is null
	 * @throws AssertionFailure if this CopyCache does not contain a mapping for the specified entity
	 */
	/* package-private */ void setIncludedInMerge(Object entity, boolean isIncludedInMerge) {
		if ( entity == null ) {
			throw new NullPointerException( "null entities are not supported by " + getClass().getName() );
		}
		if ( ! entityToIncludeInMergeFlagMap.containsKey( entity ) ||
			! entityToCopyMap.containsKey( entity ) ) {
			throw new AssertionFailure( "called CopyCache.setInMergeProcess() for entity not found in CopyCache" );
		}
		entityToIncludeInMergeFlagMap.put( entity, Boolean.valueOf( isIncludedInMerge ) );
	}

	/**
	 * Returns the copy-entity mappings
	 * @return the copy-entity mappings
	 */
	public Map getMergeMap() {
		return IdentityMap.invert( entityToCopyMap );
	}
}