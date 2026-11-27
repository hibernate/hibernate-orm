package org.hibernate.persister.entity.mutation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.MutationOperationGroup;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * Insert coordinator for
 * {@link org.hibernate.temporal.TemporalTableStrategy#HISTORY_TABLE}
 * temporal strategy.
 * <p>
 * Inserts the row of the table holding current data, and leaves its
 * first revision to the {@link org.hibernate.temporal.spi.HistoryWorkQueue}.
 *
 * @author Gavin King
 */
@org.hibernate.Internal
public class InsertCoordinatorHistory extends AbstractMutationCoordinator implements InsertCoordinator {
	private final InsertCoordinator currentInsertCoordinator;
	private final HistoryRevisionWriter revisionWriter;

	public InsertCoordinatorHistory(
			@Nonnull EntityPersister entityPersister,
			@Nonnull SessionFactoryImplementor factory,
			@Nonnull InsertCoordinator currentInsertCoordinator) {
		super( entityPersister, factory );
		this.currentInsertCoordinator = currentInsertCoordinator;
		this.revisionWriter = castNonNull( entityPersister.getHistoryRevisionWriter() );
	}

	@Nullable
	@Override
	public MutationOperationGroup getStaticMutationOperationGroup() {
		return currentInsertCoordinator.getStaticMutationOperationGroup();
	}

	@Nullable
	@Override
	protected BatchKey getBatchKey() {
		return null;
	}

	@Nullable
	@Override
	public GeneratedValues insert(@Nonnull Object entity, @Nonnull Object[] values, @Nonnull SharedSessionContractImplementor session) {
		final var generatedValues = currentInsertCoordinator.insert( entity, values, session );
		recordInsert( entity, resolveInsertedIdentifier( entity, null, generatedValues, session ), session );
		return generatedValues;
	}

	@Nullable
	@Override
	public GeneratedValues insert(
			@Nonnull Object entity,
			@Nullable Object id,
			@Nonnull Object[] values,
			@Nonnull SharedSessionContractImplementor session) {
		final var generatedValues = currentInsertCoordinator.insert( entity, id, values, session );
		recordInsert( entity, resolveInsertedIdentifier( entity, id, generatedValues, session ), session );
		return generatedValues;
	}

	private void recordInsert(Object entity, Object id, SharedSessionContractImplementor session) {
		final var queue = session.getHistoryWorkQueue();
		queue.entityInserted( entity, id, revisionWriter, session );
		queue.currentRowsWritten( session );
	}
}
