/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.action.internal.EntityUpdateAction;
import org.hibernate.action.queue.bind.BindPlan;
import org.hibernate.action.queue.exec.PostExecutionCallback;
import org.hibernate.action.queue.MutationKind;
import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.generator.EventType;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.ast.builder.MutationGroupBuilder;
import org.hibernate.sql.model.ast.builder.RestrictedTableMutationBuilder;
import org.hibernate.sql.model.ast.builder.TableMutationBuilder;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilder;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilderSkipped;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilderStandard;
import org.hibernate.sql.model.ast.MutatingTableReference;

import java.util.ArrayList;
import java.util.List;

/// Decomposer for entity update operations.
///
/// Converts an [EntityUpdateAction] into a group of [PlannedOperation] to be performed.
///
/// @see UpdateBindPlan
///
/// @author Steve Ebersole
public class UpdateDecomposer extends AbstractDecomposer<EntityUpdateAction> {
	private final MutationOperationGroup staticUpdateGroup;
	private final BasicBatchKey batchKey;

	public UpdateDecomposer(EntityPersister entityPersister, SessionFactoryImplementor sessionFactory) {
		super( entityPersister, sessionFactory );

		this.staticUpdateGroup = entityPersister.isDynamicUpdate()
				// entity specified dynamic-update - skip static operations
				? null
				: generateStaticOperationGroup();

		// Batching support for updates (unless update-generated properties)
		this.batchKey = entityPersister.hasUpdateGeneratedProperties()
				? null
				: new BasicBatchKey( entityPersister.getEntityName() + "#UPDATE" );
	}

	public MutationOperationGroup getStaticMutationGroup() {
		return staticUpdateGroup;
	}

	@Override
	public List<PlannedOperation> decompose(
			EntityUpdateAction action,
			int ordinalBase,
			java.util.function.Consumer<PostExecutionCallback> postExecutionCallbackRegistry,
			SharedSessionContractImplementor session) {
		final boolean vetoed = preUpdate( action, session );
		if ( vetoed ) {
			return List.of();
		}

		final Object entity = action.getInstance();
		final Object identifier = action.getId();
		final Object rowId = action.getRowId();
		final Object[] state = action.getState();
		final Object[] previousState = action.getPreviousState();
		final Object version = action.getPreviousVersion();
		final int[] dirtyFields = action.getDirtyFields();

		action.handleNaturalIdLocalResolutions( identifier, entityPersister, session.getPersistenceContext() );
		final Object cacheKey = action.lockCacheItem( version );

		// Determine if we need to apply optimistic locking
		final boolean applyOptimisticLocking = shouldApplyOptimisticLocking( version, previousState );

		// Determine which fields are updateable
		final boolean[] updateable = entityPersister.getPropertyUpdateability();

		// Create values analysis to track which tables need updating
		final var valuesAnalysis = new UpdateValuesAnalysisForDecomposer(
				entityPersister,
				state,
				previousState,
				dirtyFields
		);

		// Choose between static or dynamic update group
		final var effectiveGroup = chooseEffectiveUpdateGroup(
				identifier,
				rowId,
				state,
				previousState,
				version,
				dirtyFields,
				updateable,
				applyOptimisticLocking,
				valuesAnalysis,
				session
		);

		final var generatedValuesCollector = new GeneratedValuesCollector( entityPersister, EventType.UPDATE );
		final PostUpdateHandling postUpdateHandling = new PostUpdateHandling( action, cacheKey, version, generatedValuesCollector );
		postExecutionCallbackRegistry.accept( postUpdateHandling );

		final List<PlannedOperation> operations = new ArrayList<>(effectiveGroup.getNumberOfOperations());
		int localOrd = 0;

		for ( int i = 0; i < effectiveGroup.getNumberOfOperations(); i++ ) {
			var operation = effectiveGroup.getOperation( i );
			var table = (EntityTableMapping) operation.getTableDetails();
			String tableName = table.getTableName();

			final BindPlan bindPlan = createUpdateBindPlan(
					entity,
					identifier,
					rowId,
					state,
					previousState,
					version,
					dirtyFields,
					updateable,
					applyOptimisticLocking,
					valuesAnalysis
			);

			final PlannedOperation op = new PlannedOperation(
					tableName,
					MutationKind.UPDATE,
					operation,
					bindPlan,
					ordinalBase * 1_000 + (localOrd++),
					"EntityUpdateAction(" + entityPersister.getEntityName() + ")"
			);

			operations.add(op);
		}

		return operations;
	}

	protected boolean preUpdate(EntityUpdateAction action, SharedSessionContractImplementor session) {
		final var listenerGroup = session.getFactory().getEventListenerGroups().eventListenerGroup_PRE_UPDATE;
		if ( listenerGroup.isEmpty() ) {
			return false;
		}
		else {
			final PreUpdateEvent event = new PreUpdateEvent(
					action.getInstance(),
					action.getId(),
					action.getState(),
					action.getPreviousState(),
					action.getPersister(),
					session
			);
			boolean veto = false;
			for ( var listener : listenerGroup.listeners() ) {
				veto |= listener.onPreUpdate( event );
			}
			return veto;
		}
	}

	private boolean shouldApplyOptimisticLocking(Object version, Object[] previousState) {
		final OptimisticLockStyle optimisticLockStyle = entityPersister.optimisticLockStyle();

		if ( optimisticLockStyle.isVersion() ) {
			return version != null && entityPersister.getVersionMapping() != null;
		}
		else if ( optimisticLockStyle.isAllOrDirty() ) {
			return previousState != null;
		}

		return false;
	}

	private MutationOperationGroup chooseEffectiveUpdateGroup(
			Object identifier,
			Object rowId,
			Object[] state,
			Object[] previousState,
			Object version,
			int[] dirtyFields,
			boolean[] updateable,
			boolean applyOptimisticLocking,
			UpdateValuesAnalysisForDecomposer valuesAnalysis,
			SharedSessionContractImplementor session) {
		// Use dynamic update if:
		// 1. Entity specifies dynamic-update
		// 2. We need optimistic locking with DIRTY check
		final boolean needsDynamicUpdate = entityPersister.isDynamicUpdate()
				|| (applyOptimisticLocking && entityPersister.optimisticLockStyle() == OptimisticLockStyle.DIRTY);

		return needsDynamicUpdate
				? generateDynamicUpdateGroup( identifier, rowId, state, previousState, version, dirtyFields, updateable, valuesAnalysis, session )
				: staticUpdateGroup;
	}

	public MutationOperationGroup generateStaticOperationGroup() {
		final var updateGroupBuilder = new MutationGroupBuilder( MutationType.UPDATE, entityPersister );

		// Process tables in forward order
		entityPersister.forEachMutableTable( (tableMapping) -> {
			final var tableUpdateBuilder = new TableUpdateBuilderStandard<>( entityPersister, tableMapping, sessionFactory );
			updateGroupBuilder.addTableDetailsBuilder( tableUpdateBuilder );
		} );

		applyStaticUpdateDetails( updateGroupBuilder );

		return createOperationGroup( null, updateGroupBuilder.buildMutationGroup() );
	}

	protected MutationOperationGroup generateDynamicUpdateGroup(
			Object identifier,
			Object rowId,
			Object[] state,
			Object[] previousState,
			Object version,
			int[] dirtyFields,
			boolean[] updateable,
			UpdateValuesAnalysisForDecomposer valuesAnalysis,
			SharedSessionContractImplementor session) {
		final var updateGroupBuilder = new MutationGroupBuilder( MutationType.UPDATE, entityPersister );

		// Process tables in forward order
		entityPersister.forEachMutableTable( (tableMapping) -> {
			final var tableReference = new MutatingTableReference( tableMapping );
			final TableMutationBuilder<?> tableUpdateBuilder;
			if ( !valuesAnalysis.needsUpdate( tableMapping ) ) {
				// This table does not need updating
				tableUpdateBuilder = new TableUpdateBuilderSkipped( tableReference );
			}
			else {
				tableUpdateBuilder = new TableUpdateBuilderStandard<>( entityPersister, tableMapping, sessionFactory );
			}
			updateGroupBuilder.addTableDetailsBuilder( tableUpdateBuilder );
		} );

		applyDynamicUpdateDetails( updateGroupBuilder, state, previousState, version, dirtyFields, updateable, session );

		return createOperationGroup( null, updateGroupBuilder.buildMutationGroup() );
	}

	private void applyStaticUpdateDetails(MutationGroupBuilder updateGroupBuilder) {
		final var attributeMappings = entityPersister.getAttributeMappings();
		final boolean[] propertyUpdateability = entityPersister.getPropertyUpdateability();

		// Apply SET clause columns
		updateGroupBuilder.forEachTableMutationBuilder( (builder) -> {
			final var tableMapping = (EntityTableMapping) builder.getMutatingTable().getTableMapping();
			final var tableUpdateBuilder = (TableUpdateBuilder<?>) builder;

			final int[] attributeIndexes = tableMapping.getAttributeIndexes();
			for ( int i = 0; i < attributeIndexes.length; i++ ) {
				final int attributeIndex = attributeIndexes[i];
				if ( propertyUpdateability[attributeIndex] ) {
					final var attributeMapping = attributeMappings.get( attributeIndex );
					attributeMapping.forEachUpdatable( updateGroupBuilder );
				}
			}

			// Apply WHERE clause - key restrictions
			applyKeyRestriction( tableUpdateBuilder, tableMapping );

			// Apply optimistic locking
			applyOptimisticLocking( tableUpdateBuilder, tableMapping );
		} );

		// Apply partitioned selection restrictions if needed
		if ( entityPersister.hasPartitionedSelectionMapping() ) {
			applyPartitionedSelectionRestrictions( updateGroupBuilder );
		}
	}

	private void applyDynamicUpdateDetails(
			MutationGroupBuilder updateGroupBuilder,
			Object[] state,
			Object[] previousState,
			Object version,
			int[] dirtyFields,
			boolean[] updateable,
			SharedSessionContractImplementor session) {
		final var attributeMappings = entityPersister.getAttributeMappings();

		// Apply SET clause columns
		updateGroupBuilder.forEachTableMutationBuilder( (builder) -> {
			final var tableMapping = (EntityTableMapping) builder.getMutatingTable().getTableMapping();
			final var tableUpdateBuilder = (TableUpdateBuilder<?>) builder;

			final int[] attributeIndexes = tableMapping.getAttributeIndexes();
			for ( int i = 0; i < attributeIndexes.length; i++ ) {
				final int attributeIndex = attributeIndexes[i];
				if ( shouldIncludeInDynamicUpdate( attributeIndex, updateable, dirtyFields ) ) {
					final var attributeMapping = attributeMappings.get( attributeIndex );
					attributeMapping.forEachUpdatable( updateGroupBuilder );
				}
			}

			// Apply WHERE clause - key restrictions
			applyKeyRestriction( tableUpdateBuilder, tableMapping );

			// Apply optimistic locking
			applyOptimisticLocking( tableUpdateBuilder, tableMapping );
		} );

		// Apply partitioned selection restrictions if needed
		if ( entityPersister.hasPartitionedSelectionMapping() ) {
			applyPartitionedSelectionRestrictions( updateGroupBuilder );
		}
	}

	private boolean shouldIncludeInDynamicUpdate(int attributeIndex, boolean[] updateable, int[] dirtyFields) {
		if ( !updateable[attributeIndex] ) {
			return false;
		}

		// If we have dirty fields, only include dirty ones
		if ( dirtyFields != null ) {
			for ( int dirtyField : dirtyFields ) {
				if ( dirtyField == attributeIndex ) {
					return true;
				}
			}
			return false;
		}

		return true;
	}

	private void applyKeyRestriction(
			TableUpdateBuilder<?> tableUpdateBuilder,
			EntityTableMapping tableMapping) {
		tableUpdateBuilder.addKeyRestrictions( tableMapping.getKeyMapping() );
	}

	private void applyOptimisticLocking(
			TableUpdateBuilder<?> tableUpdateBuilder,
			EntityTableMapping tableMapping) {
		final var optimisticLockStyle = entityPersister.optimisticLockStyle();

		if ( optimisticLockStyle.isVersion() && entityPersister.getVersionMapping() != null ) {
			applyVersionBasedOptLocking( tableUpdateBuilder, tableMapping );
		}
		else if ( optimisticLockStyle.isAllOrDirty() ) {
			applyNonVersionOptLocking( tableUpdateBuilder, tableMapping );
		}
	}

	private void applyVersionBasedOptLocking(
			TableUpdateBuilder<?> tableUpdateBuilder,
			EntityTableMapping tableMapping) {
		final var versionMapping = entityPersister.getVersionMapping();
		if ( versionMapping != null
				&& tableMapping.getTableName().equals( versionMapping.getContainingTableExpression() ) ) {
			tableUpdateBuilder.addOptimisticLockRestriction( versionMapping );
		}
	}

	private void applyNonVersionOptLocking(
			TableUpdateBuilder<?> tableUpdateBuilder,
			EntityTableMapping tableMapping) {
		final boolean[] versionability = entityPersister.getPropertyVersionability();
		final var attributeMappings = entityPersister.getAttributeMappings();

		for ( int attributeIndex = 0; attributeIndex < versionability.length; attributeIndex++ ) {
			if ( versionability[attributeIndex] ) {
				final var attribute = attributeMappings.get( attributeIndex );
				if ( !attribute.isPluralAttributeMapping()
						&& tableMapping.getTableName().equals( attribute.getContainingTableExpression() ) ) {
					// Add each selectable (column) as an optimistic lock restriction
					final int jdbcTypeCount = attribute.getJdbcTypeCount();
					for ( int i = 0; i < jdbcTypeCount; i++ ) {
						final var selectableMapping = attribute.getSelectable( i );
						if ( !selectableMapping.isFormula() ) {
							tableUpdateBuilder.addOptimisticLockRestriction( selectableMapping );
						}
					}
				}
			}
		}
	}

	private void applyPartitionedSelectionRestrictions(MutationGroupBuilder updateGroupBuilder) {
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
							updateGroupBuilder.findTableDetailsBuilder( tableNameForMutation );
					if ( rootTableMutationBuilder != null ) {
						rootTableMutationBuilder.addKeyRestrictionLeniently( selectableMapping );
					}
				}
			}
		}
	}

	private BindPlan createUpdateBindPlan(
			Object entity,
			Object identifier,
			Object rowId,
			Object[] state,
			Object[] previousState,
			Object version,
			int[] dirtyFields,
			boolean[] updateable,
			boolean applyOptimisticLocking,
			UpdateValuesAnalysisForDecomposer valuesAnalysis) {
		// Use specialized BindPlan for union subclass inheritance
		if ( entityPersister instanceof org.hibernate.persister.entity.UnionSubclassEntityPersister ) {
			return new UnionSubclassUpdateBindPlan(
					entityPersister,
					entity,
					identifier,
					rowId,
					state,
					previousState,
					version,
					dirtyFields,
					updateable,
					applyOptimisticLocking,
					valuesAnalysis
			);
		}

		// Standard BindPlan for other inheritance strategies
		return new UpdateBindPlan(
				entityPersister,
				entity,
				identifier,
				rowId,
				state,
				previousState,
				version,
				dirtyFields,
				updateable,
				applyOptimisticLocking,
				valuesAnalysis
		);
	}
}
