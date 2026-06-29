/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.internal;

import jakarta.annotation.Nullable;
import org.hibernate.AssertionFailure;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityHolder;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.DirtyCheckEvent;
import org.hibernate.event.spi.DirtyCheckEventListener;
import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.collection.CollectionPersister;
import jakarta.annotation.Nonnull;

/**
 * Determines if the current session holds modified state which
 * would be synchronized with the database if the session were
 * flushed. Approximately reproduces the logic that is executed
 * on an {@link org.hibernate.event.spi.AutoFlushEvent}.
 *
 * @implNote Historically, {@link org.hibernate.Session#isDirty}
 * was implemented to actually execute a partial flush and then
 * return {@code true} if there were any resulting operations to
 * be executed. This was extremely inefficient and side-effecty.
 * The new implementation below is non-perfect and probably
 * misses some subtleties, but it's I guess OK to view it as an
 * approximation.
 *
 * @author Gavin King
 */
public class DefaultDirtyCheckEventListener implements DirtyCheckEventListener {

	@Override
	public void onDirtyCheck(@Nonnull DirtyCheckEvent event) {
		final var session = event.getSession();
		final var persistenceContext = session.getPersistenceContextInternal();
		final var holdersByKey = persistenceContext.getEntityHoldersByKey();
		if ( holdersByKey != null ) {
			for ( var holder : holdersByKey.values() ) {
				if ( isEntityDirty( holder, session ) ) {
					event.setDirty( true );
					return;
				}
			}
		}
		final var entriesByCollection = persistenceContext.getCollectionEntries();
		if ( entriesByCollection != null ) {
			for ( var entry : entriesByCollection.entrySet() ) {
				if ( isCollectionDirty( entry.getKey(), entry.getValue().getLoadedPersister() ) ) {
					event.setDirty( true );
					return;
				}
			}
		}
	}

	private static boolean isEntityDirty(@Nonnull EntityHolder holder, @Nonnull EventSource session) {
		final var entityEntry = holder.getEntityEntry();
		if ( entityEntry == null ) {
			// holders with no entity entry yet cannot contain dirty entities
			return false;
		}
		final Status status = entityEntry.getStatus();
		return switch ( status ) {
			case GONE, READ_ONLY -> false;
			case DELETED -> true;
			case MANAGED -> {
				final Object entity = holder.getEntity();
				assert entity != null;
				yield isManagedEntityDirty( entity, entityEntry, session );
			}
			case SAVING, LOADING -> throw new AssertionFailure( "Unexpected status: " + status );
		};
	}

	private static boolean isManagedEntityDirty(
			@Nonnull Object entity,
			@Nonnull EntityEntry entityEntry,
			@Nonnull EventSource session) {
		if ( entityEntry.requiresDirtyCheck( entity ) ) { // takes into account CustomEntityDirtinessStrategy
			final var persister = entityEntry.getPersister();
			final var propertyValues =
					entityEntry.getStatus() == Status.DELETED
							? entityEntry.getDeletedState()
							: persister.getValues( entity );
			final var dirty =
					persister.findDirty( propertyValues, entityEntry.getLoadedState(), entity, session );
			return dirty != null;
		}
		else {
			return false;
		}
	}

	private static boolean isCollectionDirty(
			@Nonnull PersistentCollection<?> collection,
			@Nullable CollectionPersister loadedPersister) {
		return collection.isDirty()
			|| collection.wasInitialized()
				&& loadedPersister != null
				&& loadedPersister.isMutable() //optimization
//				&& !loadedPersister.isInverse() // even if it's inverse, could still result in a cache update
				&& ( collection.isDirectlyAccessible() || loadedPersister.getElementType().isMutable() ) //optimization
				&& !collection.equalsSnapshot( loadedPersister );
	}
}
