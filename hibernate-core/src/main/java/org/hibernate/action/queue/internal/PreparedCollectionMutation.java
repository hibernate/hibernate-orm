/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.Internal;
import org.hibernate.action.queue.spi.CollectionEndpoint;
import org.hibernate.collection.spi.PersistentCollection;

/// Queue-neutral result of shared collection lifecycle preparation.
///
/// The stable collection delta will become part of this representation during the delta-production
/// stage. `QUEUED_OPERATIONS` is transitional execution metadata, not an independent semantic
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
		@Nullable FrozenCollectionDelta frozenDelta) {

	/// Whether the retained delta still describes the collection's current state.
	public boolean isDeltaValid() {
		return frozenDelta == null
				|| collection != null && frozenDelta.isValid( collection );
	}

	public enum Kind {
		CREATE,
		REMOVE,
		UPDATE,
		QUEUED_OPERATIONS
	}
}
