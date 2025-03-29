/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.pretty.MessageHelper;

/**
 * {@code MergeContext} is a specialized {@link Map} implementation used by a
 * {@linkplain org.hibernate.event.spi.MergeEventListener merge event listener}
 * to keep track of each entity being merged and its corresponding managed result.
 * An entity to be merged may to be added to the {@code MergeContext} before the
 * merge operation cascades to the entity.
 * <ul>
 * <li>A <em>merge entity</em> (the {@code mergeEntity} method parameter) is an
 *     entity being merged via {@link EventSource#merge(Object mergeEntity)}.
 * <li>A <em>managed entity</em> (the {@code managedEntity} method parameter) is
 *     the managed entity that is the result of merging an entity.
 * </ul>
 * <p>
 * A merge entity can be transient, detached, or managed. If it is managed, then
 * it is identical to its resulting managed entity.
 * <p>
 * If {@link #put(Object mergeEntity, Object managedEntity)} is called, and this
 * {@code MergeContext} already contains an entry with a different entity as the
 * key, but with the same (managed entity) value, this means that multiple entity
 * representations for the same persistent entity are being merged. In this
 * situation, {@link EntityCopyObserver#entityCopyDetected(
 * Object managedEntity, Object mergeEntity1, Object mergeEntity2, EventSource)}
 * is called to determine an appropriate resolution.
 * <p>
 * There are several restrictions.
 * <ul>
 * <li>Methods that return collections (e.g., {@link #keySet()}, {@link #values()},
 *     {@link #entrySet()}) return an unmodifiable view of the collection.
 * <li>If {@link #put(Object mergeEntity, Object) managedEntity} or
 *     {@link #put(Object mergeEntity, Object managedEntity, boolean isOperatedOn)}
 *     is executed and this {@code MergeContext} already contains a cross reference
 *     for {@code mergeEntity}, then {@code managedEntity} must be identical to the
 *     entity already associated with {@code mergeEntity}.
 * <li>If {@link #putAll(Map map)} is executed, the previous restriction applies to
 *     each entry in the {@link Map}.
 * <li>The {@link #remove(Object)} operation is not supported; the only way to
 *     remove data from a {@code MergeContext} is by calling {@link #clear()}.
 * <li>The map returned by {@link #invertMap()} will only contain the "newest"
 *     (most recently added) managed-to-merge cross-reference to its merge entity.
 * </ul>
 * <p>
 * The following method is intended to be used by an implementation of
 * {@link org.hibernate.event.spi.MergeEventListener} to add a merge entity and its
 * corresponding managed entity to a {@code MergeContext} and indicate if the merge
 * operation is being performed on the merge entity yet.
 * <p>
 * {@link MergeContext#put(Object mergeEntity, Object managedEntity, boolean isOperatedOn)}
 * <p>
 * The following method is intended to be used by a {@code MergeEventListener} to
 * indicate whether the merge operation is being performed on a merge entity already
 * in the {@code MergeContext}:
 * <p>
 * {@link MergeContext#setOperatedOn(Object mergeEntity, boolean isOperatedOn)}
 *
 * @author Gail Badner
 */
public class MergeContext implements Map<Object,Object> {

	private final EventSource session;
	private final EntityCopyObserver entityCopyObserver;

	private final Map<Object,Object> mergeToManagedEntityXref = new IdentityHashMap<>(10);
		// key is an entity to be merged;
		// value is the associated managed entity (result) in the persistence context.

	private final Map<Object,Object> managedToMergeEntityXref = new IdentityHashMap<>( 10 );
		// maintains the inverse of the mergeToManagedEntityXref for performance reasons.
		// key is the managed entity result in the persistence context.
		// value is the associated entity to be merged; if multiple
		// representations of the same persistent entity are added to the MergeContext,
		// value will be the most recently added merge entity that is
		// associated with the managed entity.

	// TODO: merge mergeEntityToOperatedOnFlagMap into mergeToManagedEntityXref, since they have the same key.
	//       need to check if this would hurt performance.
	private final Map<Object,Boolean> mergeEntityToOperatedOnFlagMap = new IdentityHashMap<>( 10 );
		// key is a merge entity;
		// value is a flag indicating if the merge entity is currently in the merge process.

	public MergeContext(EventSource session, EntityCopyObserver entityCopyObserver){
		this.session = session;
		this.entityCopyObserver = entityCopyObserver;
	}

	/**
	 * Clears the MergeContext.
	 */
	public void clear() {
		mergeToManagedEntityXref.clear();
		managedToMergeEntityXref.clear();
		mergeEntityToOperatedOnFlagMap.clear();
	}

	/**
	 * Returns true if this MergeContext contains a cross-reference for the specified merge entity
	 * to a managed entity result.
	 *
	 * @param mergeEntity must be non-null
	 * @return true if this MergeContext contains a cross-reference for the specified merge entity
	 * @throws NullPointerException if mergeEntity is null
	 */
	public boolean containsKey(Object mergeEntity) {
		if ( mergeEntity == null ) {
			throw new NullPointerException( "null entities are not supported by " + getClass().getName() );
		}
		return mergeToManagedEntityXref.containsKey( mergeEntity );
	}

	/**
	 * Returns true if this MergeContext contains a cross-reference from the specified managed entity
	 * to a merge entity.
	 * @param managedEntity must be non-null
	 * @return true if this MergeContext contains a cross-reference from the specified managed entity
	 * to a merge entity
	 * @throws NullPointerException if managedEntity is null
	 */
	public boolean containsValue(Object managedEntity) {
		if ( managedEntity == null ) {
			throw new NullPointerException( "null copies are not supported by " + getClass().getName() );
		}
		return managedToMergeEntityXref.containsKey( managedEntity );
	}

	/**
	 * Returns an unmodifiable set view of the merge-to-managed entity cross-references contained in this MergeContext.
	 * @return an unmodifiable set view of the merge-to-managed entity cross-references contained in this MergeContext
	 *
	 * @see Collections#unmodifiableSet(Set)
	 */
	public Set<Map.Entry<Object,Object>> entrySet() {
		return Collections.unmodifiableSet( mergeToManagedEntityXref.entrySet() );
	}

	/**
	 * Returns the managed entity associated with the specified merge Entity.
	 * @param mergeEntity the merge entity; must be non-null
	 * @return  the managed entity associated with the specified merge Entity
	 * @throws NullPointerException if mergeEntity is null
	 */
	public Object get(Object mergeEntity) {
		if ( mergeEntity == null ) {
			throw new NullPointerException( "null entities are not supported by " + getClass().getName() );
		}
		return mergeToManagedEntityXref.get( mergeEntity );
	}

	/**
	 * Returns true if this MergeContext contains no merge-to-managed entity cross-references.
	 * @return true if this MergeContext contains no merge-to-managed entity cross-references.
	 */
	public boolean isEmpty() {
		return mergeToManagedEntityXref.isEmpty();
	}

	/**
	 * Returns an unmodifiable set view of the merge entities contained in this MergeContext
	 * @return an unmodifiable set view of the merge entities contained in this MergeContext
	 *
	 * @see Collections#unmodifiableSet(Set)
	 */
	public Set<Object> keySet() {
		return Collections.unmodifiableSet( mergeToManagedEntityXref.keySet() );
	}

	/**
	 * Associates the specified merge entity with the specified managed entity result in this MergeContext.
	 * If this MergeContext already contains a cross-reference for {@code mergeEntity} when this
	 * method is called, then <code>managedEntity</code> must be the same as what is already associated
	 * with {@code mergeEntity}.
	 * <p>
	 * This method assumes that the merge process is not yet operating on {@code mergeEntity}.
	 * Later when {@code mergeEntity} enters the merge process, {@link #setOperatedOn(Object, boolean)}
	 * should be called.
	 * <p>
	 * @param mergeEntity the merge entity; must be non-null
	 * @param managedEntity the managed entity result; must be non-null
	 * @return previous managed entity associated with specified merge entity, or null if
	 * there was no mapping for mergeEntity.
	 * @throws NullPointerException if mergeEntity or managedEntity is null
	 * @throws IllegalArgumentException if <code>managedEntity</code> is not the same as the previous
	 * managed entity associated with <code>merge entity</code>
	 * @throws IllegalStateException if internal cross-references are out of sync,
	 */
	public Object put(Object mergeEntity, Object managedEntity) {
		return put( mergeEntity, managedEntity, Boolean.FALSE );
	}

	/**
	 * Associates the specified merge entity with the specified managed entity in this MergeContext.
	 * If this MergeContext already contains a cross-reference for {@code mergeEntity} when this
	 * method is called, then <code>managedEntity</code> must be the same as what is already associated
	 * with {@code mergeEntity}.
	 *
	 * @param mergeEntity the merge entity; must be non-null
	 * @param managedEntity the managed entity; must be non-null
	 * @param isOperatedOn indicates if the merge operation is performed on the mergeEntity.
	 *
	 * @return previous managed entity associated with specified merge entity, or null if
	 * there was no mapping for mergeEntity.
	 * @throws NullPointerException if mergeEntity or managedEntity is null
	 * @throws IllegalArgumentException if {@code managedEntity} is not the same as the previous
	 * managed entity associated with {@code mergeEntity}
	 * @throws IllegalStateException if internal cross-references are out of sync,
	 */
	public Object put(Object mergeEntity, Object managedEntity, boolean isOperatedOn) {
		if ( mergeEntity == null || managedEntity == null ) {
			throw new NullPointerException( "null merge and managed entities are not supported by " + getClass().getName() );
		}

		Object oldManagedEntity = mergeToManagedEntityXref.put( mergeEntity, managedEntity );
		Boolean oldOperatedOn = mergeEntityToOperatedOnFlagMap.put( mergeEntity, isOperatedOn );
		// If managedEntity already corresponds with a different merge entity, that means
		// that there are multiple entities being merged that correspond with managedEntity.
		// In the following, oldMergeEntity will be replaced with mergeEntity in managedToMergeEntityXref.
		Object oldMergeEntity = managedToMergeEntityXref.put( managedEntity, mergeEntity );

		if ( oldManagedEntity == null ) {
			// this is a new mapping for mergeEntity in mergeToManagedEntityXref
			if  ( oldMergeEntity != null ) {
				// oldMergeEntity was a different merge entity with the same corresponding managed entity;
				entityCopyObserver.entityCopyDetected( managedEntity, mergeEntity, oldMergeEntity, session );
			}
			if ( oldOperatedOn != null ) {
				throw new IllegalStateException(
						"MergeContext#mergeEntityToOperatedOnFlagMap contains a merge entity " + printEntity( mergeEntity )
								+ ", but MergeContext#mergeToManagedEntityXref does not."
				);
			}
		}
		else {
			// mergeEntity was already mapped in mergeToManagedEntityXref
			if ( oldManagedEntity != managedEntity ) {
				throw new IllegalArgumentException(
						"Error occurred while storing a merge Entity " + printEntity( mergeEntity )
								+ ". It was previously associated with managed entity " + printEntity( oldManagedEntity )
								+ ". Attempted to replace managed entity with " + printEntity( managedEntity )
				);
			}
			if ( oldOperatedOn == null ) {
				throw new IllegalStateException(
						"MergeContext#mergeToManagedEntityXref contained a merge entity " + printEntity( mergeEntity )
								+ ", but MergeContext#mergeEntityToOperatedOnFlagMap did not."
				);
			}
		}

		return oldManagedEntity;
	}

	/**
	 * Copies all of the mappings from the specified Map to this MergeContext.
	 * The key and value for each entry in <code>map</code> is subject to the same
	 * restrictions as {@link #put(Object mergeEntity, Object managedEntity)}.
	 *
	 * This method assumes that the merge process is not yet operating on any merge entity
	 *
	 * @param map keys and values must be non-null
	 * @throws NullPointerException if any key or value is null
	 * @throws IllegalArgumentException if a key in <code>map</code> was already in this MergeContext
	 * but associated value in <code>map</code> is different from the previous value in this MergeContext.
	 * @throws IllegalStateException if internal cross-references are out of sync,
	 */
	public void putAll(Map<?,?> map) {
		for ( Entry<?,?> entry : map.entrySet() ) {
			put( entry.getKey(), entry.getValue() );
		}
	}

	/**
	 * The remove operation is not supported.
	 * @param mergeEntity the merge entity.
	 * @throws UnsupportedOperationException if called.
	 */
	public Object remove(Object mergeEntity) {
		throw new UnsupportedOperationException(
				String.format( "Operation not supported: %s.remove()", getClass().getName() )
		);
	}

	/**
	 * Returns the number of merge-to-managed entity cross-references in this MergeContext
	 * @return the number of merge-to-managed entity cross-references in this MergeContext
	 */
	public int size() {
		return mergeToManagedEntityXref.size();
	}

	/**
	 * Returns an unmodifiable Set view of managed entities contained in this MergeContext.
	 * @return an unmodifiable Set view of managed entities contained in this MergeContext
	 *
	 * @see Collections#unmodifiableSet(Set)
	 */
	public Collection<Object> values() {
		return Collections.unmodifiableSet( managedToMergeEntityXref.keySet() );
	}

	/**
	 * Returns true if the listener is performing the merge operation on the specified merge entity.
	 * @param mergeEntity the merge entity; must be non-null
	 * @return true if the listener is performing the merge operation on the specified merge entity;
	 * false, if there is no mapping for mergeEntity.
	 * @throws NullPointerException if mergeEntity is null
	 */
	public boolean isOperatedOn(Object mergeEntity) {
		if ( mergeEntity == null ) {
			throw new NullPointerException( "null merge entities are not supported by " + getClass().getName() );
		}
		final Boolean isOperatedOn = mergeEntityToOperatedOnFlagMap.get( mergeEntity );
		return isOperatedOn != null && isOperatedOn;
	}

	/**
	 * Set flag to indicate if the listener is performing the merge operation on the specified merge entity.
	 * @param mergeEntity must be non-null and this MergeContext must contain a cross-reference for mergeEntity
	 *                       to a managed entity
	 * @throws NullPointerException if mergeEntity is null
	 * @throws IllegalStateException if this MergeContext does not contain a a cross-reference for mergeEntity
	 */
	public void setOperatedOn(Object mergeEntity, boolean isOperatedOn) {
		if ( mergeEntity == null ) {
			throw new NullPointerException( "null entities are not supported by " + getClass().getName() );
		}
		if ( ! mergeEntityToOperatedOnFlagMap.containsKey( mergeEntity ) ||
			! mergeToManagedEntityXref.containsKey( mergeEntity ) ) {
			throw new IllegalStateException( "called MergeContext#setOperatedOn() for mergeEntity not found in MergeContext" );
		}
		mergeEntityToOperatedOnFlagMap.put( mergeEntity, isOperatedOn );
	}

	/**
	 * Returns an unmodifiable map view of the managed-to-merge entity
	 * cross-references.
	 *
	 * The returned Map will contain a cross-reference from each managed entity
	 * to the most recently associated merge entity that was most recently put in the MergeContext.
	 *
	 * @return an unmodifiable map view of the managed-to-merge entity cross-references.
	 *
	 * @see Collections#unmodifiableMap(Map)
	 */
	public Map<Object,Object> invertMap() {
		return Collections.unmodifiableMap( managedToMergeEntityXref );
	}

	private String printEntity(Object entity) {
		if ( session.getPersistenceContextInternal().getEntry( entity ) != null ) {
			return MessageHelper.infoString( session.getEntityName( entity ), session.getIdentifier( entity ) );
		}
		// Entity was not found in current persistence context. Use Object#toString() method.
		return "[" + entity + "]";
	}

	public EventSource getEventSource() {
		return session;
	}
}
