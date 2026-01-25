/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.StaleObjectStateException;
import org.hibernate.StaleStateException;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.metamodel.mapping.EntityVersionMapping;
import org.hibernate.metamodel.mapping.TemporalMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.ast.MutationGroup;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilderStandard;
import org.hibernate.sql.model.internal.MutationGroupSingle;

import static org.hibernate.engine.jdbc.mutation.internal.ModelMutationHelper.identifiedResultsCheck;
import static org.hibernate.sql.model.internal.MutationOperationGroupFactory.singleOperation;

/**
 * Update coordinator for temporal entities.
 */
public class TemporalUpdateCoordinator extends AbstractMutationCoordinator implements UpdateCoordinator {
	private final TemporalMapping temporalMapping;
	private final MutationOperationGroup endingUpdateGroup;
	private final BasicBatchKey batchKey;
	private final UpdateCoordinatorStandard versionUpdateDelegate;

	public TemporalUpdateCoordinator(
			EntityPersister entityPersister,
			SessionFactoryImplementor factory) {
		super( entityPersister, factory );
		this.temporalMapping = entityPersister.getTemporalMapping();
		this.endingUpdateGroup = buildEndingUpdateGroup();
		this.batchKey = new BasicBatchKey( entityPersister.getEntityName() + "#TEMPORAL_UPDATE" );
		this.versionUpdateDelegate = new UpdateCoordinatorStandard( entityPersister, factory );
	}

	@Override
	public MutationOperationGroup getStaticMutationOperationGroup() {
		return endingUpdateGroup;
	}

	@Override
	protected BasicBatchKey getBatchKey() {
		return batchKey;
	}

	@Override
	public GeneratedValues update(
			Object entity,
			Object id,
			Object rowId,
			Object[] values,
			Object oldVersion,
			Object[] incomingOldValues,
			int[] dirtyAttributeIndexes,
			boolean hasDirtyCollection,
			SharedSessionContractImplementor session) {
		performEndingUpdate( entity, id, rowId, oldVersion, session );
		return entityPersister().getInsertCoordinator().insert( entity, id, values, session );
	}

	private void performEndingUpdate(
			Object entity,
			Object id,
			Object rowId,
			Object oldVersion,
			SharedSessionContractImplementor session) {
		final MutationExecutor mutationExecutor =
				mutationExecutorService.createExecutor( resolveBatchKeyAccess( false, session ), endingUpdateGroup, session );
		try {
			final JdbcValueBindings jdbcValueBindings = mutationExecutor.getJdbcValueBindings();
			for ( int i = 0; i < endingUpdateGroup.getNumberOfOperations(); i++ ) {
				final var operation = endingUpdateGroup.getOperation( i );
				breakDownKeyJdbcValues( id, rowId, session, jdbcValueBindings, (EntityTableMapping) operation.getTableDetails() );
			}

			final EntityVersionMapping versionMapping = entityPersister().getVersionMapping();
			if ( versionMapping != null && entityPersister().optimisticLockStyle().isVersion() ) {
				jdbcValueBindings.bindValue( oldVersion, versionMapping, ParameterUsage.RESTRICT );
			}

			jdbcValueBindings.bindValue(
					session.getTransactionStartInstant(),
					entityPersister().physicalTableNameForMutation( temporalMapping.getEndingColumnMapping() ),
					temporalMapping.getEndingColumnMapping().getSelectionExpression(),
					ParameterUsage.SET
			);

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

	private MutationOperationGroup buildEndingUpdateGroup() {
		final var tableMapping = entityPersister().getIdentifierTableMapping();
		final TableUpdateBuilderStandard<MutationOperation> tableUpdateBuilder =
				new TableUpdateBuilderStandard<>( entityPersister(), tableMapping, factory() );

		applyKeyRestriction( null, entityPersister(), tableUpdateBuilder, tableMapping );
		applyTemporalEnding( tableUpdateBuilder );
		applyPartitionKeyRestriction( tableUpdateBuilder );
		applyOptimisticLocking( tableUpdateBuilder );

		final var tableMutation = tableUpdateBuilder.buildMutation();
		final MutationGroup mutationGroup = new MutationGroupSingle(
				MutationType.UPDATE,
				entityPersister(),
				tableMutation
		);

		final var mutationOperation = tableMutation.createMutationOperation( null, factory() );
		return singleOperation( mutationGroup, mutationOperation );
	}

	private void applyTemporalEnding(TableUpdateBuilderStandard<MutationOperation> tableUpdateBuilder) {
		final var endingColumnReference =
				new org.hibernate.sql.ast.tree.expression.ColumnReference(
						tableUpdateBuilder.getMutatingTable(),
						temporalMapping.getEndingColumnMapping()
				);
		tableUpdateBuilder.addValueColumn( temporalMapping.createEndingValueBinding( endingColumnReference ) );
		tableUpdateBuilder.addNonKeyRestriction( temporalMapping.createNullEndingValueBinding( endingColumnReference ) );
	}

	private void applyPartitionKeyRestriction(TableUpdateBuilderStandard<MutationOperation> tableUpdateBuilder) {
		final var persister = entityPersister();
		if ( persister.hasPartitionedSelectionMapping() ) {
			final var attributeMappings = persister.getAttributeMappings();
			for ( int m = 0; m < attributeMappings.size(); m++ ) {
				final var attributeMapping = attributeMappings.get( m );
				final int jdbcTypeCount = attributeMapping.getJdbcTypeCount();
				for ( int i = 0; i < jdbcTypeCount; i++ ) {
					final var selectableMapping = attributeMapping.getSelectable( i );
					if ( selectableMapping.isPartitioned() ) {
						tableUpdateBuilder.addKeyRestrictionLeniently( selectableMapping );
					}
				}
			}
		}
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

	@Override
	public void forceVersionIncrement(
			Object id,
			Object currentVersion,
			Object nextVersion,
			SharedSessionContractImplementor session) {
		versionUpdateDelegate.forceVersionIncrement( id, currentVersion, nextVersion, session );
	}
}
