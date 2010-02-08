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
package org.hibernate.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.AssertionFailure;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.type.CollectionType;

import java.io.Serializable;

/**
 * Implements book-keeping for the collection persistence by reachability algorithm
 * @author Gavin King
 */
public final class Collections {

	private Collections() {}

	private static final Logger log = LoggerFactory.getLogger(Collections.class);

	/**
	 * record the fact that this collection was dereferenced
	 *
	 * @param coll The collection to be updated by unreachability.
	 * @throws HibernateException
	 */
	public static void processUnreachableCollection(PersistentCollection coll, SessionImplementor session)
	throws HibernateException {

		if ( coll.getOwner()==null ) {
			processNeverReferencedCollection(coll, session);
		}
		else {
			processDereferencedCollection(coll, session);
		}

	}

	private static void processDereferencedCollection(PersistentCollection coll, SessionImplementor session)
	throws HibernateException {

		final PersistenceContext persistenceContext = session.getPersistenceContext();
		CollectionEntry entry = persistenceContext.getCollectionEntry(coll);
		final CollectionPersister loadedPersister = entry.getLoadedPersister();

		if ( log.isDebugEnabled() && loadedPersister != null )
			log.debug(
					"Collection dereferenced: " +
					MessageHelper.collectionInfoString(
							loadedPersister,
					        entry.getLoadedKey(),
					        session.getFactory()
						)
				);

		// do a check
		boolean hasOrphanDelete = loadedPersister != null &&
		                          loadedPersister.hasOrphanDelete();
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
			EntityKey key = new EntityKey(
					ownerId,
			        loadedPersister.getOwnerEntityPersister(),
			        session.getEntityMode()
			);
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
		prepareCollectionForUpdate( coll, entry, session.getEntityMode(), session.getFactory() );

	}

	private static void processNeverReferencedCollection(PersistentCollection coll, SessionImplementor session)
	throws HibernateException {

		final PersistenceContext persistenceContext = session.getPersistenceContext();
		CollectionEntry entry = persistenceContext.getCollectionEntry(coll);

		log.debug(
				"Found collection with unloaded owner: " +
				MessageHelper.collectionInfoString(
						entry.getLoadedPersister(),
				        entry.getLoadedKey(),
				        session.getFactory()
				)
		);

		entry.setCurrentPersister( entry.getLoadedPersister() );
		entry.setCurrentKey( entry.getLoadedKey() );

		prepareCollectionForUpdate( coll, entry, session.getEntityMode(), session.getFactory() );

	}

	/**
	 * Initialize the role of the collection.
	 *
	 * @param collection The collection to be updated by reachibility.
	 * @param type The type of the collection.
	 * @param entity The owner of the collection.
	 * @throws HibernateException
	 */
	public static void processReachableCollection(
			PersistentCollection collection,
	        CollectionType type,
	        Object entity,
	        SessionImplementor session)
	throws HibernateException {

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

		if ( log.isDebugEnabled() ) {
			log.debug(
					"Collection found: " +
					MessageHelper.collectionInfoString( persister, ce.getCurrentKey(), factory ) +
					", was: " +
					MessageHelper.collectionInfoString( ce.getLoadedPersister(), ce.getLoadedKey(), factory ) +
					( collection.wasInitialized() ? " (initialized)" : " (uninitialized)" )
			);
		}

		prepareCollectionForUpdate( collection, ce, session.getEntityMode(), factory );

	}

	/**
	 * 1. record the collection role that this collection is referenced by
	 * 2. decide if the collection needs deleting/creating/updating (but
	 *	don't actually schedule the action yet)
	 */
	private static void prepareCollectionForUpdate(
			PersistentCollection collection,
	        CollectionEntry entry,
	        EntityMode entityMode,
	        SessionFactoryImplementor factory)
	throws HibernateException {

		if ( entry.isProcessed() ) {
			throw new AssertionFailure( "collection was processed twice by flush()" );
		}
		entry.setProcessed(true);

		final CollectionPersister loadedPersister = entry.getLoadedPersister();
		final CollectionPersister currentPersister = entry.getCurrentPersister();
		if ( loadedPersister != null || currentPersister != null ) {					// it is or was referenced _somewhere_

			boolean ownerChanged = loadedPersister != currentPersister ||				// if either its role changed,
			                       !currentPersister
					                       .getKeyType().isEqual(                       // or its key changed
													entry.getLoadedKey(),
			                                        entry.getCurrentKey(),
			                                        entityMode, factory
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
					entry.setDorecreate(true);											// we will need to create new entries
				}

				if ( loadedPersister != null ) {
					entry.setDoremove(true);											// we will need to remove ye olde entries
					if ( entry.isDorecreate() ) {
						log.trace( "Forcing collection initialization" );
						collection.forceInitialization();								// force initialize!
					}
				}

			}
			else if ( collection.isDirty() ) {											// else if it's elements changed
				entry.setDoupdate(true);
			}

		}

	}

}
