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

	private static final CoreMessageLogger log = CoreLogging.messageLogger( Collections.class );

	/**
	 * record the fact that this collection was dereferenced
	 *
	 * @param collection The collection to be updated by unreachability.
	 * @param session The session
	 */
	public static void processUnreachableCollection(PersistentCollection<?> collection, SessionImplementor session) {
		if ( collection.getOwner() == null ) {
			processNeverReferencedCollection( collection, session );
		}
		else {
			processDereferencedCollection( collection, session );
		}
	}

	private static void processDereferencedCollection(PersistentCollection<?> collection, SessionImplementor session) {
		final var persistenceContext = session.getPersistenceContextInternal();
		final var entry = persistenceContext.getCollectionEntry( collection );
		final var loadedPersister = entry.getLoadedPersister();

		if ( loadedPersister != null && log.isTraceEnabled() ) {
			log.trace( "Collection dereferenced: "
						+ collectionInfoString( loadedPersister, collection, entry.getLoadedKey(), session ) );
		}

		// do a check
		if ( loadedPersister != null && loadedPersister.hasOrphanDelete() ) {
			final Object ownerId = getOwnerId( collection, session, loadedPersister );
			final var key = session.generateEntityKey( ownerId, loadedPersister.getOwnerEntityPersister() );
			final Object owner = persistenceContext.getEntity( key );
			if ( owner == null ) {
				throw new AssertionFailure( "collection owner not associated with session: " + loadedPersister.getRole() );
			}
			final var entityEntry = persistenceContext.getEntry( owner );
			//only collections belonging to deleted entities are allowed to be dereferenced in the case of orphan delete
			if ( entityEntry != null && !entityEntry.getStatus().isDeletedOrGone() ) {
				throw new HibernateException(
						"A collection with orphan deletion was no longer referenced by the owning entity instance: "
						+ loadedPersister.getRole()
				);
			}
		}

		// do the work
		entry.setCurrentPersister( null );
		entry.setCurrentKey( null );
		prepareCollectionForUpdate( collection, entry, session.getFactory() );

	}

	private static Object getOwnerId(
			PersistentCollection<?> collection,
			SessionImplementor session,
			CollectionPersister loadedPersister) {
		final Object owner = collection.getOwner();
		Object ownerId =
				loadedPersister.getOwnerEntityPersister()
						.getIdentifier( owner, session );
		if ( ownerId == null ) {
			// the owning entity may have been deleted and its identifier unset due to
			// identifier rollback; in which case, try to look up its identifier from
			// the persistence context
			if ( session.getFactory().getSessionFactoryOptions()
					.isIdentifierRollbackEnabled() ) {
				final var ownerEntry =
						session.getPersistenceContextInternal()
								.getEntry( owner );
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

	private static void processNeverReferencedCollection(PersistentCollection<?> collection, SessionImplementor session)
			throws HibernateException {
		final var entry =
				session.getPersistenceContextInternal()
						.getCollectionEntry( collection );
		final var loadedPersister = entry.getLoadedPersister();
		final Object loadedKey = entry.getLoadedKey();

		if ( log.isTraceEnabled() ) {
			log.trace( "Found collection with unloaded owner: "
						+ collectionInfoString( loadedPersister, collection, loadedKey, session ) );
		}

		entry.setCurrentPersister( loadedPersister );
		entry.setCurrentKey( loadedKey );
		prepareCollectionForUpdate( collection, entry, session.getFactory() );
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
		final var collectionEntry =
				session.getPersistenceContextInternal()
						.getCollectionEntry( collection );

		if ( collectionEntry == null ) {
			// refer to comment in StatefulPersistenceContext.addCollection()
			throw new HibernateException( "Found two representations of same collection: " + type.getRole() );
		}

		final var factory = session.getFactory();
		final var persister =
				factory.getMappingMetamodel()
						.getCollectionDescriptor( type.getRole() );

		collectionEntry.setCurrentPersister( persister );
		//TODO: better to pass the id in as an argument?
		collectionEntry.setCurrentKey( type.getKeyOfOwner( entity, session ) );

		final boolean isBytecodeEnhanced =
				persister.getOwnerEntityPersister()
						.getBytecodeEnhancementMetadata()
						.isEnhancedForLazyLoading();
		if ( isBytecodeEnhanced && !collection.wasInitialized() ) {
			// the class of the collection owner is enhanced for lazy loading,
			// and we found an un-initialized PersistentCollection, so skip it
			if ( log.isTraceEnabled() ) {
				log.trace( "Skipping uninitialized bytecode-lazy collection: "
							+ collectionInfoString( persister, collection, collectionEntry.getCurrentKey(), session ) );
			}
			collectionEntry.setReached( true );
			collectionEntry.setProcessed( true );
		}
		// The CollectionEntry.isReached() stuff is just to detect any silly users
		// who set up circular or shared references between/to collections.
		else if ( collectionEntry.isReached() ) {
			// We've been here before
			throw new HibernateException( "Found shared references to a collection: " + type.getRole() );
		}
		else {
			collectionEntry.setReached( true );
			logReachedCollection( collection, session, persister, collectionEntry );
			prepareCollectionForUpdate( collection, collectionEntry, factory );
		}
	}

	private static void logReachedCollection(
			PersistentCollection<?> collection,
			SessionImplementor session,
			CollectionPersister persister,
			CollectionEntry collectionEntry) {
		if ( log.isTraceEnabled() ) {
			if ( collection.wasInitialized() ) {
				log.tracef(
						"Collection found: %s, was: %s (initialized)",
						collectionInfoString(
								persister,
								collection,
								collectionEntry.getCurrentKey(),
								session
						),
						collectionInfoString(
								collectionEntry.getLoadedPersister(),
								collection,
								collectionEntry.getLoadedKey(),
								session
						)
				);
			}
			else {
				log.tracef(
						"Collection found: %s, was: %s (uninitialized)",
						collectionInfoString(
								persister,
								collection,
								collectionEntry.getCurrentKey(),
								session
						),
						collectionInfoString(
								collectionEntry.getLoadedPersister(),
								collection,
								collectionEntry.getLoadedKey(),
								session
						)
				);
			}
		}
	}

	/**
	 * 1. record the collection role that this collection is referenced by
	 * 2. decide if the collection needs deleting/creating/updating (but
	 *	don't actually schedule the action yet)
	 */
	private static void prepareCollectionForUpdate(
			PersistentCollection<?> collection,
			CollectionEntry collectionEntry,
			SessionFactoryImplementor factory) {
		if ( collectionEntry.isProcessed() ) {
			throw new AssertionFailure( "collection was processed twice by flush()" );
		}
		collectionEntry.setProcessed( true );

		final var loadedPersister = collectionEntry.getLoadedPersister();
		final var currentPersister = collectionEntry.getCurrentPersister();
		if ( loadedPersister != null || currentPersister != null ) {
			// it is or was referenced _somewhere_

			// if either its role changed, or its key changed
			final boolean ownerChanged =
					loadedPersister != currentPersister
						|| wasKeyChanged( collectionEntry, factory, currentPersister );

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
					collectionEntry.setDorecreate( true );
				}

				if ( loadedPersister != null ) {
					// we will need to remove the old entries
					collectionEntry.setDoremove( true );
					if ( collectionEntry.isDorecreate() ) {
						log.trace( "Forcing collection initialization" );
						collection.forceInitialization();
					}
				}
			}
			else if ( collection.isDirty() ) {
				// the collection's elements have changed
				collectionEntry.setDoupdate( true );
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
			&& !( entry.getLoadedKey() instanceof DelayedPostInsertIdentifier );
	}

	/**
	 * Determines if we can skip the explicit SQL delete statement, since
	 * the rows will be deleted by {@code on delete cascade}.
	 */
	public static boolean skipRemoval(EventSource session, CollectionPersister persister, Object key) {
		if ( persister != null
				// TODO: same optimization for @OneToMany @OnDelete(action=SET_NULL)
				&& !persister.isOneToMany() && persister.isCascadeDeleteEnabled() ) {
			final var entityKey = session.generateEntityKey( key, persister.getOwnerEntityPersister() );
			final var persistenceContext = session.getPersistenceContextInternal();
			final var entry = persistenceContext.getEntry( persistenceContext.getEntity( entityKey ) );
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
