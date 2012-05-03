/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.event.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.AssertionFailure;

/**
 * EventCache is a Map implementation that can be used by an event
 * listener to keep track of entities involved in the operation
 * being performed. This implementation allows entities to be added
 * to the EventCache before the operation has cascaded to that
 * entity.
 * <p/>
 * There are some restriction;
 * <ul>
 *     <li>the same value cannot be associated with more than one key</li>
 *     <li>Methods that return collections (e.g., {@link #keySet()},
 *          {@link #values()}, {@link #entrySet()}) return an
 *          unnmodifiable view of the collection.</li>
 * </ul>
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
	private Map<Object,Object> entityToCopyMap = new IdentityHashMap<Object,Object>(10);
		// key is an entity involved with the operation performed by the listener;
		// value can be either a copy of the entity or the entity itself

	private Map<Object,Object> copyToEntityMap = new IdentityHashMap<Object,Object>( 10 );
		// maintains the inverse of the entityToCopyMap for performance reasons.

	private Map<Object,Boolean> entityToOperatedOnFlagMap = new IdentityHashMap<Object,Boolean>( 10 );
	    // key is an entity involved with the operation performed by the listener;
	    // value is a flag indicating if the listener explicitly operates on the entity

	/**
	 * Clears the EventCache.
	 */
	public void clear() {
		entityToCopyMap.clear();
		copyToEntityMap.clear();
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
	 * Returns true if this EventCache maps an entity to the specified copy.
	 * @param copy must be non-null
	 * @return true if this EventCache maps an entity to the specified copy
	 * @throws NullPointerException if copy is null
	 */
	public boolean containsValue(Object copy) {
		if ( copy == null ) {
			throw new NullPointerException( "null copies are not supported by " + getClass().getName() );
		}
		return copyToEntityMap.containsKey( copy );
	}

	/**
	 * Returns an unmodifiable set view of the entity-to-copy mappings contained in this EventCache.
	 * @return an unmodifiable set view of the entity-to-copy mappings contained in this EventCache
	 *
	 * @see {@link Collections#unmodifiableSet(java.util.Set)}
	 */
	public Set entrySet() {
		return Collections.unmodifiableSet( entityToCopyMap.entrySet() );
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
	 * Returns an unmodifiable set view of the entities contained in this EventCache
	 * @return an unmodifiable set view of the entities contained in this EventCache
	 *
	 * @see {@link Collections#unmodifiableSet(java.util.Set)}
	 */
	public Set keySet() {
		return Collections.unmodifiableSet( entityToCopyMap.keySet() );
	}

	/**
	 * Associates the specified entity with the specified copy in this EventCache;
	 * @param entity must be non-null
	 * @param copy must be non- null and must not be associated with any other entity in this EntityCache.
	 * @return previous copy associated with specified entity, or null if
	 * there was no mapping for entity.
	 * @throws NullPointerException if entity or copy is null
	 * @throws IllegalStateException if the specified copy is already associated with a different entity.
	 */
	public Object put(Object entity, Object copy) {
		return put( entity, copy, Boolean.FALSE );
	}

	/**
	 * Associates the specified entity with the specified copy in this EventCache;
	 * @param entity must be non-null
	 * @param copy must be non- null and must not be associated with any other entity in this EntityCache.
	 * @param isOperatedOn indicates if the operation is performed on the entity.
	 *
	 * @return previous copy associated with specified entity, or null if
	 * there was no mapping for entity.
	 * @throws NullPointerException if entity or copy is null
	 * @throws IllegalStateException if the specified copy is already associated with a different entity.
	 */
	/* package-private */ Object put(Object entity, Object copy, boolean isOperatedOn) {
		if ( entity == null || copy == null ) {
			throw new NullPointerException( "null entities and copies are not supported by " + getClass().getName() );
		}

		Object oldCopy = entityToCopyMap.put( entity, copy );
		Boolean oldOperatedOn = entityToOperatedOnFlagMap.put( entity, isOperatedOn );
		Object oldEntity = copyToEntityMap.put( copy, entity );

		if ( oldCopy == null ) {
			if  ( oldEntity != null ) {
				throw new IllegalStateException( "An entity copy was already assigned to a different entity." );
			}
			if ( oldOperatedOn != null ) {
				throw new IllegalStateException( "entityToOperatedOnFlagMap contains an entity, but entityToCopyMap does not." );
			}
		}
		else {
			if ( oldCopy != copy ) {
				// Replaced an entity copy with a new copy; need to remove the oldCopy from copyToEntityMap
				// to synch things up.
				Object removedEntity = copyToEntityMap.remove( oldCopy );
				if ( removedEntity != entity ) {
					throw new IllegalStateException( "An unexpected entity was associated with the old entity copy." );
				}
				if ( oldEntity != null ) {
					throw new IllegalStateException( "A new entity copy is already associated with a different entity." );
				}
			}
			else {
				// Replaced an entity copy with the same copy in entityToCopyMap.
				// Make sure that copy is associated with the same entity in copyToEntityMap.
				if ( oldEntity != entity ) {
					throw new IllegalStateException( "An entity copy was associated with a different entity than provided." );
				}
			}
			if ( oldOperatedOn == null ) {
				throw new IllegalStateException( "entityToCopyMap contained an entity, but entityToOperatedOnFlagMap did not." );
			}
		}

		return oldCopy;
	}

	/**
	 * Copies all of the mappings from the specified map to this EventCache
	 * @param map keys and values must be non-null
	 * @throws NullPointerException if any map keys or values are null
	 */
	public void putAll(Map map) {
		for ( Object o : map.entrySet() ) {
			Entry entry = (Entry) o;
			put( entry.getKey(), entry.getValue() );
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
		Boolean oldOperatedOn = entityToOperatedOnFlagMap.remove( entity );
		Object oldCopy = entityToCopyMap.remove( entity );
		Object oldEntity = oldCopy != null ? copyToEntityMap.remove( oldCopy ) : null;

		if ( oldCopy == null ) {
			if ( oldOperatedOn != null ) {
				throw new IllegalStateException( "Removed entity from entityToOperatedOnFlagMap, but entityToCopyMap did not contain the entity." );
			}
		}
		else {
			if ( oldEntity == null ) {
				throw new IllegalStateException( "Removed entity from entityToCopyMap, but copyToEntityMap did not contain the entity." );
			}
			if ( oldOperatedOn == null ) {
				throw new IllegalStateException( "entityToCopyMap contained an entity, but entityToOperatedOnFlagMap did not." );
			}
			if ( oldEntity != entity ) {
				throw new IllegalStateException( "An entity copy was associated with a different entity than provided." );
			}
		}

		return oldCopy;
	}

	/**
	 * Returns the number of entity-copy mappings in this EventCache
	 * @return the number of entity-copy mappings in this EventCache
	 */
	public int size() {
		return entityToCopyMap.size();
	}

	/**
	 * Returns an unmodifiable set view of the entity copies contained in this EventCache.
	 * @return an unmodifiable set view of the entity copies contained in this EventCache
	 *
	 * @see {@link Collections#unmodifiableSet(java.util.Set)}
	 */
	public Collection values() {
		return Collections.unmodifiableCollection( entityToCopyMap.values() );
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
		return entityToOperatedOnFlagMap.get( entity );
	}

	/**
	 * Set flag to indicate if the listener is performing the operation on the specified entity.
	 * @param entity must be non-null and this EventCache must contain a mapping for this entity
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
		entityToOperatedOnFlagMap.put( entity, isOperatedOn );
	}

	/**
	 * Returns an unmodifiable map view of the copy-entity mappings
	 * @return an unmodifiable map view of the copy-entity mappings
	 *
	 * @see {@link Collections#unmodifiableMap(java.util.Map)}
	 */
	public Map invertMap() {
		return Collections.unmodifiableMap( copyToEntityMap );
	}
}