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
 * EventCache is a Map implementation that can be used by an event
 * listener to keep track of entities involved in the operation
 * being performed. This implementation allows entities to be added
 * to the EventCache before the operation has cascaded to that
 * entity.
 * <p/>
 * The following methods can be used by event listeners (and other
 * classes) in the same package to add entities to an EventCache
 * and indicate if the operation is being performed on the entity:<p/>
 * {@link EventCache#put(Object entity, Object copy, boolean isOperatedOn)}
 * <p/>
 * The following method can be used by event listeners (and other
 * classes) in the same package to indicate that the operation is being
 * performed on an entity already in the EventCache:
 * {@link EventCache#setOperatedOn(Object entity, boolean isOperatedOn)
 *
 * @author Gail Badner
 */
class EventCache implements Map {
	private Map entityToCopyMap = IdentityMap.instantiate(10);
		// key is an entity involved with the operation performed by the listener;
		// value can be either a copy of the entity or the entity itself

	private Map entityToOperatedOnFlagMap = IdentityMap.instantiate(10);
	    // key is an entity involved with the operation performed by the listener;
	    // value is a flag indicating if the listener explicitly operates on the entity

	/**
	 * Clears the EventCache.
	 */
	public void clear() {
		entityToCopyMap.clear();
		entityToOperatedOnFlagMap.clear();
	}

	/**
	 * Returns true if this EventCache contains a mapping for the specified entity.
	 * @param entity must be non-null
	 * @return true if this EventCache contains a mapping for the specified entity
	 * @throws NullPointerException if entity is null
	 */
	public boolean containsKey(Object entity) {
		if ( entity == null ) {
			throw new NullPointerException( "null entities are not supported by " + getClass().getName() );
		}
		return entityToCopyMap.containsKey( entity );
	}

	/**
	 * Returns true if this EventCache maps one or more entities to the specified copy.
	 * @param copy must be non-null
	 * @return true if this EventCache maps one or more entities to the specified copy
	 * @throws NullPointerException if copy is null
	 */
	public boolean containsValue(Object copy) {
		if ( copy == null ) {
			throw new NullPointerException( "null copies are not supported by " + getClass().getName() );
		}
		return entityToCopyMap.containsValue( copy );
	}

	/**
	 * Returns a set view of the entity-to-copy mappings contained in this EventCache.
	 * @return set view of the entity-to-copy mappings contained in this EventCache
	 */
	public Set entrySet() {
		return entityToCopyMap.entrySet();
	}

	/**
	 * Returns the copy to which this EventCache maps the specified entity.
	 * @param entity must be non-null
	 * @return the copy to which this EventCache maps the specified entity
	 * @throws NullPointerException if entity is null
	 */
	public Object get(Object entity) {
		if ( entity == null ) {
			throw new NullPointerException( "null entities are not supported by " + getClass().getName() );
		}
		return entityToCopyMap.get( entity );
	}

	/**
	 * Returns true if this EventCache contains no entity-copy mappings.
	 * @return true if this EventCache contains no entity-copy mappings
	 */
	public boolean isEmpty() {
		return entityToCopyMap.isEmpty();
	}

	/**
	 * Returns a set view of the entities contained in this EventCache
	 * @return a set view of the entities contained in this EventCache
	 */
	public Set keySet() {
		return entityToCopyMap.keySet();
	}

	/**
	 * Associates the specified entity with the specified copy in this EventCache;
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
		entityToOperatedOnFlagMap.put( entity, Boolean.FALSE );
		return entityToCopyMap.put( entity, copy );
	}

	/**
	 * Associates the specified entity with the specified copy in this EventCache;
	 * @param entity must be non-null
	 * @param copy must be non- null
	 * @param isOperatedOn indicates if the operation is performed on the entity
	 *
	 * @return previous copy associated with specified entity, or null if
	 * there was no mapping for entity.
	 * @throws NullPointerException if entity or copy is null
	 */
	/* package-private */ Object put(Object entity, Object copy, boolean isOperatedOn) {
		if ( entity == null || copy == null ) {
			throw new NullPointerException( "null entities and copies are not supported by " + getClass().getName() );
		}
		entityToOperatedOnFlagMap.put( entity, Boolean.valueOf( isOperatedOn ) );
		return entityToCopyMap.put( entity, copy );
	}

	/**
	 * Copies all of the mappings from the specified map to this EventCache
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
			entityToOperatedOnFlagMap.put( entry.getKey(), Boolean.FALSE );
		}
	}

	/**
	 * Removes the mapping for this entity from this EventCache if it is present
	 * @param entity must be non-null
	 * @return previous value associated with specified entity, or null if there was no mapping for entity.
	 * @throws NullPointerException if entity is null
	 */
	public Object remove(Object entity) {
		if ( entity == null ) {
			throw new NullPointerException( "null entities are not supported by " + getClass().getName() );
		}
		entityToOperatedOnFlagMap.remove( entity );
		return entityToCopyMap.remove( entity );
	}

	/**
	 * Returns the number of entity-copy mappings in this EventCache
	 * @return the number of entity-copy mappings in this EventCache
	 */
	public int size() {
		return entityToCopyMap.size();
	}

	/**
	 * Returns a collection view of the entity copies contained in this EventCache.
	 * @return a collection view of the entity copies contained in this EventCache
	 */
	public Collection values() {
		return entityToCopyMap.values();
	}

	/**
	 * Returns true if the listener is performing the operation on the specified entity.
	 * @param entity must be non-null
	 * @return true if the listener is performing the operation on the specified entity.
	 * @throws NullPointerException if entity is null
	 */
	public boolean isOperatedOn(Object entity) {
		if ( entity == null ) {
			throw new NullPointerException( "null entities are not supported by " + getClass().getName() );
		}
		return ( ( Boolean ) entityToOperatedOnFlagMap.get( entity ) ).booleanValue();
	}

	/**
	 * Set flag to indicate if the listener is performing the operation on the specified entity.
	 * @param entity must be non-null and this EventCache must contain a mapping for this entity
	 * @return true if the listener is performing the operation on the specified entity
	 * @throws NullPointerException if entity is null
	 * @throws AssertionFailure if this EventCache does not contain a mapping for the specified entity
	 */
	/* package-private */ void setOperatedOn(Object entity, boolean isOperatedOn) {
		if ( entity == null ) {
			throw new NullPointerException( "null entities are not supported by " + getClass().getName() );
		}
		if ( ! entityToOperatedOnFlagMap.containsKey( entity ) ||
			! entityToCopyMap.containsKey( entity ) ) {
			throw new AssertionFailure( "called EventCache.setOperatedOn() for entity not found in EventCache" );
		}
		entityToOperatedOnFlagMap.put( entity, Boolean.valueOf( isOperatedOn ) );
	}

	/**
	 * Returns the copy-entity mappings
	 * @return the copy-entity mappings
	 */
	public Map invertMap() {
		return IdentityMap.invert( entityToCopyMap );
	}
}