package org.hibernate.persister.entity.mutation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.Internal;
import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.metamodel.mapping.TemporalMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.spi.mutation.TableMapping;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.spi.mutation.MutationType;
import org.hibernate.sql.ast.spi.model.builder.TableUpdateBuilderStandard;

import java.sql.SQLException;
import java.util.List;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;
import static org.hibernate.persister.entity.mutation.TemporalMutationHelper.isRevisionFromCurrentTransaction;
import static org.hibernate.sql.model.internal.MutationOperationGroupFactory.singleOperation;

/**
 * Update coordinator for
 * {@link org.hibernate.temporal.TemporalTableStrategy#HISTORY_TABLE}
 * temporal strategy.
 *
 * @author Gavin King
 */
@Internal
public class UpdateCoordinatorHistory extends AbstractTemporalUpdateCoordinator {
	private final HistoryTableMutation historyMutation;
	private final UpdateCoordinator currentUpdateCoordinator;
	private final EntityTableMapping identifierTableMapping;
	private final EntityTableMapping historyTableMapping;
	private final TemporalMapping temporalMapping;
	private final BasicBatchKey historyUpdateBatchKey;
	private final BasicBatchKey historyInsertBatchKey;
	private final MutationOperationGroup historyEndUpdateGroup;
	private final MutationOperationGroup historyInsertGroup;

	public UpdateCoordinatorHistory(
			@Nonnull EntityPersister entityPersister,
			@Nonnull SessionFactoryImplementor factory,
			@Nonnull UpdateCoordinator currentUpdateCoordinator) {
		super( entityPersister, factory );
		this.currentUpdateCoordinator = currentUpdateCoordinator;
		this.identifierTableMapping = entityPersister.getIdentifierTableMapping();
		this.temporalMapping = castNonNull( entityPersister.getTemporalMapping() );
		this.historyTableMapping =
				createAuxiliaryTableMapping( identifierTableMapping, entityPersister,
						temporalMapping.getTableName() );
		historyMutation = new HistoryTableMutation( entityPersister, historyTableMapping );
		final String entityName = entityPersister.getEntityName();
		this.historyUpdateBatchKey = new BasicBatchKey( entityName + "#HISTORY_UPDATE" );
		this.historyInsertBatchKey = new BasicBatchKey( entityName + "#HISTORY_INSERT" );
		this.historyEndUpdateGroup = buildEndingUpdateGroup( historyTableMapping, temporalMapping );
		final var insert = historyMutation.createInsert( entityPersister.getPropertyInsertability() );
		this.historyInsertGroup = singleOperation( MutationType.INSERT, entityPersister, insert.createMutationOperation( null, factory ) );
	}

	@Nullable
	@Override
	public MutationOperationGroup getStaticMutationOperationGroup() {
		return currentUpdateCoordinator.getStaticMutationOperationGroup();
	}

	@Nullable
	@Override
	protected BasicBatchKey getBatchKey() {
		return historyUpdateBatchKey;
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
		final boolean currentRevision = isRevisionFromCurrentTransaction( entityPersister(), id, session );
		if ( currentRevision || entityPersister().excludedFromTemporalVersioning( dirtyAttributeIndexes, hasDirtyCollection ) ) {
			performHistoryUpdate(
					entity,
					id,
					values,
					oldVersion,
					incomingOldValues,
					dirtyAttributeIndexes,
					currentRevision,
					session
			);
		}
		else {
			performRowEndUpdate(
					entity,
					id,
					null, // the live row and the history row have different row ids
					oldVersion,
					incomingOldValues == null ? values : incomingOldValues,
					session,
					temporalMapping,
					historyEndUpdateGroup,
					historyTableMapping.getTableName(),
					(statementDetails, affectedRowCount, batchPosition) ->
							resultCheck( id, statementDetails, affectedRowCount, batchPosition )

			);
			insertHistoryRow( id, values, session );
			registerUpdatedRevision( id, session );
		}
		return generatedValues;
	}

	private void performHistoryUpdate(
			@Nonnull Object entity,
			@Nonnull Object id,
			@Nonnull Object[] values,
			@Nullable Object oldVersion,
			@Nullable Object[] incomingOldValues,
			@Nullable int[] dirtyAttributeIndexes,
			boolean currentRevision,
			@Nonnull SharedSessionContractImplementor session) {
		final var partitionKeys = PartitionKeyValue.fromState( entityPersister(),
				incomingOldValues == null ? values : incomingOldValues, session );
		final var updateDetails = buildHistoryUpdateDetails(
				values, dirtyAttributeIndexes, currentRevision, partitionKeys );
		if ( updateDetails != null ) {
			final var mutationExecutor =
					mutationExecutorService.createExecutor( resolveBatchKeyAccess( true, session ),
							updateDetails.operationGroup, session );
			try {
				final var jdbcValueBindings = mutationExecutor.getJdbcValueBindings();
				breakDownKeyJdbcValues( id, null, session, jdbcValueBindings, historyTableMapping );

				if ( updateDetails.applyVersionRestriction ) {
					final var versionMapping = entityPersister().getVersionMapping();
					jdbcValueBindings.bindValue(
							oldVersion,
							historyTableMapping.getTableName(),
							versionMapping.getSelectionExpression(),
							ParameterUsage.RESTRICT
					);
				}

				PartitionKeyValue.bindRestrictions( partitionKeys, identifierTableMapping.getTableName(),
						jdbcValueBindings, updateDetails.operationGroup.getSingleOperation() );
				historyMutation.bindUpdate( values, updateDetails.attributeIndexes,
						(value, column) -> jdbcValueBindings.bindValue(
								value, historyTableMapping.getTableName(), column, ParameterUsage.SET ),
						session );

				mutationExecutor.execute(
						entity,
						null,
						null,
						(statementDetails, affectedRowCount, batchPosition) ->
								resultCheck( id, statementDetails, affectedRowCount, batchPosition ),
						session,
						staleStateException -> staleObjectStateException( id, staleStateException )
				);
			}
			finally {
				mutationExecutor.release();
			}
		}
	}

	@Nullable
	private HistoryUpdateDetails buildHistoryUpdateDetails(
			@Nonnull Object[] values,
			@Nullable int[] dirtyAttributeIndexes,
			boolean currentRevision,
			@Nonnull List<PartitionKeyValue> partitionKeys) {
		if ( !currentRevision && ( dirtyAttributeIndexes == null || dirtyAttributeIndexes.length == 0 ) ) {
			return null;
		}

		final var tableUpdateBuilder = new TableUpdateBuilderStandard<>( entityPersister(), historyTableMapping, factory() );
		final var assignments = historyMutation.addUpdateAssignments(
				tableUpdateBuilder, values, dirtyAttributeIndexes, currentRevision );
		if ( assignments != null ) {
			applyKeyRestriction( null, entityPersister(), tableUpdateBuilder, historyTableMapping );
			tableUpdateBuilder.addNullRestriction( temporalMapping.getEndingColumnMapping() );
			applyPartitionKeyRestriction( tableUpdateBuilder, identifierTableMapping.getTableName(),
					partitionKeys, TableMapping::getUpdateDetails );
			applyOptimisticLocking( tableUpdateBuilder );

			return new HistoryUpdateDetails(
					createMutationOperationGroup( tableUpdateBuilder ),
					assignments.bindableIndexes(),
					entityPersister().optimisticLockStyle().isVersion()
							&& entityPersister().getVersionMapping() != null
			);
		}
		else {
			return null;
		}
	}

	@Override
	void bindVersionRestriction(@Nullable Object oldVersion, @Nonnull JdbcValueBindings jdbcValueBindings, @Nonnull String temporalTableName) {
		final var versionMapping = entityPersister().getVersionMapping();
		if ( versionMapping != null && entityPersister().optimisticLockStyle().isVersion() ) {
			jdbcValueBindings.bindValue(
					oldVersion,
					temporalTableName,
					versionMapping.getSelectionExpression(),
					ParameterUsage.RESTRICT
			);
		}
	}

	private void insertHistoryRow(
			@Nonnull Object id,
			@Nonnull Object[] values,
			@Nonnull SharedSessionContractImplementor session) {
		final var mutationExecutor =
				mutationExecutorService.createExecutor( () -> historyInsertBatchKey, historyInsertGroup, session );
		try {
			final var bindings = mutationExecutor.getJdbcValueBindings();
			final var history = historyTableMapping.getTableName();
			historyMutation.bindInsert( id, values, entityPersister().getPropertyInsertability(),
					(value, column) -> bindings.bindValue( value, history, column, ParameterUsage.SET ), session );
			mutationExecutor.execute( id, null, null,
					UpdateCoordinatorHistory::verifyOutcome, session );
		}
		finally {
			mutationExecutor.release();
		}
	}

	private static final class HistoryUpdateDetails {
		private final MutationOperationGroup operationGroup;
		private final int[] attributeIndexes;
		private final boolean applyVersionRestriction;

		private HistoryUpdateDetails(
				@Nonnull MutationOperationGroup operationGroup,
				@Nonnull int[] attributeIndexes,
				boolean applyVersionRestriction) {
			this.operationGroup = operationGroup;
			this.attributeIndexes = attributeIndexes;
			this.applyVersionRestriction = applyVersionRestriction;
		}
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

	@Override
	public void forceVersionIncrement(
			@Nonnull Object id,
			@Nullable Object currentVersion,
			@Nonnull Object nextVersion,
			@Nonnull SharedSessionContractImplementor session) {
		currentUpdateCoordinator.forceVersionIncrement( id, currentVersion, nextVersion, session );
	}
}
