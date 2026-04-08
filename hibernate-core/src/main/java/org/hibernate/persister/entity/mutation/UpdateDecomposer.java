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
import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.Generator;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.EntityVersionMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.UnionSubclassEntityPersister;
import org.hibernate.sql.model.ast.LogicalTableUpdate;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilder;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilderStandard;
import org.hibernate.sql.model.ast.builder.VersionUpdateBuilder;
import org.hibernate.sql.model.internal.TableUpdateStandard;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hibernate.generator.EventType.UPDATE;
import static org.hibernate.internal.util.collections.ArrayHelper.EMPTY_INT_ARRAY;
import static org.hibernate.internal.util.collections.ArrayHelper.join;
import static org.hibernate.internal.util.collections.ArrayHelper.trim;


/// Decomposer for entity update operations.
///
/// Converts an [EntityUpdateAction] into a group of [PlannedOperation] to be performed.
///
/// @see EntityUpdateBindPlan
///
/// @author Steve Ebersole
public class UpdateDecomposer extends AbstractDecomposer<EntityUpdateAction> {
	private final Map<String, LogicalTableUpdate<?>> staticUpdateOperations;
	private final TableUpdateStandard versionUpdate;

	public UpdateDecomposer(EntityPersister entityPersister, SessionFactoryImplementor sessionFactory) {
		super( entityPersister, sessionFactory );

		this.staticUpdateOperations = entityPersister.isDynamicUpdate()
				// entity specified dynamic-update - skip static operations
				? null
				: generateStaticOperations();

		this.versionUpdate = entityPersister.getVersionMapping() != null
				? new VersionUpdateBuilder( entityPersister ).buildMutation()
				: null;
	}

	public Map<String, LogicalTableUpdate<?>> getStaticUpdateOperations() {
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
		final Object previousVersion = action.getPreviousVersion();

		action.handleNaturalIdLocalResolutions( identifier, entityPersister, session.getPersistenceContext() );
		final Object cacheKey = action.lockCacheItem( previousVersion );

		final var versionMapping = entityPersister.getVersionMapping();
		if ( versionMapping != null ) {
			var forceIncrementOperation = possiblyBuildForcedVersionIncrementOperation(
					action,
					ordinalBase,
					cacheKey,
					entity,
					identifier,
					state,
					previousVersion,
					action.getDirtyFields(),
					versionMapping,
					session
			);
			if ( forceIncrementOperation != null ) {
				return List.of( forceIncrementOperation );
			}
		}

		// apply any pre-update in-memory value generation
		final int[] preUpdateGeneratedAttributeIndexes = preUpdateInMemoryValueGeneration( entity, state, session );
		final int[] dirtyAttributeIndexes = combine( action.getDirtyFields(), preUpdateGeneratedAttributeIndexes );

		// Determine if we need to apply optimistic locking
		final var effectiveOptLockStyle = effectiveOptLockStyle( previousVersion, previousState );

		// Determine which fields are updateable
		final boolean[] updateable = entityPersister.getPropertyUpdateability();

		// Create values analysis to track which tables need updating
		final var valuesAnalysis = new UpdateValuesAnalysisForDecomposer(
				entityPersister,
				state,
				previousState,
				dirtyAttributeIndexes
		);


		// Choose between static or dynamic update group.  Use dynamic update if:
		// 		1. Entity specifies dynamic-update
		// 		2. We need optimistic locking with DIRTY check
		//		3. The entity has any uninitialized state
		final boolean needsDynamicUpdate = entityPersister.isDynamicUpdate()
				|| effectiveOptLockStyle.isAllOrDirty()
				|| entityPersister.hasUninitializedLazyProperties( entity );

		final Map<String, LogicalTableUpdate<?>> effectiveGroup = needsDynamicUpdate
				? generateDynamicUpdateOperations( identifier, rowId, state, previousState, previousState, updateable, valuesAnalysis, session )
				: staticUpdateOperations;

		var generatedValuesCollector = GeneratedValuesCollector.forUpdate( entityPersister, sessionFactory );
		final PostUpdateHandling postUpdateHandling = new PostUpdateHandling( action, cacheKey, previousVersion, generatedValuesCollector );

		final List<PlannedOperation> operations = CollectionHelper.arrayList( effectiveGroup.size() );
		int localOrd = 0;

		for ( Map.Entry<String, LogicalTableUpdate<?>> entry : effectiveGroup.entrySet() ) {
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
					previousVersion,
					updateable,
					effectiveOptLockStyle,
					valuesAnalysis,
					needsDynamicUpdate,
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

	/// Handle the case where the only value being updated is the version.
	/// We treat this case specially in `#coordinateUpdate` to leverage
	/// `#doVersionUpdate`.
	private PlannedOperation possiblyBuildForcedVersionIncrementOperation(
			EntityUpdateAction action,
			int ordinalBase,
			Object cacheKey,
			Object entity,
			Object identifier,
			Object[] state,
			Object previousVersion,
			int[] dirtyFields,
			EntityVersionMapping versionMapping,
			SharedSessionContractImplementor session) {
		if ( entityPersister.hasUpdateGeneratedProperties() || entityPersister.hasPreUpdateGeneratedProperties() ) {
			// if we have any fields generated by the UPDATE event,
			// then we have to include the generated fields in the
			// update statement
			return null;
		}

		Object newVersion = null;
		if ( dirtyFields != null ) {
			switch ( dirtyFields.length ) {
				case 1:
					final int dirtyAttributeIndex = dirtyFields[0];
					final var versionAttribute = versionMapping.getVersionAttribute();
					final var dirtyAttribute = entityPersister.getAttributeMapping( dirtyAttributeIndex );
					if ( versionAttribute == dirtyAttribute ) {
						// only the version attribute itself is dirty
						newVersion = state[dirtyAttributeIndex];
					}
					else {
						// the dirty field is some other field
						return null;
					}
					break;
				case 0:
					if ( previousVersion != null ) {
						newVersion = state[versionMapping.getVersionAttribute().getStateArrayPosition()];
						if ( versionMapping.areEqual( newVersion, previousVersion, session ) ) {
							return null;
						}
					}
					else {
						return null;
					}
					break;
				default:
					return null;
			}
		}
		else {
			return null;
		}

		// we have just the version being updated - use the special handling
		assert newVersion != null;

		var jdbcUpdate = versionUpdate.createMutationOperation( null, sessionFactory );
		var bindPlan = new ForceVersionBindPlan( entityPersister, entity, identifier, previousVersion, newVersion );
		final PlannedOperation op = new PlannedOperation(
				entityPersister.getIdentifierTableDescriptor(),
				MutationKind.UPDATE,
				jdbcUpdate,
				bindPlan,
				ordinalBase * 1_000,
				"EntityUpdateAction(" + entityPersister.getEntityName() + ")"
		);

		op.setPostExecutionCallback( new PostUpdateHandling(
				action,
				cacheKey,
				newVersion,
				null
		) );

		return op;
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

	private int[] preUpdateInMemoryValueGeneration(
			Object object,
			Object[] newValues,
			SharedSessionContractImplementor session) {
		if ( !entityPersister.hasPreUpdateGeneratedProperties() ) {
			return EMPTY_INT_ARRAY;
		}

		final var generators = entityPersister.getGenerators();
		if ( generators.length != 0 ) {
			final int[] fieldsPreUpdateNeeded = new int[generators.length];
			int count = 0;
			for ( int i = 0; i < generators.length; i++ ) {
				final Generator generator = generators[i];
				if ( generator != null
						&& generator.generatesOnUpdate()
						&& generator.generatedBeforeExecution( object, session ) ) {
					newValues[i] = ( (BeforeExecutionGenerator) generator ).generate( session, object, newValues[i], UPDATE );
					entityPersister.setValue( object, i, newValues[i] );
					fieldsPreUpdateNeeded[count++] = i;
				}
			}

			if ( count > 0 ) {
				return trim( fieldsPreUpdateNeeded, count );
			}
		}

		return EMPTY_INT_ARRAY;
	}

	private int[] combine(int[] dirtyFields, int[] preUpdateGeneratedIndexes) {
		if ( preUpdateGeneratedIndexes.length == 0 ) {
			return dirtyFields;
		}
		else {
			return dirtyFields == null
					? preUpdateGeneratedIndexes
					: join( dirtyFields, preUpdateGeneratedIndexes );
		}
	}

	private OptimisticLockStyle effectiveOptLockStyle(Object version, Object[] previousState) {
		final OptimisticLockStyle optimisticLockStyle = entityPersister.optimisticLockStyle();

		if ( optimisticLockStyle.isVersion() ) {
			if ( version == null || entityPersister.getVersionMapping() == null) {
				return OptimisticLockStyle.NONE;
			}
		}

		if ( optimisticLockStyle.isAllOrDirty() ) {
			if ( previousState == null ) {
				return OptimisticLockStyle.NONE;
			}
		}

		return optimisticLockStyle;
	}

	private Map<String, LogicalTableUpdate<?>> generateStaticOperations() {
		final Map<String, TableUpdateBuilder<?>> staticOperationBuilders = new HashMap<>();

		// Process tables in forward order
		entityPersister.forEachMutableTableDescriptor( (tableDescriptor) -> {
			staticOperationBuilders.put(
					tableDescriptor.name(),
					createTableUpdateBuilder(tableDescriptor)
			);
		} );

		applyStaticUpdateDetails( staticOperationBuilders );

		final Map<String, LogicalTableUpdate<?>> staticOperations = new HashMap<>();
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

	protected Map<String, LogicalTableUpdate<?>> generateDynamicUpdateOperations(
			Object identifier,
			Object rowId,
			Object[] state,
			Object[] previousState,
			Object version,
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

		applyDynamicUpdateDetails( operationBuilders, state, previousState, version, valuesAnalysis, updateable, session );

		final Map<String, LogicalTableUpdate<?>> operations = new HashMap<>();
		operationBuilders.forEach( (name, operationBuilder) -> {
			// Only build mutation if there are columns to update
			if ( operationBuilder.hasAssignmentBindings() ) {
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
					attribute.forEachUpdatable( builder );
				}
			}

			// Apply WHERE clause - key restrictions
			applyKeyRestriction( builder );

			// Apply optimistic locking
			applyOptimisticLocking( builder, null, null, null );
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
			UpdateValuesAnalysisForDecomposer valuesAnalysis,
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
				if ( shouldIncludeInDynamicUpdate( attribute.getStateArrayPosition(), updateable, valuesAnalysis ) ) {
					if ( state[attribute.getStateArrayPosition()] == LazyPropertyInitializer.UNFETCHED_PROPERTY ) {
						// it was not fetched and so could not have changed, skip it
						continue;
					}

					attribute.forEachUpdatable( builder );
				}
			}

			// Apply WHERE clause - key restrictions
			applyKeyRestriction( builder );

			// Apply optimistic locking
			applyOptimisticLocking( builder, previousState, valuesAnalysis, session );
		} );

		// Apply partitioned selection restrictions if needed
		if ( entityPersister.hasPartitionedSelectionMapping() ) {
			applyPartitionedSelectionRestrictions( builders );
		}
	}

	private boolean shouldIncludeInDynamicUpdate(
			int attributeIndex,
			boolean[] updateable,
			UpdateValuesAnalysisForDecomposer valuesAnalysis) {
		if ( !updateable[attributeIndex] ) {
			return false;
		}

		// If we have dirty fields, only include dirty ones
		if ( valuesAnalysis.hasDirtyAttributes() ) {
			return valuesAnalysis.getDirtiness()[attributeIndex];
		}

		return true;
	}

	private void applyKeyRestriction(TableUpdateBuilder<?> tableUpdateBuilder) {
		// Get the TableDescriptor from the adapter
		final var tableMapping = (TableDescriptorAsTableMapping) tableUpdateBuilder.getMutatingTable().getTableMapping();
		final var tableDescriptor = tableMapping.getDescriptor();
		tableUpdateBuilder.addKeyRestrictions( tableDescriptor.keyDescriptor() );
	}

	private void applyOptimisticLocking(
			TableUpdateBuilder<?> tableUpdateBuilder,
			Object[] previousState,
			UpdateValuesAnalysisForDecomposer valuesAnalysis,
			SharedSessionContractImplementor session) {
		final var optimisticLockStyle = entityPersister.optimisticLockStyle();

		if ( optimisticLockStyle.isVersion() && entityPersister.getVersionMapping() != null ) {
			applyVersionBasedOptLocking( tableUpdateBuilder );
		}
		else if ( optimisticLockStyle.isAllOrDirty() ) {
			applyNonVersionOptLocking( tableUpdateBuilder, previousState, optimisticLockStyle, valuesAnalysis, session );
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

	private void applyNonVersionOptLocking(
			TableUpdateBuilder<?> tableUpdateBuilder,
			Object[] previousState,
			OptimisticLockStyle optimisticLockStyle,
			UpdateValuesAnalysisForDecomposer valuesAnalysis,
			SharedSessionContractImplementor session) {
		if ( previousState == null ) {
			// this indicates that the state was never loaded from the database -
			// there is no locking to apply
			//
			// EARLY EXIT!!
			return;
		}

		final boolean[] versionability = entityPersister.getPropertyVersionability();
		// Get the TableDescriptor from the adapter
		final var tableMapping = (TableDescriptorAsTableMapping) tableUpdateBuilder.getMutatingTable().getTableMapping();
		final var tableDescriptor = (EntityTableDescriptor) tableMapping.getDescriptor();

		for ( int i = 0; i < tableDescriptor.attributes().size(); i++ ) {
			var attribute = tableDescriptor.attributes().get( i );
			if ( !versionability[attribute.getStateArrayPosition()] ) {
				continue;
			}

			if ( optimisticLockStyle.isDirty() && !valuesAnalysis.getDirtiness()[attribute.getStateArrayPosition()] ) {
				continue;
			}

			// we know the attribute is part of optimistically locking the entity.
			// check the loaded state to determine if we need `where col = ?` or `where col is null`
			var previousValue = previousState[attribute.getStateArrayPosition()];
			// todo : not sure how all ModelParts handle `null` for breakDownJdbcValues...
			//		we may need explicit handling of that here...
			attribute.breakDownJdbcValues(
					previousValue,
					(valueIndex, value, jdbcValueMapping) -> {
						if ( jdbcValueMapping.isFormula() ) {
							// skip formulas
						}
						else {
							if ( value == null ) {
								tableUpdateBuilder.addNullOptimisticLockRestriction( jdbcValueMapping );
							}
							else {
								tableUpdateBuilder.addOptimisticLockRestriction( jdbcValueMapping );
							}
						}
					},
					session
			);
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
			boolean[] updateable,
			OptimisticLockStyle effectiveOptLockStyle,
			UpdateValuesAnalysisForDecomposer valuesAnalysis,
			boolean needsDynamicUpdate,
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
				updateable,
				effectiveOptLockStyle,
				valuesAnalysis,
				needsDynamicUpdate,
				generatedValuesCollector
		);
	}
}
