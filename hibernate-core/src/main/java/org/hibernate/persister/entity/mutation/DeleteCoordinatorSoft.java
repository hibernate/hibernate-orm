/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.AttributeMappingsList;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SoftDeleteMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.ast.ColumnValueBindingList;
import org.hibernate.sql.model.ast.RestrictedTableMutation;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilder;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilderStandard;
import org.hibernate.sql.model.internal.MutationGroupSingle;
import org.hibernate.sql.model.internal.MutationOperationGroupFactory;

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
		final EntityTableMapping rootTableMapping = entityPersister().getIdentifierTableMapping();
		final TableUpdateBuilderStandard<MutationOperation> tableUpdateBuilder = new TableUpdateBuilderStandard<>(
				entityPersister(),
				rootTableMapping,
				factory()
		);

		applyKeyRestriction( rowId, entityPersister(), tableUpdateBuilder, rootTableMapping );
		applySoftDelete( entityPersister().getSoftDeleteMapping(), tableUpdateBuilder );
		applyPartitionKeyRestriction( tableUpdateBuilder );
		applyOptimisticLocking( tableUpdateBuilder, loadedState, session );

		final RestrictedTableMutation<MutationOperation> tableMutation = tableUpdateBuilder.buildMutation();
		final MutationGroupSingle mutationGroup = new MutationGroupSingle(
				MutationType.DELETE,
				entityPersister(),
				tableMutation
		);

		final MutationOperation mutationOperation = tableMutation.createMutationOperation( null, factory() );
		return MutationOperationGroupFactory.singleOperation( mutationGroup, mutationOperation );
	}

	private void applyPartitionKeyRestriction(TableUpdateBuilder<?> tableUpdateBuilder) {
		final EntityPersister persister = entityPersister();
		if ( persister.hasPartitionedSelectionMapping() ) {
			final AttributeMappingsList attributeMappings = persister.getAttributeMappings();
			for ( int m = 0; m < attributeMappings.size(); m++ ) {
				final AttributeMapping attributeMapping = attributeMappings.get( m );
				final int jdbcTypeCount = attributeMapping.getJdbcTypeCount();
				for ( int i = 0; i < jdbcTypeCount; i++ ) {
					final SelectableMapping selectableMapping = attributeMapping.getSelectable( i );
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
		final ColumnReference softDeleteColumnReference = new ColumnReference( tableUpdateBuilder.getMutatingTable(), softDeleteMapping );

		// apply the assignment
		tableUpdateBuilder.addValueColumn( softDeleteMapping.createDeletedValueBinding( softDeleteColumnReference ) );
		// apply the restriction
		tableUpdateBuilder.addNonKeyRestriction( softDeleteMapping.createNonDeletedValueBinding( softDeleteColumnReference ) );
	}

	protected void applyOptimisticLocking(
			TableUpdateBuilderStandard<MutationOperation> tableUpdateBuilder,
			Object[] loadedState,
			SharedSessionContractImplementor session) {
		final OptimisticLockStyle optimisticLockStyle = entityPersister().optimisticLockStyle();
		if ( optimisticLockStyle.isVersion() && entityPersister().getVersionMapping() != null ) {
			applyVersionBasedOptLocking( tableUpdateBuilder );
		}
		else if ( loadedState != null && entityPersister().optimisticLockStyle().isAllOrDirty() ) {
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
		final EntityPersister persister = entityPersister();
		assert loadedState != null;
		assert lockStyle.isAllOrDirty();
		assert persister.optimisticLockStyle().isAllOrDirty();
		assert session != null;

		final boolean[] versionability = persister.getPropertyVersionability();
		for ( int attributeIndex = 0; attributeIndex < versionability.length; attributeIndex++ ) {
			final AttributeMapping attribute;
			// only makes sense to lock on singular attributes which are not excluded from optimistic locking
			if ( versionability[attributeIndex]
					&& !( attribute = persister.getAttributeMapping( attributeIndex ) ).isPluralAttributeMapping() ) {
				breakDownJdbcValues( tableUpdateBuilder, session, attribute, loadedState[attributeIndex] );
			}
		}
	}

	private void breakDownJdbcValues(
			TableUpdateBuilderStandard<MutationOperation> tableUpdateBuilder,
			SharedSessionContractImplementor session,
			AttributeMapping attribute,
			Object loadedValue) {
		if ( !tableUpdateBuilder.getMutatingTable()
				.getTableName()
				.equals( attribute.getContainingTableExpression() ) ) {
			// it is not on the root table, skip it
			return;
		}

		final ColumnValueBindingList optimisticLockBindings = tableUpdateBuilder.getOptimisticLockBindings();
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
					}
					,
					session
			);
		}
	}
}
