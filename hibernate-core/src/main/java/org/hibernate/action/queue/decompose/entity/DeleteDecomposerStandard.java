/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.decompose.entity;

import org.hibernate.action.internal.EntityDeleteAction;
import org.hibernate.action.queue.MutationKind;
import org.hibernate.action.queue.decompose.DecompositionContext;
import org.hibernate.action.queue.decompose.collection.DecompositionSupport;
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
import org.hibernate.persister.entity.EntityPersister;

import java.util.Map;
import java.util.function.Consumer;

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

		this.staticDeleteMutations = generateMutations( "", null, null, true, null );
	}

	public Map<String, ? extends TableMutation<?>> getStaticDeleteOperations() {
		return staticDeleteMutations;
	}

	@Override
	public void decompose(
			EntityDeleteAction action,
			int ordinalBase,
			SharedSessionContractImplementor session,
			DecompositionContext decompositionContext,
			Consumer<PlannedOperation> operationConsumer) {
		final Object identifier = action.getId();
		final Object version = action.getVersion();
		final Object[] state = action.getState();

		final Object naturalIdValues = DeleteNaturalIdHandling.removeLocalResolution( action, session );

		final DeleteCacheHandling.CacheLock cacheLock = DeleteCacheHandling.lockItem( action, session );
		registerAfterTransactionCompletion( action, cacheLock, session );
		final PreDeleteHandling preDeleteHandling = new PreDeleteHandling( action );
		final PostDeleteHandling postDeleteHandling = new PostDeleteHandling(
				action,
				cacheLock.cacheKey(),
				naturalIdValues,
				preDeleteHandling
		);

		if ( decompositionContext != null && decompositionContext.isBeingInsertedInCurrentFlush( action.getInstance() ) ) {
			operationConsumer.accept( createNoOpDeleteCallbackCarrier( ordinalBase, preDeleteHandling, postDeleteHandling ) );
			return;
		}

		// If an owning association's database cascade will delete this entity row,
		// skip creating DELETE operations but still run post-delete cleanup.
		//
		// Do not apply this shortcut when the entity itself has cascaded secondary
		// or joined-subclass tables. In that case we still need the root table
		// delete; generateMutations() will skip only the cascaded tables.
		if ( action.isCascadeDeleteEnabled() && !hasCascadeDeleteTables() ) {
			operationConsumer.accept( createNoOpDeleteCallbackCarrier( ordinalBase, preDeleteHandling, postDeleteHandling ) );
			return;
		}

		final var definedOptimisticLockStyle = definedOptimisticLockStyle();

		// Match AbstractDeleteCoordinator pattern for obtaining loadedState and rowId
		final var isImpliedOptimisticLocking = definedOptimisticLockStyle.isAllOrDirty();
		final var entityEntry = session.getPersistenceContextInternal().getEntry( action.getInstance() );
		final var loadedState = entityEntry != null && isImpliedOptimisticLocking ? entityEntry.getLoadedState() : null;
		final Object rowId = entityEntry != null ? entityEntry.getRowId() : null;

		// Decide between static and dynamic delete operations (matches AbstractDeleteCoordinator pattern)
		// Use dynamic if:
		// - IMPLIED optimistic locking (ALL/DIRTY) AND we have loadedState
		// - entity defines a partition, but we can not know the partition (because loaded state is unknown)
		// - OR rowId is null but entityPersister has rowId mapping
		if ( (isImpliedOptimisticLocking && loadedState != null)
				|| ((entityEntry == null || entityEntry.getLoadedState() == null) && entityPersister.hasPartitionedSelectionMapping())
				|| (rowId == null && entityPersister.hasRowId()) ) {
			decomposeDynamicDelete(
					ordinalBase,
					action.getInstance(),
					identifier,
					rowId,
					version,
					state,
					loadedState,
					getUpdatedAttributeIndexesForDeletedEntity( decompositionContext, action ),
					definedOptimisticLockStyle,
					postDeleteHandling,
					session,
					operationConsumer
			);
		}
		else {
			decomposeStaticDelete(
					ordinalBase,
					action.getInstance(),
					identifier,
					rowId,
					version,
					state,
					entityEntry == null ? null : entityEntry.getLoadedState(),
					getUpdatedAttributeIndexesForDeletedEntity( decompositionContext, action ),
					definedOptimisticLockStyle,
					postDeleteHandling,
					session,
					operationConsumer
			);
		}
	}

	private void registerAfterTransactionCompletion(
			EntityDeleteAction action,
			DeleteCacheHandling.CacheLock cacheLock,
			SharedSessionContractImplementor session) {
		final var callback = new DeleteAfterTransactionCompletionHandling( action, cacheLock );
		if ( callback.isNeeded( session ) ) {
			session.getTransactionCompletionCallbacks().registerCallback( callback );
		}
	}

	private boolean hasCascadeDeleteTables() {
		for ( var tableDescriptor : entityPersister.getTableDescriptors() ) {
			if ( tableDescriptor.cascadeDeleteEnabled() ) {
				return true;
			}
		}
		return false;
	}

	private PlannedOperation createNoOpDeleteCallbackCarrier(
			int ordinalBase,
			PreDeleteHandling preDeleteHandling,
			PostDeleteHandling postDeleteHandling) {
		final PlannedOperation operation = DecompositionSupport.createNoOpCallbackCarrier(
				entityPersister.getIdentifierTableDescriptor(),
				ordinalBase * 1_000,
				postDeleteHandling
		);
		operation.setPreExecutionCallback( preDeleteHandling );
		return operation;
	}

	private static int[] getUpdatedAttributeIndexesForDeletedEntity(
			DecompositionContext decompositionContext,
			EntityDeleteAction action) {
		return decompositionContext == null
				? null
				: decompositionContext.getUpdatedAttributeIndexesForDeletedEntity( action.getInstance() );
	}

	private void decomposeDynamicDelete(
			int ordinalBase,
			Object instance,
			Object identifier,
			Object rowId,
			Object version,
			Object[] state,
			Object[] loadedState,
			int[] sameFlushUpdatedAttributeIndexes,
			OptimisticLockStyle optimisticLockStyle,
			PostDeleteHandling postDeleteHandling,
			SharedSessionContractImplementor session,
			Consumer<PlannedOperation> operationConsumer) {
		final var dynamicMutations = generateMutations( rowId, loadedState, sameFlushUpdatedAttributeIndexes, true, session );

		int localOrd = 0;
		PlannedOperation previousOperation = null;
		for ( Map.Entry<String, TableDelete> entry : dynamicMutations.entrySet() ) {
			var mutation = entry.getValue().createMutationOperation(null, sessionFactory);
			var tableMapping = (TableDescriptorAsTableMapping) mutation.getTableDetails();
			var tableDescriptor = (EntityTableDescriptor) tableMapping.getDescriptor();

			final EntityDeleteBindPlan bindPlan = new EntityDeleteBindPlan(
					tableDescriptor,
					entityPersister,
					identifier,
					rowId,
					version,
					state,
					loadedState,
					sameFlushUpdatedAttributeIndexes,
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
			op.setPreExecutionCallback( postDeleteHandling.getPreDeleteHandling() );

			if ( previousOperation != null ) {
				operationConsumer.accept( previousOperation );
			}
			previousOperation = op;
		}

		// Attach post-execution callback to the last operation
		if ( previousOperation != null ) {
			previousOperation.setPostExecutionCallback( postDeleteHandling );
			operationConsumer.accept( previousOperation );
		}
	}

	private void decomposeStaticDelete(
			int ordinalBase,
			Object instance,
			Object identifier,
			Object rowId,
			Object version,
			Object[] state,
			Object[] loadedState,
			int[] sameFlushUpdatedAttributeIndexes,
			OptimisticLockStyle optimisticLockStyle,
			PostDeleteHandling postDeleteHandling,
			SharedSessionContractImplementor session,
			Consumer<PlannedOperation> operationConsumer) {
		final boolean applyVersion;
		final Map<String,TableDelete> tableDeletesToUse;
		if ( instance == null ) {
			applyVersion = false;
			if ( staticNoVersionDeleteMutations == null ) {
				staticNoVersionDeleteMutations = generateMutations("", null, null, false, session );
			}
			tableDeletesToUse = staticNoVersionDeleteMutations;
		}
		else {
			applyVersion = true;
			tableDeletesToUse = staticDeleteMutations;
		}

		int localOrd = 0;
		PlannedOperation previousOperation = null;
		for ( Map.Entry<String, TableDelete> entry : tableDeletesToUse.entrySet() ) {
			var mutation = entry.getValue().createMutationOperation(null, sessionFactory);
			var tableMapping = (TableDescriptorAsTableMapping) mutation.getTableDetails();
			var tableDescriptor = (EntityTableDescriptor) tableMapping.getDescriptor();

			final EntityDeleteBindPlan bindPlan = new EntityDeleteBindPlan(
					tableDescriptor,
					entityPersister,
					identifier,
					rowId,
					applyVersion ? version : null,
					applyVersion ? state : null,
					applyVersion ? loadedState : null,
					applyVersion ? sameFlushUpdatedAttributeIndexes : null,
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
			op.setPreExecutionCallback( postDeleteHandling.getPreDeleteHandling() );

			if ( previousOperation != null ) {
				operationConsumer.accept( previousOperation );
			}
			previousOperation = op;
		}

		// Attach post-execution callback to the last operation
		if ( previousOperation != null ) {
			previousOperation.setPostExecutionCallback( postDeleteHandling );
			operationConsumer.accept( previousOperation );
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

	protected Map<String, TableDelete> generateMutations(
			Object rowId,
			Object[] loadedState,
			int[] sameFlushUpdatedAttributeIndexes,
			boolean applyVersion,
			SharedSessionContractImplementor session) {
		final Map<String, TableDeleteBuilder> deleteBuilders = linkedMapOfSize( entityPersister.getTableDescriptors().length );

		// Process tables in reverse order (child tables before parent)
		entityPersister.forEachMutableTableDescriptorReverse( (tableDescriptor) -> {
			if ( !tableDescriptor.cascadeDeleteEnabled() ) {
				deleteBuilders.put( tableDescriptor.name(), createTableDeleteBuilder(tableDescriptor) );
			}
		} );

		applyTableDeleteDetails(
				deleteBuilders,
				rowId,
				loadedState,
				sameFlushUpdatedAttributeIndexes,
				applyVersion,
				session
		);

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
			int[] sameFlushUpdatedAttributeIndexes,
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
					sameFlushUpdatedAttributeIndexes,
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
			int[] sameFlushUpdatedAttributeIndexes,
			SharedSessionContractImplementor session) {
		if ( optimisticLockStyle.isVersion() && entityPersister.getVersionMapping() != null ) {
			applyVersionOptLocking( tableDeleteBuilders );
		}
		else if ( loadedState != null && optimisticLockStyle.isAllOrDirty() ) {
			applyNonVersionOptLocking(
					optimisticLockStyle,
					tableDeleteBuilders,
					loadedState,
					sameFlushUpdatedAttributeIndexes,
					session
			);
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
			int[] sameFlushUpdatedAttributeIndexes,
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
				if ( contains( sameFlushUpdatedAttributeIndexes, attribute.getStateArrayPosition() ) ) {
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

	private static boolean contains(int[] values, int value) {
		if ( values == null ) {
			return false;
		}
		for ( int candidate : values ) {
			if ( candidate == value ) {
				return true;
			}
		}
		return false;
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
