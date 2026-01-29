/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.TemporalMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilderStandard;
import org.hibernate.sql.model.internal.MutationGroupSingle;

import static org.hibernate.persister.entity.mutation.AbstractTemporalUpdateCoordinator.applyTemporalEnding;
import static org.hibernate.sql.model.internal.MutationOperationGroupFactory.singleOperation;

/**
 * Delete coordinator for
 * {@link org.hibernate.cfg.TemporalTableStrategy#HISTORY_TABLE}
 * temporal strategy.
 *
 * @author Gavin King
 */
public class DeleteCoordinatorHistory
		extends AbstractMutationCoordinator
		implements DeleteCoordinator {

	private final DeleteCoordinator currentDeleteCoordinator;
	private final EntityTableMapping historyTableMapping;
	private final TemporalMapping temporalMapping;
	private final BasicBatchKey historyBatchKey;
	private final MutationOperationGroup historyEndUpdateGroup;

	public DeleteCoordinatorHistory(
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
		final var mutationExecutor =
				mutationExecutorService.createExecutor( resolveBatchKeyAccess( false, session ),
						historyEndUpdateGroup, session );
		try {
			final var jdbcValueBindings = mutationExecutor.getJdbcValueBindings();
			for ( int i = 0; i < historyEndUpdateGroup.getNumberOfOperations(); i++ ) {
				final var operation = historyEndUpdateGroup.getOperation( i );
				breakDownKeyJdbcValues( id, null, session, jdbcValueBindings,
						(EntityTableMapping) operation.getTableDetails() );
			}

			final var versionMapping = entityPersister().getVersionMapping();
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
						session.getCurrentTransactionIdentifier(),
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
		final var tableUpdateBuilder =
				new TableUpdateBuilderStandard<>( entityPersister(), historyTableMapping, factory() );

		applyKeyRestriction( null, entityPersister(), tableUpdateBuilder, historyTableMapping );
		applyTemporalEnding( tableUpdateBuilder, temporalMapping );
		if ( entityPersister().optimisticLockStyle().isVersion() ) {
			applyVersionOptimisticLocking( tableUpdateBuilder );
		}

		final var tableMutation = tableUpdateBuilder.buildMutation();
		return singleOperation(
				new MutationGroupSingle( MutationType.DELETE, entityPersister(), tableMutation ),
				tableMutation.createMutationOperation( null, factory() )
		);
	}
}
