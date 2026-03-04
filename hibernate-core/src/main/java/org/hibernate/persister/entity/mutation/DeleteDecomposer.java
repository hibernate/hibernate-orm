/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.action.internal.EntityDeleteAction;
import org.hibernate.action.queue.bind.BindPlan;
import org.hibernate.action.queue.exec.PostExecutionCallback;
import org.hibernate.action.queue.MutationKind;
import org.hibernate.action.queue.StatementShapeKey;
import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.action.queue.plan.PlannedOperationGroup;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.SoftDeleteMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.ast.builder.MutationGroupBuilder;
import org.hibernate.sql.model.ast.builder.RestrictedTableMutationBuilder;
import org.hibernate.sql.model.ast.builder.TableDeleteBuilder;
import org.hibernate.sql.model.ast.builder.TableDeleteBuilderSkipped;
import org.hibernate.sql.model.ast.builder.TableDeleteBuilderStandard;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilderStandard;
import org.hibernate.sql.model.internal.MutationGroupSingle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;
import static org.hibernate.sql.model.internal.MutationOperationGroupFactory.singleOperation;

/// [Decomposer][org.hibernate.action.queue.graph.MutationDecomposer] for entity delete operations.
///
/// Converts an [EntityDeleteAction] into a set of [PlannedOperationGroup]s that can
/// be executed in the correct order respecting foreign key constraints.  The delete
/// operations are created in "reverse order", meaning child (key) tables before parent
/// (target) tables - e.g. `orders` before `customers`.
///
/// @see DeleteBindPlan
/// @see SoftDeleteBindPlan
///
/// @author Steve Ebersole
public class DeleteDecomposer extends AbstractDecomposer<EntityDeleteAction> {
	private final MutationOperationGroup staticDeleteGroup;
	private final BasicBatchKey batchKey;

	public DeleteDecomposer(EntityPersister entityPersister, SessionFactoryImplementor sessionFactory) {
		super( entityPersister, sessionFactory );

		// Generate static operations based on whether soft delete is enabled
		// Soft delete uses UPDATE instead of DELETE
		this.staticDeleteGroup = entityPersister.getSoftDeleteMapping() == null
				? generateStaticOperationGroup()
				: generateSoftDeleteOperationGroup();

		// Batching is generally safe for deletes and soft deletes
		this.batchKey = new BasicBatchKey( entityPersister.getEntityName() + "#DELETE" );
	}

	public MutationOperationGroup getStaticMutationGroup() {
		return staticDeleteGroup;
	}

	@Override
	public List<PlannedOperationGroup> decompose(
			EntityDeleteAction action,
			int ordinalBase,
			java.util.function.Consumer<PostExecutionCallback> postExecutionCallbackRegistry,
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

		// Register post-execution callback for finalization work
		final PostDeleteHandling postDeleteHandling = new PostDeleteHandling(action, cacheKey, veto);
		postExecutionCallbackRegistry.accept(postDeleteHandling);

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

		// Choose between static or dynamic delete group
		final var effectiveGroup = isSoftDelete
				? chooseEffectiveSoftDeleteGroup( identifier, version, state, session, applyOptimisticLocking )
				: chooseEffectiveDeleteGroup( identifier, version, state, session, applyOptimisticLocking );

		LinkedHashMap<String, List<PlannedOperation>> byTable = new LinkedHashMap<>();
		int localOrd = 0;

		// For soft deletes, use UPDATE mutation kind; for hard deletes, use DELETE
		final MutationKind mutationKind = isSoftDelete ? MutationKind.UPDATE : MutationKind.DELETE;

		for ( int i = 0; i < effectiveGroup.getNumberOfOperations(); i++ ) {
			var operation = effectiveGroup.getOperation( i );
			var table = (EntityTableMapping) operation.getTableDetails();
			String tableName = table.getTableName();

			final BindPlan bindPlan = isSoftDelete
					? new SoftDeleteBindPlan(
							entityPersister,
							identifier,
							version,
							state,
							applyOptimisticLocking,
							softDeleteMapping
					)
					: new DeleteBindPlan(
							entityPersister,
							identifier,
							version,
							state,
							applyOptimisticLocking
					);

			final PlannedOperation op = new PlannedOperation(
					tableName,
					mutationKind,
					operation,
					bindPlan,
					ordinalBase * 1_000 + (localOrd++),
					"EntityDeleteAction(" + entityPersister.getEntityName() + ")"
			);

			byTable.computeIfAbsent( tableName, t -> new ArrayList<>() ).add( op );
		}

		ArrayList<PlannedOperationGroup> out = arrayList( byTable.size() );
		int ord = 0;
		for ( var e : byTable.entrySet() ) {
			String tableName = e.getKey();
			List<PlannedOperation> plannedOperations = e.getValue();

			final StatementShapeKey shapeKey = isSoftDelete
					? StatementShapeKey.forUpdate( tableName, plannedOperations )
					: StatementShapeKey.forDelete( tableName, plannedOperations );

			out.add( new PlannedOperationGroup(
					tableName,
					mutationKind,
					shapeKey,
					plannedOperations,
					false,  // deletes never need identity pre-phase
					ordinalBase * 1_000 + (ord++),
					"EntityDeleteAction(" + entityPersister.getEntityName() + ")"
			) );
		}

		return out;
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

	private MutationOperationGroup chooseEffectiveDeleteGroup(
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
				? generateDynamicDeleteGroup( identifier, version, state, session )
				: staticDeleteGroup;
	}

	public MutationOperationGroup generateStaticOperationGroup() {
		final var deleteGroupBuilder = new MutationGroupBuilder( MutationType.DELETE, entityPersister );

		// Process tables in reverse order (child tables before parent)
		entityPersister.forEachMutableTableReverse( (tableMapping) -> {
			final var tableDeleteBuilder = tableMapping.isCascadeDeleteEnabled()
					? new TableDeleteBuilderSkipped( tableMapping )
					: new TableDeleteBuilderStandard( entityPersister, tableMapping, sessionFactory );
			deleteGroupBuilder.addTableDetailsBuilder( tableDeleteBuilder );
		} );

		applyStaticDeleteDetails( deleteGroupBuilder );

		return createOperationGroup( null, deleteGroupBuilder.buildMutationGroup() );
	}

	protected MutationOperationGroup generateDynamicDeleteGroup(
			Object identifier,
			Object version,
			Object[] state,
			SharedSessionContractImplementor session) {
		final var deleteGroupBuilder = new MutationGroupBuilder( MutationType.DELETE, entityPersister );

		// Process tables in reverse order
		entityPersister.forEachMutableTableReverse( (tableMapping) -> {
			final var tableDeleteBuilder = tableMapping.isCascadeDeleteEnabled()
					? new TableDeleteBuilderSkipped( tableMapping )
					: new TableDeleteBuilderStandard( entityPersister, tableMapping, sessionFactory );
			deleteGroupBuilder.addTableDetailsBuilder( tableDeleteBuilder );
		} );

		applyDynamicDeleteDetails( deleteGroupBuilder, version, state, session );

		return createOperationGroup( null, deleteGroupBuilder.buildMutationGroup() );
	}

	private void applyStaticDeleteDetails(MutationGroupBuilder deleteGroupBuilder) {
		// Apply key restrictions for all tables
		deleteGroupBuilder.forEachTableMutationBuilder( (builder) -> {
			final var tableMapping = (EntityTableMapping) builder.getMutatingTable().getTableMapping();
			final var tableDeleteBuilder = (TableDeleteBuilder) builder;
			applyKeyRestriction( tableDeleteBuilder, tableMapping );
		} );

		// Apply version-based optimistic locking if applicable
		if ( entityPersister.optimisticLockStyle().isVersion() && entityPersister.getVersionMapping() != null ) {
			applyVersionBasedOptLocking( deleteGroupBuilder );
		}
	}

	private void applyDynamicDeleteDetails(
			MutationGroupBuilder deleteGroupBuilder,
			Object version,
			Object[] state,
			SharedSessionContractImplementor session) {
		// Apply key restrictions for all tables
		deleteGroupBuilder.forEachTableMutationBuilder( (builder) -> {
			final var tableMapping = (EntityTableMapping) builder.getMutatingTable().getTableMapping();
			final var tableDeleteBuilder = (TableDeleteBuilder) builder;
			applyKeyRestriction( tableDeleteBuilder, tableMapping );
		} );

		// Apply optimistic locking
		final var optimisticLockStyle = entityPersister.optimisticLockStyle();
		if ( optimisticLockStyle.isVersion() && entityPersister.getVersionMapping() != null ) {
			applyVersionBasedOptLocking( deleteGroupBuilder );
		}
		else if ( state != null && optimisticLockStyle.isAllOrDirty() ) {
			applyNonVersionOptLocking( optimisticLockStyle, deleteGroupBuilder, state, session );
		}

		// Apply partitioned selection restrictions if needed
		if ( entityPersister.hasPartitionedSelectionMapping() && state != null ) {
			applyPartitionedSelectionRestrictions( deleteGroupBuilder, state );
		}
	}

	private void applyKeyRestriction(
			TableDeleteBuilder tableDeleteBuilder,
			EntityTableMapping tableMapping) {
		tableDeleteBuilder.addKeyRestrictions( tableMapping.getKeyMapping() );
	}

	private void applyVersionBasedOptLocking(MutationGroupBuilder mutationGroupBuilder) {
		assert entityPersister.optimisticLockStyle() == OptimisticLockStyle.VERSION;
		assert entityPersister.getVersionMapping() != null;

		final String tableNameForMutation =
				entityPersister.physicalTableNameForMutation( entityPersister.getVersionMapping() );
		final RestrictedTableMutationBuilder<?, ?> rootTableMutationBuilder =
				mutationGroupBuilder.findTableDetailsBuilder( tableNameForMutation );
		rootTableMutationBuilder.addOptimisticLockRestriction( entityPersister.getVersionMapping() );
	}

	private void applyNonVersionOptLocking(
			OptimisticLockStyle lockStyle,
			MutationGroupBuilder mutationGroupBuilder,
			Object[] loadedState,
			SharedSessionContractImplementor session) {
		final var persister = entityPersister;
		assert loadedState != null;
		assert lockStyle.isAllOrDirty();
		assert persister.optimisticLockStyle().isAllOrDirty();
		assert session != null;

		final boolean[] versionability = persister.getPropertyVersionability();
		for ( int attributeIndex = 0; attributeIndex < versionability.length; attributeIndex++ ) {
			if ( versionability[attributeIndex] ) {
				final var attribute = persister.getAttributeMapping( attributeIndex );
				if ( !attribute.isPluralAttributeMapping() ) {
					breakDownJdbcValues( mutationGroupBuilder, session, attribute, loadedState[attributeIndex] );
				}
			}
		}
	}

	private void applyPartitionedSelectionRestrictions(
			MutationGroupBuilder deleteGroupBuilder,
			Object[] state) {
		final var attributeMappings = entityPersister.getAttributeMappings();

		for ( int m = 0; m < attributeMappings.size(); m++ ) {
			final var attributeMapping = attributeMappings.get( m );
			final int jdbcTypeCount = attributeMapping.getJdbcTypeCount();
			for ( int i = 0; i < jdbcTypeCount; i++ ) {
				final var selectableMapping = attributeMapping.getSelectable( i );
				if ( selectableMapping.isPartitioned() ) {
					final String tableNameForMutation =
							entityPersister.physicalTableNameForMutation( selectableMapping );
					final RestrictedTableMutationBuilder<?, ?> rootTableMutationBuilder =
							deleteGroupBuilder.findTableDetailsBuilder( tableNameForMutation );
					rootTableMutationBuilder.addKeyRestrictionLeniently( selectableMapping );
				}
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
			final var optimisticLockBindings = tableMutationBuilder.getOptimisticLockBindings();
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
						},
						session
				);
			}
		}
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Soft Delete Support

	private MutationOperationGroup chooseEffectiveSoftDeleteGroup(
			Object identifier,
			Object version,
			Object[] state,
			SharedSessionContractImplementor session,
			boolean applyOptimisticLocking) {
		// If we need optimistic locking with dirty-check (state-based),
		// we need to generate a dynamic soft delete group
		final boolean needsDynamicSoftDelete = applyOptimisticLocking
				&& entityPersister.optimisticLockStyle().isAllOrDirty();

		return needsDynamicSoftDelete
				? generateDynamicSoftDeleteGroup( identifier, version, state, session )
				: staticDeleteGroup;
	}

	public MutationOperationGroup generateSoftDeleteOperationGroup() {
		final var softDeleteMapping = entityPersister.getSoftDeleteMapping();
		assert softDeleteMapping != null;

		// Soft delete only operates on the root/identifier table
		final var rootTableMapping = entityPersister.getIdentifierTableMapping();
		final TableUpdateBuilderStandard<MutationOperation> tableUpdateBuilder =
				new TableUpdateBuilderStandard<>( entityPersister, rootTableMapping, sessionFactory );

		// Apply key restriction (WHERE id = ?)
		tableUpdateBuilder.addKeyRestrictions( rootTableMapping.getKeyMapping() );

		// Apply soft delete assignment and restriction
		applySoftDelete( softDeleteMapping, tableUpdateBuilder );

		// Apply partition key restriction if needed
		applyPartitionKeyRestrictionForSoftDelete( tableUpdateBuilder );

		// Version-based optimistic locking (if applicable)
		if ( entityPersister.optimisticLockStyle().isVersion() && entityPersister.getVersionMapping() != null ) {
			tableUpdateBuilder.addOptimisticLockRestriction( entityPersister.getVersionMapping() );
		}

		final var tableMutation = tableUpdateBuilder.buildMutation();
		final MutationGroupSingle mutationGroup = new MutationGroupSingle(
				MutationType.DELETE,
				entityPersister,
				tableMutation
		);

		final var mutationOperation = tableMutation.createMutationOperation( null, sessionFactory );
		return singleOperation( mutationGroup, mutationOperation );
	}

	protected MutationOperationGroup generateDynamicSoftDeleteGroup(
			Object identifier,
			Object version,
			Object[] state,
			SharedSessionContractImplementor session) {
		final var softDeleteMapping = entityPersister.getSoftDeleteMapping();
		assert softDeleteMapping != null;

		// Soft delete only operates on the root/identifier table
		final var rootTableMapping = entityPersister.getIdentifierTableMapping();
		final TableUpdateBuilderStandard<MutationOperation> tableUpdateBuilder =
				new TableUpdateBuilderStandard<>( entityPersister, rootTableMapping, sessionFactory );

		// Apply key restriction (WHERE id = ?)
		tableUpdateBuilder.addKeyRestrictions( rootTableMapping.getKeyMapping() );

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

		final var tableMutation = tableUpdateBuilder.buildMutation();
		final MutationGroupSingle mutationGroup = new MutationGroupSingle(
				MutationType.DELETE,
				entityPersister,
				tableMutation
		);

		final var mutationOperation = tableMutation.createMutationOperation( null, sessionFactory );
		return singleOperation( mutationGroup, mutationOperation );
	}

	private void applySoftDelete(
			SoftDeleteMapping softDeleteMapping,
			TableUpdateBuilderStandard<MutationOperation> tableUpdateBuilder) {
		final var softDeleteColumnReference = new ColumnReference(
				tableUpdateBuilder.getMutatingTable(),
				softDeleteMapping
		);

		// Apply the assignment: SET deleted_column = deleted_value
		tableUpdateBuilder.addValueColumn( softDeleteMapping.createDeletedValueBinding( softDeleteColumnReference ) );

		// Apply the restriction: WHERE deleted_column = not_deleted_value
		tableUpdateBuilder.addNonKeyRestriction( softDeleteMapping.createNonDeletedValueBinding( softDeleteColumnReference ) );
	}

	private void applyPartitionKeyRestrictionForSoftDelete(TableUpdateBuilderStandard<MutationOperation> tableUpdateBuilder) {
		if ( entityPersister.hasPartitionedSelectionMapping() ) {
			final var attributeMappings = entityPersister.getAttributeMappings();
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

	private void applyNonVersionOptLockingForSoftDelete(
			TableUpdateBuilderStandard<MutationOperation> tableUpdateBuilder,
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
