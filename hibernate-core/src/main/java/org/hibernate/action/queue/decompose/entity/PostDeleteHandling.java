/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.decompose.entity;

import org.hibernate.AssertionFailure;
import org.hibernate.action.internal.EntityDeleteAction;
import org.hibernate.action.queue.exec.PostExecutionCallback;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.entity.EntityPersister;

/// Post-execution callback for entity delete actions.
///
/// This handles all the finalization work that needs to happen after all table `DELETE`s
/// (or soft-delete `UPDATE`s)  for the entity  have been executed, including:
///
///   - Removing entity entry from persistence context
///   - Updating EntityEntry state (postDelete)
///   - Removing entity holder from persistence context
///   - Removing item from cache
///   - Natural ID resolution cleanup
///   - Firing POST_DELETE event listeners
///   - Updating statistics
///
/// @see EntityDeleteAction
///
/// @author Steve Ebersole
public class PostDeleteHandling implements PostExecutionCallback {
	private final EntityDeleteAction action;
	private final Object cacheKey;
	private final boolean veto;

	public PostDeleteHandling(EntityDeleteAction action, Object cacheKey, boolean veto) {
		this.action = action;
		this.cacheKey = cacheKey;
		this.veto = veto;
	}

	@Override
	public void handle(SessionImplementor session) {
		final var persister = action.getPersister();
		final Object id = action.getId();
		final Object instance = action.getInstance();

		if ( instance != null ) {
			// Loaded entity delete
			postDeleteLoaded( id, persister, session, instance );
		}
		else {
			// Unloaded proxy delete
			postDeleteUnloaded( id, persister, session );
		}

		// Update statistics (only if not vetoed by PRE_DELETE listener)
		final var statistics = session.getFactory().getStatistics();
		if ( statistics.isStatisticsEnabled() && !veto ) {
			statistics.deleteEntity( persister.getEntityName() );
		}
	}

	private void postDeleteLoaded(
			Object id,
			EntityPersister persister,
			SessionImplementor session,
			Object instance) {
		// After actually deleting a row, record the fact that the instance no longer
		// exists on the database (needed for identity-column key generation), and
		// remove it from the session cache
		final var persistenceContext = session.getPersistenceContextInternal();
		final var entry = persistenceContext.removeEntry( instance );
		if ( entry == null ) {
			throw new AssertionFailure( "possible non-threadsafe access to session" );
		}
		entry.postDelete();
		final var key = entry.getEntityKey();
		persistenceContext.removeEntityHolder( key );
		removeCacheItem( session, cacheKey );
		persistenceContext.getNaturalIdResolutions()
				.removeSharedResolution( id, action.getNaturalIdValues(), persister, true );
		action.postDelete();
	}

	private void postDeleteUnloaded(
			Object id,
			EntityPersister persister,
			SessionImplementor session) {
		final var persistenceContext = session.getPersistenceContextInternal();
		final var key = session.generateEntityKey( id, persister );
		if ( !persistenceContext.containsDeletedUnloadedEntityKey( key ) ) {
			throw new AssertionFailure( "deleted proxy should be for an unloaded entity: " + key );
		}
		persistenceContext.removeProxy( key );
		removeCacheItem( session, cacheKey );
	}

	private void removeCacheItem(SessionImplementor session, Object cacheKey) {
		final var persister = action.getPersister();
		if ( persister.canWriteToCache() ) {
			final var cache = persister.getCacheAccessStrategy();
			cache.remove( session, cacheKey );

			final var statistics = session.getFactory().getStatistics();
			if ( statistics.isStatisticsEnabled() ) {
				statistics.entityCacheRemove(
						org.hibernate.stat.internal.StatsHelper.getRootEntityRole( persister ),
						cache.getRegion().getName()
				);
			}
		}
	}
}
