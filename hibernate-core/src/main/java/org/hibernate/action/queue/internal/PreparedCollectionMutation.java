/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.Internal;
import org.hibernate.action.internal.DelayedPostInsertIdentifier;
import org.hibernate.action.queue.spi.CollectionEndpoint;
import org.hibernate.collection.spi.CollectionMutationInterpretation;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;

/// Queue-neutral result of shared collection lifecycle preparation.
///
/// The frozen collection interpretation is part of this representation.
/// `QUEUED_OPERATIONS` is transitional execution metadata, not an independent semantic
/// lifecycle or mutation identity.
///
/// @since 8.0
/// @author Steve Ebersole
@Internal
public record PreparedCollectionMutation(
		@Nonnull Kind kind,
		@Nullable PersistentCollection<?> collection,
		@Nonnull CollectionEndpoint endpoint,
		boolean emptySnapshot,
		@Nullable Object affectedOwner,
		@Nullable Object affectedOwnerId,
		@Nonnull CollectionMutationInterpretation interpretation) {

	/// Whether the retained interpretation still describes the collection's current state.
	public boolean isInterpretationValid() {
		return collection == null || interpretation.isValid( collection );
	}

	/// Returns a mutation whose endpoint key is ready for graph decomposition.
	public PreparedCollectionMutation resolveKey(SessionImplementor session) {
		if ( !(endpoint.key() instanceof DelayedPostInsertIdentifier) ) {
			return this;
		}
		if ( collection == null ) {
			throw new IllegalStateException( "Delayed collection key has no collection wrapper" );
		}
		final var ownerEntry = session.getPersistenceContextInternal().getEntry( collection.getOwner() );
		if ( ownerEntry == null ) {
			throw new IllegalStateException( "Delayed collection key has no managed owner" );
		}
		return new PreparedCollectionMutation(
				kind,
				collection,
				new CollectionEndpoint( endpoint.persister(), ownerEntry.getId() ),
				emptySnapshot,
				affectedOwner,
				affectedOwnerId,
				interpretation
		);
	}

	public PersistentCollection<?> getCollection() {
		return collection;
	}

	public CollectionPersister getPersister() {
		return endpoint.persister();
	}

	public Object getKey() {
		return endpoint.key();
	}

	public boolean isEmptySnapshot() {
		return emptySnapshot;
	}

	public Object getAffectedOwner() {
		return affectedOwner;
	}

	public Object getAffectedOwnerId() {
		return affectedOwnerId;
	}

	public CollectionMutationInterpretation getCollectionMutationInterpretation() {
		return interpretation;
	}

	public enum Kind {
		CREATE,
		REMOVE,
		UPDATE,
		QUEUED_OPERATIONS
	}
}
