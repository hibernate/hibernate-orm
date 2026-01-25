/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.TemporalMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilderStandard;
import org.hibernate.sql.model.internal.MutationGroupSingle;

import static org.hibernate.sql.model.internal.MutationOperationGroupFactory.singleOperation;

/**
 * DeleteCoordinator for temporal entities.
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
		final TableUpdateBuilderStandard<MutationOperation> tableUpdateBuilder = new TableUpdateBuilderStandard<>(
				entityPersister(),
				rootTableMapping,
				factory()
		);

		applyKeyRestriction( rowId, entityPersister(), tableUpdateBuilder, rootTableMapping );
		applyTemporalEnding( entityPersister().getTemporalMapping(), tableUpdateBuilder );
		applyPartitionKeyRestriction( tableUpdateBuilder );
		applyOptimisticLocking( tableUpdateBuilder, loadedState, session );

		final var tableMutation = tableUpdateBuilder.buildMutation();
		final MutationGroupSingle mutationGroup = new MutationGroupSingle(
				MutationType.DELETE,
				entityPersister(),
				tableMutation
		);

		final var mutationOperation = tableMutation.createMutationOperation( null, factory() );
		return singleOperation( mutationGroup, mutationOperation );
	}

	private void applyTemporalEnding(
			TemporalMapping temporalMapping,
			TableUpdateBuilderStandard<MutationOperation> tableUpdateBuilder) {
		final var endingColumnReference =
				new ColumnReference( tableUpdateBuilder.getMutatingTable(), temporalMapping.getEndingColumnMapping() );

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

	private void applyOptimisticLocking(
			TableUpdateBuilderStandard<MutationOperation> tableUpdateBuilder,
			Object[] loadedState,
			SharedSessionContractImplementor session) {
		final var persister = entityPersister();
		final var optimisticLockStyle = persister.optimisticLockStyle();
		if ( optimisticLockStyle.isVersion() && persister.getVersionMapping() != null ) {
			tableUpdateBuilder.addOptimisticLockRestriction( persister.getVersionMapping() );
		}
		else if ( loadedState != null && optimisticLockStyle.isAllOrDirty() ) {
			applyNonVersionOptLocking(
					optimisticLockStyle,
					tableUpdateBuilder,
					loadedState,
					session
			);
		}
	}

	private void applyNonVersionOptLocking(
			OptimisticLockStyle lockStyle,
			TableUpdateBuilderStandard<MutationOperation> tableUpdateBuilder,
			Object[] loadedState,
			SharedSessionContractImplementor session) {
		assert loadedState != null;
		assert lockStyle.isAllOrDirty();
		assert entityPersister().optimisticLockStyle().isAllOrDirty();
		assert session != null;

		final boolean[] versionability = entityPersister().getPropertyVersionability();
		for ( int attributeIndex = 0; attributeIndex < versionability.length; attributeIndex++ ) {
			if ( versionability[attributeIndex] ) {
				final var attribute = entityPersister().getAttributeMapping( attributeIndex );
				if ( !attribute.isPluralAttributeMapping() ) {
					breakDownJdbcValues( tableUpdateBuilder, session, attribute, loadedState[attributeIndex] );
				}
			}
		}
	}

	private void breakDownJdbcValues(
			TableUpdateBuilderStandard<MutationOperation> tableUpdateBuilder,
			SharedSessionContractImplementor session,
			AttributeMapping attribute,
			Object loadedValue) {
		if ( tableUpdateBuilder.getMutatingTable().getTableName()
				.equals( attribute.getContainingTableExpression() ) ) {
			final var optimisticLockBindings = tableUpdateBuilder.getOptimisticLockBindings();
			if ( optimisticLockBindings != null ) {
				attribute.breakDownJdbcValues(
						loadedValue,
						(valueIndex, value, jdbcValueMapping) -> {
							if ( !tableUpdateBuilder.getKeyRestrictionBindings()
									.containsColumn(
											jdbcValueMapping.getSelectableName(),
											jdbcValueMapping.getJdbcMapping()
									) ) {
								optimisticLockBindings.consume( valueIndex, value, jdbcValueMapping );
							}
						},
						session
				);
			}
		}
	}
}
