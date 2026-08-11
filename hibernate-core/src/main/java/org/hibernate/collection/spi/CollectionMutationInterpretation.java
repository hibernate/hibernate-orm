/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.collection.spi;

import jakarta.annotation.Nonnull;

import org.hibernate.Incubating;
import org.hibernate.action.queue.spi.CollectionTransition;

/// A frozen, queue-neutral interpretation of one semantic collection mutation.
///
/// @since 8.0
/// @author Steve Ebersole
@Incubating
public record CollectionMutationInterpretation(
		@Nonnull CollectionTransition transition,
		@Nonnull SemanticCollectionChange semanticChange,
		@Nonnull PhysicalCollectionMutation physicalMutation,
		long mutationGeneration) {

	/// Whether this interpretation still describes the collection's structural state.
	public boolean isValid(PersistentCollection<?> collection) {
		return mutationGeneration == collection.getMutationGeneration();
	}
}
