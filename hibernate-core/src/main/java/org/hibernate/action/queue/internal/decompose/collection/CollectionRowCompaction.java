/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.decompose.collection;

import org.hibernate.action.queue.internal.PreparedCollectionMutation;
import org.hibernate.action.queue.spi.decompose.DecompositionContext;
import org.hibernate.action.queue.spi.decompose.collection.CollectionMutationPlanContributor;
import org.hibernate.metamodel.mapping.internal.BasicValuedCollectionPart;
import org.hibernate.persister.collection.BasicCollectionPersister;

/// Decides whether collection-row inserts may share one graph operation.
///
/// Compaction is deliberately based on graph-placement equivalence rather than
/// statement shape alone. Rows remain expanded when a state-management
/// contributor may add row-local work, the table has row-sensitive unique-slot
/// or cycle facts, or the collection shape can introduce element-local graph
/// dependencies. The conservative expanded representation remains correct for
/// every rejected shape.
///
/// @author Steve Ebersole
/// @since 8.0
final class CollectionRowCompaction {
	private final boolean commonInsertPlacement;

	CollectionRowCompaction(
			BasicCollectionPersister persister,
			CollectionMutationPlanContributor mutationPlanContributor,
			boolean tableHasNonPrimaryUniqueConstraints) {
		commonInsertPlacement = mutationPlanContributor == CollectionMutationPlanContributor.STANDARD
				&& !tableHasNonPrimaryUniqueConstraints
				&& !persister.getCollectionTableDescriptor().isSelfReferential()
				&& persister.getAttributeMapping().getElementDescriptor() instanceof BasicValuedCollectionPart;
	}

	/// A new owner's rows all depend on the same owner insert and cannot conflict
	/// with pre-existing rows for that owner.
	boolean canCompactCreate(
			PreparedCollectionMutation mutation,
			DecompositionContext decompositionContext) {
		return commonInsertPlacement
				&& mutation.getAffectedOwner() != null
				&& decompositionContext.isBeingInsertedInCurrentFlush( mutation.getAffectedOwner() );
	}

	/// A bulk remove gives all replacement rows the same remove-before-create
	/// placement. Non-primary unique slots and self-references were rejected when
	/// this policy was constructed.
	boolean canCompactReplacement() {
		return commonInsertPlacement;
	}
}
