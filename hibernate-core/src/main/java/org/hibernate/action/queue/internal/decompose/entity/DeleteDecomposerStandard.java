/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.decompose.entity;

import org.hibernate.action.queue.spi.decompose.entity.EntityMutationPlanContributor;
import org.hibernate.action.queue.spi.decompose.entity.PreDeleteHandling;
import org.hibernate.action.queue.spi.decompose.entity.PostDeleteHandling;
import org.hibernate.action.queue.spi.decompose.entity.CancelledInsertPostDeleteHandling;

import org.hibernate.action.internal.EntityDeleteAction;
import org.hibernate.action.queue.spi.MutationKind;
import org.hibernate.action.queue.spi.StatementShapeKey;
import org.hibernate.action.queue.spi.bind.PostExecutionCallback;
import org.hibernate.action.queue.spi.decompose.DecompositionContext;
import org.hibernate.action.queue.internal.decompose.collection.DecompositionSupport;
import org.hibernate.action.queue.spi.meta.EntityTableDescriptor;
import org.hibernate.action.queue.spi.meta.TableDescriptor;
import org.hibernate.action.queue.spi.meta.TableDescriptorAsTableMapping;
import org.hibernate.sql.model.TableMapping;
import org.hibernate.sql.model.ast.TableDelete;
import org.hibernate.sql.model.ast.TableMutation;
import org.hibernate.sql.model.ast.builder.TableDeleteBuilder;
import org.hibernate.sql.model.ast.builder.TableDeleteBuilderStandard;
import org.hibernate.action.queue.spi.plan.FlushOperation;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.internal.CacheHelper.CacheLock;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.MutationOperation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.hibernate.internal.util.collections.CollectionHelper.linkedMapOfSize;


/// [Decomposer][org.hibernate.action.queue.spi.decompose.entity.EntityActionDecomposer] for entity delete operations.
///
/// Converts an [EntityDeleteAction] into a group of [FlushOperation] to be performed.
/// The standard delete lifecycle is always handled here, including natural-id
/// cleanup, cache locking, pre/post delete callbacks, same-flush insert/delete
/// no-op handling, and cascade-delete shortcuts.  Before physical DELETE
/// operations are planned, this decomposer asks its [EntityMutationPlanContributor]
/// whether the logical delete should be represented by an alternate complete
/// mutation plan.  Soft-delete, for example, contributes an UPDATE plan while
/// still reusing this decomposer's lifecycle handling.
///
/// See [EntityDeleteBindPlan].
/// See [EntitySoftDeleteBindPlan].
/// See [EntityMutationPlanContributor].
///
/// @author Steve Ebersole
public class DeleteDecomposerStandard extends AbstractDecomposer<EntityDeleteAction> implements DeleteDecomposer {
	private final Map<String, TableDelete> staticDeleteMutations;
	private final Map<String, MutationOperation> staticJdbcDeleteMutations;
	private final Map<String, StatementShapeKey> staticStatementShapeKeys;
	private Map<String, TableDelete> staticNoVersionDeleteMutations;
	private Map<String, MutationOperation> staticNoVersionJdbcDeleteMutations;
	private Map<String, StatementShapeKey> staticNoVersionStatementShapeKeys;
	private final EntityMutationPlanContributor mutationPlanContributor;
	private final Map<String, ? extends TableMutation<?>> staticDeleteOperations;

	private final String origin;

	public DeleteDecomposerStandard(EntityPersister entityPersister, SessionFactoryImplementor sessionFactory) {
		this( entityPersister, sessionFactory, EntityMutationPlanContributor.STANDARD );
	}

	public DeleteDecomposerStandard(
			EntityPersister entityPersister,
			SessionFactoryImplementor sessionFactory,
			EntityMutationPlanContributor mutationPlanContributor) {
		super( entityPersister, sessionFactory );

		this.mutationPlanContributor = mutationPlanContributor;
		this.staticDeleteOperations = mutationPlanContributor.getStaticDeleteOperations();
		this.staticDeleteMutations = staticDeleteOperations.isEmpty()
				? generateMutations( "", null, null, true, null )
				: Map.of();
		this.staticJdbcDeleteMutations = generateStaticJdbcOperations( staticDeleteMutations );
		this.staticStatementShapeKeys = generateStaticStatementShapeKeys( staticJdbcDeleteMutations );

		this.origin = "EntityDeleteAction(" + entityPersister.getEntityName() + ")";
	}

	public Map<String, ? extends TableMutation<?>> getStaticDeleteOperations() {
		return staticDeleteOperations.isEmpty() ? staticDeleteMutations : staticDeleteOperations;
	}

	@Override
	public void decompose(
			EntityDeleteAction action,
			int ordinalBase,
			SharedSessionContractImplementor session,
			DecompositionContext decompositionContext,
			Consumer<FlushOperation> operationConsumer) {
		final Object identifier = action.getId();
		final Object version = action.getVersion();
		final Object[] state = action.getState();

		if ( decompositionContext != null && decompositionContext.isBeingInsertedInCurrentFlush( action.getInstance() ) ) {
			operationConsumer.accept( createCancelledDeleteCallbackCarrier( ordinalBase, action ) );
			return;
		}

		final Object naturalIdValues = DeleteNaturalIdHandling.removeLocalResolution( action, session );

		final var cacheLock = DeleteCacheHandling.lockItem( action, session );
		registerAfterTransactionCompletion( action, cacheLock, session );
		final var preDeleteHandling = new PreDeleteHandling( action );
		final var postDeleteHandling = new PostDeleteHandling(
				action,
				cacheLock == null ? null : cacheLock.cacheKey(),
				naturalIdValues,
				preDeleteHandling
		);

		final var context = new EntityMutationPlanContributor.DeleteContext(
				entityPersister,
				action,
				ordinalBase,
				session,
				decompositionContext,
				identifier,
				version,
				state,
				postDeleteHandling
		);

		if ( mutationPlanContributor.contributeReplacementDelete( context, operationConsumer ) ) {
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
			final var previousOperation = decomposeDynamicDelete(
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
			emitTailOperations( previousOperation, context, postDeleteHandling, operationConsumer );
		}
		else {
			final var previousOperation = decomposeStaticDelete(
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
			emitTailOperations( previousOperation, context, postDeleteHandling, operationConsumer );
		}
	}

	private void emitTailOperations(
			FlushOperation previousOperation,
			EntityMutationPlanContributor.DeleteContext context,
			PostExecutionCallback postExecutionCallback,
			Consumer<FlushOperation> operationConsumer) {
		final List<FlushOperation> additionalOperations = new ArrayList<>();
		mutationPlanContributor.contributeAdditionalDelete( context, additionalOperations::add );
		if ( additionalOperations.isEmpty() ) {
			if ( previousOperation != null ) {
				previousOperation.setPostExecutionCallback( postExecutionCallback );
				operationConsumer.accept( previousOperation );
			}
		}
		else {
			if ( previousOperation != null ) {
				operationConsumer.accept( previousOperation );
			}
			for ( int i = 0; i < additionalOperations.size() - 1; i++ ) {
				operationConsumer.accept( additionalOperations.get( i ) );
			}
			final var lastOperation = additionalOperations.get( additionalOperations.size() - 1 );
			lastOperation.setPostExecutionCallback( postExecutionCallback );
			operationConsumer.accept( lastOperation );
		}
	}

	private void registerAfterTransactionCompletion(
			EntityDeleteAction action,
			CacheLock cacheLock,
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

	private FlushOperation createNoOpDeleteCallbackCarrier(
			int ordinalBase,
			PreDeleteHandling preDeleteHandling,
			PostDeleteHandling postDeleteHandling) {
		final var operation = DecompositionSupport.createNoOpCallbackCarrier(
				entityPersister.getIdentifierTableDescriptor(),
				ordinalBase * 1_000,
				postDeleteHandling
		);
		operation.setPreExecutionCallback( preDeleteHandling );
		return operation;
	}

	private FlushOperation createCancelledDeleteCallbackCarrier(
			int ordinalBase,
			EntityDeleteAction action) {
		return DecompositionSupport.createNoOpCallbackCarrier(
				entityPersister.getIdentifierTableDescriptor(),
				ordinalBase * 1_000,
				new CancelledInsertPostDeleteHandling( action )
		);
	}

	private static int[] getUpdatedAttributeIndexesForDeletedEntity(
			DecompositionContext decompositionContext,
			EntityDeleteAction action) {
		return decompositionContext == null
				? null
				: decompositionContext.getUpdatedAttributeIndexesForDeletedEntity( action.getInstance() );
	}

	private FlushOperation decomposeDynamicDelete(
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
			Consumer<FlushOperation> operationConsumer) {
		final var dynamicMutations =
				generateMutations( rowId, loadedState, sameFlushUpdatedAttributeIndexes, true, session );

		int localOrd = 0;
		FlushOperation previousOperation = null;
		for ( var entry : dynamicMutations.entrySet() ) {
			final var mutation = entry.getValue().createMutationOperation(null, sessionFactory);
			final var tableMapping = (TableDescriptorAsTableMapping) mutation.getTableDetails();
			final var tableDescriptor = (EntityTableDescriptor) tableMapping.descriptor();
			final var op = new FlushOperation(
					tableDescriptor,
					MutationKind.DELETE,
					mutation,
					new EntityDeleteBindPlan(
							tableDescriptor,
							entityPersister,
							identifier,
							rowId,
							version,
							state,
							loadedState,
							sameFlushUpdatedAttributeIndexes,
							optimisticLockStyle
					),
					ordinalBase * 1_000 + (localOrd++),
					origin,
					null
			);
			op.setPreExecutionCallback( postDeleteHandling.getPreDeleteHandling() );

			if ( previousOperation != null ) {
				operationConsumer.accept( previousOperation );
			}
			previousOperation = op;
		}

		return previousOperation;
	}

	private FlushOperation decomposeStaticDelete(
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
			Consumer<FlushOperation> operationConsumer) {
		final boolean applyVersion;
		final Map<String,TableDelete> tableDeletesToUse;
		if ( instance == null ) {
			applyVersion = false;
			if ( staticNoVersionDeleteMutations == null ) {
				staticNoVersionDeleteMutations = generateMutations("", null, null, false, session );
				staticNoVersionJdbcDeleteMutations = generateStaticJdbcOperations( staticNoVersionDeleteMutations );
				staticNoVersionStatementShapeKeys = generateStaticStatementShapeKeys( staticNoVersionJdbcDeleteMutations );
			}
			tableDeletesToUse = staticNoVersionDeleteMutations;
		}
		else {
			applyVersion = true;
			tableDeletesToUse = staticDeleteMutations;
		}

		int localOrd = 0;
		FlushOperation previousOperation = null;
		for ( var entry : tableDeletesToUse.entrySet() ) {
			final var mutation = resolveJdbcDeleteOperation( entry.getKey(), entry.getValue(), applyVersion );
			final var shapeKey = resolveStatementShapeKey( entry.getKey(), entry.getValue(), applyVersion );
			final var tableMapping = (TableDescriptorAsTableMapping) mutation.getTableDetails();
			final var tableDescriptor = (EntityTableDescriptor) tableMapping.descriptor();
			final var op = new FlushOperation(
					tableDescriptor,
					MutationKind.DELETE,
					mutation,
					new EntityDeleteBindPlan(
							tableDescriptor,
							entityPersister,
							identifier,
							rowId,
							applyVersion ? version : null,
							applyVersion ? state : null,
							applyVersion ? loadedState : null,
							applyVersion ? sameFlushUpdatedAttributeIndexes : null,
							applyVersion ? optimisticLockStyle : OptimisticLockStyle.NONE
					),
					ordinalBase * 1_000 + (localOrd++),
					origin,
					shapeKey
			);
			op.setPreExecutionCallback( postDeleteHandling.getPreDeleteHandling() );

			if ( previousOperation != null ) {
				operationConsumer.accept( previousOperation );
			}
			previousOperation = op;
		}

		return previousOperation;
	}

	private Map<String, MutationOperation> generateStaticJdbcOperations(Map<String, TableDelete> staticOperations) {
		final Map<String, MutationOperation> jdbcOperations = linkedMapOfSize( staticOperations.size() );
		staticOperations.forEach( (name, operation) -> {
			jdbcOperations.put( name, operation.createMutationOperation( null, sessionFactory ) );
		} );
		return Collections.unmodifiableMap( jdbcOperations );
	}

	private Map<String, StatementShapeKey> generateStaticStatementShapeKeys(
			Map<String, MutationOperation> staticJdbcOperations) {
		final Map<String, StatementShapeKey> shapeKeys = linkedMapOfSize( staticJdbcOperations.size() );
		staticJdbcOperations.forEach( (name, operation) -> {
			shapeKeys.put( name, StatementShapeKey.forMutation(
					name,
					MutationKind.DELETE,
					findTableDescriptor( name ),
					operation
			) );
		} );
		return Collections.unmodifiableMap( shapeKeys );
	}

	private MutationOperation resolveJdbcDeleteOperation(
			String tableName,
			TableDelete tableDelete,
			boolean applyVersion) {
		final var staticDeletes = applyVersion
				? staticDeleteMutations
				: staticNoVersionDeleteMutations;
		final var staticJdbcDeletes = applyVersion
				? staticJdbcDeleteMutations
				: staticNoVersionJdbcDeleteMutations;
		return staticJdbcDeletes != null
			&& tableDelete == staticDeletes.get( tableName )
				? staticJdbcDeletes.get( tableName )
				: tableDelete.createMutationOperation( null, sessionFactory );
	}

	private StatementShapeKey resolveStatementShapeKey(
			String tableName,
			TableDelete tableDelete,
			boolean applyVersion) {
		final var staticDeletes = applyVersion
				? staticDeleteMutations
				: staticNoVersionDeleteMutations;
		final var staticShapeKeys = applyVersion
				? staticStatementShapeKeys
				: staticNoVersionStatementShapeKeys;
		return staticShapeKeys != null
			&& tableDelete == staticDeletes.get( tableName )
				? staticShapeKeys.get( tableName )
				: null;
	}

	private OptimisticLockStyle definedOptimisticLockStyle() {
		final var optimisticLockStyle = entityPersister.optimisticLockStyle();
		return optimisticLockStyle.isVersion()
			&& entityPersister.getVersionMapping() == null
				? OptimisticLockStyle.NONE
				: optimisticLockStyle;

	}

//	private OptimisticLockStyle effectiveOptimisticLockStyle(
//			OptimisticLockStyle optimisticLockStyle,
//			Object loadedVersion,
//			Object[] loadedState) {
//		if ( optimisticLockStyle.isVersion()  ) {
//			if ( loadedVersion == null ) {
//				return OptimisticLockStyle.NONE;
//			}
//		}
//		else if ( optimisticLockStyle.isAllOrDirty() ) {
//			if ( loadedState == null ) {
//				return OptimisticLockStyle.NONE;
//			}
//		}
//		return optimisticLockStyle;
//	}

	protected Map<String, TableDelete> generateMutations(
			Object rowId,
			Object[] loadedState,
			int[] sameFlushUpdatedAttributeIndexes,
			boolean applyVersion,
			SharedSessionContractImplementor session) {
		final Map<String, TableDeleteBuilder> deleteBuilders =
				linkedMapOfSize( entityPersister.getTableDescriptors().length );

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
		final boolean isIdentifierTable =
				tableDescriptor instanceof EntityTableDescriptor etd
						&& etd.isIdentifierTable();
		final var tableMapping = new TableDescriptorAsTableMapping(
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
		final var tableMapping = builder.getMutatingTable().getTableMapping();
		final var tableDescriptor = ( (TableDescriptorAsTableMapping) tableMapping ).descriptor();
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
			var tableDescriptor = (EntityTableDescriptor) adapter.descriptor();
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
