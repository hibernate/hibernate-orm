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
 *
 NOTICE: File changed by Wim Ockerman @ CISCO on 2011/10/20
 *         Reasons of change:
 *         1. The EventCache is used during the merging of object graphs
 *            to persisted objects. During this process the EventCache
 *            builds up a map of entities (persistable objects) to
 *            copies (original detached objects or loaded entities).
 *            At certain places in the merge algorithm the inverse relation
 *            is needed, see
 *             ...def.AbstractSaveEventListener.performSaveOrReplicate
 *              -> calling persister.getPropertyValuesToInsert(.., getMergeMap(),..)
 *            The getMergeMap() call calls through this this class' invertMap method.
 *
 *            In the original implementation of invertMap() a map was created on the spot
 *            and all the elements in the entityToCopyMap where put in, now as copy
 *            to entity pair.
 *            Because this action can happen a lot in a big detached graph wile merging, the size
 *            of the entityToCopyMap can become substantial and thus also the creation of the
 *            inverted map.
 *            Tests with large graphs thus showed quadratic loss of performance. From a certain
 *            graph size this became substantial.
 *
 *
 *            As solution an inverseEntitiyToCopyMap (so a copyToEntityMap) is maintained now
 *            along with the changes to the entityToCopyMap.
 *
 *            This made the merge action more linear in terms of performance.
 *
 *            The changes are covered by a new UnitTest.
 *
 *
 *            Changes in the code have preceding comments of format
 *            CHANGE Wim Ockerman <date> [:<comment>]
 *            And end with:
 *            END OF CHANGE
 */
package org.hibernate.event.internal;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
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
	private Map entityToCopyMap = new IdentityHashMap(10);
		// key is an entity involved with the operation performed by the listener;
		// value can be either a copy of the entity or the entity itself

	// CHANGE Wim Ockerman 2011/10/20: maintains the inverse of the entityToCopyMap for performance reasons.
	private Map copyToEntityMap = new IdentityHashMap( 10 );
	// END OF CHANGE

	private Map entityToOperatedOnFlagMap = new IdentityHashMap( 10 );
	    // key is an entity involved with the operation performed by the listener;
	    // value is a flag indicating if the listener explicitly operates on the entity

	/**
	 * Clears the EventCache.
	 */
	public void clear() {
		entityToCopyMap.clear();
		
		// CHANGE Wim Ockerman 2011/10/20
		copyToEntityMap.clear();
		// END OF CHANGE
		
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
		// CHANGE Wim Ockerman 2011/10/20
		copyToEntityMap.put(copy, entity);
		// END OF CHANGE
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
		// CHANGE Wim Ockerman 2011/10/20
		copyToEntityMap.put(copy, entity);
		// END OF CHANGE
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
			// CHANGE Wim Ockerman 2011/10/20
			copyToEntityMap.put(entry.getValue(), entry.getKey());
			// END OF CHANGE
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
		// CHANGE Wim Ockerman 2011/10/20
		Object result = entityToCopyMap.remove( entity ); 
		copyToEntityMap.remove(result);
		// END OF CHANGE
		return result;
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
		// CHANGE Wim Ockerman 2011/10/20
		return copyToEntityMap;
		// END OF CHANGE
	}
}