/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.state.spi;

import org.hibernate.Incubating;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.mutation.DeleteRowsCoordinator;
import org.hibernate.persister.collection.mutation.InsertRowsCoordinator;
import org.hibernate.persister.collection.mutation.RemoveCoordinator;
import org.hibernate.persister.collection.mutation.UpdateRowsCoordinator;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.mutation.DeleteCoordinator;
import org.hibernate.persister.entity.mutation.InsertCoordinator;
import org.hibernate.persister.entity.mutation.UpdateCoordinator;

/// Legacy action-queue integration for a state-management strategy.
///
/// This contract isolates the coordinator factory surface used by the legacy
/// action queue.  Keeping it separate from the root [StateManagement] contract
/// makes the legacy integration explicit while graph-queue support continues to
/// evolve through [StateManagementGraphIntegration].
///
/// @author Gavin King
/// @author Steve Ebersole
/// @since 8.0
@Incubating
public interface StateManagementLegacyIntegration {
	/// Creates the entity insert coordinator for the given persister.
	InsertCoordinator createInsertCoordinator(EntityPersister persister);

	/// Creates the entity update coordinator for the given persister.
	UpdateCoordinator createUpdateCoordinator(EntityPersister persister);

	/// Creates the entity merge coordinator for the given persister.
	UpdateCoordinator createMergeCoordinator(EntityPersister persister);

	/// Creates the entity delete coordinator for the given persister.
	DeleteCoordinator createDeleteCoordinator(EntityPersister persister);

	/// Creates the collection row-insert coordinator for the given persister.
	InsertRowsCoordinator createInsertRowsCoordinator(CollectionPersister persister);

	/// Creates the collection row-update coordinator for the given persister.
	UpdateRowsCoordinator createUpdateRowsCoordinator(CollectionPersister persister);

	/// Creates the collection row-delete coordinator for the given persister.
	DeleteRowsCoordinator createDeleteRowsCoordinator(CollectionPersister persister);

	/// Creates the collection remove coordinator for the given persister.
	RemoveCoordinator createRemoveCoordinator(CollectionPersister persister);
}
