/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.decompose.entity;

import org.hibernate.action.internal.EntityDeleteAction;
import org.hibernate.action.queue.MutationKind;
import org.hibernate.action.queue.decompose.DecompositionContext;
import org.hibernate.action.queue.meta.EntityTableDescriptor;
import org.hibernate.action.queue.meta.TableDescriptor;
import org.hibernate.action.queue.meta.TableDescriptorAsTableMapping;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.ast.TableDelete;
import org.hibernate.sql.model.ast.TableMutation;
import org.hibernate.sql.model.ast.TableUpdate;
import org.hibernate.sql.model.ast.builder.TableDeleteBuilder;
import org.hibernate.sql.model.ast.builder.TableDeleteBuilderStandard;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilder;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilderStandard;
import org.hibernate.action.queue.plan.PlannedOperation;
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

import java.util.List;
import java.util.Map;


/// [Decomposer][EntityActionDecomposer] for entity delete operations.
///
/// Converts an [EntityDeleteAction] into a group of [PlannedOperation] to be performed.
///
/// @see EntityDeleteBindPlan
/// @see EntitySoftDeleteBindPlan
///
/// @author Steve Ebersole
public class DeleteDecomposer extends AbstractDecomposer<EntityDeleteAction> {
	// Map<String, GraphTableDelete> or GraphTableUpdate for soft delete
	private final Map<String, ? extends TableMutation<?>> staticDeleteOperations;

	public DeleteDecomposer(EntityPersister entityPersister, SessionFactoryImplementor sessionFactory) {
		super( entityPersister, sessionFactory );

		// Generate static operations based on whether soft delete is enabled
		// Soft delete uses UPDATE instead of DELETE
		// For joined-subclass hierarchies, only the root entity generates soft-delete operations
		final boolean shouldGenerateSoftDelete = entityPersister.getSoftDeleteMapping() != null
				&& entityPersister.getSuperMappingType() == null;  // Only root entities

		this.staticDeleteOperations = shouldGenerateSoftDelete
				? generateSoftDeleteOperation()
				: generateStaticOperations();
	}

	public Map<String, ? extends TableMutation<?>> getStaticDeleteOperations() {
		return staticDeleteOperations;
	}

	@Override
	public List<PlannedOperation> decompose(
			EntityDeleteAction action,
			int ordinalBase,
			SharedSessionContractImplementor session,
			DecompositionContext decompositionContext) {
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
		final var definedOptimisticLockStyle = definedOptimisticLockStyle();
		final Object[] loadedState;
		final Object rowId;
		if ( definedOptimisticLockStyle.isAllOrDirty() ) {
			final var entityEntry = session.getPersistenceContextInternal().getEntry( action.getInstance() );
			loadedState = entityEntry != null ? entityEntry.getLoadedState() : null;
			rowId = entityEntry != null ? entityEntry.getRowId() : null;
		}
		else {
			loadedState = null;
			rowId = null;
		}

		final var effectiveOptLockStyle = effectiveOptimisticLockStyle( definedOptimisticLockStyle, version, state );

		// For soft deletes, use UPDATE mutation kind; for hard deletes, use DELETE
		final MutationKind mutationKind = isSoftDelete ? MutationKind.UPDATE : MutationKind.DELETE;

		if ( isSoftDelete ) {
			// Handle soft delete (single UPDATE operation)
			final var effectiveOperation = chooseEffectiveSoftDeleteOperation(
					identifier,
					version,
					state,
					rowId,
					definedOptimisticLockStyle,
					effectiveOptLockStyle,
					session
			);
			final var mutation = effectiveOperation.createMutationOperation(null, sessionFactory);
			final var tableMapping = (TableDescriptorAsTableMapping) mutation.getTableDetails();
			final var tableDescriptor = (EntityTableDescriptor) tableMapping.getDescriptor();

			final EntitySoftDeleteBindPlan bindPlan = new EntitySoftDeleteBindPlan(
					tableDescriptor,
					entityPersister,
					identifier,
					version,
					loadedState,
					effectiveOptLockStyle
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
			final var effectiveGroup = chooseEffectiveDeleteGroup(
					identifier,
					version,
					state,
					rowId,
					definedOptimisticLockStyle,
					effectiveOptLockStyle,
					session
			);

			final List<PlannedOperation> operations = CollectionHelper.arrayList( effectiveGroup.size() );
			int localOrd = 0;
			for ( Map.Entry<String, TableDelete> entry : effectiveGroup.entrySet() ) {
				var mutation = entry.getValue().createMutationOperation(null, sessionFactory);
				var tableMapping = (TableDescriptorAsTableMapping) mutation.getTableDetails();
				var tableDescriptor = (EntityTableDescriptor) tableMapping.getDescriptor();

				final EntityDeleteBindPlan bindPlan = new EntityDeleteBindPlan(
						tableDescriptor,
						entityPersister,
						identifier,
						version,
						state,
						loadedState,
						effectiveOptLockStyle
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

	private OptimisticLockStyle definedOptimisticLockStyle() {
		final OptimisticLockStyle optimisticLockStyle = entityPersister.optimisticLockStyle();

		if ( optimisticLockStyle.isVersion() && entityPersister.getVersionMapping() == null ) {
			return OptimisticLockStyle.NONE;
		}

		return optimisticLockStyle;
	}

	private OptimisticLockStyle effectiveOptimisticLockStyle(
			OptimisticLockStyle optimisticLockStyle,
			Object loadedVersion,
			Object[] loadedState) {
		if ( optimisticLockStyle.isVersion()  ) {
			if ( loadedVersion == null ) {
				return OptimisticLockStyle.NONE;
			}
		}
		else if ( optimisticLockStyle.isAllOrDirty() ) {
			if ( loadedState == null ) {
				return OptimisticLockStyle.NONE;
			}
		}

		return optimisticLockStyle;
	}

	@SuppressWarnings("unchecked")
	private Map<String, TableDelete> chooseEffectiveDeleteGroup(
			Object identifier,
			Object version,
			Object[] state,
			Object rowId,
			OptimisticLockStyle definedOptLockStyle,
			OptimisticLockStyle effectiveOptLockStyle,
			SharedSessionContractImplementor session) {
		// If we need optimistic locking with dirty-check (state-based), we need to generate a dynamic delete group.
		// NOTE: if loadedState is null we can just use the static operations as there is no locking to apply
		return definedOptLockStyle != effectiveOptLockStyle
				? generateDynamicDeleteOperations( identifier, version, state, rowId, session )
				: (Map<String, TableDelete>) staticDeleteOperations;
	}

	private Map<String, TableDelete> generateStaticOperations() {
		final Map<String, TableDeleteBuilder> staticOperationBuilders =
				CollectionHelper.linkedMapOfSize( entityPersister.getTableDescriptors().length );

		// Process tables in reverse order (child tables before parent)
		entityPersister.forEachMutableTableDescriptorReverse( (tableDescriptor) -> {
			if ( !tableDescriptor.cascadeDeleteEnabled() ) {
				staticOperationBuilders.put(
						tableDescriptor.name(),
						createTableDeleteBuilder(tableDescriptor)
				);
			}
		} );

		applyStaticDeleteDetails( staticOperationBuilders );

		final Map<String, TableDelete> staticOperations = CollectionHelper.linkedMapOfSize( staticOperationBuilders.size() );
		staticOperationBuilders.forEach( (name, operationBuilder) -> {
			staticOperations.put( name, operationBuilder.buildMutation() );
		} );
		return staticOperations;
	}

	private TableDeleteBuilder createTableDeleteBuilder(TableDescriptor tableDescriptor) {
		// Create adapter to convert TableDescriptor to TableMapping
		final boolean isIdentifierTable = tableDescriptor instanceof EntityTableDescriptor etd
				&& etd.isIdentifierTable();
		final TableDescriptorAsTableMapping tableMapping = new TableDescriptorAsTableMapping(
				tableDescriptor,
				tableDescriptor.getRelativePosition(),
				isIdentifierTable,
				false // isInverse
		);

		return new TableDeleteBuilderStandard(
				entityPersister,
				tableMapping,
				sessionFactory
		);
	}

	protected Map<String, TableDelete> generateDynamicDeleteOperations(
			Object identifier,
			Object version,
			Object[] state,
			Object rowId,
			SharedSessionContractImplementor session) {
		final Map<String, TableDeleteBuilder> operationBuilders = CollectionHelper.linkedMapOfSize( entityPersister.getTableDescriptors().length );

		// Process tables in reverse order
		entityPersister.forEachMutableTableDescriptorReverse( (tableDescriptor) -> {
			if ( !tableDescriptor.cascadeDeleteEnabled() ) {
				operationBuilders.put(
						tableDescriptor.name(),
						createTableDeleteBuilder(tableDescriptor)
				);
			}
		} );

		applyDynamicDeleteDetails( operationBuilders, version, state, session );

		final Map<String, TableDelete> operations = CollectionHelper.linkedMapOfSize( operationBuilders.size() );
		operationBuilders.forEach( (name, operationBuilder) -> {
			operations.put( name, operationBuilder.buildMutation() );
		} );
		return operations;
	}

	private void applyStaticDeleteDetails(Map<String, TableDeleteBuilder> builders) {
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
			Map<String, TableDeleteBuilder> builders,
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

	private void applyKeyRestriction(TableDeleteBuilder tableDeleteBuilder) {
		// Get the TableDescriptor from the adapter
		final var tableMapping = (TableDescriptorAsTableMapping) tableDeleteBuilder.getMutatingTable().getTableMapping();
		final var tableDescriptor = tableMapping.getDescriptor();
		tableDeleteBuilder.addKeyRestrictions( tableDescriptor.keyDescriptor() );
	}

	private void applyVersionBasedOptLocking(Map<String, TableDeleteBuilder> builders) {
		assert entityPersister.optimisticLockStyle() == OptimisticLockStyle.VERSION;
		assert entityPersister.getVersionMapping() != null;

		final String tableNameForMutation = entityPersister.physicalTableNameForMutation( entityPersister.getVersionMapping() );
		final TableDeleteBuilder builder = builders.get( tableNameForMutation );
		if ( builder != null ) {
			builder.addOptimisticLockRestriction( entityPersister.getVersionMapping() );
		}
	}

	private void applyNonVersionOptLocking(
			OptimisticLockStyle lockStyle,
			Map<String, TableDeleteBuilder> builders,
			Object[] loadedState,
			SharedSessionContractImplementor session) {
		assert loadedState != null;
		assert lockStyle.isAllOrDirty();
		assert entityPersister.optimisticLockStyle().isAllOrDirty();
		assert session != null;

		builders.forEach( (name, builder) -> {
			var adapter = (TableDescriptorAsTableMapping) builder.getMutatingTable().getTableMapping();
			var tableDescriptor = (EntityTableDescriptor) adapter.getDescriptor();
			for ( int i = 0; i < tableDescriptor.attributes().size(); i++ ) {
				var attribute = tableDescriptor.attributes().get( i );
				if ( attribute.isPluralAttributeMapping() ) {
					continue;
				}

				attribute.breakDownJdbcValues(
						loadedState[attribute.getStateArrayPosition()],
						(valueIndex, jdbcValue, jdbcValueMapping) -> {
							if ( !jdbcValueMapping.isFormula() ) {
								if ( jdbcValue == null ) {
									builder.addNullOptimisticLockRestriction( jdbcValueMapping );
								}
								else {
									builder.addOptimisticLockRestriction( jdbcValueMapping );
								}
							}
						},
						session
				);
			}
		} );

		final boolean[] versionability = entityPersister.getPropertyVersionability();
		for ( int attributeIndex = 0; attributeIndex < versionability.length; attributeIndex++ ) {
			if ( versionability[attributeIndex] ) {
				final var attribute = entityPersister.getAttributeMapping( attributeIndex );
				if ( !attribute.isPluralAttributeMapping() ) {
				}
			}
		}
	}

	private void applyPartitionedSelectionRestrictions(
			Map<String, TableDeleteBuilder> builders,
			Object[] state) {
		final var attributeMappings = entityPersister.getAttributeMappings();

		for ( int m = 0; m < attributeMappings.size(); m++ ) {
			final var attributeMapping = attributeMappings.get( m );
			final int jdbcTypeCount = attributeMapping.getJdbcTypeCount();
			for ( int i = 0; i < jdbcTypeCount; i++ ) {
				final var selectableMapping = attributeMapping.getSelectable( i );
				if ( selectableMapping.isPartitioned() ) {
					final String tableNameForMutation = entityPersister.physicalTableNameForMutation( selectableMapping );
					final TableDeleteBuilder builder = builders.get( tableNameForMutation );
					if ( builder != null ) {
						builder.addKeyRestriction( selectableMapping );
					}
				}
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

	private TableUpdate<?> chooseEffectiveSoftDeleteOperation(
			Object identifier,
			Object version,
			Object[] state,
			Object rowId,
			OptimisticLockStyle definedOptimisticLockStyle,
			OptimisticLockStyle effectiveOptLockStyle,
			SharedSessionContractImplementor session) {
		// If we need optimistic locking with dirty-check (state-based),
		// we need to generate a dynamic soft delete operation
		final boolean needsDynamicSoftDelete = effectiveOptLockStyle.isAllOrDirty()
				|| definedOptimisticLockStyle != effectiveOptLockStyle;

		return needsDynamicSoftDelete
				? generateDynamicSoftDeleteOperation( identifier, version, state, effectiveOptLockStyle, session )
				: (TableUpdate<?>) staticDeleteOperations.get( entityPersister.getIdentifierTableDescriptor().name() );
	}

	private Map<String, TableUpdate<?>> generateSoftDeleteOperation() {
		final var softDeleteMapping = entityPersister.getSoftDeleteMapping();
		assert softDeleteMapping != null;

		// Soft delete only operates on the root/identifier table
		final var rootTableDescriptor = entityPersister.getIdentifierTableDescriptor();

		// Create adapter to convert TableDescriptor to TableMapping
		final TableDescriptorAsTableMapping tableMapping = new TableDescriptorAsTableMapping(
				rootTableDescriptor,
				0, // relativePosition
				true, // isIdentifierTable
				false // isInverse
		);

		final TableUpdateBuilder<?> tableUpdateBuilder = new TableUpdateBuilderStandard<>(
				entityPersister,
				new MutatingTableReference(tableMapping),
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

		return Map.of( rootTableDescriptor.name(), (TableUpdate<?>) tableUpdateBuilder.buildMutation() );
	}

	protected TableUpdate<?> generateDynamicSoftDeleteOperation(
			Object identifier,
			Object version,
			Object[] state,
			OptimisticLockStyle effectiveOptLockStyle,
			SharedSessionContractImplementor session) {
		final var softDeleteMapping = entityPersister.getSoftDeleteMapping();
		assert softDeleteMapping != null;

		// Soft delete only operates on the root/identifier table
		final var rootTableDescriptor = entityPersister.getIdentifierTableDescriptor();

		// Create adapter to convert TableDescriptor to TableMapping
		final TableDescriptorAsTableMapping tableMapping = new TableDescriptorAsTableMapping(
				rootTableDescriptor,
				0, // relativePosition
				true, // isIdentifierTable
				false // isInverse
		);

		final TableUpdateBuilder<?> tableUpdateBuilder = new TableUpdateBuilderStandard<>(
				entityPersister,
				new MutatingTableReference(tableMapping),
				sessionFactory
		);

		// Apply key restriction (WHERE id = ?)
		tableUpdateBuilder.addKeyRestrictions( rootTableDescriptor.keyDescriptor() );

		// Apply soft delete assignment and restriction
		applySoftDelete( softDeleteMapping, tableUpdateBuilder );

		// Apply partition key restriction if needed
		applyPartitionKeyRestrictionForSoftDelete( tableUpdateBuilder );

		// Apply optimistic locking
		if ( effectiveOptLockStyle.isVersion() ) {
			tableUpdateBuilder.addOptimisticLockRestriction( entityPersister.getVersionMapping() );
		}
		else if ( effectiveOptLockStyle.isAllOrDirty() ) {
			applyNonVersionOptLockingForSoftDelete( tableUpdateBuilder, state, session );
		}

		return (TableUpdate<?>) tableUpdateBuilder.buildMutation();
	}

	private void applySoftDelete(
			SoftDeleteMapping softDeleteMapping,
			TableUpdateBuilder<?> tableUpdateBuilder) {
		final var softDeleteColumnReference = new ColumnReference(
				tableUpdateBuilder.getMutatingTable(),
				softDeleteMapping
		);

		// Apply the assignment: SET deleted_column = deleted_value
		tableUpdateBuilder.addValueColumn( softDeleteMapping.createDeletedValueBinding( softDeleteColumnReference ) );

		// Apply the restriction: WHERE deleted_column = not_deleted_value
		tableUpdateBuilder.addNonKeyRestriction( softDeleteMapping.createNonDeletedValueBinding( softDeleteColumnReference ) );
	}

	private void applyPartitionKeyRestrictionForSoftDelete(TableUpdateBuilder<?> tableUpdateBuilder) {
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
			TableUpdateBuilder<?> tableUpdateBuilder,
			Object[] loadedState,
			SharedSessionContractImplementor session) {
		if ( loadedState == null ) {
			// this indicates that the state was never loaded from the database -
			// there is no locking to apply
			//
			// EARLY EXIT!!
			return;
		}

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
			TableUpdateBuilder<?> tableUpdateBuilder,
			SharedSessionContractImplementor session,
			AttributeMapping attribute,
			Object loadedValue) {
		// Get the TableDescriptor from the adapter
		final var tableMapping = (TableDescriptorAsTableMapping) tableUpdateBuilder.getMutatingTable().getTableMapping();
		final String builderTableName = tableMapping.getDescriptor().name();
		final String attributeTableName = attribute.getContainingTableExpression();
		if ( builderTableName.equals( attributeTableName ) ) {
			final var optimisticLockBindings = tableUpdateBuilder.getOptimisticLockBindings();
			if ( optimisticLockBindings != null ) {
				attribute.breakDownJdbcValues(
						loadedValue,
						(valueIndex, value, jdbcValueMapping) -> {
							if ( !containsColumn( tableUpdateBuilder.getKeyRestrictionBindings(), jdbcValueMapping ) ) {
								optimisticLockBindings.consume( valueIndex, value, jdbcValueMapping );
							}
						},
						session
				);
			}
		}
	}
}
