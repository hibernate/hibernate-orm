/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.decompose.entity;

import org.hibernate.action.queue.spi.decompose.entity.EntityMutationPlanContributor;
import org.hibernate.action.queue.spi.decompose.entity.UpdateCacheHandling;

import org.hibernate.action.internal.EntityUpdateAction;
import org.hibernate.action.queue.spi.MutationKind;
import org.hibernate.action.queue.spi.bind.PostExecutionCallback;
import org.hibernate.action.queue.internal.decompose.collection.DecompositionSupport;
import org.hibernate.action.queue.spi.bind.GeneratedValuesCollector;
import org.hibernate.action.queue.spi.decompose.DecompositionContext;
import org.hibernate.action.queue.spi.meta.EntityTableDescriptor;
import org.hibernate.action.queue.spi.meta.TableDescriptor;
import org.hibernate.action.queue.spi.meta.TableDescriptorAsTableMapping;
import org.hibernate.action.queue.spi.plan.FlushOperation;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.Generator;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EntityVersionMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.UnionSubclassEntityPersister;
import org.hibernate.sql.model.ast.LogicalTableUpdate;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.ast.builder.AssigningTableMutationBuilder;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilder;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilderStandard;
import org.hibernate.sql.model.ast.builder.VersionUpdateBuilder;
import org.hibernate.sql.model.internal.TableUpdateStandard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.hibernate.action.queue.internal.decompose.entity.DecompositionHelper.hasValueGenerationOnExecution;
import static org.hibernate.generator.EventType.UPDATE;
import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;
import static org.hibernate.internal.util.collections.ArrayHelper.EMPTY_INT_ARRAY;
import static org.hibernate.internal.util.collections.ArrayHelper.join;
import static org.hibernate.internal.util.collections.ArrayHelper.trim;


/// Decomposer for entity update operations.
///
/// Converts an [EntityUpdateAction] into a group of [FlushOperation] to be performed.
/// The standard update lifecycle is always handled here.  After pre-update
/// handling, natural-id handling, cache locking, and no-op callback-carrier
/// handling are resolved, this decomposer gives its
/// [EntityMutationPlanContributor] a chance to emit an alternate complete
/// mutation plan.  If no contributor handles the action, the decomposer emits
/// the normal physical table update operations.
///
/// See [EntityUpdateBindPlan].
/// See [EntityMutationPlanContributor].
///
/// @author Steve Ebersole
public class UpdateDecomposer extends AbstractDecomposer<EntityUpdateAction> {
	private final Map<String, LogicalTableUpdate<?>> staticUpdateOperations;
	private final Map<TableDescriptor, TableDescriptorAsTableMapping> tableMappingAdapters = new IdentityHashMap<>();
	private final TableUpdateStandard versionUpdate;
	private final EntityMutationPlanContributor mutationPlanContributor;

	public UpdateDecomposer(EntityPersister entityPersister, SessionFactoryImplementor sessionFactory) {
		this( entityPersister, sessionFactory, EntityMutationPlanContributor.STANDARD );
	}

	public UpdateDecomposer(
			EntityPersister entityPersister,
			SessionFactoryImplementor sessionFactory,
			EntityMutationPlanContributor mutationPlanContributor) {
		super( entityPersister, sessionFactory );

		this.staticUpdateOperations = entityPersister.isDynamicUpdate()
				// entity specified dynamic-update - skip static operations
				? null
				: generateStaticOperations();

		this.versionUpdate = entityPersister.getVersionMapping() != null
				? new VersionUpdateBuilder( entityPersister ).buildMutation()
				: null;
		this.mutationPlanContributor = mutationPlanContributor;
	}

	public Map<String, LogicalTableUpdate<?>> getStaticUpdateOperations() {
		return staticUpdateOperations;
	}

	@Override
	public void decompose(
			EntityUpdateAction action,
			int ordinalBase,
			SharedSessionContractImplementor session,
			DecompositionContext decompositionContext,
			Consumer<FlushOperation> operationConsumer) {
		final boolean vetoed = preUpdate( action, session );
		if ( vetoed ) {
			return;
		}
		if ( decompositionContext != null ) {
			decompositionContext.registerOwnerUpdateCallbacks( action.getInstance() );
		}

		final Object entity = action.getInstance();
		Object identifier = action.getId();
		if ( identifier == null && decompositionContext != null ) {
			identifier = decompositionContext.getGeneratedIdentifierHandle( entity );
		}
		final Object rowId = action.getRowId();
		final Object[] state = action.getState();
		final Object[] previousState = action.getPreviousState();
		final Object previousVersion = action.getPreviousVersion();

		// Capture the EntityEntry now, at decomposition time, because for entities being deleted
		// in the same flush, the DELETE operation may remove the entity from the persistence context
		// before the UPDATE's post-execution callback runs.
		final var entityEntry = session.getPersistenceContextInternal().getEntry( entity );

		// Skip UPDATE operations for entities with DELETED status ONLY if there are no dirty fields.
		// When an entity is marked for deletion in the same flush, UPDATEs with no dirty fields
		// are unnecessary since the DELETE will remove the row entirely.
		// HOWEVER: For self-referential FK cycles, Hibernate generates UPDATEs to NULL the FK
		// before DELETE. These UPDATEs have dirty fields and MUST execute to avoid constraint violations.
		if ( entityEntry != null && entityEntry.getStatus() == Status.DELETED ) {
			final int[] dirtyFields = action.getDirtyFields();
			if ( dirtyFields == null || dirtyFields.length == 0 ) {
				// No dirty fields - skip the UPDATE
				return;
			}
			// Has dirty fields - must be a NULL-before-DELETE update, allow it to proceed
		}

		action.handleNaturalIdLocalResolutions( identifier, entityPersister, session.getPersistenceContext() );
		final UpdateCacheHandling.CacheUpdate cacheUpdate = UpdateCacheHandling.lockItem( action, previousVersion, session );
		registerAfterTransactionCompletion( action, cacheUpdate, session );

		if ( hasOnlyInversePluralDirtiness( action ) ) {
			operationConsumer.accept( DecompositionSupport.createNoOpCallbackCarrier(
					entityPersister.getIdentifierTableDescriptor(),
					ordinalBase * 1_000,
					new PostUpdateHandling( action, cacheUpdate, previousVersion, null, entityEntry )
			) );
			return;
		}

		final EntityMutationPlanContributor.UpdateContext context = new EntityMutationPlanContributor.UpdateContext(
				entityPersister,
				action,
				ordinalBase,
				session,
				decompositionContext,
				entity,
				identifier,
				rowId,
				state,
				previousState,
				previousVersion,
				entityEntry,
				cacheUpdate
		);

		if ( mutationPlanContributor.contributeReplacementUpdate(
				context,
				operationConsumer
		) ) {
			return;
		}

		var generatedValuesCollector = GeneratedValuesCollector.forUpdate( entityPersister, sessionFactory );
		final PostUpdateHandling postUpdateHandling = new PostUpdateHandling(
				action,
				cacheUpdate,
				previousVersion,
				generatedValuesCollector,
				entityEntry
		);

		final var versionMapping = entityPersister.getVersionMapping();
		if ( versionMapping != null ) {
			var forceIncrementOperation = possiblyBuildForcedVersionIncrementOperation(
					action,
					ordinalBase,
					cacheUpdate,
					entity,
					identifier,
					state,
					previousVersion,
					action.getDirtyFields(),
					versionMapping,
					session,
					entityEntry
			);
			if ( forceIncrementOperation != null ) {
				final List<FlushOperation> additionalOperations = new ArrayList<>();
				mutationPlanContributor.contributeAdditionalUpdate(
						context,
						additionalOperations::add
				);
				emitTailOperations( forceIncrementOperation, additionalOperations, postUpdateHandling, ordinalBase, operationConsumer );
				return;
			}
		}

		// apply any pre-update in-memory value generation
		final int[] preUpdateGeneratedAttributeIndexes = preUpdateInMemoryValueGeneration( entity, state, session );
		final int[] dirtyAttributeIndexes = combine( action.getDirtyFields(), preUpdateGeneratedAttributeIndexes );

		// Determine if we need to apply optimistic locking
		final var effectiveOptLockStyle = effectiveOptLockStyle( previousVersion, previousState );

		// Determine which fields are updateable
		final boolean[] updateability = entityPersister.getPropertyUpdateability();
		logImmutablePropertyModifications( dirtyAttributeIndexes, updateability );

		// Create values analysis to track which tables need updating
		final var valuesAnalysis = new UpdateValuesAnalysis(
				entityPersister,
				state,
				previousState,
				dirtyAttributeIndexes,
				this::getTableMappingAdapter
		);


		// Choose between static or dynamic update group.  Use dynamic update if:
		// 		1. Entity specifies dynamic-update
		// 		2. We need optimistic locking with DIRTY check
		//		3. The entity has any uninitialized state
		final boolean needsDynamicUpdate = entityPersister.isDynamicUpdate()
				|| rowId != null
				|| effectiveOptLockStyle.isAllOrDirty()
				|| preUpdateGeneratedAttributeIndexes.length > 0
				|| entityPersister.hasUninitializedLazyProperties( entity );

		final Map<String, LogicalTableUpdate<?>> effectiveGroup;
		if ( needsDynamicUpdate ) {
			effectiveGroup = generateDynamicUpdateOperations(
					entity,
					identifier,
					rowId,
					state,
					previousState,
					previousVersion,
					updateability,
					valuesAnalysis,
					session
			);
		}
		else {
			effectiveGroup = staticUpdateOperations;
		}

		int localOrd = 0;
		FlushOperation previousOperation = null;

		// determine whether the entity we are about to update is being deleted in the same flush
		final boolean isBeingDeleted = decompositionContext != null
				&& decompositionContext.isBeingDeletedInCurrentFlush( entity );

		for ( Map.Entry<String, LogicalTableUpdate<?>> entry : effectiveGroup.entrySet() ) {
			var operation = entry.getValue().createMutationOperation(null, sessionFactory);
			var tableMapping = (TableDescriptorAsTableMapping) operation.getTableDetails();
			var tableDescriptor = (EntityTableDescriptor) tableMapping.descriptor();

			// For static updates, only execute secondary tables when one of their
			// attributes actually drove the update.  Static mutation groups may
			// contain generated SQL assignments for joined-subclass tables, but
			// legacy execution only runs tables registered as needing update and
			// reads other generated values through the post-update select.
			if ( !needsDynamicUpdate
					&& !tableDescriptor.isIdentifierTable()
					&& !valuesAnalysis.needsUpdate( tableDescriptor ) ) {
				continue;
			}

			// If this entity is being deleted in the same flush, skip UPDATEs to optional tables.
			// These UPDATEs can re-insert rows that were just deleted, causing constraint violations.
			// DELETE → DELETE edges ensure proper FK ordering, so UPDATEs are unnecessary.
			//
			// Technically we could alo skip updates to the primary table if no foreign keys are being nullified.
			// This should be a rare enough occurrence that it is probably not worth even checking for.
			if ( isBeingDeleted && tableDescriptor.isOptional() ) {
				continue;
			}

			final EntityUpdateBindPlan bindPlan = createUpdateBindPlan(
					tableDescriptor,
					entity,
					identifier,
					rowId,
					state,
					previousState,
					previousVersion,
					updateability,
					effectiveOptLockStyle,
					valuesAnalysis,
					needsDynamicUpdate,
					generatedValuesCollector
			);

			final FlushOperation op = new FlushOperation(
					tableDescriptor,
					MutationKind.UPDATE,
					operation,
					bindPlan,
					ordinalBase * 1_000 + (localOrd++),
					"EntityUpdateAction(" + entityPersister.getEntityName() + ")"
			);

			if ( previousOperation != null ) {
				operationConsumer.accept( previousOperation );
			}
			previousOperation = op;
		}

		final List<FlushOperation> additionalOperations = new ArrayList<>();
		mutationPlanContributor.contributeAdditionalUpdate(
				context,
				additionalOperations::add
		);

		emitTailOperations( previousOperation, additionalOperations, postUpdateHandling, ordinalBase, operationConsumer );
	}

	private void logImmutablePropertyModifications(int[] dirtyAttributeIndexes, boolean[] updateability) {
		if ( dirtyAttributeIndexes == null ) {
			return;
		}
		for ( int dirtyAttributeIndex : dirtyAttributeIndexes ) {
			if ( !updateability[dirtyAttributeIndex] ) {
				final AttributeMapping attributeMapping = entityPersister.getAttributeMapping( dirtyAttributeIndex );
				CORE_LOGGER.ignoreImmutablePropertyModification(
						attributeMapping.getAttributeName(),
						entityPersister.getEntityName()
				);
			}
		}
	}

	private void emitTailOperations(
			FlushOperation previousOperation,
			List<FlushOperation> additionalOperations,
			PostExecutionCallback postExecutionCallback,
			int ordinalBase,
			Consumer<FlushOperation> operationConsumer) {
		if ( additionalOperations.isEmpty() ) {
			if ( previousOperation != null ) {
				previousOperation.setPostExecutionCallback( postExecutionCallback );
				operationConsumer.accept( previousOperation );
			}
			else {
				operationConsumer.accept( DecompositionSupport.createNoOpCallbackCarrier(
						entityPersister.getIdentifierTableDescriptor(),
						ordinalBase * 1_000,
						postExecutionCallback
				) );
			}
			return;
		}

		if ( previousOperation != null ) {
			operationConsumer.accept( previousOperation );
		}
		for ( int i = 0; i < additionalOperations.size() - 1; i++ ) {
			operationConsumer.accept( additionalOperations.get( i ) );
		}
		final FlushOperation lastOperation = additionalOperations.get( additionalOperations.size() - 1 );
		lastOperation.setPostExecutionCallback( postExecutionCallback );
		operationConsumer.accept( lastOperation );
	}

	private boolean hasOnlyInversePluralDirtiness(EntityUpdateAction action) {
		final int[] dirtyFields = action.getDirtyFields();
		if ( dirtyFields == null || action.getNextVersion() != null ) {
			return false;
		}

		for ( int dirtyField : dirtyFields ) {
			final AttributeMapping attribute = entityPersister.getAttributeMapping( dirtyField );
			final PluralAttributeMapping pluralAttribute = attribute.asPluralAttributeMapping();
			if ( pluralAttribute == null || !pluralAttribute.getCollectionDescriptor().isInverse() ) {
				return false;
			}
		}

		return true;
	}

	private void registerAfterTransactionCompletion(
			EntityUpdateAction action,
			UpdateCacheHandling.CacheUpdate cacheUpdate,
			SharedSessionContractImplementor session) {
		final var callback = new UpdateAfterTransactionCompletionHandling( action, cacheUpdate );
		if ( callback.isNeeded( session ) ) {
			session.getTransactionCompletionCallbacks().registerCallback( callback );
		}
	}

	/// Handle the case where the only value being updated is the version.
	/// We treat this case specially in `#coordinateUpdate` to leverage
	/// `#doVersionUpdate`.
	private FlushOperation possiblyBuildForcedVersionIncrementOperation(
			EntityUpdateAction action,
			int ordinalBase,
			UpdateCacheHandling.CacheUpdate cacheUpdate,
			Object entity,
			Object identifier,
			Object[] state,
			Object previousVersion,
			int[] dirtyFields,
			EntityVersionMapping versionMapping,
			SharedSessionContractImplementor session,
			EntityEntry entityEntry) {
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
		final var identifierTableDescriptor = entityPersister.getIdentifierTableDescriptor();
		var bindPlan = new ForceVersionBindPlan(
				identifierTableDescriptor,
				entityPersister,
				entity,
				identifier,
				previousVersion,
				newVersion
		);
		final FlushOperation op = new FlushOperation(
				identifierTableDescriptor,
				MutationKind.UPDATE,
				jdbcUpdate,
				bindPlan,
				ordinalBase * 1_000,
				"EntityUpdateAction(" + entityPersister.getEntityName() + ")"
		);

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
		// todo : we should be skipping static update generation for DIRTY opt locking?

		final Map<String, TableUpdateBuilder<?>> staticOperationBuilders = new HashMap<>();

		// Process tables in forward order
		entityPersister.forEachMutableTableDescriptor( (tableDescriptor) -> {
			if ( tableDescriptor.isInverse() ) {
				// skip inverse tables
				return;
			}
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
			if ( operationBuilder.hasAssignmentBindings() ) {
				staticOperations.put( name, operationBuilder.buildMutation() );
			}
		} );
		return Collections.unmodifiableMap( staticOperations );
	}

	private TableUpdateBuilder<?> createTableUpdateBuilder(TableDescriptor tableDescriptor) {
		return new TableUpdateBuilderStandard<>(
				entityPersister,
				new MutatingTableReference( getTableMappingAdapter( tableDescriptor ) ),
				sessionFactory
		);
	}

	private TableDescriptorAsTableMapping getTableMappingAdapter(TableDescriptor tableDescriptor) {
		return tableMappingAdapters.computeIfAbsent( tableDescriptor, this::createTableMappingAdapter );
	}

	private TableDescriptorAsTableMapping createTableMappingAdapter(TableDescriptor tableDescriptor) {
		final boolean isIdentifierTable = tableDescriptor instanceof EntityTableDescriptor entityTableDescriptor
				&& entityTableDescriptor.isIdentifierTable();
		final boolean isInverse = tableDescriptor instanceof EntityTableDescriptor entityTableDescriptor
				&& entityTableDescriptor.isInverse();
		return new TableDescriptorAsTableMapping(
				tableDescriptor,
				tableDescriptor.getRelativePosition(),
				isIdentifierTable,
				isInverse
		);
	}

	protected Map<String, LogicalTableUpdate<?>> generateDynamicUpdateOperations(
			Object entity,
			Object identifier,
			Object rowId,
			Object[] state,
			Object[] previousState,
			Object version,
			boolean[] updateable,
			UpdateValuesAnalysis valuesAnalysis,
			SharedSessionContractImplementor session) {
		final Map<String, TableUpdateBuilder<?>> operationBuilders = new HashMap<>();

		// Process tables in forward order
		entityPersister.forEachMutableTableDescriptor( (tableDescriptor) -> {
			if (  tableDescriptor.isInverse() ) {
				// skip inverse tables
				return;
			}
			if ( valuesAnalysis.needsUpdate( tableDescriptor ) ) {
				operationBuilders.put(
						tableDescriptor.name(),
						createTableUpdateBuilder(tableDescriptor)
				);
			}
		} );

		applyDynamicUpdateDetails(
				operationBuilders,
				entity,
				state,
				previousState,
				version,
				rowId,
				valuesAnalysis,
				updateable,
				session
		);

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

		builders.forEach( (name, builder) -> {
			// Get the TableDescriptor from the adapter
			final var tableMapping = (TableDescriptorAsTableMapping) builder.getMutatingTable().getTableMapping();
			final var tableDescriptor = (EntityTableDescriptor) tableMapping.descriptor();
			// Skip inverse tables
			if (tableDescriptor.isInverse()) {
				return;
			}

			// Apply SET clause columns for attributes
			for ( int i = 0; i < tableDescriptor.attributes().size(); i++ ) {
				var attribute = tableDescriptor.attributes().get( i );
				if ( propertyUpdateability[attribute.getStateArrayPosition()]
						|| isValueGenerationOnUpdateInSql( attribute.getGenerator() )) {
					applyValueAssignment(
							// entity
							null,
							entityPersister.getVersionMapping(),
							attribute,
							builder,
							// session
							null
					);
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


	private void applyValueAssignment(
			Object entity,
			EntityVersionMapping versionMapping,
			AttributeMapping attributeMapping,
			AssigningTableMutationBuilder<?> tableUpdateBuilder,
			SharedSessionContractImplementor session) {
		final var generator = attributeMapping.getGenerator();
		if ( generator instanceof OnExecutionGenerator onExecutionGenerator
				&& hasValueGenerationOnExecution( onExecutionGenerator, UPDATE, entity, session, dialect() ) ) {
			handleValueGeneration( attributeMapping, tableUpdateBuilder, onExecutionGenerator );
		}
		else if ( versionMapping != null
				&& versionMapping.getVersionAttribute() == attributeMapping) {
			tableUpdateBuilder.addColumnAssignment( versionMapping.getVersionAttribute() );
		}
		else {
			attributeMapping.forEachUpdatable( tableUpdateBuilder::addColumnAssignment );
		}
	}

	private Dialect dialect() {
		return sessionFactory.getJdbcServices().getDialect();
	}

	protected void handleValueGeneration(
			AttributeMapping attributeMapping,
			AssigningTableMutationBuilder<?> tableUpdateBuilder,
			OnExecutionGenerator generator) {
		final var dialect = sessionFactory.getJdbcServices().getDialect();
		final var columnValues = generator.getReferencedColumnValues( dialect, UPDATE );
		final var columnInclusions = generator.getColumnInclusions( dialect, UPDATE );
		attributeMapping.forEachSelectable( (j, mapping) -> {
			if ( columnInclusions == null || columnInclusions[j] ) {
				final String columnValue = columnValues != null && columnValues[j] != null
						? columnValues[j]
						: "?";
				tableUpdateBuilder.addColumnAssignment( mapping, columnValue );
			}
		} );
	}

	private boolean isValueGenerationOnUpdateInSql(Generator generator) {
		return generator != null
				&& generator.generatedOnExecution()
				&& generator.generatesOnUpdate()
				&& ( (OnExecutionGenerator) generator ).referenceColumnsInSql( dialect(), EventType.UPDATE );
	}

	private void applyDynamicUpdateDetails(
			Map<String, TableUpdateBuilder<?>> builders,
			Object entity,
			Object[] state,
			Object[] previousState,
			Object version,
			Object rowId,
			UpdateValuesAnalysis valuesAnalysis,
			boolean[] updateable,
			SharedSessionContractImplementor session) {
		builders.forEach( (name, builder) -> {
			// Get the TableDescriptor from the adapter
			final var tableMapping = (TableDescriptorAsTableMapping) builder.getMutatingTable().getTableMapping();
			final var tableDescriptor = (EntityTableDescriptor) tableMapping.descriptor();
			// Skip inverse tables
			if (tableDescriptor.isInverse()) {
				return;
			}

			// Apply SET clause columns for attributes
			for ( int i = 0; i < tableDescriptor.attributes().size(); i++ ) {
				var attribute = tableDescriptor.attributes().get( i );
				if ( shouldIncludeInDynamicUpdate( attribute, builder, updateable, entity, valuesAnalysis, session ) ) {
					if ( state[attribute.getStateArrayPosition()] == LazyPropertyInitializer.UNFETCHED_PROPERTY ) {
						// it was not fetched and so could not have changed, skip it
						continue;
					}
					applyValueAssignment( entity, entityPersister.getVersionMapping(), attribute, builder, session );
				}
			}

			// Apply WHERE clause - key restrictions
			applyKeyRestriction( builder, rowId );

			// Apply optimistic locking
			applyOptimisticLocking( builder, previousState, valuesAnalysis, session );
		} );

		// Apply partitioned selection restrictions if needed
		if ( entityPersister.hasPartitionedSelectionMapping() ) {
			applyPartitionedSelectionRestrictions( builders );
		}
	}

	private boolean shouldIncludeInDynamicUpdate(
			AttributeMapping attribute,
			AssigningTableMutationBuilder<?> builder,
			boolean[] updateable,
			Object entity,
			UpdateValuesAnalysis valuesAnalysis, SharedSessionContractImplementor session) {
		final var versionMapping = entityPersister.getVersionMapping();
		if ( versionMapping != null
				&& versionMapping.getVersionAttribute() == attribute ) {
			return true;
		}

		// First check if the attribute is updateable or has update-generated values
		if ( !updateable[attribute.getStateArrayPosition()]
				&& !isValueGenerationOnUpdateInSql( attribute.getGenerator() )) {
			return false;
		}

		// If we have dirty fields, only include dirty ones
		if ( valuesAnalysis.hasDirtyAttributes() ) {
			return valuesAnalysis.getDirtiness()[attribute.getStateArrayPosition()];
		}

		return true;
	}

	private void applyKeyRestriction(TableUpdateBuilder<?> tableUpdateBuilder) {
		applyKeyRestriction( tableUpdateBuilder, null );
	}

	private void applyKeyRestriction(TableUpdateBuilder<?> tableUpdateBuilder, Object rowId) {
		// Get the TableDescriptor from the adapter
		final var tableMapping = (TableDescriptorAsTableMapping) tableUpdateBuilder.getMutatingTable().getTableMapping();
		final var tableDescriptor = tableMapping.descriptor();
		if ( rowId != null
				&& tableDescriptor instanceof EntityTableDescriptor entityTableDescriptor
				&& entityTableDescriptor.isIdentifierTable()
				&& entityPersister.getRowIdMapping() != null ) {
			tableUpdateBuilder.addKeyRestrictionLeniently( entityPersister.getRowIdMapping() );
		}
		else {
			tableUpdateBuilder.addKeyRestrictions( tableDescriptor.keyDescriptor() );
		}
	}

	private void applyOptimisticLocking(
			TableUpdateBuilder<?> tableUpdateBuilder,
			Object[] previousState,
			UpdateValuesAnalysis valuesAnalysis,
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
		final var tableDescriptor = tableMapping.descriptor();
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
			UpdateValuesAnalysis valuesAnalysis,
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
		final var tableDescriptor = (EntityTableDescriptor) tableMapping.descriptor();

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
						builder.addKeyRestrictionLeniently( selectableMapping );
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
			UpdateValuesAnalysis valuesAnalysis,
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
