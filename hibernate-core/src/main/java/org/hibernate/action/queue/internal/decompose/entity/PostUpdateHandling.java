/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.decompose.entity;

import org.hibernate.action.queue.spi.decompose.entity.UpdateCacheHandling;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.AssertionFailure;
import org.hibernate.action.internal.EntityUpdateAction;
import org.hibernate.action.queue.spi.bind.GeneratedValuesCollector;
import org.hibernate.action.queue.spi.bind.PostExecutionCallback;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.generator.values.GeneratedValues;

/// Post-execution callback for entity update actions.
///
/// This handles all the finalization work that needs to happen after
/// all table `UPDATE`s for the entity have been executed, including:
///
/// See [ - Processing database-generated values (timestamps, etc.)].
/// See [ - Setting application-generated values (version increments)].
/// See [ - Updating EntityEntry state with new version].
/// See [ - Handling deleted entities].
/// See [ - Updating cache].
/// See [ - Natural ID shared resolutions].
/// See [ - Firing POST_UPDATE event listeners].
/// See [ - Updating statistics].
///
/// See [EntityUpdateAction].
/// See [GeneratedValuesCollector].
///
/// @author Steve Ebersole
public class PostUpdateHandling implements PostExecutionCallback {
	private final EntityUpdateAction action;
	private final UpdateCacheHandling.CacheUpdate cacheUpdate;
	private final Object previousVersion;
	private final GeneratedValuesCollector generatedValuesCollector;
	private final EntityEntry entityEntry;
	private final boolean preservePreviousVersion;

	public PostUpdateHandling(
			EntityUpdateAction action,
			UpdateCacheHandling.CacheUpdate cacheUpdate,
			Object previousVersion,
			@Nullable GeneratedValuesCollector generatedValuesCollector,
			EntityEntry entityEntry) {
		this( action, cacheUpdate, previousVersion, generatedValuesCollector, entityEntry, false );
	}

	public PostUpdateHandling(
			EntityUpdateAction action,
			UpdateCacheHandling.CacheUpdate cacheUpdate,
			Object previousVersion,
			@Nullable GeneratedValuesCollector generatedValuesCollector,
			EntityEntry entityEntry,
			boolean preservePreviousVersion) {
		this.action = action;
		this.cacheUpdate = cacheUpdate;
		this.previousVersion = previousVersion;
		this.generatedValuesCollector = generatedValuesCollector;
		this.entityEntry = entityEntry;
		this.preservePreviousVersion = preservePreviousVersion;
	}

	@Override
	public void handle(SessionImplementor session) {
		final EntityEntry entry = resolveEntityEntry( session );
		if ( !action.getPersister().isMutable() ) {
			handleImmutableEntity( entry, session );
		}
		else {
			handleMutableEntity( entry, session );
		}
	}

	private EntityEntry resolveEntityEntry(SessionImplementor session) {
		if ( entityEntry != null ) {
			return entityEntry;
		}
		final EntityEntry entry = session.getPersistenceContextInternal().getEntry( action.getInstance() );
		if ( entry == null ) {
			throw new AssertionFailure(
					"possible non-threadsafe access to session : could not locate entry during update post-execution"
			);
		}
		return entry;
	}

	private void handleImmutableEntity(EntityEntry entry, SessionImplementor session) {
		UpdateCacheHandling.updateItem( action, cacheUpdate, action.getNextVersion(), entry, session );

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

	private void handleMutableEntity(EntityEntry entry, SessionImplementor session) {
		final var persister = action.getPersister();
		final Object entity = action.getInstance();
		final Object[] state = action.getState();
		Object nextVersion = preservePreviousVersion
				? previousVersion
				: action.getNextVersion();

		// Apply generated values and update entity state
		if ( entry.getStatus() == Status.MANAGED || persister.isVersionPropertyGenerated() ) {

			// Deep copy state FIRST to clone component objects before applying generated values.
			// This prevents component reference sharing that breaks dirty checking.
			// It is safe to copy in place here.
			org.hibernate.type.TypeHelper.deepCopy(
					state,
					persister.getPropertyTypes(),
					persister.getPropertyCheckability(),
					state,
					session
			);

			// Apply aggregated GeneratedValues from all tables
			final GeneratedValues generatedValues = generatedValuesCollector == null
					? null
					: generatedValuesCollector.generatedValues();
			if ( persister.hasUpdateGeneratedProperties() ) {
				persister.processUpdateGeneratedProperties( action.getId(), entity, state, generatedValues, session );
			}
			// Check if version was DB-generated
			if ( generatedValuesCollector != null && persister.isVersionPropertyGenerated() ) {
				nextVersion = persister.getVersion( entity );
			}

			// Handle application-generated version increment (if not already DB-generated)
			if ( nextVersion != null ) {
				final Object previousVersion = action.getPreviousVersion();
				final Object currentVersion = persister.getVersion( entity );
				// Only increment if not already updated by DB generation
				if ( currentVersion == null || currentVersion.equals( previousVersion ) ) {
					final int versionPropertyIndex = persister.getVersionPropertyIndex();
					if ( versionPropertyIndex >= 0 ) {
						persister.setPropertyValue( entity, versionPropertyIndex, nextVersion );
					}
				}
				else {
					// Version was DB-generated, use that value
					nextVersion = currentVersion;
				}
			}
		}

		// Single call to postUpdate with deep-copied state
		entry.postUpdate( entity, state, nextVersion );
		entry.setMaybeLazySet( null );

		action.handleDeleted( entry );
		UpdateCacheHandling.updateItem( action, cacheUpdate, nextVersion, entry, session );
		action.handleNaturalIdSharedResolutions( action.getId(), action.getPersister(), session.getPersistenceContext() );

		// For entities being deleted in the same flush, skip firing POST_UPDATE events
		// because the DELETE operation may have already removed the entity from the
		// persistence context, causing event listeners to fail when they try to look up
		// the EntityEntry.
		final Status status = entry.getStatus();
		if ( status != Status.DELETED && status != Status.GONE ) {
			action.postUpdate();

			final var statistics = session.getFactory().getStatistics();
			if ( statistics.isStatisticsEnabled() ) {
				statistics.updateEntity( action.getPersister().getEntityName() );
			}
		}
	}

}
