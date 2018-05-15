/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.spi;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;

/**
 * Defines a base class for events involving collections.
 *
 * @author Gail Badner
 */
public abstract class AbstractCollectionEvent extends AbstractEvent {

	private final PersistentCollection collection;
	private final Object affectedOwner;
	private final Object affectedOwnerId;
	private final String affectedOwnerEntityName;

	/**
	 * Constructs an AbstractCollectionEvent object.
	 *
	 * @param collectionDescriptor - the collection descriptor
	 * @param collection - the collection
	 * @param source - the Session source
	 * @param affectedOwner - the owner that is affected by this event;
	 * can be null if unavailable
	 * @param affectedOwnerId - the ID for the owner that is affected
	 * by this event; can be null if unavailable
	 */
	public AbstractCollectionEvent(
			PersistentCollectionDescriptor collectionDescriptor,
			PersistentCollection collection,
			EventSource source,
			Object affectedOwner,
			Object affectedOwnerId) {
		super( source );
		this.collection = collection;
		this.affectedOwner = affectedOwner;
		this.affectedOwnerId = affectedOwnerId;
		this.affectedOwnerEntityName =
				getAffectedOwnerEntityName( collectionDescriptor, affectedOwner, source );
	}

	protected static PersistentCollectionDescriptor getLoadedCollectionDescriptor(PersistentCollection collection, EventSource source ) {
		CollectionEntry ce = source.getPersistenceContext().getCollectionEntry( collection );
		return ( ce == null ? null : ce.getLoadedCollectionDescriptor() );
	}

	protected static Object getLoadedOwnerOrNull( PersistentCollection collection, EventSource source ) {
		return source.getPersistenceContext().getLoadedCollectionOwnerOrNull( collection );
	}

	protected static Object getLoadedOwnerIdOrNull(PersistentCollection collection, EventSource source ) {
		return source.getPersistenceContext().getLoadedCollectionOwnerIdOrNull( collection );
	}

	protected static Object getOwnerIdOrNull(Object owner, EventSource source ) {
		EntityEntry ownerEntry = source.getPersistenceContext().getEntry( owner );
		return ( ownerEntry == null ? null : ownerEntry.getId() );
	}

	protected static String getAffectedOwnerEntityName(PersistentCollectionDescriptor collectionDescriptor, Object affectedOwner, EventSource source ) {

		// collectionDescriptor should not be null, but we don't want to throw
		// an exception if it is null
		String entityName =
				( collectionDescriptor == null ? null : collectionDescriptor.findEntityOwnerDescriptor().getEntityName() );
		if ( affectedOwner != null ) {
			EntityEntry ee = source.getPersistenceContext().getEntry( affectedOwner );
			if ( ee != null && ee.getEntityName() != null) {
				entityName = ee.getEntityName();
			}
		}	
		return entityName;
	}

	public PersistentCollection getCollection() {
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
