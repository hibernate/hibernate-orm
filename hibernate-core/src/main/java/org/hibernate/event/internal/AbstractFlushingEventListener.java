/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.action.internal.CollectionRecreateAction;
import org.hibernate.action.internal.CollectionRemoveAction;
import org.hibernate.action.internal.CollectionUpdateAction;
import org.hibernate.action.internal.QueuedOperationCollectionAction;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.internal.Cascade;
import org.hibernate.engine.internal.CascadePoint;
import org.hibernate.engine.internal.Collections;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.spi.ActionQueue;
import org.hibernate.engine.spi.CascadingAction;
import org.hibernate.engine.spi.CascadingActions;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.service.spi.JpaBootstrapSensitive;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.FlushEntityEvent;
import org.hibernate.event.spi.FlushEntityEventListener;
import org.hibernate.event.spi.FlushEvent;
import org.hibernate.event.spi.PersistContext;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.EntityPrinter;
import org.hibernate.internal.util.collections.IdentityMap;
import org.hibernate.persister.entity.EntityPersister;

import org.jboss.logging.Logger;

import static org.hibernate.engine.internal.Collections.skipRemoval;

/**
 * A convenience base class for listeners whose functionality results in flushing.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractFlushingEventListener implements JpaBootstrapSensitive {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger( CoreMessageLogger.class, AbstractFlushingEventListener.class.getName() );

	private boolean jpaBootstrap;

	@Override
	public void wasJpaBootstrap(boolean wasJpaBootstrap) {
		this.jpaBootstrap = wasJpaBootstrap;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Pre-flushing section
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Coordinates the processing necessary to get things ready for executions
	 * as db calls by preping the session caches and moving the appropriate
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
			int entityCount = flushEntities( event, persistenceContext );
			int collectionCount = flushCollections( session, persistenceContext );

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
		if ( !LOG.isDebugEnabled() ) {
			return;
		}
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
		new EntityPrinter( session.getFactory() ).toString(
				persistenceContext.getEntityHoldersByKey().entrySet()
		);
	}

	/**
	 * process cascade save/update at the start of a flush to discover
	 * any newly referenced entity that must be passed to saveOrUpdate(),
	 * and also apply orphan delete
	 */
	private void prepareEntityFlushes(EventSource session, PersistenceContext persistenceContext) throws HibernateException {

		LOG.debug( "Processing flush-time cascades" );

		final PersistContext context = getContext( session );
		//safe from concurrent modification because of how concurrentEntries() is implemented on IdentityMap
		for ( Map.Entry<Object,EntityEntry> me : persistenceContext.reentrantSafeEntityEntries() ) {
//		for ( Map.Entry me : IdentityMap.concurrentEntries( persistenceContext.getEntityEntries() ) ) {
			final EntityEntry entry = me.getValue();
			if ( flushable( entry ) ) {
				cascadeOnFlush( session, entry.getPersister(), me.getKey(), context );
			}
		}

		// perform these checks after all cascade persist events have been
		// processed, so that all entities which will be persisted are
		// persistent when we do the check (I wonder if we could move this
		// into Nullability, instead of abusing the Cascade infrastructure)
		for ( Map.Entry<Object, EntityEntry> me : persistenceContext.reentrantSafeEntityEntries() ) {
			final EntityEntry entry = me.getValue();
			if ( flushable( entry ) ) {
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
			|| status == Status.READ_ONLY;
	}

	private void cascadeOnFlush(EventSource session, EntityPersister persister, Object object, PersistContext anything)
			throws HibernateException {
		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
		persistenceContext.incrementCascadeLevel();
		try {
			Cascade.cascade( getCascadingAction(session), CascadePoint.BEFORE_FLUSH, session, persister, object, anything );
		}
		finally {
			persistenceContext.decrementCascadeLevel();
		}
	}

	protected PersistContext getContext(EventSource session) {
		return jpaBootstrap || isJpaCascadeComplianceEnabled( session ) ? PersistContext.create() : null;
	}

	protected CascadingAction<PersistContext> getCascadingAction(EventSource session) {
		return jpaBootstrap || isJpaCascadeComplianceEnabled( session )
				? CascadingActions.PERSIST_ON_FLUSH
				: CascadingActions.SAVE_UPDATE;
	}

	private static boolean isJpaCascadeComplianceEnabled(EventSource session) {
		return session.getSessionFactory().getSessionFactoryOptions().getJpaCompliance().isJpaCascadeComplianceEnabled();
	}

	/**
	 * Initialize the flags of the CollectionEntry, including the
	 * dirty check.
	 */
	private void prepareCollectionFlushes(PersistenceContext persistenceContext) throws HibernateException {

		// Initialize dirty flags for arrays + collections with composite elements
		// and reset reached, doupdate, etc.

		LOG.debug( "Dirty checking collections" );
		final Map<PersistentCollection<?>, CollectionEntry> collectionEntries = persistenceContext.getCollectionEntries();
		if ( collectionEntries != null ) {
			for ( Map.Entry<PersistentCollection<?>, CollectionEntry> entry : ( (IdentityMap<PersistentCollection<?>, CollectionEntry>) collectionEntries ).entryArray() ) {
				entry.getValue().preFlush( entry.getKey() );
			}
		}
	}

	/**
	 * 1. detect any dirty entities
	 * 2. schedule any entity updates
	 * 3. search out any reachable collections
	 */
	private int flushEntities(final FlushEvent event, final PersistenceContext persistenceContext)
			throws HibernateException {

		LOG.trace( "Flushing entities and processing referenced collections" );

		final EventSource source = event.getSession();
		final EventListenerGroup<FlushEntityEventListener> flushListeners =
				event.getFactory().getFastSessionServices().eventListenerGroup_FLUSH_ENTITY;

		// Among other things, updateReachables() will recursively load all
		// collections that are moving roles. This might cause entities to
		// be loaded.

		// So this needs to be safe from concurrent modification problems.

		final Map.Entry<Object,EntityEntry>[] entityEntries = persistenceContext.reentrantSafeEntityEntries();
		final int count = entityEntries.length;

		FlushEntityEvent entityEvent = null; //allow reuse of the event as it's heavily allocated in certain use cases
		int eventGenerationId = 0; //Used to double-check the instance reuse won't cause problems

		for ( Map.Entry<Object,EntityEntry> me : entityEntries ) {
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
	 * Reuses a FlushEntityEvent for a new purpose, if possible;
	 * if not possible a new actual instance is returned.
	 */
	private FlushEntityEvent createOrReuseEventInstance(
			FlushEntityEvent possiblyValidExistingInstance,
			EventSource source,
			Object key,
			EntityEntry entry) {
		final FlushEntityEvent entityEvent = possiblyValidExistingInstance;
		if ( entityEvent == null || !entityEvent.isAllowedToReuse() ) {
			//need to create a new instance
			return new FlushEntityEvent( source, key, entry );
		}
		else {
			entityEvent.resetAndReuseEventInstance( key, entry );
			return entityEvent;
		}
	}

	/**
	 * process any unreferenced collections and then inspect all known collections,
	 * scheduling creates/removes/updates
	 */
	private int flushCollections(final EventSource session, final PersistenceContext persistenceContext)
			throws HibernateException {
		LOG.trace( "Processing unreferenced collections" );

		final Map<PersistentCollection<?>, CollectionEntry> collectionEntries = persistenceContext.getCollectionEntries();
		final int count;
		if ( collectionEntries == null ) {
			count = 0;
		}
		else {
			count = collectionEntries.size();
			for ( Map.Entry<PersistentCollection<?>, CollectionEntry> me : ( (IdentityMap<PersistentCollection<?>, CollectionEntry>) collectionEntries ).entryArray() ) {
				final CollectionEntry ce = me.getValue();
				if ( !ce.isReached() && !ce.isIgnore() ) {
					Collections.processUnreachableCollection( me.getKey(), session );
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
	 * 1. Recreate the collection key -> collection map
	 * 2. rebuild the collection entries
	 * 3. call Interceptor.postFlush()
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
						final CollectionKey collectionKey = new CollectionKey(
								collectionEntry.getLoadedPersister(),
								key
						);
						persistenceContext.addCollectionByKey( collectionKey, persistentCollection );
					}
				}, true
		);
	}

	protected void postPostFlush(SessionImplementor session) {
		session.getInterceptor().postFlush( session.getPersistenceContextInternal().managedEntitiesIterator() );
	}

}
