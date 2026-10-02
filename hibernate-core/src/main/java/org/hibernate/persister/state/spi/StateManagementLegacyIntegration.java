package org.hibernate.persister.state.spi;

import jakarta.annotation.Nonnull;

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
@Incubating(since = "8.0")
public interface StateManagementLegacyIntegration {
	/// Creates the entity insert coordinator for the given persister.
	@Nonnull
	InsertCoordinator createInsertCoordinator(@Nonnull EntityPersister persister);

	/// Creates the entity update coordinator for the given persister.
	@Nonnull
	UpdateCoordinator createUpdateCoordinator(@Nonnull EntityPersister persister);

	/// Creates the entity merge coordinator for the given persister.
	@Nonnull
	UpdateCoordinator createMergeCoordinator(@Nonnull EntityPersister persister);

	/// Creates the entity delete coordinator for the given persister.
	@Nonnull
	DeleteCoordinator createDeleteCoordinator(@Nonnull EntityPersister persister);

	/// Creates the collection row-insert coordinator for the given persister.
	@Nonnull
	InsertRowsCoordinator createInsertRowsCoordinator(@Nonnull CollectionPersister persister);

	/// Creates the collection row-update coordinator for the given persister.
	@Nonnull
	UpdateRowsCoordinator createUpdateRowsCoordinator(@Nonnull CollectionPersister persister);

	/// Creates the collection row-delete coordinator for the given persister.
	@Nonnull
	DeleteRowsCoordinator createDeleteRowsCoordinator(@Nonnull CollectionPersister persister);

	/// Creates the collection remove coordinator for the given persister.
	@Nonnull
	RemoveCoordinator createRemoveCoordinator(@Nonnull CollectionPersister persister);
}
