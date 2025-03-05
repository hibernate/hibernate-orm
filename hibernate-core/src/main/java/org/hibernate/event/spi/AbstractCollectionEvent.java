/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * Defines a base class for events involving collections.
 *
 * @author Gail Badner
 */
public abstract class AbstractCollectionEvent extends AbstractEvent {

	private final PersistentCollection<?> collection;
	private final Object affectedOwner;
	private final Object affectedOwnerId;
	private final String affectedOwnerEntityName;

	/**
	 * Constructs an AbstractCollectionEvent object.
	 *  @param collection - the collection
	 * @param source - the Session source
	 * @param affectedOwner - the owner that is affected by this event;
 * can be null if unavailable
	 * @param affectedOwnerId - the ID for the owner that is affected
* by this event; can be null if unavailable
	 */
	public AbstractCollectionEvent(
			CollectionPersister collectionPersister,
			PersistentCollection<?> collection,
			EventSource source,
			Object affectedOwner,
			Object affectedOwnerId) {
		super( source );
		this.collection = collection;
		this.affectedOwner = affectedOwner;
		this.affectedOwnerId = affectedOwnerId;
		this.affectedOwnerEntityName =
				getAffectedOwnerEntityName( collectionPersister, affectedOwner, source );
	}

	protected static CollectionPersister getLoadedCollectionPersister( PersistentCollection<?> collection, EventSource source ) {
		CollectionEntry ce = source.getPersistenceContextInternal().getCollectionEntry( collection );
		return ce == null ? null : ce.getLoadedPersister();
	}

	protected static Object getLoadedOwnerOrNull( PersistentCollection<?> collection, EventSource source ) {
		return source.getPersistenceContextInternal().getLoadedCollectionOwnerOrNull( collection );
	}

	protected static Object getLoadedOwnerIdOrNull(PersistentCollection<?> collection, EventSource source ) {
		return source.getPersistenceContextInternal().getLoadedCollectionOwnerIdOrNull( collection );
	}

	protected static Object getOwnerIdOrNull(Object owner, EventSource source ) {
		EntityEntry ownerEntry = source.getPersistenceContextInternal().getEntry( owner );
		return ownerEntry == null ? null : ownerEntry.getId();
	}

	protected static String getAffectedOwnerEntityName(
			CollectionPersister collectionPersister,
			Object affectedOwner,
			EventSource source ) {
		if ( affectedOwner != null ) {
			final EntityEntry entry =
					source.getPersistenceContextInternal()
							.getEntry( affectedOwner );
			if ( entry != null && entry.getEntityName() != null ) {
				return entry.getEntityName();
			}
		}

		if ( collectionPersister != null ) {
			return collectionPersister.getOwnerEntityPersister().getEntityName();
		}
		else {
			// collectionPersister should not be null,
			// but we don't want to throw an exception
			// if it is null
			return null;
		}
	}

	public PersistentCollection<?> getCollection() {
		return collection;
	}

	/**
	 * Get the collection owner entity that is affected by this event.
	 *
	 * @return the affected owner; returns null if the entity is not in the persistence context
	 * (e.g., because the collection from a detached entity was moved to a new owner)
	 */
	public Object getAffectedOwnerOrNull() {
		return affectedOwner;
	}

	/**
	 * Get the ID for the collection owner entity that is affected by this event.
	 *
	 * @return the affected owner ID; returns null if the ID cannot be obtained
	 * from the collection's loaded key (e.g., a property-ref is used for the
	 * collection and does not include the entity's ID)
	 */
	public Object getAffectedOwnerIdOrNull() {
		return affectedOwnerId;
	}

	/**
	 * Get the entity name for the collection owner entity that is affected by this event.
	 *
	 * @return the entity name; if the owner is not in the PersistenceContext, the
	 * returned value may be a superclass name, instead of the actual class name
	 */
	public String getAffectedOwnerEntityName() {
		return affectedOwnerEntityName;
	}

}
