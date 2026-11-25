package org.hibernate.persister.entity.mutation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.TemporalMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.spi.mutation.MutationType;
import org.hibernate.sql.ast.spi.model.builder.TableUpdateBuilderStandard;
import org.hibernate.sql.model.internal.MutationGroupSingle;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;
import static org.hibernate.persister.entity.mutation.AbstractTemporalUpdateCoordinator.applyTemporalEnding;
import static org.hibernate.sql.model.internal.MutationOperationGroupFactory.singleOperation;

/**
 * Delete coordinator for
 * {@link org.hibernate.temporal.TemporalTableStrategy#HISTORY_TABLE}
 * temporal strategy.
 *
 * @author Gavin King
 */
@org.hibernate.Internal
public class DeleteCoordinatorHistory
		extends AbstractMutationCoordinator
		implements DeleteCoordinator {

	private final DeleteCoordinator currentDeleteCoordinator;
	private final EntityTableMapping historyTableMapping;
	private final TemporalMapping temporalMapping;
	private final BasicBatchKey historyBatchKey;
	private final MutationOperationGroup historyEndUpdateGroup;
	private final MutationOperationGroup unversionedHistoryEndUpdateGroup;

	public DeleteCoordinatorHistory(
			@Nonnull EntityPersister entityPersister,
			@Nonnull SessionFactoryImplementor factory,
			@Nonnull DeleteCoordinator currentDeleteCoordinator) {
		super( entityPersister, factory );
		this.currentDeleteCoordinator = currentDeleteCoordinator;
		this.temporalMapping = castNonNull( entityPersister.getTemporalMapping() );
		this.historyTableMapping =
				createAuxiliaryTableMapping( entityPersister.getIdentifierTableMapping(),
						entityPersister, temporalMapping.getTableName() );
		this.historyBatchKey = new BasicBatchKey( entityPersister.getEntityName() + "#HISTORY_DELETE" );
		final boolean versioned = entityPersister.optimisticLockStyle().isVersion()
				&& entityPersister.getVersionMapping() != null;
		this.historyEndUpdateGroup = buildHistoryEndUpdateGroup( versioned );
		this.unversionedHistoryEndUpdateGroup = versioned ? buildHistoryEndUpdateGroup( false ) : historyEndUpdateGroup;
	}

	@Nullable
	@Override
	public MutationOperationGroup getStaticMutationOperationGroup() {
		return currentDeleteCoordinator.getStaticMutationOperationGroup();
	}

	@Nullable
	@Override
	protected BatchKey getBatchKey() {
		return historyBatchKey;
	}

	@Override
	public void delete(
			@Nullable Object entity,
			@Nonnull Object id,
			@Nullable Object version,
			@Nonnull SharedSessionContractImplementor session) {
		currentDeleteCoordinator.delete( entity, id, version, session );
		performHistoryEndingUpdate( entity, id, version, session );
	}

	private void performHistoryEndingUpdate(
			@Nullable Object entity,
			@Nonnull Object id,
			@Nullable Object oldVersion,
			@Nonnull SharedSessionContractImplementor session) {
		final var operationGroup = entity == null ? unversionedHistoryEndUpdateGroup : historyEndUpdateGroup;
		final var mutationExecutor =
				mutationExecutorService.createExecutor( resolveBatchKeyAccess( operationGroup != historyEndUpdateGroup, session ),
						operationGroup, session );
		try {
			final var jdbcValueBindings = mutationExecutor.getJdbcValueBindings();
			for ( int i = 0; i < operationGroup.getNumberOfOperations(); i++ ) {
				final var operation = operationGroup.getOperation( i );
				breakDownKeyJdbcValues( id, null, session, jdbcValueBindings,
						(EntityTableMapping) operation.getTableDetails() );
			}

			final var versionMapping = entityPersister().getVersionMapping();
			if ( entity != null && versionMapping != null && entityPersister().optimisticLockStyle().isVersion() ) {
				jdbcValueBindings.bindValue(
						oldVersion,
						historyTableMapping.getTableName(),
						versionMapping.getSelectionExpression(),
						ParameterUsage.RESTRICT
				);
			}

			if ( TemporalMutationHelper.isUsingParameters( session ) ) {
				jdbcValueBindings.bindValue(
						session.getCurrentChangesetIdentifier(),
						historyTableMapping.getTableName(),
						temporalMapping.getEndingColumnMapping().getSelectionExpression(),
						ParameterUsage.SET
				);
			}

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

	@Nonnull
	private MutationOperationGroup buildHistoryEndUpdateGroup(boolean applyVersion) {
		final var tableUpdateBuilder =
				new TableUpdateBuilderStandard<>( entityPersister(), historyTableMapping, factory() );

		applyKeyRestriction( null, entityPersister(), tableUpdateBuilder, historyTableMapping );
		applyTemporalEnding( tableUpdateBuilder, temporalMapping );
		if ( applyVersion ) {
			applyVersionOptimisticLocking( tableUpdateBuilder );
		}

		final var tableMutation = tableUpdateBuilder.buildMutation();
		return singleOperation(
				new MutationGroupSingle( MutationType.DELETE, entityPersister(), tableMutation ),
				tableMutation.createMutationOperation( null, factory() )
		);
	}
}
