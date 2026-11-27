package org.hibernate.persister.entity.mutation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.sql.SQLException;

import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.spi.mutation.MutationType;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;
import static org.hibernate.persister.entity.mutation.InsertCoordinatorStandard.getPropertiesToInsert;
import static org.hibernate.sql.model.internal.MutationOperationGroupFactory.singleOperation;

/**
 * Insert coordinator for
 * {@link org.hibernate.temporal.TemporalTableStrategy#HISTORY_TABLE}
 * temporal strategy.
 *
 * @author Gavin King
 */
@org.hibernate.Internal
public class InsertCoordinatorHistory extends AbstractMutationCoordinator implements InsertCoordinator {
	private final HistoryTableMutation historyMutation;
	private final InsertCoordinator currentInsertCoordinator;
	private final EntityTableMapping historyTableMapping;
	private final BasicBatchKey historyBatchKey;
	@Nullable
	private final MutationOperationGroup staticHistoryInsertGroup;

	public InsertCoordinatorHistory(
			@Nonnull EntityPersister entityPersister,
			@Nonnull SessionFactoryImplementor factory,
			@Nonnull InsertCoordinator currentInsertCoordinator) {
		super( entityPersister, factory );
		this.currentInsertCoordinator = currentInsertCoordinator;
		final var identifierTableMapping = entityPersister.getIdentifierTableMapping();
		final var temporalMapping = castNonNull( entityPersister.getTemporalMapping() );
		historyTableMapping =
				createAuxiliaryTableMapping( identifierTableMapping,
						entityPersister, temporalMapping.getTableName()
		);
		historyMutation = new HistoryTableMutation( entityPersister, historyTableMapping );
		historyBatchKey = new BasicBatchKey( entityPersister.getEntityName() + "#HISTORY_INSERT" );
		staticHistoryInsertGroup = entityPersister.isDynamicInsert()
				? null
				: buildHistoryInsertGroup( entityPersister.getPropertyInsertability() );
	}

	@Nullable
	@Override
	public MutationOperationGroup getStaticMutationOperationGroup() {
		return currentInsertCoordinator.getStaticMutationOperationGroup();
	}

	@Nullable
	@Override
	protected BatchKey getBatchKey() {
		return historyBatchKey;
	}

	@Nullable
	@Override
	public GeneratedValues insert(@Nonnull Object entity, @Nonnull Object[] values, @Nonnull SharedSessionContractImplementor session) {
		final var generatedValues = currentInsertCoordinator.insert( entity, values, session );
		final Object id = resolveInsertedIdentifier( entity, null, generatedValues, session );
		insertHistoryRow( entity, id, values, session );
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
		final Object resolvedId = resolveInsertedIdentifier( entity, id, generatedValues, session );
		insertHistoryRow( entity, resolvedId, values, session );
		return generatedValues;
	}

	private void insertHistoryRow(
			@Nonnull Object entity,
			@Nonnull Object id,
			@Nonnull Object[] values,
			@Nonnull SharedSessionContractImplementor session) {
		final boolean dynamicInsert = entityPersister().isDynamicInsert();
		final boolean[] propertyInclusions = dynamicInsert
				? getPropertiesToInsert( entityPersister(), values )
				: entityPersister().getPropertyInsertability();
		final var operationGroup = dynamicInsert
				? buildHistoryInsertGroup( propertyInclusions )
				: staticHistoryInsertGroup;

		final var mutationExecutor =
				mutationExecutorService.createExecutor( resolveBatchKeyAccess( dynamicInsert, session ),
						operationGroup, session );
		try {
			final var bindings = mutationExecutor.getJdbcValueBindings();
			final var history = historyTableMapping.getTableName();
			historyMutation.bindInsert( id, values, propertyInclusions,
					(value, column) -> bindings.bindValue( value, history, column, ParameterUsage.SET ), session );
			mutationExecutor.execute( entity, null, null, InsertCoordinatorHistory::verifyOutcome, session );
		}
		finally {
			mutationExecutor.release();
		}
	}

	@Nonnull
	private MutationOperationGroup buildHistoryInsertGroup(@Nonnull boolean[] inclusions) {
		final var insert = historyMutation.createInsert( inclusions );
		return singleOperation( MutationType.INSERT, entityPersister(), insert.createMutationOperation( null, factory() ) );
	}

	private static boolean verifyOutcome(
			@Nonnull PreparedStatementDetails statementDetails,
			int affectedRowCount,
			int batchPosition) throws SQLException {
		statementDetails.getExpectation().verifyOutcome(
				affectedRowCount,
				statementDetails.getStatement(),
				batchPosition,
				statementDetails.getSqlString()
		);
		return true;
	}
}
