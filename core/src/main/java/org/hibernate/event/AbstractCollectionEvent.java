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
package org.hibernate.event;

import java.io.Serializable;

import org.hibernate.collection.PersistentCollection;
import org.hibernate.engine.CollectionEntry;
import org.hibernate.engine.EntityEntry;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * Defines a base class for events involving collections.
 *
 * @author Gail Badner
 */
public abstract class AbstractCollectionEvent extends AbstractEvent {

	private final PersistentCollection collection;
	private final Object affectedOwner;
	private final Serializable affectedOwnerId;
	private final String affectedOwnerEntityName;

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
	public AbstractCollectionEvent( CollectionPersister collectionPersister,
					PersistentCollection collection,
					EventSource source,
					Object affectedOwner,
					Serializable affectedOwnerId) {
		super(source);
		this.collection = collection;
		this.affectedOwner = affectedOwner;
		this.affectedOwnerId = affectedOwnerId;
		this.affectedOwnerEntityName =
				getAffectedOwnerEntityName( collectionPersister, affectedOwner, source );
	}

	protected static CollectionPersister getLoadedCollectionPersister( PersistentCollection collection, EventSource source ) {
		CollectionEntry ce = source.getPersistenceContext().getCollectionEntry( collection );
		return ( ce == null ? null : ce.getLoadedPersister() );		
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

	protected static String getAffectedOwnerEntityName(CollectionPersister collectionPersister, Object affectedOwner, EventSource source ) {

		// collectionPersister should not be null, but we don't want to throw
		// an exception if it is null
		String entityName =
				( collectionPersister == null ? null : collectionPersister.getOwnerEntityPersister().getEntityName() );
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
	public Serializable getAffectedOwnerIdOrNull() {
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
