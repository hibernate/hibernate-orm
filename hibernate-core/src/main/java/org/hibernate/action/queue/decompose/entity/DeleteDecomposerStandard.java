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
import org.hibernate.sql.model.TableMapping;
import org.hibernate.sql.model.ast.TableDelete;
import org.hibernate.sql.model.ast.TableMutation;
import org.hibernate.sql.model.ast.builder.TableDeleteBuilder;
import org.hibernate.sql.model.ast.builder.TableDeleteBuilderStandard;
import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.persister.entity.EntityPersister;

import java.util.List;
import java.util.Map;

import static org.hibernate.internal.util.collections.CollectionHelper.linkedMapOfSize;


/// [Decomposer][EntityActionDecomposer] for entity delete operations.
///
/// Converts an [EntityDeleteAction] into a group of [PlannedOperation] to be performed.
///
/// @see EntityDeleteBindPlan
/// @see EntitySoftDeleteBindPlan
///
/// @author Steve Ebersole
public class DeleteDecomposerStandard extends AbstractDeleteDecomposer {
	private final Map<String, TableDelete> staticDeleteMutations;
	private Map<String, TableDelete> staticNoVersionDeleteMutations;

	public DeleteDecomposerStandard(EntityPersister entityPersister, SessionFactoryImplementor sessionFactory) {
		super( entityPersister, sessionFactory );
		assert entityPersister.getSoftDeleteMapping() == null;

		this.staticDeleteMutations = generateMutations( "", null, true, null );
	}

	public Map<String, ? extends TableMutation<?>> getStaticDeleteOperations() {
		return staticDeleteMutations;
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

		// If vetoed by PRE_DELETE listener, skip creating DELETE operations
		// Post-execution callback will still run to handle cleanup (cache unlock, etc.)
		if ( veto || action.isCascadeDeleteEnabled() ) {
			return List.of();
		}

		final var definedOptimisticLockStyle = definedOptimisticLockStyle();

		// Match AbstractDeleteCoordinator pattern for obtaining loadedState and rowId
		final var isImpliedOptimisticLocking = definedOptimisticLockStyle.isAllOrDirty();
		final var entityEntry = session.getPersistenceContextInternal().getEntry( action.getInstance() );
		final var loadedState = entityEntry != null && isImpliedOptimisticLocking ? entityEntry.getLoadedState() : null;
		final Object rowId = entityEntry != null ? entityEntry.getRowId() : null;

		// Create post-execution callback for finalization work
		final PostDeleteHandling postDeleteHandling = new PostDeleteHandling(action, cacheKey, veto);

		// Decide between static and dynamic delete operations (matches AbstractDeleteCoordinator pattern)
		// Use dynamic if:
		// - IMPLIED optimistic locking (ALL/DIRTY) AND we have loadedState
		// - entity defines a partition, but we can not know the partition (because loaded state is unknown)
		// - OR rowId is null but entityPersister has rowId mapping
		if ( (isImpliedOptimisticLocking && loadedState != null)
				|| ((entityEntry == null || entityEntry.getLoadedState() == null) && entityPersister.hasPartitionedSelectionMapping())
				|| (rowId == null && entityPersister.hasRowId()) ) {
			return decomposeDynamicDelete(
					ordinalBase,
					action.getInstance(),
					identifier,
					rowId,
					version,
					state,
					loadedState,
					definedOptimisticLockStyle,
					postDeleteHandling,
					session
			);
		}
		else {
			return decomposeStaticDelete(
					ordinalBase,
					action.getInstance(),
					identifier,
					rowId,
					version,
					state,
					entityEntry == null ? null : entityEntry.getLoadedState(),
					definedOptimisticLockStyle,
					postDeleteHandling,
					session
			);
		}
	}

	private List<PlannedOperation> decomposeDynamicDelete(
			int ordinalBase,
			Object instance,
			Object identifier,
			Object rowId,
			Object version,
			Object[] state,
			Object[] loadedState,
			OptimisticLockStyle optimisticLockStyle,
			PostDeleteHandling postDeleteHandling,
			SharedSessionContractImplementor session) {
		final var dynamicMutations = generateMutations( rowId, loadedState, true, session );

		final List<PlannedOperation> operations = CollectionHelper.arrayList( dynamicMutations.size() );
		int localOrd = 0;
		for ( Map.Entry<String, TableDelete> entry : dynamicMutations.entrySet() ) {
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
					optimisticLockStyle
			);

			final PlannedOperation op = new PlannedOperation(
					tableDescriptor,
					MutationKind.DELETE,
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

	private List<PlannedOperation> decomposeStaticDelete(
			int ordinalBase,
			Object instance,
			Object identifier,
			Object rowId,
			Object version,
			Object[] state,
			Object[] loadedState,
			OptimisticLockStyle optimisticLockStyle,
			PostDeleteHandling postDeleteHandling,
			SharedSessionContractImplementor session) {
		final boolean applyVersion;
		final Map<String,TableDelete> tableDeletesToUse;
		if ( instance == null ) {
			applyVersion = false;
			if ( staticNoVersionDeleteMutations == null ) {
				staticNoVersionDeleteMutations = generateMutations("", null, false, session );
			}
			tableDeletesToUse = staticNoVersionDeleteMutations;
		}
		else {
			applyVersion = true;
			tableDeletesToUse = staticDeleteMutations;
		}

		final List<PlannedOperation> operations = CollectionHelper.arrayList( tableDeletesToUse.size() );
		int localOrd = 0;
		for ( Map.Entry<String, TableDelete> entry : tableDeletesToUse.entrySet() ) {
			var mutation = entry.getValue().createMutationOperation(null, sessionFactory);
			var tableMapping = (TableDescriptorAsTableMapping) mutation.getTableDetails();
			var tableDescriptor = (EntityTableDescriptor) tableMapping.getDescriptor();

			final EntityDeleteBindPlan bindPlan = new EntityDeleteBindPlan(
					tableDescriptor,
					entityPersister,
					identifier,
					applyVersion ? version : null,
					applyVersion ? state : null,
					applyVersion ? loadedState : null,
					applyVersion ? optimisticLockStyle : OptimisticLockStyle.NONE
			);

			final PlannedOperation op = new PlannedOperation(
					tableDescriptor,
					MutationKind.DELETE,
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

	protected Map<String, TableDelete> generateMutations(
			Object rowId,
			Object[] loadedState,
			boolean applyVersion,
			SharedSessionContractImplementor session) {
		final Map<String, TableDeleteBuilder> deleteBuilders = linkedMapOfSize( entityPersister.getTableDescriptors().length );

		// Process tables in reverse order (child tables before parent)
		entityPersister.forEachMutableTableDescriptorReverse( (tableDescriptor) -> {
			if ( !tableDescriptor.cascadeDeleteEnabled() ) {
				deleteBuilders.put( tableDescriptor.name(), createTableDeleteBuilder(tableDescriptor) );
			}
		} );

		applyTableDeleteDetails( deleteBuilders, rowId, loadedState, applyVersion, session );

		final Map<String, TableDelete> operations = linkedMapOfSize( deleteBuilders.size() );
		deleteBuilders.forEach( (name, operationBuilder) -> {
			operations.put( name, operationBuilder.buildMutation() );
		} );

		return operations;
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

	private void applyTableDeleteDetails(
			Map<String, TableDeleteBuilder> tableDeleteBuilders,
			Object rowId,
			Object[] loadedState,
			boolean applyVersion,
			SharedSessionContractImplementor session) {
		tableDeleteBuilders.forEach( (name, builder) -> {
			// Apply key restrictions for all tables
			applyKeyRestriction( builder, rowId );

			if ( builder.getMutatingTable().getTableMapping().isIdentifierTable() ) {
				entityPersister.addDiscriminatorToDelete( builder );
			}
		} );

		if ( applyVersion ) {
			// apply any optimistic locking
			applyOptimisticLocking(
					entityPersister.optimisticLockStyle(),
					tableDeleteBuilders,
					loadedState,
					session
			);
			applyPartitionRestrictions( tableDeleteBuilders );
		}
	}

	private void applyKeyRestriction(TableDeleteBuilder builder, Object rowId) {
		var tableMapping = builder.getMutatingTable().getTableMapping();
		var tableDescriptor = ( (TableDescriptorAsTableMapping) tableMapping ).getDescriptor();
		if ( rowId != null && needsRowId( entityPersister, tableMapping ) ) {
			builder.addKeyRestrictionLeniently( entityPersister.getRowIdMapping() );
		}
		else {
			builder.addKeyRestrictions( tableDescriptor.keyDescriptor() );
		}
	}

	protected static boolean needsRowId(EntityPersister entityPersister, TableMapping tableMapping) {
		return entityPersister.getRowIdMapping() != null && tableMapping.isIdentifierTable();
	}

	protected void applyOptimisticLocking(
			OptimisticLockStyle optimisticLockStyle,
			Map<String,TableDeleteBuilder> tableDeleteBuilders,
			Object[] loadedState,
			SharedSessionContractImplementor session) {
		if ( optimisticLockStyle.isVersion() && entityPersister.getVersionMapping() != null ) {
			applyVersionOptLocking( tableDeleteBuilders );
		}
		else if ( loadedState != null && optimisticLockStyle.isAllOrDirty() ) {
			applyNonVersionOptLocking( optimisticLockStyle, tableDeleteBuilders, loadedState, session );
		}
	}

	protected void applyVersionOptLocking(Map<String,TableDeleteBuilder> tableDeleteBuilders) {
		final var versionMapping = entityPersister.getVersionMapping();
		if ( versionMapping != null ) {
			final String tableNameForMutation = entityPersister.physicalTableNameForMutation( versionMapping );
			final var tableMutationBuilder = tableDeleteBuilders.get( tableNameForMutation );
			if ( tableMutationBuilder != null ) {
				tableMutationBuilder.addNonKeyRestriction( versionMapping );
			}
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

		final boolean[] versionability = entityPersister.getPropertyVersionability();

		builders.forEach( (name, builder) -> {
			var adapter = (TableDescriptorAsTableMapping) builder.getMutatingTable().getTableMapping();
			var tableDescriptor = (EntityTableDescriptor) adapter.getDescriptor();
			for ( int i = 0; i < tableDescriptor.attributes().size(); i++ ) {
				var attribute = tableDescriptor.attributes().get( i );
				if ( attribute.isPluralAttributeMapping() ) {
					continue;
				}

				if ( versionability[attribute.getStateArrayPosition()] ) {
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
			}
		} );
	}

	private void applyPartitionRestrictions(Map<String, TableDeleteBuilder> builders) {
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
						builder.addKeyRestrictionLeniently( selectableMapping );
					}
				}
			}
		}
	}
}
