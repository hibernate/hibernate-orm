/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilderStandard;

import static org.hibernate.persister.entity.mutation.AbstractTemporalUpdateCoordinator.applyTemporalEnding;

/**
 * Delete coordinator for
 * {@link org.hibernate.cfg.TemporalTableStrategy#SINGLE_TABLE}
 * temporal strategy.
 *
 * @author Gavin King
 */
public class DeleteCoordinatorTemporal extends AbstractDeleteCoordinator {
	public DeleteCoordinatorTemporal(EntityPersister entityPersister, SessionFactoryImplementor factory) {
		super( entityPersister, factory );
	}

	@Override
	protected void applyStaticDeleteTableDetails(
			Object id,
			Object rowId,
			Object[] loadedState,
			Object version,
			boolean applyVersion,
			MutationExecutor mutationExecutor,
			SharedSessionContractImplementor session) {
		super.applyStaticDeleteTableDetails( id, rowId, loadedState, version, applyVersion, mutationExecutor, session );
		bindTemporalEndingValue( session, mutationExecutor.getJdbcValueBindings() );
	}

	@Override
	protected void applyDynamicDeleteTableDetails(
			Object id,
			Object rowId,
			Object[] loadedState,
			MutationExecutor mutationExecutor,
			MutationOperationGroup operationGroup,
			SharedSessionContractImplementor session) {
		super.applyDynamicDeleteTableDetails( id, rowId, loadedState, mutationExecutor, operationGroup, session );
		bindTemporalEndingValue( session, mutationExecutor.getJdbcValueBindings() );
	}

	private void bindTemporalEndingValue(
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings) {
		final var temporalMapping = entityPersister().getTemporalMapping();
		if ( temporalMapping != null && TemporalMutationHelper.isUsingParameters( session ) ) {
			jdbcValueBindings.bindValue(
					session.getTransactionStartInstant(),
					entityPersister().physicalTableNameForMutation( temporalMapping.getEndingColumnMapping() ),
					temporalMapping.getEndingColumnMapping().getSelectionExpression(),
					ParameterUsage.SET
			);
		}
	}

	@Override
	protected MutationOperationGroup generateOperationGroup(
			Object rowId,
			Object[] loadedState,
			boolean applyVersion,
			SharedSessionContractImplementor session) {
		final var rootTableMapping = entityPersister().getIdentifierTableMapping();
		final var tableUpdateBuilder = new TableUpdateBuilderStandard<>( entityPersister(), rootTableMapping, factory() );

		applyKeyRestriction( rowId, entityPersister(), tableUpdateBuilder, rootTableMapping );
		applyTemporalEnding( tableUpdateBuilder, entityPersister().getTemporalMapping() );
		applyPartitionKeyRestriction( tableName -> tableUpdateBuilder );
		applyOptimisticLocking(
				entityPersister().optimisticLockStyle(),
				tableMutationBuilderResolver( tableUpdateBuilder ),
				loadedState,
				session
		);

		return createMutationOperationGroup( tableUpdateBuilder );
	}
}
