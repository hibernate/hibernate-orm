/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.internal;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.action.internal.DelayedPostInsertIdentifier;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.CollectionType;

import static org.hibernate.pretty.MessageHelper.collectionInfoString;

/**
 * Implements book-keeping for the collection persistence by reachability algorithm
 *
 * @author Gavin King
 */
public final class Collections {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( Collections.class );

	/**
	 * record the fact that this collection was dereferenced
	 *
	 * @param coll The collection to be updated by un-reachability.
	 * @param session The session
	 */
	public static void processUnreachableCollection(PersistentCollection<?> coll, SessionImplementor session) {
		if ( coll.getOwner() == null ) {
			processNeverReferencedCollection( coll, session );
		}
		else {
			processDereferencedCollection( coll, session );
		}
	}

	private static void processDereferencedCollection(PersistentCollection<?> coll, SessionImplementor session) {
		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
		final CollectionEntry entry = persistenceContext.getCollectionEntry( coll );
		final CollectionPersister loadedPersister = entry.getLoadedPersister();

		if ( loadedPersister != null && LOG.isTraceEnabled() ) {
			LOG.trace("Collection dereferenced: "
					+ collectionInfoString( loadedPersister, coll, entry.getLoadedKey(), session ) );
		}

		// do a check
		final boolean hasOrphanDelete = loadedPersister != null && loadedPersister.hasOrphanDelete();
		if ( hasOrphanDelete ) {
			final Object ownerId = getOwnerId( coll, session, loadedPersister );
			final EntityKey key = session.generateEntityKey( ownerId, loadedPersister.getOwnerEntityPersister() );
			final Object owner = persistenceContext.getEntity( key );
			if ( owner == null ) {
				throw new AssertionFailure( "collection owner not associated with session: " + loadedPersister.getRole() );
			}
			final EntityEntry e = persistenceContext.getEntry( owner );
			//only collections belonging to deleted entities are allowed to be dereferenced in the case of orphan delete
			if ( e != null && !e.getStatus().isDeletedOrGone() ) {
				throw new HibernateException(
						"A collection with orphan deletion was no longer referenced by the owning entity instance: "
						+ loadedPersister.getRole()
				);
			}
		}

		// do the work
		entry.setCurrentPersister( null );
		entry.setCurrentKey( null );
		prepareCollectionForUpdate( coll, entry, session.getFactory() );

	}

	private static Object getOwnerId(
			PersistentCollection<?> coll,
			SessionImplementor session,
			CollectionPersister loadedPersister) {

		Object ownerId =
				loadedPersister.getOwnerEntityPersister()
						.getIdentifier( coll.getOwner(), session );
		if ( ownerId == null ) {
			// the owning entity may have been deleted and its identifier unset due to
			// identifier-rollback; in which case, try to look up its identifier from
			// the persistence context
			if ( session.getFactory().getSessionFactoryOptions()
					.isIdentifierRollbackEnabled() ) {
				final EntityEntry ownerEntry =
						session.getPersistenceContextInternal()
								.getEntry( coll.getOwner() );
				if ( ownerEntry != null ) {
					ownerId = ownerEntry.getId();
				}
			}
			if ( ownerId == null ) {
				throw new AssertionFailure( "Unable to determine collection owner identifier for orphan delete processing" );
			}
		}
		return ownerId;
	}

	private static void processNeverReferencedCollection(PersistentCollection<?> coll, SessionImplementor session)
			throws HibernateException {
		final CollectionEntry entry =
				session.getPersistenceContextInternal()
						.getCollectionEntry( coll );

		if ( LOG.isTraceEnabled() ) {
			LOG.trace( "Found collection with unloaded owner: " +
					collectionInfoString( entry.getLoadedPersister(), coll, entry.getLoadedKey(), session ) );
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
			PersistentCollection<?> collection,
			CollectionType type,
			Object entity,
			SessionImplementor session) {
		collection.setOwner( entity );
		final CollectionEntry ce =
				session.getPersistenceContextInternal()
						.getCollectionEntry( collection );

		if ( ce == null ) {
			// refer to comment in StatefulPersistenceContext.addCollection()
			throw new HibernateException( "Found two representations of same collection: " + type.getRole() );
		}

		final SessionFactoryImplementor factory = session.getFactory();
		final CollectionPersister persister =
				factory.getMappingMetamodel()
						.getCollectionDescriptor( type.getRole() );

		ce.setCurrentPersister( persister );
		//TODO: better to pass the id in as an argument?
		ce.setCurrentKey( type.getKeyOfOwner( entity, session ) );

		final boolean isBytecodeEnhanced =
				persister.getOwnerEntityPersister()
						.getBytecodeEnhancementMetadata()
						.isEnhancedForLazyLoading();
		if ( isBytecodeEnhanced && !collection.wasInitialized() ) {
			// the class of the collection owner is enhanced for lazy loading and we found an un-initialized PersistentCollection
			// 		- skip it
			if ( LOG.isTraceEnabled() ) {
				LOG.trace( "Skipping uninitialized bytecode-lazy collection: "
						+ collectionInfoString( persister, collection, ce.getCurrentKey(), session ) );
			}
			ce.setReached( true );
			ce.setProcessed( true );
			return;
		}

		// The CollectionEntry.isReached() stuff is just to detect any silly users
		// who set up circular or shared references between/to collections.
		if ( ce.isReached() ) {
			// We've been here before
			throw new HibernateException( "Found shared references to a collection: " + type.getRole() );
		}

		ce.setReached( true );

		if ( LOG.isTraceEnabled() ) {
			if ( collection.wasInitialized() ) {
				LOG.tracef(
						"Collection found: %s, was: %s (initialized)",
						collectionInfoString(
								persister,
								collection,
								ce.getCurrentKey(),
								session
						),
						collectionInfoString(
								ce.getLoadedPersister(),
								collection,
								ce.getLoadedKey(),
								session
						)
				);
			}
			else {
				LOG.tracef(
						"Collection found: %s, was: %s (uninitialized)",
						collectionInfoString(
								persister,
								collection,
								ce.getCurrentKey(),
								session
						),
						collectionInfoString(
								ce.getLoadedPersister(),
								collection,
								ce.getLoadedKey(),
								session
						)
				);
			}
		}

		prepareCollectionForUpdate( collection, ce, factory );
	}

	/**
	 * 1. record the collection role that this collection is referenced by
	 * 2. decide if the collection needs deleting/creating/updating (but
	 *	don't actually schedule the action yet)
	 */
	private static void prepareCollectionForUpdate(
			PersistentCollection<?> collection,
			CollectionEntry entry,
			SessionFactoryImplementor factory) {
		if ( entry.isProcessed() ) {
			throw new AssertionFailure( "collection was processed twice by flush()" );
		}
		entry.setProcessed( true );

		final CollectionPersister loadedPersister = entry.getLoadedPersister();
		final CollectionPersister currentPersister = entry.getCurrentPersister();
		if ( loadedPersister != null || currentPersister != null ) {
			// it is or was referenced _somewhere_


			// if either its role changed, or its key changed
			final boolean ownerChanged =
					loadedPersister != currentPersister
						|| wasKeyChanged( entry, factory, currentPersister );

			if ( ownerChanged ) {
				// do a check
				final boolean orphanDeleteAndRoleChanged =
						loadedPersister != null && currentPersister != null && loadedPersister.hasOrphanDelete();

				if ( orphanDeleteAndRoleChanged ) {
					throw new HibernateException(
							"Don't change the reference to a collection with delete orphan enabled: "
							+ loadedPersister.getRole()
					);
				}

				// do the work
				if ( currentPersister != null ) {
					entry.setDorecreate( true );
				}

				if ( loadedPersister != null ) {
					// we will need to remove the old entries
					entry.setDoremove( true );
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

	/**
	 * Check if the key changed.
	 * Excludes marking key changed when the loaded key is a {@code DelayedPostInsertIdentifier}.
	 */
	private static boolean wasKeyChanged(
			CollectionEntry entry, SessionFactoryImplementor factory, CollectionPersister currentPersister) {
		return currentPersister != null
			&& !currentPersister.getKeyType().isEqual( entry.getLoadedKey(), entry.getCurrentKey(), factory )
			&& !(entry.getLoadedKey() instanceof DelayedPostInsertIdentifier);
	}

	/**
	 * Determines if we can skip the explicit SQL delete statement, since
	 * the rows will be deleted by {@code on delete cascade}.
	 */
	public static boolean skipRemoval(EventSource session, CollectionPersister persister, Object key) {
		if ( persister != null
				// TODO: same optimization for @OneToMany @OnDelete(action=SET_NULL)
				&& !persister.isOneToMany() && persister.isCascadeDeleteEnabled() ) {
			final EntityKey entityKey = session.generateEntityKey( key, persister.getOwnerEntityPersister() );
			final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
			final EntityEntry entry = persistenceContext.getEntry( persistenceContext.getEntity( entityKey ) );
			return entry == null || entry.getStatus().isDeletedOrGone();
		}
		else {
			return false;
		}
	}

	/**
	 * Disallow instantiation
	 */
	private Collections() {
	}

}
