/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal;

import org.hibernate.Internal;
import org.hibernate.collection.spi.CollectionDelta;
import org.hibernate.collection.spi.PersistentCollection;

/// A structurally immutable collection delta paired with the collection state
/// generation from which it was produced.
///
/// @since 8.0
/// @author Steve Ebersole
@Internal
public record FrozenCollectionDelta(
		CollectionDelta delta,
		long mutationGeneration) {

	/// Freeze the delta against the collection's current structural state.
	public static FrozenCollectionDelta freeze(
			CollectionDelta delta,
			PersistentCollection<?> collection) {
		return new FrozenCollectionDelta( delta, collection.getMutationGeneration() );
	}

	/// Whether the collection still has the state represented by this delta.
	public boolean isValid(PersistentCollection<?> collection) {
		return mutationGeneration == collection.getMutationGeneration();
	}
}
