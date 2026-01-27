/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.StaleObjectStateException;
import org.hibernate.StaleStateException;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.EntityVersionMapping;
import org.hibernate.metamodel.mapping.TemporalMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilderStandard;
import org.hibernate.sql.model.internal.MutationGroupSingle;

import static org.hibernate.engine.jdbc.mutation.internal.ModelMutationHelper.identifiedResultsCheck;
import static org.hibernate.sql.model.internal.MutationOperationGroupFactory.singleOperation;

/**
 * Delete coordinator for HISTORY temporal strategy.
 */
public class HistoryDeleteCoordinator extends AbstractMutationCoordinator implements DeleteCoordinator {
	private final DeleteCoordinator currentDeleteCoordinator;
	private final EntityTableMapping historyTableMapping;
	private final TemporalMapping temporalMapping;
	private final BasicBatchKey historyBatchKey;
	private final MutationOperationGroup historyEndUpdateGroup;

	public HistoryDeleteCoordinator(
			EntityPersister entityPersister,
			SessionFactoryImplementor factory,
			DeleteCoordinator currentDeleteCoordinator) {
		super( entityPersister, factory );
		this.currentDeleteCoordinator = currentDeleteCoordinator;
		this.temporalMapping = entityPersister.getTemporalMapping();
		this.historyTableMapping = HistoryTableMappingHelper.createHistoryTableMapping(
				entityPersister.getIdentifierTableMapping(),
				entityPersister,
				temporalMapping.getTableName()
		);
		this.historyBatchKey = new BasicBatchKey( entityPersister.getEntityName() + "#HISTORY_DELETE" );
		this.historyEndUpdateGroup = buildHistoryEndUpdateGroup();
	}

	@Override
	public MutationOperationGroup getStaticMutationOperationGroup() {
		return currentDeleteCoordinator.getStaticMutationOperationGroup();
	}

	@Override
	protected BatchKey getBatchKey() {
		return historyBatchKey;
	}

	@Override
	public void delete(
			Object entity,
			Object id,
			Object version,
			SharedSessionContractImplementor session) {
		currentDeleteCoordinator.delete( entity, id, version, session );
		performHistoryEndingUpdate( entity, id, version, session );
	}

	private void performHistoryEndingUpdate(
			Object entity,
			Object id,
			Object oldVersion,
			SharedSessionContractImplementor session) {
		final MutationExecutor mutationExecutor =
				mutationExecutorService.createExecutor( resolveBatchKeyAccess( false, session ), historyEndUpdateGroup, session );
		try {
			final JdbcValueBindings jdbcValueBindings = mutationExecutor.getJdbcValueBindings();
			for ( int i = 0; i < historyEndUpdateGroup.getNumberOfOperations(); i++ ) {
				final var operation = historyEndUpdateGroup.getOperation( i );
				breakDownKeyJdbcValues( id, null, session, jdbcValueBindings, (EntityTableMapping) operation.getTableDetails() );
			}

			final EntityVersionMapping versionMapping = entityPersister().getVersionMapping();
			if ( versionMapping != null && entityPersister().optimisticLockStyle().isVersion() ) {
				jdbcValueBindings.bindValue(
						oldVersion,
						historyTableMapping.getTableName(),
						versionMapping.getSelectionExpression(),
						ParameterUsage.RESTRICT
				);
			}

			if ( TemporalMutationHelper.isUsingParameters( session ) ) {
				jdbcValueBindings.bindValue(
						session.getTransactionStartInstant(),
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

	private MutationOperationGroup buildHistoryEndUpdateGroup() {
		final TableUpdateBuilderStandard<MutationOperation> tableUpdateBuilder =
				new TableUpdateBuilderStandard<>( entityPersister(), historyTableMapping, factory() );

		applyKeyRestriction( null, entityPersister(), tableUpdateBuilder, historyTableMapping );
		applyTemporalEnding( tableUpdateBuilder );
		applyOptimisticLocking( tableUpdateBuilder );

		final var tableMutation = tableUpdateBuilder.buildMutation();
		final MutationGroupSingle mutationGroup = new MutationGroupSingle(
				MutationType.DELETE,
				entityPersister(),
				tableMutation
		);

		return singleOperation( mutationGroup, tableMutation.createMutationOperation( null, factory() ) );
	}

	private void applyTemporalEnding(TableUpdateBuilderStandard<MutationOperation> tableUpdateBuilder) {
		final var endingColumnReference =
				new ColumnReference( tableUpdateBuilder.getMutatingTable(), temporalMapping.getEndingColumnMapping() );
		tableUpdateBuilder.addValueColumn( temporalMapping.createEndingValueBinding( endingColumnReference ) );
		tableUpdateBuilder.addNonKeyRestriction( temporalMapping.createNullEndingValueBinding( endingColumnReference ) );
	}

	private void applyOptimisticLocking(TableUpdateBuilderStandard<MutationOperation> tableUpdateBuilder) {
		if ( entityPersister().optimisticLockStyle() == OptimisticLockStyle.VERSION
				&& entityPersister().getVersionMapping() != null ) {
			tableUpdateBuilder.addOptimisticLockRestriction( entityPersister().getVersionMapping() );
		}
	}

	private boolean resultCheck(
			Object id,
			PreparedStatementDetails statementDetails,
			int affectedRowCount,
			int batchPosition) {
		return identifiedResultsCheck(
				statementDetails,
				affectedRowCount,
				batchPosition,
				entityPersister(),
				id,
				factory()
		);
	}

	private StaleObjectStateException staleObjectStateException(Object id, StaleStateException cause) {
		return new StaleObjectStateException( entityPersister().getEntityName(), id, cause );
	}
}
