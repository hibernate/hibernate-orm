/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.action.internal.EntityUpdateAction;
import org.hibernate.action.queue.MutationKind;
import org.hibernate.action.queue.bind.EntityUpdateBindPlan;
import org.hibernate.action.queue.bind.GeneratedValuesCollector;
import org.hibernate.action.queue.meta.EntityTableDescriptor;
import org.hibernate.action.queue.meta.TableDescriptor;
import org.hibernate.action.queue.meta.TableDescriptorAsTableMapping;
import org.hibernate.action.queue.op.PlannedOperation;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.UnionSubclassEntityPersister;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.ast.RestrictedTableMutation;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilder;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilderStandard;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/// Decomposer for entity update operations.
///
/// Converts an [EntityUpdateAction] into a group of [PlannedOperation] to be performed.
///
/// @see EntityUpdateBindPlan
///
/// @author Steve Ebersole
public class UpdateDecomposer extends AbstractDecomposer<EntityUpdateAction> {
	private final Map<String, RestrictedTableMutation<?>> staticUpdateOperations;

	public UpdateDecomposer(EntityPersister entityPersister, SessionFactoryImplementor sessionFactory) {
		super( entityPersister, sessionFactory );

		this.staticUpdateOperations = entityPersister.isDynamicUpdate()
				// entity specified dynamic-update - skip static operations
				? null
				: generateStaticOperations();
	}

	public Map<String, RestrictedTableMutation<?>> getStaticUpdateOperations() {
		return staticUpdateOperations;
	}

	@Override
	public List<PlannedOperation> decompose(
			EntityUpdateAction action,
			int ordinalBase,
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

		var generatedValuesCollector = GeneratedValuesCollector.forUpdate( entityPersister );
		final PostUpdateHandling postUpdateHandling = new PostUpdateHandling( action, cacheKey, version, generatedValuesCollector );

		final List<PlannedOperation> operations = CollectionHelper.arrayList( effectiveGroup.size() );
		int localOrd = 0;

		for ( Map.Entry<String, RestrictedTableMutation<?>> entry : effectiveGroup.entrySet() ) {
			var operation = entry.getValue().createMutationOperation(null, sessionFactory);
			var tableMapping = (TableDescriptorAsTableMapping) operation.getTableDetails();
			var tableDescriptor = (EntityTableDescriptor) tableMapping.getDescriptor();

			final EntityUpdateBindPlan bindPlan = createUpdateBindPlan(
					tableDescriptor,
					entity,
					identifier,
					rowId,
					state,
					previousState,
					version,
					dirtyFields,
					updateable,
					applyOptimisticLocking,
					valuesAnalysis,
					generatedValuesCollector
			);

			final PlannedOperation op = new PlannedOperation(
					tableDescriptor,
					MutationKind.UPDATE,
					operation,
					bindPlan,
					ordinalBase * 1_000 + (localOrd++),
					"EntityUpdateAction(" + entityPersister.getEntityName() + ")"
			);

			operations.add(op);
		}

		// Attach post-execution callback to the last operation
		if ( !operations.isEmpty() ) {
			operations.get( operations.size() - 1 ).setPostExecutionCallback( postUpdateHandling );
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

	private Map<String, RestrictedTableMutation<?>> chooseEffectiveUpdateGroup(
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
				? generateDynamicUpdateOperations( identifier, rowId, state, previousState, version, dirtyFields, updateable, valuesAnalysis, session )
				: staticUpdateOperations;
	}

	private Map<String, RestrictedTableMutation<?>> generateStaticOperations() {
		final Map<String, TableUpdateBuilder<?>> staticOperationBuilders = new HashMap<>();

		// Process tables in forward order
		entityPersister.forEachMutableTableDescriptor( (tableDescriptor) -> {
			staticOperationBuilders.put(
					tableDescriptor.name(),
					createTableUpdateBuilder(tableDescriptor)
			);
		} );

		applyStaticUpdateDetails( staticOperationBuilders );

		final Map<String, RestrictedTableMutation<?>> staticOperations = new HashMap<>();
		staticOperationBuilders.forEach( (name, operationBuilder) -> {
			// Only build mutation if there are columns to update
			// todo : hmmm, technically we might also need to delete for optional tables
			if ( operationBuilder.hasValueBindings() ) {
				staticOperations.put( name, operationBuilder.buildMutation() );
			}
		} );
		return Collections.unmodifiableMap( staticOperations );
	}

	private TableUpdateBuilder<?> createTableUpdateBuilder(TableDescriptor tableDescriptor) {
		// Create adapter to convert TableDescriptor to TableMapping
		final boolean isIdentifierTable = tableDescriptor instanceof EntityTableDescriptor etd
				&& etd.isIdentifierTable();
		final TableDescriptorAsTableMapping tableMapping = new TableDescriptorAsTableMapping(
				tableDescriptor,
				0, // relativePosition - will be set correctly by persister
				isIdentifierTable,
				false // isInverse
		);

		return new TableUpdateBuilderStandard<>(
				entityPersister,
				new MutatingTableReference(tableMapping),
				sessionFactory
		);
	}

	protected Map<String, RestrictedTableMutation<?>> generateDynamicUpdateOperations(
			Object identifier,
			Object rowId,
			Object[] state,
			Object[] previousState,
			Object version,
			int[] dirtyFields,
			boolean[] updateable,
			UpdateValuesAnalysisForDecomposer valuesAnalysis,
			SharedSessionContractImplementor session) {
		final Map<String, TableUpdateBuilder<?>> operationBuilders = new HashMap<>();

		// Process tables in forward order
		entityPersister.forEachMutableTableDescriptor( (tableDescriptor) -> {
			if ( valuesAnalysis.needsUpdate( tableDescriptor ) ) {
				operationBuilders.put(
						tableDescriptor.name(),
						createTableUpdateBuilder(tableDescriptor)
				);
			}
		} );

		applyDynamicUpdateDetails( operationBuilders, state, previousState, version, dirtyFields, updateable, session );

		final Map<String, RestrictedTableMutation<?>> operations = new HashMap<>();
		operationBuilders.forEach( (name, operationBuilder) -> {
			// Only build mutation if there are columns to update
			if ( operationBuilder.hasValueBindings() ) {
				operations.put( name, operationBuilder.buildMutation() );
			}
		} );
		return operations;
	}

	private void applyStaticUpdateDetails(Map<String, TableUpdateBuilder<?>> builders) {
		final boolean[] propertyUpdateability = entityPersister.getPropertyUpdateability();

		// Apply SET clause columns
		builders.forEach( (name, builder) -> {
			// Get the TableDescriptor from the adapter
			final var tableMapping = (TableDescriptorAsTableMapping) builder.getMutatingTable().getTableMapping();
			final var tableDescriptor = (EntityTableDescriptor) tableMapping.getDescriptor();
			// Skip inverse tables
			if (tableDescriptor.isInverse()) {
				return;
			}

			for ( int i = 0; i < tableDescriptor.attributes().size(); i++ ) {
				var attribute = tableDescriptor.attributes().get( i );
				if ( propertyUpdateability[attribute.getStateArrayPosition()] ) {
					tableDescriptor.forEachAttributeColumn( attribute, builder::addValueColumn );
				}
			}

			// Apply WHERE clause - key restrictions
			applyKeyRestriction( builder );

			// Apply optimistic locking
			applyOptimisticLocking( builder );
		} );

		// Apply partitioned selection restrictions if needed
		if ( entityPersister.hasPartitionedSelectionMapping() ) {
			applyPartitionedSelectionRestrictions( builders );
		}
	}

	private void applyDynamicUpdateDetails(
			Map<String, TableUpdateBuilder<?>> builders,
			Object[] state,
			Object[] previousState,
			Object version,
			int[] dirtyFields,
			boolean[] updateable,
			SharedSessionContractImplementor session) {
		final Dialect dialect = sessionFactory.getJdbcServices().getDialect();

		// Apply SET clause columns
		builders.forEach( (name, builder) -> {
			// Get the TableDescriptor from the adapter
			final var tableMapping = (TableDescriptorAsTableMapping) builder.getMutatingTable().getTableMapping();
			final var tableDescriptor = (EntityTableDescriptor) tableMapping.getDescriptor();
			// Skip inverse tables
			if (tableDescriptor.isInverse()) {
				return;
			}

			for ( int i = 0; i < tableDescriptor.attributes().size(); i++ ) {
				var attribute = tableDescriptor.attributes().get( i );
				if ( shouldIncludeInDynamicUpdate( attribute.getStateArrayPosition(), updateable, dirtyFields ) ) {
					tableDescriptor.forEachAttributeColumn( attribute, builder::addValueColumn );
				}
			}

			// Apply WHERE clause - key restrictions
			applyKeyRestriction( builder );

			// Apply optimistic locking
			applyOptimisticLocking( builder );
		} );

		// Apply partitioned selection restrictions if needed
		if ( entityPersister.hasPartitionedSelectionMapping() ) {
			applyPartitionedSelectionRestrictions( builders );
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

	private void applyKeyRestriction(TableUpdateBuilder<?> tableUpdateBuilder) {
		// Get the TableDescriptor from the adapter
		final var tableMapping = (TableDescriptorAsTableMapping) tableUpdateBuilder.getMutatingTable().getTableMapping();
		final var tableDescriptor = tableMapping.getDescriptor();
		tableUpdateBuilder.addKeyRestrictions( tableDescriptor.keyDescriptor() );
	}

	private void applyOptimisticLocking(TableUpdateBuilder<?> tableUpdateBuilder) {
		final var optimisticLockStyle = entityPersister.optimisticLockStyle();

		if ( optimisticLockStyle.isVersion() && entityPersister.getVersionMapping() != null ) {
			applyVersionBasedOptLocking( tableUpdateBuilder );
		}
		else if ( optimisticLockStyle.isAllOrDirty() ) {
			applyNonVersionOptLocking( tableUpdateBuilder );
		}
	}

	private void applyVersionBasedOptLocking(TableUpdateBuilder<?> tableUpdateBuilder) {
		final var versionMapping = entityPersister.getVersionMapping();
		// Get the TableDescriptor from the adapter
		final var tableMapping = (TableDescriptorAsTableMapping) tableUpdateBuilder.getMutatingTable().getTableMapping();
		final var tableDescriptor = tableMapping.getDescriptor();
		if ( versionMapping != null
				&& tableDescriptor.name().equals(
					versionMapping.getContainingTableExpression() ) ) {
			tableUpdateBuilder.addOptimisticLockRestriction( versionMapping );
		}
	}

	private void applyNonVersionOptLocking(TableUpdateBuilder<?> tableUpdateBuilder) {
		final boolean[] versionability = entityPersister.getPropertyVersionability();
		final var attributeMappings = entityPersister.getAttributeMappings();
		// Get the TableDescriptor from the adapter
		final var tableMapping = (TableDescriptorAsTableMapping) tableUpdateBuilder.getMutatingTable().getTableMapping();
		final var tableDescriptor = tableMapping.getDescriptor();

		for ( int attributeIndex = 0; attributeIndex < versionability.length; attributeIndex++ ) {
			if ( versionability[attributeIndex] ) {
				final var attribute = attributeMappings.get( attributeIndex );
				if ( !attribute.isPluralAttributeMapping()
						&& tableDescriptor.name().equals(
							attribute.getContainingTableExpression() ) ) {
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

	private void applyPartitionedSelectionRestrictions(Map<String, TableUpdateBuilder<?>> builders) {
		final var attributeMappings = entityPersister.getAttributeMappings();

		for ( int m = 0; m < attributeMappings.size(); m++ ) {
			final var attributeMapping = attributeMappings.get( m );
			final int jdbcTypeCount = attributeMapping.getJdbcTypeCount();
			for ( int i = 0; i < jdbcTypeCount; i++ ) {
				final var selectableMapping = attributeMapping.getSelectable( i );
				if ( selectableMapping.isPartitioned() ) {
					final String tableNameForMutation =
							entityPersister.physicalTableNameForMutation( selectableMapping );
					final TableUpdateBuilder<?> builder = builders.get( tableNameForMutation );
					if ( builder != null ) {
						builder.addKeyRestriction( selectableMapping );
					}
				}
			}
		}
	}

	private EntityUpdateBindPlan createUpdateBindPlan(
			EntityTableDescriptor tableDescriptor,
			Object entity,
			Object identifier,
			Object rowId,
			Object[] state,
			Object[] previousState,
			Object version,
			int[] dirtyFields,
			boolean[] updateable,
			boolean applyOptimisticLocking,
			UpdateValuesAnalysisForDecomposer valuesAnalysis,
			GeneratedValuesCollector generatedValuesCollector) {
		final EntityTableDescriptor tableDescriptorToUse;

		if ( entityPersister instanceof UnionSubclassEntityPersister ) {
			tableDescriptorToUse = entityPersister.getIdentifierTableDescriptor();
		}
		else {
			tableDescriptorToUse = tableDescriptor;
		}

		return new EntityUpdateBindPlan(
				tableDescriptorToUse,
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
				valuesAnalysis,
				generatedValuesCollector
		);
	}
}
