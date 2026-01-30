/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.TemporalMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.MutationOperationGroup;

/**
 * Merge coordinator for
 * {@link org.hibernate.cfg.TemporalTableStrategy#HISTORY_TABLE}
 * temporal strategy.
 *
 * @author Gavin King
 */
public class MergeCoordinatorHistory extends UpdateCoordinatorHistory {
	public MergeCoordinatorHistory(EntityPersister entityPersister, SessionFactoryImplementor factory) {
		super( entityPersister, factory, new MergeCoordinator( entityPersister, factory ) );
	}

	@Override
	void performRowEndUpdate(
			Object entity,
			Object id,
			Object rowId,
			Object oldVersion,
			SharedSessionContractImplementor session,
			TemporalMapping temporalMapping,
			MutationOperationGroup endUpdateGroup,
			String temporalTableName) {
		final var mutationExecutor =
				mutationExecutorService.createExecutor( resolveBatchKeyAccess( false, session ),
						endUpdateGroup, session );
		try {
			final var jdbcValueBindings = mutationExecutor.getJdbcValueBindings();
			for ( int i = 0; i < endUpdateGroup.getNumberOfOperations(); i++ ) {
				breakDownKeyJdbcValues( id, rowId, session, jdbcValueBindings,
						(EntityTableMapping) endUpdateGroup.getOperation( i ).getTableDetails() );
			}

			bindVersionRestriction( oldVersion, jdbcValueBindings, temporalTableName );

			if ( TemporalMutationHelper.isUsingParameters( session ) ) {
				jdbcValueBindings.bindValue(
						session.getCurrentTransactionIdentifier(),
						temporalTableName,
						temporalMapping.getEndingColumnMapping().getSelectionExpression(),
						ParameterUsage.SET
				);
			}

			mutationExecutor.execute(
					entity,
					null,
					null,
					(statementDetails, affectedRowCount, batchPosition) ->
							affectedRowCount != 0
									&& resultCheck( id, statementDetails, affectedRowCount, batchPosition ),
					session,
					staleStateException -> staleObjectStateException( id, staleStateException )
			);
		}
		finally {
			mutationExecutor.release();
		}
	}
}
