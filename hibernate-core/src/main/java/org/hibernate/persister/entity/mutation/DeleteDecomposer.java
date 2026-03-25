/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.action.internal.EntityDeleteAction;
import org.hibernate.action.queue.MutationKind;
import org.hibernate.action.queue.bind.EntityDeleteBindPlan;
import org.hibernate.action.queue.bind.EntitySoftDeleteBindPlan;
import org.hibernate.action.queue.meta.EntityTableDescriptor;
import org.hibernate.action.queue.meta.TableDescriptor;
import org.hibernate.action.queue.mutation.ast.MutatingTableReference;
import org.hibernate.action.queue.mutation.ast.TableDelete;
import org.hibernate.action.queue.mutation.ast.TableMutation;
import org.hibernate.action.queue.mutation.ast.TableUpdate;
import org.hibernate.action.queue.mutation.ast.builder.GraphTableDeleteBuilder;
import org.hibernate.action.queue.mutation.ast.builder.GraphTableDeleteBuilderStandard;
import org.hibernate.action.queue.mutation.ast.builder.GraphTableUpdateBuilder;
import org.hibernate.action.queue.mutation.ast.builder.GraphTableUpdateBuilderStandard;
import org.hibernate.action.queue.op.PlannedOperation;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SoftDeleteMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.model.ast.ColumnValueBinding;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/// [Decomposer][org.hibernate.action.queue.graph.MutationDecomposer] for entity delete operations.
///
/// Converts an [EntityDeleteAction] into a group of [PlannedOperation] to be performed.
///
/// @see EntityDeleteBindPlan
/// @see EntitySoftDeleteBindPlan
///
/// @author Steve Ebersole
public class DeleteDecomposer extends AbstractDecomposer<EntityDeleteAction> {
	private final Map<String, ? extends TableMutation<?>> staticDeleteOperations;  // Map<String, GraphTableDelete> or GraphTableUpdate for soft delete

	public DeleteDecomposer(EntityPersister entityPersister, SessionFactoryImplementor sessionFactory) {
		super( entityPersister, sessionFactory );

		// Generate static operations based on whether soft delete is enabled
		// Soft delete uses UPDATE instead of DELETE
		this.staticDeleteOperations = entityPersister.getSoftDeleteMapping() == null
				? generateStaticOperations()
				: generateSoftDeleteOperation();
	}

	public Map<String, ? extends TableMutation<?>> getStaticDeleteOperations() {
		return staticDeleteOperations;
	}

	@Override
	public List<PlannedOperation> decompose(
			EntityDeleteAction action,
			int ordinalBase,
			SharedSessionContractImplementor session) {
		final Object identifier = action.getId();
		final Object version = action.getVersion();
		final Object[] state = action.getState();

		// PRE-EXECUTION PHASE: Fire PRE_DELETE event listeners
		// Note: Only if the entity instance is loaded (not for unloaded proxy deletes)
		final boolean veto = (action.getInstance() != null) && action.preDelete();

		// Handle natural ID local resolutions before delete
		action.handleNaturalIdLocalResolutions();

		// Lock cache item before delete (returns cacheKey for post-execution)
		final Object cacheKey = action.lockCacheItem();

		// Create post-execution callback for finalization work
		final PostDeleteHandling postDeleteHandling = new PostDeleteHandling(action, cacheKey, veto);

		// If vetoed by PRE_DELETE listener, skip creating DELETE operations
		// Post-execution callback will still run to handle cleanup (cache unlock, etc.)
		if ( veto || action.isCascadeDeleteEnabled() ) {
			return List.of();
		}

		// Check if this is a soft delete
		final SoftDeleteMapping softDeleteMapping = entityPersister.getSoftDeleteMapping();
		final boolean isSoftDelete = softDeleteMapping != null;

		// Determine if we need to apply optimistic locking
		final boolean applyOptimisticLocking = shouldApplyOptimisticLocking( version, state );

		// For soft deletes, use UPDATE mutation kind; for hard deletes, use DELETE
		final MutationKind mutationKind = isSoftDelete ? MutationKind.UPDATE : MutationKind.DELETE;

		if ( isSoftDelete ) {
			// Handle soft delete (single UPDATE operation)
			final var effectiveOperation = chooseEffectiveSoftDeleteOperation( identifier, version, state, session, applyOptimisticLocking );
			final var mutation = effectiveOperation.createMutationOperation();
			final var tableDescriptor = (EntityTableDescriptor) mutation.getTableDescriptor();

			final EntitySoftDeleteBindPlan bindPlan = new EntitySoftDeleteBindPlan(
					tableDescriptor,
					entityPersister,
					identifier,
					version,
					state,
					applyOptimisticLocking
			);

			final PlannedOperation op = new PlannedOperation(
					tableDescriptor,
					mutationKind,
					mutation,
					bindPlan,
					ordinalBase * 1_000,
					"EntityDeleteAction(" + entityPersister.getEntityName() + ")"
			);

			// Attach post-execution callback to the operation
			op.setPostExecutionCallback( postDeleteHandling );

			return List.of(op);
		}
		else {
			// Handle hard delete (multiple DELETE operations in reverse order)
			final var effectiveGroup = chooseEffectiveDeleteGroup( identifier, version, state, session, applyOptimisticLocking );

			final List<PlannedOperation> operations = CollectionHelper.arrayList( effectiveGroup.size() );
			int localOrd = 0;

			for ( Map.Entry<String, TableDelete> entry : effectiveGroup.entrySet() ) {
				var mutation = entry.getValue().createMutationOperation();
				var tableDescriptor = (EntityTableDescriptor) mutation.getTableDescriptor();

				final EntityDeleteBindPlan bindPlan = new EntityDeleteBindPlan(
						tableDescriptor,
						entityPersister,
						identifier,
						version,
						state,
						applyOptimisticLocking
				);

				final PlannedOperation op = new PlannedOperation(
						tableDescriptor,
						mutationKind,
						mutation,
						bindPlan,
						ordinalBase * 1_000 + (localOrd++),
						"EntityDeleteAction(" + entityPersister.getEntityName() + ")"
				);

				operations.add(op);
			}

			// Attach post-execution callback to the last operation
			if ( !operations.isEmpty() ) {
				operations.get( operations.size() - 1 ).setPostExecutionCallback( postDeleteHandling );
			}

			return operations;
		}
	}

	private boolean shouldApplyOptimisticLocking(Object version, Object[] state) {
		final OptimisticLockStyle optimisticLockStyle = entityPersister.optimisticLockStyle();

		if ( optimisticLockStyle.isVersion() ) {
			return version != null && entityPersister.getVersionMapping() != null;
		}
		else if ( optimisticLockStyle.isAllOrDirty() ) {
			return state != null;
		}

		return false;
	}

	@SuppressWarnings("unchecked")
	private Map<String, TableDelete> chooseEffectiveDeleteGroup(
			Object identifier,
			Object version,
			Object[] state,
			SharedSessionContractImplementor session,
			boolean applyOptimisticLocking) {
		// If we need optimistic locking with dirty-check (state-based),
		// we need to generate a dynamic delete group
		final boolean needsDynamicDelete = applyOptimisticLocking
				&& entityPersister.optimisticLockStyle().isAllOrDirty();

		return needsDynamicDelete
				? generateDynamicDeleteOperations( identifier, version, state, session )
				: (Map<String, TableDelete>) staticDeleteOperations;
	}

	private Map<String, TableDelete> generateStaticOperations() {
		final Map<String, GraphTableDeleteBuilder> staticOperationBuilders = new HashMap<>();

		// Process tables in reverse order (child tables before parent)
		entityPersister.forEachMutableTableDescriptorReverse( (tableDescriptor) -> {
			if ( !tableDescriptor.cascadeDeleteEnabled() ) {
				staticOperationBuilders.put(
						tableDescriptor.name(),
						createGraphTableDeleteBuilder(tableDescriptor)
				);
			}
		} );

		applyStaticDeleteDetails( staticOperationBuilders );

		final Map<String, TableDelete> staticOperations = new HashMap<>();
		staticOperationBuilders.forEach( (name, operationBuilder) -> {
			staticOperations.put( name, operationBuilder.buildMutation() );
		} );
		return staticOperations;
	}

	private GraphTableDeleteBuilder createGraphTableDeleteBuilder(TableDescriptor tableDescriptor) {
		return new GraphTableDeleteBuilderStandard(
				entityPersister,
				tableDescriptor,
				sessionFactory
		);
	}

	protected Map<String, TableDelete> generateDynamicDeleteOperations(
			Object identifier,
			Object version,
			Object[] state,
			SharedSessionContractImplementor session) {
		final Map<String, GraphTableDeleteBuilder> operationBuilders = new HashMap<>();

		// Process tables in reverse order
		entityPersister.forEachMutableTableDescriptorReverse( (tableDescriptor) -> {
			if ( !tableDescriptor.cascadeDeleteEnabled() ) {
				operationBuilders.put(
						tableDescriptor.name(),
						createGraphTableDeleteBuilder(tableDescriptor)
				);
			}
		} );

		applyDynamicDeleteDetails( operationBuilders, version, state, session );

		final Map<String, TableDelete> operations = new HashMap<>();
		operationBuilders.forEach( (name, operationBuilder) -> {
			operations.put( name, operationBuilder.buildMutation() );
		} );
		return operations;
	}

	private void applyStaticDeleteDetails(Map<String, GraphTableDeleteBuilder> builders) {
		// Apply key restrictions for all tables
		builders.forEach( (name, builder) -> {
			applyKeyRestriction( builder );
		} );

		// Apply version-based optimistic locking if applicable
		if ( entityPersister.optimisticLockStyle().isVersion() && entityPersister.getVersionMapping() != null ) {
			applyVersionBasedOptLocking( builders );
		}
	}

	private void applyDynamicDeleteDetails(
			Map<String, GraphTableDeleteBuilder> builders,
			Object version,
			Object[] state,
			SharedSessionContractImplementor session) {
		// Apply key restrictions for all tables
		builders.forEach( (name, builder) -> {
			applyKeyRestriction( builder );
		} );

		// Apply optimistic locking
		final var optimisticLockStyle = entityPersister.optimisticLockStyle();
		if ( optimisticLockStyle.isVersion() && entityPersister.getVersionMapping() != null ) {
			applyVersionBasedOptLocking( builders );
		}
		else if ( state != null && optimisticLockStyle.isAllOrDirty() ) {
			applyNonVersionOptLocking( optimisticLockStyle, builders, state, session );
		}

		// Apply partitioned selection restrictions if needed
		if ( entityPersister.hasPartitionedSelectionMapping() && state != null ) {
			applyPartitionedSelectionRestrictions( builders, state );
		}
	}

	private void applyKeyRestriction(GraphTableDeleteBuilder tableDeleteBuilder) {
		tableDeleteBuilder.addKeyRestrictions( tableDeleteBuilder.getTableDescriptor().keyDescriptor() );
	}

	private void applyVersionBasedOptLocking(Map<String, GraphTableDeleteBuilder> builders) {
		assert entityPersister.optimisticLockStyle() == OptimisticLockStyle.VERSION;
		assert entityPersister.getVersionMapping() != null;

		final String tableNameForMutation = entityPersister.physicalTableNameForMutation( entityPersister.getVersionMapping() );
		final GraphTableDeleteBuilder builder = builders.get( tableNameForMutation );
		if ( builder != null ) {
			builder.addOptimisticLockRestriction( entityPersister.getVersionMapping() );
		}
	}

	private void applyNonVersionOptLocking(
			OptimisticLockStyle lockStyle,
			Map<String, GraphTableDeleteBuilder> builders,
			Object[] loadedState,
			SharedSessionContractImplementor session) {
		assert loadedState != null;
		assert lockStyle.isAllOrDirty();
		assert entityPersister.optimisticLockStyle().isAllOrDirty();
		assert session != null;

		final boolean[] versionability = entityPersister.getPropertyVersionability();
		for ( int attributeIndex = 0; attributeIndex < versionability.length; attributeIndex++ ) {
			if ( versionability[attributeIndex] ) {
				final var attribute = entityPersister.getAttributeMapping( attributeIndex );
				if ( !attribute.isPluralAttributeMapping() ) {
					breakDownJdbcValues( builders, session, attribute, loadedState[attributeIndex] );
				}
			}
		}
	}

	private void applyPartitionedSelectionRestrictions(
			Map<String, GraphTableDeleteBuilder> builders,
			Object[] state) {
		final var attributeMappings = entityPersister.getAttributeMappings();

		for ( int m = 0; m < attributeMappings.size(); m++ ) {
			final var attributeMapping = attributeMappings.get( m );
			final int jdbcTypeCount = attributeMapping.getJdbcTypeCount();
			for ( int i = 0; i < jdbcTypeCount; i++ ) {
				final var selectableMapping = attributeMapping.getSelectable( i );
				if ( selectableMapping.isPartitioned() ) {
					final String tableNameForMutation = entityPersister.physicalTableNameForMutation( selectableMapping );
					final GraphTableDeleteBuilder builder = builders.get( tableNameForMutation );
					if ( builder != null ) {
						builder.addKeyRestriction( selectableMapping );
					}
				}
			}
		}
	}

	private void breakDownJdbcValues(
			Map<String, GraphTableDeleteBuilder> builders,
			SharedSessionContractImplementor session,
			AttributeMapping attribute,
			Object loadedValue) {
		final String tableName = attribute.getContainingTableExpression();
		final GraphTableDeleteBuilder builder = builders.get( tableName );
		if ( builder != null ) {
			final var optimisticLockBindings = builder.getOptimisticLockBindings();
			if ( optimisticLockBindings != null ) {
				attribute.breakDownJdbcValues(
						loadedValue,
						(valueIndex, value, jdbcValueMapping) -> {
							if ( !containsColumn( builder.getKeyRestrictionBindings(), jdbcValueMapping ) ) {
								builder.addOptimisticLockRestriction( value, jdbcValueMapping );
							}
						},
						session
				);
			}
		}
	}

	private boolean containsColumn(List<ColumnValueBinding> bindings, SelectableMapping selectableMapping) {
		for ( ColumnValueBinding binding : bindings ) {
			if ( binding.matches( selectableMapping ) ) {
				return true;
			}
		}
		return false;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Soft Delete Support

	private TableUpdate chooseEffectiveSoftDeleteOperation(
			Object identifier,
			Object version,
			Object[] state,
			SharedSessionContractImplementor session,
			boolean applyOptimisticLocking) {
		// If we need optimistic locking with dirty-check (state-based),
		// we need to generate a dynamic soft delete operation
		final boolean needsDynamicSoftDelete = applyOptimisticLocking
				&& entityPersister.optimisticLockStyle().isAllOrDirty();

		return needsDynamicSoftDelete
				? generateDynamicSoftDeleteOperation( identifier, version, state, session )
				: (TableUpdate) staticDeleteOperations.get( entityPersister.getIdentifierTableDescriptor().name() );
	}

	private Map<String, TableUpdate> generateSoftDeleteOperation() {
		final var softDeleteMapping = entityPersister.getSoftDeleteMapping();
		assert softDeleteMapping != null;

		// Soft delete only operates on the root/identifier table
		final var rootTableDescriptor = entityPersister.getIdentifierTableDescriptor();
		final GraphTableUpdateBuilder tableUpdateBuilder =
				new GraphTableUpdateBuilderStandard(
						entityPersister,
						rootTableDescriptor,
						sessionFactory
				);

		// Apply key restriction (WHERE id = ?)
		tableUpdateBuilder.addKeyRestrictions( rootTableDescriptor.keyDescriptor() );

		// Apply soft delete assignment and restriction
		applySoftDelete( softDeleteMapping, tableUpdateBuilder );

		// Apply partition key restriction if needed
		applyPartitionKeyRestrictionForSoftDelete( tableUpdateBuilder );

		// Version-based optimistic locking (if applicable)
		if ( entityPersister.optimisticLockStyle().isVersion() && entityPersister.getVersionMapping() != null ) {
			tableUpdateBuilder.addOptimisticLockRestriction( entityPersister.getVersionMapping() );
		}

		return Map.of( rootTableDescriptor.name(), tableUpdateBuilder.buildMutation() );
	}

	protected TableUpdate generateDynamicSoftDeleteOperation(
			Object identifier,
			Object version,
			Object[] state,
			SharedSessionContractImplementor session) {
		final var softDeleteMapping = entityPersister.getSoftDeleteMapping();
		assert softDeleteMapping != null;

		// Soft delete only operates on the root/identifier table
		final var rootTableDescriptor = entityPersister.getIdentifierTableDescriptor();
		final GraphTableUpdateBuilder tableUpdateBuilder =
				new GraphTableUpdateBuilderStandard(
						entityPersister,
						rootTableDescriptor,
						sessionFactory
				);

		// Apply key restriction (WHERE id = ?)
		tableUpdateBuilder.addKeyRestrictions( rootTableDescriptor.keyDescriptor() );

		// Apply soft delete assignment and restriction
		applySoftDelete( softDeleteMapping, tableUpdateBuilder );

		// Apply partition key restriction if needed
		applyPartitionKeyRestrictionForSoftDelete( tableUpdateBuilder );

		// Apply optimistic locking
		final var optimisticLockStyle = entityPersister.optimisticLockStyle();
		if ( optimisticLockStyle.isVersion() && entityPersister.getVersionMapping() != null ) {
			tableUpdateBuilder.addOptimisticLockRestriction( entityPersister.getVersionMapping() );
		}
		else if ( state != null && optimisticLockStyle.isAllOrDirty() ) {
			applyNonVersionOptLockingForSoftDelete( tableUpdateBuilder, state, session );
		}

		return tableUpdateBuilder.buildMutation();
	}

	private void applySoftDelete(
			SoftDeleteMapping softDeleteMapping,
			GraphTableUpdateBuilder tableUpdateBuilder) {
		final var softDeleteColumnReference = new ColumnReference(
				new MutatingTableReference( tableUpdateBuilder.getTableDescriptor() ),
				softDeleteMapping
		);

		// Apply the assignment: SET deleted_column = deleted_value
		tableUpdateBuilder.addValueColumn( softDeleteMapping.createDeletedValueBinding( softDeleteColumnReference ) );

		// Apply the restriction: WHERE deleted_column = not_deleted_value
		tableUpdateBuilder.addNonKeyRestriction( softDeleteMapping.createNonDeletedValueBinding( softDeleteColumnReference ) );
	}

	private void applyPartitionKeyRestrictionForSoftDelete(GraphTableUpdateBuilder tableUpdateBuilder) {
		if ( entityPersister.hasPartitionedSelectionMapping() ) {
			final var attributeMappings = entityPersister.getAttributeMappings();
			for ( int m = 0; m < attributeMappings.size(); m++ ) {
				final var attributeMapping = attributeMappings.get( m );
				final int jdbcTypeCount = attributeMapping.getJdbcTypeCount();
				for ( int i = 0; i < jdbcTypeCount; i++ ) {
					final var selectableMapping = attributeMapping.getSelectable( i );
					if ( selectableMapping.isPartitioned() ) {
						tableUpdateBuilder.addKeyRestriction( selectableMapping );
					}
				}
			}
		}
	}

	private void applyNonVersionOptLockingForSoftDelete(
			GraphTableUpdateBuilder tableUpdateBuilder,
			Object[] loadedState,
			SharedSessionContractImplementor session) {
		final boolean[] versionability = entityPersister.getPropertyVersionability();
		for ( int attributeIndex = 0; attributeIndex < versionability.length; attributeIndex++ ) {
			if ( versionability[attributeIndex] ) {
				final var attribute = entityPersister.getAttributeMapping( attributeIndex );
				if ( !attribute.isPluralAttributeMapping() ) {
					breakDownJdbcValuesForSoftDelete( tableUpdateBuilder, session, attribute, loadedState[attributeIndex] );
				}
			}
		}
	}

	private void breakDownJdbcValuesForSoftDelete(
			GraphTableUpdateBuilder tableUpdateBuilder,
			SharedSessionContractImplementor session,
			AttributeMapping attribute,
			Object loadedValue) {
		final String builderTableName = tableUpdateBuilder.getTableDescriptor().name();
		final String attributeTableName = attribute.getContainingTableExpression();
		if ( builderTableName.equals( attributeTableName ) ) {
			final var optimisticLockBindings = tableUpdateBuilder.getOptimisticLockBindings();
			if ( optimisticLockBindings != null ) {
				attribute.breakDownJdbcValues(
						loadedValue,
						(valueIndex, value, jdbcValueMapping) -> {
							if ( !containsColumn( tableUpdateBuilder.getKeyRestrictionBindings(), jdbcValueMapping ) ) {
								tableUpdateBuilder.addOptimisticLockRestriction( value, jdbcValueMapping );
							}
						},
						session
				);
			}
		}
	}
}
