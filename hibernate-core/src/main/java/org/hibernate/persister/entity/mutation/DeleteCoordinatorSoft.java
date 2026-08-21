/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.SoftDeleteMapping;
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
 * DeleteCoordinator for soft-deletes
 *
 * @author Steve Ebersole
 */
public class DeleteCoordinatorSoft extends AbstractDeleteCoordinator {
	public DeleteCoordinatorSoft(EntityPersister entityPersister, SessionFactoryImplementor factory) {
		super( entityPersister, factory );
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
		applySoftDelete( entityPersister().getSoftDeleteMapping(), tableUpdateBuilder );
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

	private void applyPartitionKeyRestriction(TableUpdateBuilder<?> tableUpdateBuilder) {
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

	private void applySoftDelete(
			SoftDeleteMapping softDeleteMapping,
			TableUpdateBuilderStandard<MutationOperation> tableUpdateBuilder) {
		final var softDeleteColumnReference = new ColumnReference( tableUpdateBuilder.getMutatingTable(), softDeleteMapping );

		// apply the assignment
		tableUpdateBuilder.addValueColumn( softDeleteMapping.createDeletedValueBinding( softDeleteColumnReference ) );
		// apply the restriction
		tableUpdateBuilder.addNonKeyRestriction( softDeleteMapping.createNonDeletedValueBinding( softDeleteColumnReference ) );
	}

	protected void applyOptimisticLocking(
			TableUpdateBuilderStandard<MutationOperation> tableUpdateBuilder,
			Object[] loadedState,
			SharedSessionContractImplementor session) {
		final var persister = entityPersister();
		final var optimisticLockStyle = persister.optimisticLockStyle();
		if ( optimisticLockStyle.isVersion() && persister.getVersionMapping() != null ) {
			applyVersionBasedOptLocking( tableUpdateBuilder );
		}
		else if ( loadedState != null && persister.optimisticLockStyle().isAllOrDirty() ) {
			applyNonVersionOptLocking(
					optimisticLockStyle,
					tableUpdateBuilder,
					loadedState,
					session
			);
		}
	}

	protected void applyVersionBasedOptLocking(TableUpdateBuilderStandard<MutationOperation> tableUpdateBuilder) {
		assert entityPersister().optimisticLockStyle() == OptimisticLockStyle.VERSION;
		assert entityPersister().getVersionMapping() != null;

		tableUpdateBuilder.addOptimisticLockRestriction( entityPersister().getVersionMapping() );
	}

	protected void applyNonVersionOptLocking(
			OptimisticLockStyle lockStyle,
			TableUpdateBuilderStandard<MutationOperation> tableUpdateBuilder,
			Object[] loadedState,
			SharedSessionContractImplementor session) {
		final var persister = entityPersister();
		assert loadedState != null;
		assert lockStyle.isAllOrDirty();
		assert persister.optimisticLockStyle().isAllOrDirty();
		assert session != null;

		final boolean[] versionability = persister.getPropertyVersionability();
		for ( int attributeIndex = 0; attributeIndex < versionability.length; attributeIndex++ ) {
			// only makes sense to lock on singular attributes which are not excluded from optimistic locking
			if ( versionability[attributeIndex] ) {
				final var attribute = persister.getAttributeMapping( attributeIndex );
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
		// else if it is not on the root table, skip it
	}
}
