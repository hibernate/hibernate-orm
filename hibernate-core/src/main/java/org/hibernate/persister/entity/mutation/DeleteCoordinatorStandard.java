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
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.ast.ColumnValueBindingList;
import org.hibernate.sql.model.ast.builder.MutationGroupBuilder;
import org.hibernate.sql.model.ast.builder.RestrictedTableMutationBuilder;
import org.hibernate.sql.model.ast.builder.TableDeleteBuilder;
import org.hibernate.sql.model.ast.builder.TableDeleteBuilderSkipped;
import org.hibernate.sql.model.ast.builder.TableDeleteBuilderStandard;

/**
 * Coordinates standard deleting of an entity.
 *
 * @author Steve Ebersole
 */
public class DeleteCoordinatorStandard extends AbstractDeleteCoordinator {

	public DeleteCoordinatorStandard(EntityPersister entityPersister, SessionFactoryImplementor factory) {
		super( entityPersister, factory );
	}

	@Override
	protected MutationOperationGroup generateOperationGroup(
			Object rowId,
			Object[] loadedState,
			boolean applyVersion,
			SharedSessionContractImplementor session) {
		final MutationGroupBuilder deleteGroupBuilder = new MutationGroupBuilder( MutationType.DELETE, entityPersister() );

		entityPersister().forEachMutableTableReverse( (tableMapping) -> {
			final TableDeleteBuilder tableDeleteBuilder = tableMapping.isCascadeDeleteEnabled()
					? new TableDeleteBuilderSkipped( tableMapping )
					: new TableDeleteBuilderStandard( entityPersister(), tableMapping, factory() );
			deleteGroupBuilder.addTableDetailsBuilder( tableDeleteBuilder );
		} );

		applyTableDeleteDetails( deleteGroupBuilder, rowId, loadedState, applyVersion, session );

		return createOperationGroup( null, deleteGroupBuilder.buildMutationGroup() );
	}

	private void applyTableDeleteDetails(
			MutationGroupBuilder deleteGroupBuilder,
			Object rowId,
			Object[] loadedState,
			boolean applyVersion,
			SharedSessionContractImplementor session) {
		// first, the table key column(s)
		deleteGroupBuilder.forEachTableMutationBuilder( (builder) -> {
			final EntityTableMapping tableMapping = (EntityTableMapping) builder.getMutatingTable().getTableMapping();
			final TableDeleteBuilder tableDeleteBuilder = (TableDeleteBuilder) builder;
			applyKeyRestriction( rowId, entityPersister(), tableDeleteBuilder, tableMapping );
		} );

		if ( applyVersion ) {
			// apply any optimistic locking
			applyOptimisticLocking( deleteGroupBuilder, loadedState, session );
			final EntityPersister persister = entityPersister();
			if ( persister.hasPartitionedSelectionMapping() ) {
				final AttributeMappingsList attributeMappings = persister.getAttributeMappings();
				for ( int m = 0; m < attributeMappings.size(); m++ ) {
					final AttributeMapping attributeMapping = attributeMappings.get( m );
					final int jdbcTypeCount = attributeMapping.getJdbcTypeCount();
					for ( int i = 0; i < jdbcTypeCount; i++ ) {
						final SelectableMapping selectableMapping = attributeMapping.getSelectable( i );
						if ( selectableMapping.isPartitioned() ) {
							final String tableNameForMutation =
									persister.physicalTableNameForMutation( selectableMapping );
							final RestrictedTableMutationBuilder<?, ?> rootTableMutationBuilder =
									deleteGroupBuilder.findTableDetailsBuilder( tableNameForMutation );
							rootTableMutationBuilder.addKeyRestrictionLeniently( selectableMapping );
						}
					}
				}
			}
		}
	}

	protected void applyOptimisticLocking(
			MutationGroupBuilder mutationGroupBuilder,
			Object[] loadedState,
			SharedSessionContractImplementor session) {
		final OptimisticLockStyle optimisticLockStyle = entityPersister().optimisticLockStyle();
		if ( optimisticLockStyle.isVersion() && entityPersister().getVersionMapping() != null ) {
			applyVersionBasedOptLocking( mutationGroupBuilder );
		}
		else if ( loadedState != null && entityPersister().optimisticLockStyle().isAllOrDirty() ) {
			applyNonVersionOptLocking(
					optimisticLockStyle,
					mutationGroupBuilder,
					loadedState,
					session
			);
		}
	}

	protected void applyVersionBasedOptLocking(MutationGroupBuilder mutationGroupBuilder) {
		assert entityPersister().optimisticLockStyle() == OptimisticLockStyle.VERSION;
		assert entityPersister().getVersionMapping() != null;

		final String tableNameForMutation = entityPersister().physicalTableNameForMutation( entityPersister().getVersionMapping() );
		final RestrictedTableMutationBuilder<?,?> rootTableMutationBuilder = mutationGroupBuilder.findTableDetailsBuilder( tableNameForMutation );
		rootTableMutationBuilder.addOptimisticLockRestriction( entityPersister().getVersionMapping() );
	}

	protected void applyNonVersionOptLocking(
			OptimisticLockStyle lockStyle,
			MutationGroupBuilder mutationGroupBuilder,
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
			if ( versionability[attributeIndex] && !( attribute = persister.getAttributeMapping( attributeIndex ) ).isPluralAttributeMapping() ) {
				breakDownJdbcValues( mutationGroupBuilder, session, attribute, loadedState[attributeIndex] );
			}
		}
	}

	private void breakDownJdbcValues(
			MutationGroupBuilder mutationGroupBuilder,
			SharedSessionContractImplementor session,
			AttributeMapping attribute,
			Object loadedValue) {
		final RestrictedTableMutationBuilder<?, ?> tableMutationBuilder =
				mutationGroupBuilder.findTableDetailsBuilder( attribute.getContainingTableExpression() );
		if ( tableMutationBuilder != null ) {
			final ColumnValueBindingList optimisticLockBindings = tableMutationBuilder.getOptimisticLockBindings();
			if ( optimisticLockBindings != null ) {
				attribute.breakDownJdbcValues(
						loadedValue,
						(valueIndex, value, jdbcValueMapping) -> {
							if ( !tableMutationBuilder.getKeyRestrictionBindings()
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

}
