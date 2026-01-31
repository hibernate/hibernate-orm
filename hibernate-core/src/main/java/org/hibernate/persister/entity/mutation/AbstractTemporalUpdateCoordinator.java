/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.OperationResultChecker;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.TemporalMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilder;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilderStandard;
import org.hibernate.sql.model.internal.MutationGroupSingle;

import static org.hibernate.sql.model.internal.MutationOperationGroupFactory.singleOperation;

/**
 * @author Gavin King
 */
abstract class AbstractTemporalUpdateCoordinator extends AbstractMutationCoordinator implements UpdateCoordinator {
	AbstractTemporalUpdateCoordinator(EntityPersister entityPersister, SessionFactoryImplementor factory) {
		super( entityPersister, factory );
	}

	static void applyTemporalEnding(TableUpdateBuilder<?> tableUpdateBuilder, TemporalMapping temporalMapping) {
		final var endingColumnReference =
				new ColumnReference( tableUpdateBuilder.getMutatingTable(), temporalMapping.getEndingColumnMapping() );
		tableUpdateBuilder.addValueColumn( temporalMapping.createEndingValueBinding( endingColumnReference ) );
		tableUpdateBuilder.addNonKeyRestriction( temporalMapping.createNullEndingValueBinding( endingColumnReference ) );
	}

	MutationOperationGroup buildEndingUpdateGroup(EntityTableMapping tableMapping, TemporalMapping temporalMapping) {
		final var tableUpdateBuilder =
				new TableUpdateBuilderStandard<>( entityPersister(), tableMapping, factory() );

		applyKeyRestriction( null, entityPersister(), tableUpdateBuilder, tableMapping );
		applyTemporalEnding( tableUpdateBuilder, temporalMapping );
		applyPartitionKeyRestriction( tableUpdateBuilder );
		applyOptimisticLocking( tableUpdateBuilder );

		return createMutationOperationGroup( tableUpdateBuilder );
	}

	MutationOperationGroup createMutationOperationGroup(TableUpdateBuilderStandard<MutationOperation> tableUpdateBuilder) {
		final var tableMutation = tableUpdateBuilder.buildMutation();
		return singleOperation(
				new MutationGroupSingle( MutationType.UPDATE, entityPersister(), tableMutation ),
				tableMutation.createMutationOperation( null, factory() )
		);
	}

	abstract void bindVersionRestriction(Object oldVersion, JdbcValueBindings jdbcValueBindings, String temporalTableName);

	void performRowEndUpdate(
			Object entity,
			Object id,
			Object rowId,
			Object oldVersion,
			SharedSessionContractImplementor session,
			TemporalMapping temporalMapping,
			MutationOperationGroup endUpdateGroup,
			String temporalTableName,
			OperationResultChecker resultChecker) {
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
					resultChecker,
					session,
					staleStateException -> staleObjectStateException( id, staleStateException )
			);
		}
		finally {
			mutationExecutor.release();
		}
	}
}
