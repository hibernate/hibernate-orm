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
package org.hibernate.event.internal;

import java.io.Serializable;
import java.util.Map;

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.action.internal.CollectionRecreateAction;
import org.hibernate.action.internal.CollectionRemoveAction;
import org.hibernate.action.internal.CollectionUpdateAction;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.internal.Cascade;
import org.hibernate.engine.internal.Collections;
import org.hibernate.engine.spi.ActionQueue;
import org.hibernate.engine.spi.CascadingAction;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.FlushEntityEvent;
import org.hibernate.event.spi.FlushEntityEventListener;
import org.hibernate.event.spi.FlushEvent;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.EntityPrinter;
import org.hibernate.internal.util.collections.IdentityMap;
import org.hibernate.internal.util.collections.LazyIterator;
import org.hibernate.persister.entity.EntityPersister;

/**
 * A convenience base class for listeners whose functionality results in flushing.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractFlushingEventListener implements Serializable {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger( CoreMessageLogger.class, AbstractFlushingEventListener.class.getName() );

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

		EventSource session = event.getSession();

		final PersistenceContext persistenceContext = session.getPersistenceContext();
		session.getInterceptor().preFlush( new LazyIterator( persistenceContext.getEntitiesByKey() ) );

		prepareEntityFlushes( session, persistenceContext );
		// we could move this inside if we wanted to
		// tolerate collection initializations during
		// collection dirty checking:
		prepareCollectionFlushes( persistenceContext );
		// now, any collections that are initialized
		// inside this block do not get updated - they
		// are ignored until the next flush

		persistenceContext.setFlushing(true);
		try {
			flushEntities( event, persistenceContext );
			flushCollections( session, persistenceContext );
		}
		finally {
			persistenceContext.setFlushing(false);
		}

		//some statistics
		logFlushResults( event );
	}

	@SuppressWarnings( value = {"unchecked"} )
	private void logFlushResults(FlushEvent event) {
		if ( !LOG.isDebugEnabled() ) {
			return;
		}
		final EventSource session = event.getSession();
		final PersistenceContext persistenceContext = session.getPersistenceContext();
		LOG.debugf(
				"Flushed: %s insertions, %s updates, %s deletions to %s objects",
				session.getActionQueue().numberOfInsertions(),
				session.getActionQueue().numberOfUpdates(),
				session.getActionQueue().numberOfDeletions(),
				persistenceContext.getEntityEntries().size()
		);
		LOG.debugf(
				"Flushed: %s (re)creations, %s updates, %s removals to %s collections",
				session.getActionQueue().numberOfCollectionCreations(),
				session.getActionQueue().numberOfCollectionUpdates(),
				session.getActionQueue().numberOfCollectionRemovals(),
				persistenceContext.getCollectionEntries().size()
		);
		new EntityPrinter( session.getFactory() ).toString(
				persistenceContext.getEntitiesByKey().entrySet()
		);
	}

	/**
	 * process cascade save/update at the start of a flush to discover
	 * any newly referenced entity that must be passed to saveOrUpdate(),
	 * and also apply orphan delete
	 */
	private void prepareEntityFlushes(EventSource session, PersistenceContext persistenceContext) throws HibernateException {

		LOG.debug( "Processing flush-time cascades" );

		final Object anything = getAnything();
		//safe from concurrent modification because of how concurrentEntries() is implemented on IdentityMap
		for ( Map.Entry me : IdentityMap.concurrentEntries( persistenceContext.getEntityEntries() ) ) {
			EntityEntry entry = (EntityEntry) me.getValue();
			Status status = entry.getStatus();
			if ( status == Status.MANAGED || status == Status.SAVING || status == Status.READ_ONLY ) {
				cascadeOnFlush( session, entry.getPersister(), me.getKey(), anything );
			}
		}
	}

	private void cascadeOnFlush(EventSource session, EntityPersister persister, Object object, Object anything)
	throws HibernateException {
		session.getPersistenceContext().incrementCascadeLevel();
		try {
			new Cascade( getCascadingAction(), Cascade.BEFORE_FLUSH, session )
			.cascade( persister, object, anything );
		}
		finally {
			session.getPersistenceContext().decrementCascadeLevel();
		}
	}

	protected Object getAnything() { return null; }

	protected CascadingAction getCascadingAction() {
		return CascadingAction.SAVE_UPDATE;
	}

	/**
	 * Initialize the flags of the CollectionEntry, including the
	 * dirty check.
	 */
	private void prepareCollectionFlushes(PersistenceContext persistenceContext) throws HibernateException {

		// Initialize dirty flags for arrays + collections with composite elements
		// and reset reached, doupdate, etc.

		LOG.debug( "Dirty checking collections" );

		for ( Map.Entry<PersistentCollection,CollectionEntry> entry :
				IdentityMap.concurrentEntries( (Map<PersistentCollection,CollectionEntry>) persistenceContext.getCollectionEntries() )) {
			entry.getValue().preFlush( entry.getKey() );
		}
	}

	/**
	 * 1. detect any dirty entities
	 * 2. schedule any entity updates
	 * 3. search out any reachable collections
	 */
	private void flushEntities(final FlushEvent event, final PersistenceContext persistenceContext) throws HibernateException {

		LOG.trace( "Flushing entities and processing referenced collections" );

		final EventSource source = event.getSession();
		final Iterable<FlushEntityEventListener> flushListeners = source
				.getFactory()
				.getServiceRegistry()
				.getService( EventListenerRegistry.class )
				.getEventListenerGroup( EventType.FLUSH_ENTITY )
				.listeners();

		// Among other things, updateReachables() will recursively load all
		// collections that are moving roles. This might cause entities to
		// be loaded.

		// So this needs to be safe from concurrent modification problems.
		// It is safe because of how IdentityMap implements entrySet()

		for ( Map.Entry me : IdentityMap.concurrentEntries( persistenceContext.getEntityEntries() ) ) {

			// Update the status of the object and if necessary, schedule an update

			EntityEntry entry = (EntityEntry) me.getValue();
			Status status = entry.getStatus();

			if ( status != Status.LOADING && status != Status.GONE ) {
				final FlushEntityEvent entityEvent = new FlushEntityEvent( source, me.getKey(), entry );
				for ( FlushEntityEventListener listener : flushListeners ) {
					listener.onFlushEntity( entityEvent );
				}
			}
		}

		source.getActionQueue().sortActions();
	}

	/**
	 * process any unreferenced collections and then inspect all known collections,
	 * scheduling creates/removes/updates
	 */
	private void flushCollections(final EventSource session, final PersistenceContext persistenceContext) throws HibernateException {

		LOG.trace( "Processing unreferenced collections" );

		for ( Map.Entry<PersistentCollection,CollectionEntry> me :
				IdentityMap.concurrentEntries( (Map<PersistentCollection,CollectionEntry>) persistenceContext.getCollectionEntries() )) {
			CollectionEntry ce = me.getValue();
			if ( !ce.isReached() && !ce.isIgnore() ) {
				Collections.processUnreachableCollection( me.getKey(), session );
			}
		}

		// Schedule updates to collections:

		LOG.trace( "Scheduling collection removes/(re)creates/updates" );

		ActionQueue actionQueue = session.getActionQueue();
		for ( Map.Entry<PersistentCollection,CollectionEntry> me :
			IdentityMap.concurrentEntries( (Map<PersistentCollection,CollectionEntry>) persistenceContext.getCollectionEntries() )) {
			PersistentCollection coll = me.getKey();
			CollectionEntry ce = me.getValue();

			if ( ce.isDorecreate() ) {
				session.getInterceptor().onCollectionRecreate( coll, ce.getCurrentKey() );
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
				session.getInterceptor().onCollectionRemove( coll, ce.getLoadedKey() );
				actionQueue.addAction(
						new CollectionRemoveAction(
								coll,
								ce.getLoadedPersister(),
								ce.getLoadedKey(),
								ce.isSnapshotEmpty(coll),
								session
							)
					);
			}
			if ( ce.isDoupdate() ) {
				session.getInterceptor().onCollectionUpdate( coll, ce.getLoadedKey() );
				actionQueue.addAction(
						new CollectionUpdateAction(
								coll,
								ce.getLoadedPersister(),
								ce.getLoadedKey(),
								ce.isSnapshotEmpty(coll),
								session
							)
					);
			}

		}

		actionQueue.sortCollectionActions();

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
		try {
			session.getTransactionCoordinator().getJdbcCoordinator().flushBeginning();
			session.getPersistenceContext().setFlushing( true );
			// we need to lock the collection caches before executing entity inserts/updates in order to
			// account for bi-directional associations
			session.getActionQueue().prepareActions();
			session.getActionQueue().executeActions();
		}
		finally {
			session.getPersistenceContext().setFlushing( false );
			session.getTransactionCoordinator().getJdbcCoordinator().flushEnding();
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

		final PersistenceContext persistenceContext = session.getPersistenceContext();
		persistenceContext.getCollectionsByKey().clear();
		persistenceContext.getBatchFetchQueue()
				.clearSubselects(); //the database has changed now, so the subselect results need to be invalidated

		for ( Map.Entry<PersistentCollection, CollectionEntry> me : IdentityMap.concurrentEntries( persistenceContext.getCollectionEntries() ) ) {
			CollectionEntry collectionEntry = me.getValue();
			PersistentCollection persistentCollection = me.getKey();
			collectionEntry.postFlush(persistentCollection);
			if ( collectionEntry.getLoadedPersister() == null ) {
				//if the collection is dereferenced, remove from the session cache
				//iter.remove(); //does not work, since the entrySet is not backed by the set
				persistenceContext.getCollectionEntries()
						.remove(persistentCollection);
			}
			else {
				//otherwise recreate the mapping between the collection and its key
				CollectionKey collectionKey = new CollectionKey(
						collectionEntry.getLoadedPersister(),
						collectionEntry.getLoadedKey()
				);
				persistenceContext.getCollectionsByKey().put(collectionKey, persistentCollection);
			}
		}

		session.getInterceptor().postFlush( new LazyIterator( persistenceContext.getEntitiesByKey() ) );

	}
}
