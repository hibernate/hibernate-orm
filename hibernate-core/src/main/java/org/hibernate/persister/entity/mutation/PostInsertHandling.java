/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.AssertionFailure;
import org.hibernate.action.internal.AbstractEntityInsertAction;
import org.hibernate.action.internal.EntityInsertAction;
import org.hibernate.action.queue.bind.GeneratedValuesCollector;
import org.hibernate.action.queue.exec.PostExecutionCallback;
import org.hibernate.engine.internal.Versioning;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.persister.entity.EntityPersister;

/// Post-execution callback for entity insert actions.
///
/// This handles all the finalization work that needs to happen after all table
/// `INSERT`s for the entity have been executed, including:
///
///   - Processing database-generated values (IDs, timestamps, versions, etc.)
///   - Updating EntityEntry state (postInsert, postUpdate)
///   - Registering the inserted key in the persistence context
///   - Adding collections by key to the persistence context
///   - Caching the entity (if caching is enabled)
///   - Natural ID post-save notifications
///   - Firing POST_INSERT event listeners
///   - Updating statistics
///   - Marking the action as executed
///
/// @see EntityInsertAction
/// @see org.hibernate.action.internal.EntityIdentityInsertAction
/// @see GeneratedValuesCollector
///
/// @author Steve Ebersole
public class PostInsertHandling implements PostExecutionCallback {
	private final AbstractEntityInsertAction action;
	private final GeneratedValuesCollector generatedValuesCollector;

	public PostInsertHandling(
			AbstractEntityInsertAction action,
			@Nullable GeneratedValuesCollector generatedValuesCollector) {
		this.action = action;
		this.generatedValuesCollector = generatedValuesCollector;
	}

	@Override
	public void handle(SessionImplementor session) {
		final var persister = action.getPersister();
		final Object entity = action.getInstance();
		final Object id = action.getId();
		final Object[] state = action.getState();


		// 1. Get EntityEntry
		final var persistenceContext = session.getPersistenceContextInternal();
		final var entry = persistenceContext.getEntry( entity );
		if ( entry == null ) {
			throw new AssertionFailure( "possible non-threadsafe access to session" );
		}

		// 2. Update EntityEntry state after insert
		entry.postInsert( state );

		// 3. Handle generated properties (IDs, timestamps, versions, etc.) across all the tables
		if ( generatedValuesCollector != null ) {
			final GeneratedValues generatedValues = generatedValuesCollector.generatedValues();
			handleGeneratedProperties( entry, generatedValues, persistenceContext, persister, state );
		}

		// 4. Register inserted key in persistence context
		persistenceContext.registerInsertedKey( persister, id );

		// 5. Add collections by key to persistence context (if not early insert)
		if ( !action.isEarlyInsert() ) {
			action.addCollectionsByKeyToPersistenceContext( persistenceContext, state );
		}

		// 6-10. Final finalization (only for EntityInsertAction - EntityIdentityInsertAction
		// handles this inline in its execute() method)
		if ( action instanceof EntityInsertAction insertAction ) {
			// 6. Cache entity (if caching enabled)
			insertAction.putCacheIfNecessary();

			// 7. Handle natural ID post-save notifications
			insertAction.handleNaturalIdPostSaveNotifications( id );

			// 8. Fire POST_INSERT event listeners
			insertAction.postInsert();

			// 9. Update statistics
			final var statistics = session.getFactory().getStatistics();
			if ( statistics.isStatisticsEnabled() ) {
				statistics.insertEntity( persister.getEntityName() );
			}

			// 10. Mark action as executed
			insertAction.markExecuted();
		}
	}

	private void handleGeneratedProperties(
			EntityEntry entry,
			GeneratedValues generatedValues,
			PersistenceContext persistenceContext,
			EntityPersister persister,
			Object[] state) {

		if ( persister.hasInsertGeneratedProperties() ) {
			final Object instance = action.getInstance();
			persister.processInsertGeneratedProperties(
					action.getId(),
					instance,
					state,
					generatedValues,
					action.getSession()
			);

			// If version is generated, extract it from state and update action
			if ( persister.isVersionPropertyGenerated() ) {
				final Object version = Versioning.getVersion( state, persister );
				if ( action instanceof EntityInsertAction insertAction ) {
					insertAction.setVersion( version );
				}
			}
			entry.postUpdate( instance, state, getVersionFromAction() );
		}
		else if ( persister.isVersionPropertyGenerated() ) {
			// Version generated but no other generated properties
			final Object version = Versioning.getVersion( state, persister );
			if ( action instanceof EntityInsertAction insertAction ) {
				insertAction.setVersion( version );
			}
			entry.postInsert( version );
		}

		// Handle row-id mapping if present
		if ( generatedValues != null && persister.getRowIdMapping() != null ) {
			final Object rowId = generatedValues.getGeneratedValue( persister.getRowIdMapping() );
			if ( rowId != null ) {
				persistenceContext.replaceEntityEntryRowId( action.getInstance(), rowId );
			}
		}
	}

	private Object getVersionFromAction() {
		if ( action instanceof EntityInsertAction insertAction ) {
			return insertAction.getVersion();
		}
		return null;
	}
}
