package org.hibernate.persister.entity.mutation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.Internal;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.MutationOperationGroup;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * Update coordinator for
 * {@link org.hibernate.temporal.TemporalTableStrategy#HISTORY_TABLE}
 * temporal strategy.
 * <p>
 * Updates the row of the table holding current data, and leaves its
 * revisions to the {@link org.hibernate.temporal.spi.HistoryWorkQueue}.
 *
 * @author Gavin King
 */
@Internal
public class UpdateCoordinatorHistory implements UpdateCoordinator {
	private final EntityPersister entityPersister;
	private final UpdateCoordinator currentUpdateCoordinator;
	private final HistoryRevisionWriter revisionWriter;

	public UpdateCoordinatorHistory(
			@Nonnull EntityPersister entityPersister,
			@Nonnull SessionFactoryImplementor factory,
			@Nonnull UpdateCoordinator currentUpdateCoordinator) {
		this.entityPersister = entityPersister;
		this.currentUpdateCoordinator = currentUpdateCoordinator;
		this.revisionWriter = castNonNull( entityPersister.getHistoryRevisionWriter() );
	}

	@Nullable
	@Override
	public MutationOperationGroup getStaticMutationOperationGroup() {
		return currentUpdateCoordinator.getStaticMutationOperationGroup();
	}

	@Nullable
	@Override
	public GeneratedValues update(
			@Nonnull Object entity,
			@Nonnull Object id,
			@Nullable Object rowId,
			@Nonnull Object[] values,
			@Nullable Object oldVersion,
			@Nullable Object[] incomingOldValues,
			@Nullable int[] dirtyAttributeIndexes,
			boolean hasDirtyCollection,
			@Nonnull SharedSessionContractImplementor session) {
		final var generatedValues = currentUpdateCoordinator.update(
				entity,
				id,
				rowId,
				values,
				oldVersion,
				incomingOldValues,
				dirtyAttributeIndexes,
				hasDirtyCollection,
				session
		);
		final var queue = session.getHistoryWorkQueue();
		queue.entityUpdated(
				entity,
				id,
				!entityPersister.excludedFromTemporalVersioning( dirtyAttributeIndexes, hasDirtyCollection ),
				dirtyAttributeIndexes,
				// the open revision has the partition key of the row before the update
				PartitionKeyValue.fromState( entityPersister, incomingOldValues == null ? values : incomingOldValues, session ),
				revisionWriter,
				session
		);
		queue.currentRowsWritten( session );
		return generatedValues;
	}

	@Override
	public void forceVersionIncrement(
			@Nonnull Object id,
			@Nullable Object currentVersion,
			@Nonnull Object nextVersion,
			@Nonnull SharedSessionContractImplementor session) {
		currentUpdateCoordinator.forceVersionIncrement( id, currentVersion, nextVersion, session );
	}
}
