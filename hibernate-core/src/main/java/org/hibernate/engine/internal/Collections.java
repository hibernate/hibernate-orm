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
package org.hibernate.engine.internal;

import java.io.Serializable;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.type.CollectionType;
import org.jboss.logging.Logger;

/**
 * Implements book-keeping for the collection persistence by reachability algorithm
 *
 * @author Gavin King
 */
public final class Collections {
    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, Collections.class.getName());

	private Collections() {
	}

	/**
	 * record the fact that this collection was dereferenced
	 *
	 * @param coll The collection to be updated by un-reachability.
	 */
	@SuppressWarnings( {"JavaDoc"})
	public static void processUnreachableCollection(PersistentCollection coll, SessionImplementor session) {
		if ( coll.getOwner()==null ) {
			processNeverReferencedCollection(coll, session);
		}
		else {
			processDereferencedCollection(coll, session);
		}
	}

	private static void processDereferencedCollection(PersistentCollection coll, SessionImplementor session) {
		final PersistenceContext persistenceContext = session.getPersistenceContext();
		CollectionEntry entry = persistenceContext.getCollectionEntry(coll);
		final CollectionPersister loadedPersister = entry.getLoadedPersister();

		if ( LOG.isDebugEnabled() && loadedPersister != null ) {
			LOG.debugf(
					"Collection dereferenced: %s",
					MessageHelper.collectionInfoString( loadedPersister, 
							coll, entry.getLoadedKey(), session
					)
			);
		}

		// do a check
		boolean hasOrphanDelete = loadedPersister != null && loadedPersister.hasOrphanDelete();
		if (hasOrphanDelete) {
			Serializable ownerId = loadedPersister.getOwnerEntityPersister().getIdentifier( coll.getOwner(), session );
			if ( ownerId == null ) {
				// the owning entity may have been deleted and its identifier unset due to
				// identifier-rollback; in which case, try to look up its identifier from
				// the persistence context
				if ( session.getFactory().getSettings().isIdentifierRollbackEnabled() ) {
					EntityEntry ownerEntry = persistenceContext.getEntry( coll.getOwner() );
					if ( ownerEntry != null ) {
						ownerId = ownerEntry.getId();
					}
				}
				if ( ownerId == null ) {
					throw new AssertionFailure( "Unable to determine collection owner identifier for orphan-delete processing" );
				}
			}
			EntityKey key = session.generateEntityKey( ownerId, loadedPersister.getOwnerEntityPersister() );
			Object owner = persistenceContext.getEntity(key);
			if ( owner == null ) {
				throw new AssertionFailure(
						"collection owner not associated with session: " +
						loadedPersister.getRole()
				);
			}
			EntityEntry e = persistenceContext.getEntry(owner);
			//only collections belonging to deleted entities are allowed to be dereferenced in the case of orphan delete
			if ( e != null && e.getStatus() != Status.DELETED && e.getStatus() != Status.GONE ) {
				throw new HibernateException(
						"A collection with cascade=\"all-delete-orphan\" was no longer referenced by the owning entity instance: " +
						loadedPersister.getRole()
				);
			}
		}

		// do the work
		entry.setCurrentPersister(null);
		entry.setCurrentKey(null);
		prepareCollectionForUpdate( coll, entry, session.getFactory() );

	}

	private static void processNeverReferencedCollection(PersistentCollection coll, SessionImplementor session)
	throws HibernateException {

		final PersistenceContext persistenceContext = session.getPersistenceContext();
		CollectionEntry entry = persistenceContext.getCollectionEntry(coll);

		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Found collection with unloaded owner: %s",
					MessageHelper.collectionInfoString( 
							entry.getLoadedPersister(), coll,
							entry.getLoadedKey(), session ) );
		}

		entry.setCurrentPersister( entry.getLoadedPersister() );
		entry.setCurrentKey( entry.getLoadedKey() );

		prepareCollectionForUpdate( coll, entry, session.getFactory() );

	}

    /**
     * Initialize the role of the collection.
     *
     * @param collection The collection to be updated by reachability.
     * @param type The type of the collection.
     * @param entity The owner of the collection.
	 * @param session The session from which this request originates
     */
	public static void processReachableCollection(
			PersistentCollection collection,
	        CollectionType type,
	        Object entity,
	        SessionImplementor session) {

		collection.setOwner(entity);

		CollectionEntry ce = session.getPersistenceContext().getCollectionEntry(collection);

		if ( ce == null ) {
			// refer to comment in StatefulPersistenceContext.addCollection()
			throw new HibernateException(
					"Found two representations of same collection: " +
					type.getRole()
			);
		}

		// The CollectionEntry.isReached() stuff is just to detect any silly users
		// who set up circular or shared references between/to collections.
		if ( ce.isReached() ) {
			// We've been here before
			throw new HibernateException(
					"Found shared references to a collection: " +
					type.getRole()
			);
		}
		ce.setReached(true);

		SessionFactoryImplementor factory = session.getFactory();
		CollectionPersister persister = factory.getCollectionPersister( type.getRole() );
		ce.setCurrentPersister(persister);
		ce.setCurrentKey( type.getKeyOfOwner(entity, session) ); //TODO: better to pass the id in as an argument?

        if (LOG.isDebugEnabled()) {
            if (collection.wasInitialized()) LOG.debugf("Collection found: %s, was: %s (initialized)",
                                                        MessageHelper.collectionInfoString(persister, collection, ce.getCurrentKey(), session),
                                                        MessageHelper.collectionInfoString(ce.getLoadedPersister(), collection, 
                                                                                           ce.getLoadedKey(),
                                                                                           session));
            else LOG.debugf("Collection found: %s, was: %s (uninitialized)",
                            MessageHelper.collectionInfoString(persister, collection, ce.getCurrentKey(), session),
                            MessageHelper.collectionInfoString(ce.getLoadedPersister(), collection, ce.getLoadedKey(), session));
        }

		prepareCollectionForUpdate( collection, ce, factory );

	}

	/**
	 * 1. record the collection role that this collection is referenced by
	 * 2. decide if the collection needs deleting/creating/updating (but
	 *	don't actually schedule the action yet)
	 */
	@SuppressWarnings( {"JavaDoc"})
	private static void prepareCollectionForUpdate(
			PersistentCollection collection,
	        CollectionEntry entry,
	        SessionFactoryImplementor factory) {

		if ( entry.isProcessed() ) {
			throw new AssertionFailure( "collection was processed twice by flush()" );
		}
		entry.setProcessed( true );

		final CollectionPersister loadedPersister = entry.getLoadedPersister();
		final CollectionPersister currentPersister = entry.getCurrentPersister();
		if ( loadedPersister != null || currentPersister != null ) {					// it is or was referenced _somewhere_

			boolean ownerChanged = loadedPersister != currentPersister ||				// if either its role changed,
			                       !currentPersister
					                       .getKeyType().isEqual(                       // or its key changed
													entry.getLoadedKey(),
			                                        entry.getCurrentKey(),
			                                        factory
			                       );

			if (ownerChanged) {

				// do a check
				final boolean orphanDeleteAndRoleChanged = loadedPersister != null &&
				                                           currentPersister != null &&
				                                           loadedPersister.hasOrphanDelete();

				if (orphanDeleteAndRoleChanged) {
					throw new HibernateException(
							"Don't change the reference to a collection with cascade=\"all-delete-orphan\": " +
							loadedPersister.getRole()
					);
				}

				// do the work
				if ( currentPersister != null ) {
					entry.setDorecreate( true );	// we will need to create new entries
				}

				if ( loadedPersister != null ) {
					entry.setDoremove( true );		// we will need to remove ye olde entries
					if ( entry.isDorecreate() ) {
						LOG.trace( "Forcing collection initialization" );
						collection.forceInitialization();
					}
				}
			}
			else if ( collection.isDirty() ) {
				// the collection's elements have changed
				entry.setDoupdate( true );
			}

		}

	}
}
