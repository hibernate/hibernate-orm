/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import org.hibernate.Internal;import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * Defines a base class for events involving collections.
 *
 * @author Gail Badner
 * @author Steve Ebersole
 *
 * @apiNote AbstractCollectionEvent and its implementations are defined
 * as SPI for consumption; their constructors are defined as
 * {@linkplain Internal internal}
 */
public abstract class AbstractCollectionEvent extends AbstractSessionEvent {
	private final PersistentCollection<?> collection;
	private final CollectionPersister collectionPersister;
	private final Object owner;
	private final Object ownerId;
	private final String ownerEntityName;

	/**
	 * Constructs an instance for a stateful session.
	 *
	 * @param collectionPersister The descriptor for the collection mapping.
	 * @param collection - The (wrapped) collection instance.
	 * @param source - The {@linkplain org.hibernate.Session session}
	 * @param owner - The entity instance that "owns" the {@code collection}
	 * 		affected by this event; can be {@code null} if unavailable.
	 * @param ownerId - The identifier value for the {@code owner}; can be
	 * 		{@code null} if unavailable.
	 */
	@Internal
	public AbstractCollectionEvent(
			CollectionPersister collectionPersister,
			PersistentCollection<?> collection,
			EventSource source,
			Object owner,
			Object ownerId) {
		super( source );
		this.collection = collection;
		this.collectionPersister = collectionPersister;
		this.owner = owner;
		this.ownerId = ownerId;
		this.ownerEntityName = getAffectedOwnerEntityName( collectionPersister, owner, source );
	}

	/**
	 * Constructs an instance for a stateless session.
	 *
	 * @param collectionPersister The descriptor for the collection mapping.
	 * @param collection - The (wrapped) collection instance.
	 * @param ownerEntityName - The entity-name of the {@code owner}.
	 * @param owner - The entity instance that "owns" the {@code collection}
	 * 		affected by this event; can be {@code null} if unavailable.
	 * @param ownerId - The identifier value for the {@code owner}; can be
	 * 		{@code null} if unavailable.
	 */
	@Internal
	public AbstractCollectionEvent(
			CollectionPersister collectionPersister,
			PersistentCollection<?> collection,
			String ownerEntityName,
			Object owner,
			Object ownerId) {
		super( null );
		this.collection = collection;
		this.owner = owner;
		this.ownerId = ownerId;
		this.ownerEntityName = ownerEntityName;
		this.collectionPersister = collectionPersister;
	}

	/**
	 * The descriptor for the {@linkplain #getCollection() collection} mapping.
	 */
	public CollectionPersister getCollectionPersister() {
		return collectionPersister;
	}

	/**
	 * The (wrapped) collection instance affected by this event.
	 */
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
		return owner;
	}

	/**
	 * Get the ID for the collection owner entity that is affected by this event.
	 *
	 * @return the affected owner ID; returns null if the ID cannot be obtained
	 * from the collection's loaded key (e.g., a property-ref is used for the
	 * collection and does not include the entity's ID)
	 */
	public Object getAffectedOwnerIdOrNull() {
		return ownerId;
	}

	/**
	 * Get the entity name for the collection owner entity that is affected by this event.
	 *
	 * @return the entity name; if the owner is not in the PersistenceContext, the
	 * returned value may be a superclass name, instead of the actual class name
	 */
	public String getAffectedOwnerEntityName() {
		return ownerEntityName;
	}

	protected static CollectionPersister getLoadedCollectionPersister(PersistentCollection<?> collection, EventSource source) {
		final var entry = source.getPersistenceContextInternal().getCollectionEntry( collection );
		return entry == null ? null : entry.getLoadedPersister();
	}

	protected static Object getLoadedOwnerOrNull( PersistentCollection<?> collection, EventSource source ) {
		return source.getPersistenceContextInternal().getLoadedCollectionOwnerOrNull( collection );
	}

	protected static Object getLoadedOwnerIdOrNull(PersistentCollection<?> collection, EventSource source ) {
		return source.getPersistenceContextInternal().getLoadedCollectionOwnerIdOrNull( collection );
	}

	protected static Object getOwnerIdOrNull(Object owner, EventSource source ) {
		final var ownerEntry = source.getPersistenceContextInternal().getEntry( owner );
		return ownerEntry == null ? null : ownerEntry.getId();
	}

	protected static String getAffectedOwnerEntityName(
			CollectionPersister collectionPersister,
			Object affectedOwner,
			EventSource source ) {
		if ( affectedOwner != null ) {
			final var entry =
					source.getPersistenceContextInternal()
							.getEntry( affectedOwner );
			if ( entry != null && entry.getEntityName() != null ) {
				return entry.getEntityName();
			}
		}

		return collectionPersister != null
				? collectionPersister.getOwnerEntityPersister().getEntityName()
				// collectionPersister should not be null,
				// but we don't want to throw an exception
				// if it is null
				: null;
	}

}
