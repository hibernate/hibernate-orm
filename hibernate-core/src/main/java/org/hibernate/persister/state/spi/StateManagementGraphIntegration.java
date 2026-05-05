/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.state.spi;

import org.hibernate.Incubating;
import org.hibernate.action.queue.spi.decompose.collection.CollectionMutationPlanContributor;
import org.hibernate.action.queue.spi.decompose.entity.EntityMutationPlanContributor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;

/// Graph action-queue integration for a state-management strategy.
///
/// The root [StateManagement] contract identifies the state-management model for
/// a mapping.  This nested integration contract exposes the graph-queue-specific
/// mutation-plan contributors for that model without mixing graph decomposition
/// concerns into the legacy coordinator factory surface.
///
/// @author Steve Ebersole
/// @since 8.0
@Incubating
public interface StateManagementGraphIntegration {
	/// The graph integration used by the standard state-management model.  It
	/// does not contribute any alternate graph mutation plans.
	StateManagementGraphIntegration STANDARD = new StateManagementGraphIntegration() {
	};

	/// Creates the entity mutation-plan contributor for the given persister.
	default EntityMutationPlanContributor createEntityMutationPlanContributor(EntityPersister persister) {
		return EntityMutationPlanContributor.STANDARD;
	}

	/// Creates the collection mutation-plan contributor for the given persister.
	default CollectionMutationPlanContributor createCollectionMutationPlanContributor(CollectionPersister persister) {
		return CollectionMutationPlanContributor.STANDARD;
	}
}
