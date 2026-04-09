/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.decompose.entity;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.AssertionFailure;
import org.hibernate.action.internal.EntityUpdateAction;
import org.hibernate.action.queue.exec.GeneratedValuesCollector;
import org.hibernate.action.queue.exec.PostExecutionCallback;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.generator.values.GeneratedValues;

/// Post-execution callback for entity update actions.
///
/// This handles all the finalization work that needs to happen after
/// all table `UPDATE`s for the entity have been executed, including:
///
///   - Processing database-generated values (timestamps, etc.)
///   - Setting application-generated values (version increments)
///   - Updating EntityEntry state with new version
///   - Handling deleted entities
///   - Updating cache
///   - Natural ID shared resolutions
///   - Firing POST_UPDATE event listeners
///   - Updating statistics
///
/// @see EntityUpdateAction
/// @see GeneratedValuesCollector
///
/// @author Steve Ebersole
public class PostUpdateHandling implements PostExecutionCallback {
	private final EntityUpdateAction action;
	private final Object cacheKey;
	private final Object previousVersion;
	private final GeneratedValuesCollector generatedValuesCollector;

	public PostUpdateHandling(
			EntityUpdateAction action,
			Object cacheKey,
			Object previousVersion,
			@Nullable GeneratedValuesCollector generatedValuesCollector) {
		this.action = action;
		this.cacheKey = cacheKey;
		this.previousVersion = previousVersion;
		this.generatedValuesCollector = generatedValuesCollector;
	}

	@Override
	public void handle(SessionImplementor session) {
		final var persistenceContext = session.getPersistenceContextInternal();
		final var entry = persistenceContext.getEntry( action.getInstance() );
		if ( entry == null ) {
			throw new AssertionFailure( "possible non-threadsafe access to session : could not locate entry after update" );
		}

		if ( action.getPersister().isMutable() ) {
			handleMutableEntity( entry, session );
		}
		else {
			handleImmutableEntity( entry, session );
		}
	}

	private void handleMutableEntity(EntityEntry entry, SessionImplementor session) {
		// Apply aggregated GeneratedValues from all tables
		if ( generatedValuesCollector != null ) {
			final GeneratedValues generatedValues = generatedValuesCollector.generatedValues();
			action.handleGeneratedProperties( entry, generatedValues );
		}

		// Handle application-generated version increment
		// This is complementary to UpdateBindPlan's GeneratedValues processing:
		// - UpdateBindPlan handles database-generated values (timestamps, etc.)
		// - This section handles application-generated values (version increments)
		finalizeVersion( entry );

		// For non-versioned entities, finalizeVersion() returns early without calling
		// entry.postUpdate(). We must still synchronize the loadedState to prevent
		// the entity from appearing dirty on subsequent flushes.
		if ( action.getNextVersion() == null ) {
			entry.postUpdate( action.getInstance(), action.getState(), null );
		}

		action.handleDeleted( entry );
		action.updateCacheItem( previousVersion, cacheKey, entry );
		action.handleNaturalIdSharedResolutions( action.getId(), action.getPersister(), session.getPersistenceContext() );
		action.postUpdate();

		final var statistics = session.getFactory().getStatistics();
		if ( statistics.isStatisticsEnabled() ) {
			statistics.updateEntity( action.getPersister().getEntityName() );
		}
	}

	private void handleImmutableEntity(EntityEntry entry, SessionImplementor session) {
		action.updateCacheItem( previousVersion, cacheKey, entry );

		// For versioned immutable entities, increment statistics
		// This seems completely counter-intuitive, but there are tests with exactly
		// this expectation.
		if ( action.getPersister().isVersioned() ) {
			final var statistics = session.getFactory().getStatistics();
			if ( statistics.isStatisticsEnabled() ) {
				statistics.updateEntity( action.getPersister().getEntityName() );
			}
		}
	}

	private void finalizeVersion(org.hibernate.engine.spi.EntityEntry entry) {
		final Object nextVersion = action.getNextVersion();
		if ( nextVersion == null ) {
			return;  // No version to update
		}

		final var persister = action.getPersister();
		final Object entity = action.getInstance();
		final Object previousVersion = action.getPreviousVersion();

		// Defensive check: only update if not already updated by GeneratedValues
		// (Rare case: database-generated version via trigger or RETURNING clause)
		final Object currentVersion = persister.getVersion( entity );
		if ( currentVersion != null && !currentVersion.equals( previousVersion ) ) {
			// Version was already updated by GeneratedValues processing in UpdateBindPlan
			// Still need to update EntityEntry to ensure loadedState is correct
			if ( entry != null ) {
				entry.postUpdate( entity, action.getState(), nextVersion );
			}
			return;
		}

		// Version not yet updated - update it now (application-generated)
		final int versionPropertyIndex = persister.getVersionPropertyIndex();
		if ( versionPropertyIndex >= 0 ) {
			persister.setPropertyValue( entity, versionPropertyIndex, nextVersion );
		}

		// Update EntityEntry: this is critical for subsequent optimistic locking checks
		// as it updates loadedState with the new version
		if ( entry != null ) {
			entry.postUpdate( entity, action.getState(), nextVersion );
		}
	}

}
