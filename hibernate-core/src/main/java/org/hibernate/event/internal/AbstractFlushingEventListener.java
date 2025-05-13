/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.internal;

import java.lang.invoke.MethodHandles;

import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.action.internal.CollectionRecreateAction;
import org.hibernate.action.internal.CollectionRemoveAction;
import org.hibernate.action.internal.CollectionUpdateAction;
import org.hibernate.action.internal.QueuedOperationCollectionAction;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.internal.Cascade;
import org.hibernate.engine.internal.CascadePoint;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.spi.ActionQueue;
import org.hibernate.engine.spi.CascadingActions;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.FlushEntityEvent;
import org.hibernate.event.spi.FlushEntityEventListener;
import org.hibernate.event.spi.FlushEvent;
import org.hibernate.event.spi.PersistContext;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.EntityPrinter;
import org.hibernate.internal.util.collections.InstanceIdentityMap;
import org.hibernate.persister.entity.EntityPersister;

import org.jboss.logging.Logger;

import static org.hibernate.engine.internal.Collections.processUnreachableCollection;
import static org.hibernate.engine.internal.Collections.skipRemoval;

/**
 * A convenience base class for listeners whose functionality results in flushing.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractFlushingEventListener {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger( MethodHandles.lookup(), CoreMessageLogger.class, AbstractFlushingEventListener.class.getName() );

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Pre-flushing section
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Coordinates the processing necessary to get things ready for executions
	 * as db calls by preparing the session caches and moving the appropriate
	 * entities and collections to their respective execution queues.
	 *
	 * @param event The flush event.
	 * @throws HibernateException Error flushing caches to execution queues.
	 */
	protected void flushEverythingToExecutions(FlushEvent event) throws HibernateException {
		LOG.trace( "Flushing session" );
		final EventSource session = event.getSession();
		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
		preFlush( session, persistenceContext );
		flushEverythingToExecutions( event, persistenceContext, session );
	}

	protected void flushEverythingToExecutions(FlushEvent event, PersistenceContext persistenceContext, EventSource session) {
		persistenceContext.setFlushing( true );
		try {
			final int entityCount = flushEntities( event, persistenceContext );
			final int collectionCount = flushCollections( session, persistenceContext );
			event.setNumberOfEntitiesProcessed( entityCount );
			event.setNumberOfCollectionsProcessed( collectionCount );
		}
		finally {
			persistenceContext.setFlushing( false);
		}
		//some statistics
		logFlushResults( event );
	}

	protected void preFlush(EventSource session, PersistenceContext persistenceContext) {
		session.getInterceptor().preFlush( persistenceContext.managedEntitiesIterator() );
		prepareEntityFlushes( session, persistenceContext );
		// we could move this inside if we wanted to
		// tolerate collection initializations during
		// collection dirty checking:
		prepareCollectionFlushes( persistenceContext );
		// now, any collections that are initialized
		// inside this block do not get updated - they
		// are ignored until the next flush
	}

	protected void logFlushResults(FlushEvent event) {
		if ( LOG.isDebugEnabled() ) {
			final EventSource session = event.getSession();
			final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
			final ActionQueue actionQueue = session.getActionQueue();
			LOG.debugf(
					"Flushed: %s insertions, %s updates, %s deletions to %s objects",
					actionQueue.numberOfInsertions(),
					actionQueue.numberOfUpdates(),
					actionQueue.numberOfDeletions(),
					persistenceContext.getNumberOfManagedEntities()
			);
			LOG.debugf(
					"Flushed: %s (re)creations, %s updates, %s removals to %s collections",
					actionQueue.numberOfCollectionCreations(),
					actionQueue.numberOfCollectionUpdates(),
					actionQueue.numberOfCollectionRemovals(),
					persistenceContext.getCollectionEntriesSize()
			);
			new EntityPrinter( session.getFactory() )
					.logEntities( persistenceContext.getEntityHoldersByKey().entrySet() );
		}
	}

	/**
	 * process cascade save/update at the start of a flush to discover
	 * any newly referenced entity that must be passed to saveOrUpdate(),
	 * and also apply orphan delete
	 */
	private void prepareEntityFlushes(EventSource session, PersistenceContext persistenceContext)
			throws HibernateException {
		LOG.debug( "Processing flush-time cascades" );
		final PersistContext context = PersistContext.create();
		// safe from concurrent modification because of how concurrentEntries() is implemented on IdentityMap
		for ( var me : persistenceContext.reentrantSafeEntityEntries() ) {
//		for ( Map.Entry me : IdentityMap.concurrentEntries( persistenceContext.getEntityEntries() ) ) {
			final EntityEntry entry = me.getValue();
			if ( flushable( entry ) ) {
				cascadeOnFlush( session, entry.getPersister(), me.getKey(), context );
			}
		}
		checkForTransientReferences( session, persistenceContext );
	}

	void checkForTransientReferences(EventSource session, PersistenceContext persistenceContext) {
		// perform these checks after all cascade persist events have been
		// processed, so that all entities which will be persisted are
		// persistent when we do the check (I wonder if we could move this
		// into Nullability, instead of abusing the Cascade infrastructure)
		for ( var me : persistenceContext.reentrantSafeEntityEntries() ) {
			final EntityEntry entry = me.getValue();
			if ( checkable( entry ) ) {
				Cascade.cascade(
						CascadingActions.CHECK_ON_FLUSH,
						CascadePoint.BEFORE_FLUSH,
						session,
						entry.getPersister(),
						me.getKey(),
						null
				);
			}
		}
	}

	private static boolean flushable(EntityEntry entry) {
		final Status status = entry.getStatus();
		return status == Status.MANAGED
			|| status == Status.SAVING
			|| status == Status.READ_ONLY; // debatable, see HHH-19398
	}

	private static boolean checkable(EntityEntry entry) {
		final Status status = entry.getStatus();
		return status == Status.MANAGED
			|| status == Status.SAVING;
	}

	private void cascadeOnFlush(EventSource session, EntityPersister persister, Object object, PersistContext anything)
			throws HibernateException {
		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
		persistenceContext.incrementCascadeLevel();
		try {
			Cascade.cascade( CascadingActions.PERSIST_ON_FLUSH, CascadePoint.BEFORE_FLUSH, session, persister, object, anything );
		}
		finally {
			persistenceContext.decrementCascadeLevel();
		}
	}

	/**
	 * Initialize the flags of the {@link CollectionEntry}, including the dirty check.
	 */
	private void prepareCollectionFlushes(PersistenceContext persistenceContext) throws HibernateException {
		// Initialize dirty flags for arrays + collections with composite elements
		// and reset reached, doupdate, etc.
		LOG.debug( "Dirty checking collections" );
		final var collectionEntries = persistenceContext.getCollectionEntries();
		if ( collectionEntries != null ) {
			final var identityMap = (InstanceIdentityMap<PersistentCollection<?>, CollectionEntry>) collectionEntries;
			for ( var entry : identityMap.toArray() ) {
				entry.getValue().preFlush( entry.getKey() );
			}
		}
	}

	/**
	 * <ol>
	 * <li> detect any dirty entities
	 * <li> schedule any entity updates
	 * <li> search out any reachable collections
	 * </ol>
	 */
	private int flushEntities(final FlushEvent event, final PersistenceContext persistenceContext)
			throws HibernateException {
		LOG.trace( "Flushing entities and processing referenced collections" );

		final EventSource source = event.getSession();
		final EventListenerGroup<FlushEntityEventListener> flushListeners =
				event.getFactory().getEventListenerGroups().eventListenerGroup_FLUSH_ENTITY;

		// Among other things, updateReachables() recursively loads all
		// collections that are changing roles. This might cause entities
		// to be loaded.
		// So this needs to be safe from concurrent modification problems.
		final var entityEntries = persistenceContext.reentrantSafeEntityEntries();
		final int count = entityEntries.length;

		FlushEntityEvent entityEvent = null; //allow reuse of the event as it's heavily allocated in certain use cases
		int eventGenerationId = 0; //Used to double-check the instance reuse won't cause problems
		for ( var me : entityEntries ) {
			// Update the status of the object and if necessary, schedule an update
			final EntityEntry entry = me.getValue();
			final Status status = entry.getStatus();
			if ( status != Status.LOADING && status != Status.GONE ) {
				entityEvent = createOrReuseEventInstance( entityEvent, source, me.getKey(), entry );
				entityEvent.setInstanceGenerationId( ++eventGenerationId );
				flushListeners.fireEventOnEachListener( entityEvent, FlushEntityEventListener::onFlushEntity );
				entityEvent.setAllowedToReuse( true );
				assert entityEvent.getInstanceGenerationId() == eventGenerationId;
			}
		}

		source.getActionQueue().sortActions();

		return count;
	}

	/**
	 * Reuses a {@link FlushEntityEvent} for a new purpose, if possible;
	 * or if not possible, a new actual instance is returned.
	 */
	private FlushEntityEvent createOrReuseEventInstance(
			FlushEntityEvent possiblyValidExistingInstance,
			EventSource source,
			Object key,
			EntityEntry entry) {
		if ( possiblyValidExistingInstance == null || !possiblyValidExistingInstance.isAllowedToReuse() ) {
			//need to create a new instance
			return new FlushEntityEvent( source, key, entry );
		}
		else {
			possiblyValidExistingInstance.resetAndReuseEventInstance( key, entry );
			return possiblyValidExistingInstance;
		}
	}

	/**
	 * Process any unreferenced collections and then inspect all known collections,
	 * scheduling creates/removes/updates.
	 */
	private int flushCollections(final EventSource session, final PersistenceContext persistenceContext)
			throws HibernateException {
		LOG.trace( "Processing unreferenced collections" );
		final var collectionEntries = persistenceContext.getCollectionEntries();
		final int count;
		if ( collectionEntries == null ) {
			count = 0;
		}
		else {
			count = collectionEntries.size();
			final var identityMap = (InstanceIdentityMap<PersistentCollection<?>, CollectionEntry>) collectionEntries;
			for ( var me : identityMap.toArray() ) {
				final CollectionEntry ce = me.getValue();
				if ( !ce.isReached() && !ce.isIgnore() ) {
					processUnreachableCollection( me.getKey(), session );
				}
			}
		}

		// Schedule updates to collections:

		LOG.trace( "Scheduling collection removes/(re)creates/updates" );
		final ActionQueue actionQueue = session.getActionQueue();
		final Interceptor interceptor = session.getInterceptor();
		persistenceContext.forEachCollectionEntry(
				(coll, ce) -> {
					if ( ce.isDorecreate() ) {
						interceptor.onCollectionRecreate( coll, ce.getCurrentKey() );
						actionQueue.addAction(
								new CollectionRecreateAction(
										coll,
										ce.getCurrentPersister(),
										ce.getCurrentKey(),
										session
								)
						);
					}
					if ( ce.isDoremove() ) {
						interceptor.onCollectionRemove( coll, ce.getLoadedKey() );
						if ( !skipRemoval( session, ce.getLoadedPersister(), ce.getLoadedKey() ) ) {
							actionQueue.addAction(
									new CollectionRemoveAction(
											coll,
											ce.getLoadedPersister(),
											ce.getLoadedKey(),
											ce.isSnapshotEmpty( coll ),
											session
									)
							);
						}
					}
					if ( ce.isDoupdate() ) {
						interceptor.onCollectionUpdate( coll, ce.getLoadedKey() );
						actionQueue.addAction(
								new CollectionUpdateAction(
										coll,
										ce.getLoadedPersister(),
										ce.getLoadedKey(),
										ce.isSnapshotEmpty( coll ),
										session
								)
						);
					}
					// todo : I'm not sure the !wasInitialized part should really be part of this check
					if ( !coll.wasInitialized() && coll.hasQueuedOperations() ) {
						actionQueue.addAction(
								new QueuedOperationCollectionAction(
										coll,
										ce.getLoadedPersister(),
										ce.getLoadedKey(),
										session
								)
						);
					}
				}, true );

		actionQueue.sortCollectionActions();

		return count;
	}

	/**
	 * Execute all SQL (and second-level cache updates) in a special order so that foreign-key constraints cannot
	 * be violated: <ol>
	 * <li> Inserts, in the order they were performed
	 * <li> Updates
	 * <li> Deletion of collection elements
	 * <li> Insertion of collection elements
	 * <li> Deletes, in the order they were performed
	 * </ol>
	 *
	 * @param session The session being flushed
	 */
	protected void performExecutions(EventSource session) {
		LOG.trace( "Executing flush" );

		// IMPL NOTE : here we alter the flushing flag of the persistence context to allow
		//		during-flush callbacks more leniency in regards to initializing proxies and
		//		lazy collections during their processing.
		// For more information, see HHH-2763
		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
		final JdbcCoordinator jdbcCoordinator = session.getJdbcCoordinator();
		try {
			jdbcCoordinator.flushBeginning();
			persistenceContext.setFlushing( true );
			// we need to lock the collection caches before executing entity inserts/updates
			// in order to account for bidirectional associations
			final ActionQueue actionQueue = session.getActionQueue();
			actionQueue.prepareActions();
			actionQueue.executeActions();
		}
		finally {
			persistenceContext.setFlushing( false );
			jdbcCoordinator.flushEnding();
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Post-flushing section
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * <ol>
	 * <li> Recreate the collection key to collection mapping
	 * <li> rebuild the collection entries
	 * <li> call {@link Interceptor#postFlush}
	 * </ol>
	 */
	protected void postFlush(SessionImplementor session) throws HibernateException {
		LOG.trace( "Post flush" );

		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
		persistenceContext.clearCollectionsByKey();

		// the database has changed now, so the subselect results need to be invalidated
		// the batch fetching queues should also be cleared - especially the collection batch fetching one
		persistenceContext.getBatchFetchQueue().clear();

		persistenceContext.forEachCollectionEntry(
				(persistentCollection, collectionEntry) -> {
					collectionEntry.postFlush( persistentCollection );
					final Object key;
					if ( collectionEntry.getLoadedPersister() == null || ( key = collectionEntry.getLoadedKey() ) == null ) {
						//if the collection is dereferenced, unset its session reference and remove from the session cache
						//iter.remove(); //does not work, since the entrySet is not backed by the set
						persistentCollection.unsetSession( session );
						persistenceContext.removeCollectionEntry( persistentCollection );
					}
					else {
						//otherwise recreate the mapping between the collection and its key
						final CollectionKey collectionKey =
								new CollectionKey( collectionEntry.getLoadedPersister(), key );
						persistenceContext.addCollectionByKey( collectionKey, persistentCollection );
					}
				},
				true
		);
	}

	protected void postPostFlush(SessionImplementor session) {
		session.getInterceptor().postFlush( session.getPersistenceContextInternal().managedEntitiesIterator() );
	}

}
