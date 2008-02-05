//$Id: $
package org.hibernate.event;

import java.io.Serializable;

import org.hibernate.collection.PersistentCollection;
import org.hibernate.engine.EntityEntry;

/**
 * Defines a base class for events involving collections.
 *
 * @author Gail Badner
 */
public abstract class AbstractCollectionEvent extends AbstractEvent {

	private final PersistentCollection collection;
	private final Object affectedOwner;
	private final Serializable affectedOwnerId;

	/**
	 * Constructs an AbstractCollectionEvent object.
	 *
	 * @param collection - the collection
	 * @param source - the Session source
	 * @param affectedOwner - the owner that is affected by this event;
	 * can be null if unavailable
	 * @param affectedOwnerId - the ID for the owner that is affected
	 * by this event; can be null if unavailable
	 * that is affected by this event; can be null if unavailable
	 */
	public AbstractCollectionEvent(PersistentCollection collection,
								   EventSource source,
								   Object affectedOwner,
								   Serializable affectedOwnerId ) {
		super(source);
		this.collection = collection;
		this.affectedOwner = affectedOwner;
		this.affectedOwnerId = affectedOwnerId;
	}

	protected static Object getLoadedOwnerOrNull( PersistentCollection collection, EventSource source ) {
		return source.getPersistenceContext().getLoadedCollectionOwnerOrNull( collection );
	}

	protected static Serializable getLoadedOwnerIdOrNull( PersistentCollection collection, EventSource source ) {
		return source.getPersistenceContext().getLoadedCollectionOwnerIdOrNull( collection );
	}

	protected static Serializable getOwnerIdOrNull( Object owner, EventSource source ) {
		EntityEntry ownerEntry = source.getPersistenceContext().getEntry( owner );
		return ( ownerEntry == null ? null : ownerEntry.getId() );
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
	public Serializable getAffectedOwnerIdOrNull() {
		return affectedOwnerId;
	}
}
