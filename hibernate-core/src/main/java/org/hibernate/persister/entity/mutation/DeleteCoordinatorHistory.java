package org.hibernate.persister.entity.mutation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.MutationOperationGroup;

import java.util.List;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * Delete coordinator for
 * {@link org.hibernate.temporal.TemporalTableStrategy#HISTORY_TABLE}
 * temporal strategy.
 * <p>
 * Deletes the row of the table holding current data, and leaves the
 * end of its open revision to the {@link org.hibernate.temporal.spi.HistoryWorkQueue}.
 *
 * @author Gavin King
 */
@org.hibernate.Internal
public class DeleteCoordinatorHistory implements DeleteCoordinator {
	private final EntityPersister entityPersister;
	private final DeleteCoordinator currentDeleteCoordinator;
	private final HistoryRevisionWriter revisionWriter;

	public DeleteCoordinatorHistory(
			@Nonnull EntityPersister entityPersister,
			@Nonnull SessionFactoryImplementor factory,
			@Nonnull DeleteCoordinator currentDeleteCoordinator) {
		this.entityPersister = entityPersister;
		this.currentDeleteCoordinator = currentDeleteCoordinator;
		this.revisionWriter = castNonNull( entityPersister.getHistoryRevisionWriter() );
	}

	/**
	 * The partition key of the row, taken from its loaded state, or from the given
	 * instance if it does not belong to a persistence context, as in a stateless
	 * session.
	 */
	@Nonnull
	private List<PartitionKeyValue> partitionKeys(
			@Nullable Object entity,
			@Nonnull SharedSessionContractImplementor session) {
		if ( entity == null || !entityPersister.hasPartitionedSelectionMapping() ) {
			return List.of();
		}
		final var entry = session.getPersistenceContextInternal().getEntry( entity );
		return PartitionKeyValue.fromState( entityPersister,
				entry == null ? entityPersister.getValues( entity ) : entry.getLoadedState(), session );
	}

	@Nullable
	@Override
	public MutationOperationGroup getStaticMutationOperationGroup() {
		return currentDeleteCoordinator.getStaticMutationOperationGroup();
	}

	@Override
	public void delete(
			@Nullable Object entity,
			@Nonnull Object id,
			@Nullable Object version,
			@Nonnull SharedSessionContractImplementor session) {
		currentDeleteCoordinator.delete( entity, id, version, session );
		final var queue = session.getHistoryWorkQueue();
		queue.entityDeleted( id, partitionKeys( entity, session ), revisionWriter, session );
		queue.currentRowsWritten( session );
	}
}
