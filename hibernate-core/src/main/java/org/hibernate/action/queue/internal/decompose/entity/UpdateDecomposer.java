/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.decompose.entity;

import static org.hibernate.engine.internal.TenantIdHelper.MissingRowPolicy.THROW;

import org.hibernate.engine.internal.TenantIdHelper;
import org.hibernate.action.queue.spi.decompose.entity.EntityMutationPlanContributor;
import org.hibernate.action.queue.spi.decompose.entity.UpdateCacheHandling;

import org.hibernate.action.internal.EntityUpdateAction;
import org.hibernate.action.queue.spi.MutationKind;
import org.hibernate.action.queue.spi.StatementShapeKey;
import org.hibernate.action.queue.spi.bind.PostExecutionCallback;
import org.hibernate.action.queue.spi.bind.PreExecutionCallback;
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
import org.hibernate.engine.internal.FilteredAssociationMutation;
import org.hibernate.engine.internal.FilteredUpdateCache;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
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
import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.sql.spi.mutation.TableMapping;
import org.hibernate.sql.ast.spi.model.LogicalTableUpdate;
import org.hibernate.sql.ast.spi.model.MutatingTableReference;
import org.hibernate.sql.ast.spi.model.builder.AssigningTableMutationBuilder;
import org.hibernate.sql.ast.spi.model.builder.TableUpdateBuilder;
import org.hibernate.sql.ast.spi.model.builder.TableUpdateBuilderStandard;
import org.hibernate.sql.ast.internal.model.builder.VersionUpdateBuilder;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

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
public class UpdateDecomposer extends AbstractDecomposer<EntityUpdateAction>
		implements Function<EntityTableDescriptor, TableMapping> {
	private final EntityMutationPlanContributor mutationPlanContributor;

	private final Map<TableDescriptor, TableDescriptorAsTableMapping> tableMappingAdapters = new IdentityHashMap<>();

	private final Map<String, LogicalTableUpdate<?>> staticUpdateOperations;
	private final Map<String, MutationOperation> staticJdbcUpdateOperations;
	private final MutationOperation versionJdbcUpdate;
	private final Map<String, StatementShapeKey> staticStatementShapeKeys;
	private final FilteredUpdateCache<CachedUpdates> filteredUpdateCache = new FilteredUpdateCache<>();
	private final String origin;

	private record CachedUpdates(
			Map<String, LogicalTableUpdate<?>> updates,
			Map<String, MutationOperation> jdbcOperations,
			Map<String, StatementShapeKey> shapeKeys) {
	}

	public UpdateDecomposer(EntityPersister entityPersister, SessionFactoryImplementor sessionFactory) {
		this( entityPersister, sessionFactory, EntityMutationPlanContributor.STANDARD );
	}

	public UpdateDecomposer(
			EntityPersister entityPersister,
			SessionFactoryImplementor sessionFactory,
			EntityMutationPlanContributor mutationPlanContributor) {
		super( entityPersister, sessionFactory );

		this.mutationPlanContributor = mutationPlanContributor;

		this.staticUpdateOperations = entityPersister.isDynamicUpdate()
				// entity specified dynamic-update - skip static operations
				? null
				: generateStaticOperations();
		this.staticJdbcUpdateOperations = staticUpdateOperations == null
				? null
				: generateStaticJdbcOperations( staticUpdateOperations );
		this.staticStatementShapeKeys = staticJdbcUpdateOperations == null
				? null
				: generateStaticStatementShapeKeys( staticJdbcUpdateOperations );

		var versionUpdate = entityPersister.getVersionMapping() != null
				? new VersionUpdateBuilder( entityPersister ).buildMutation()
				: null;
		this.versionJdbcUpdate = versionUpdate == null
				? null
				: versionUpdate.createMutationOperation( null, sessionFactory );

		this.origin = "EntityUpdateAction(" + entityPersister.getEntityName() + ")";
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
		if ( TenantIdHelper.needsMultiTableUpdateCheck( entityPersister, session ) ) {
			final List<FlushOperation> operations = new ArrayList<>();
			decomposeUpdate( action, ordinalBase, session, decompositionContext, operations::add );
			applyTenantOwnershipCheck( action, operations );
			operations.forEach( operationConsumer );
		}
		else {
			decomposeUpdate( action, ordinalBase, session, decompositionContext, operationConsumer );
		}
	}

	private void applyTenantOwnershipCheck(EntityUpdateAction action, List<FlushOperation> operations) {
		final String tenantTable = entityPersister.physicalTableNameForMutation(
				TenantIdHelper.tenantIdAttribute( entityPersister ).getSelectable( 0 ) );
		if ( operations.stream()
				.anyMatch( operation -> operation.getKind() != MutationKind.NO_OP
										&& !tenantTable.equals( operation.getTableExpression() ) ) ) {
			for ( var flushOperation : operations ) {
				if ( flushOperation.getKind() == MutationKind.UPDATE
						&& TenantIdHelper.checksTenantId( entityPersister, flushOperation.getJdbcOperation() ) ) {
					for ( var operation : operations ) {
						if ( operation.getKind() != MutationKind.NO_OP && operation != flushOperation ) {
							operation.setExecutionPrerequisite( flushOperation );
						}
					}
					return;
				}
			}
			final var ownershipCheck = new PreExecutionCallback() {
				private boolean checked;

				@Override
				public boolean requiresBatchFlush() {
					return !checked;
				}

				@Override
				public boolean beforeExecution(SessionImplementor session) {
					if ( !checked ) {
						TenantIdHelper.checkStoredTenantOwnership( action.getId(), entityPersister, session, THROW );
						checked = true;
					}
					return true;
				}
			};
			for ( var operation : operations ) {
				if ( operation.getKind() != MutationKind.NO_OP ) {
					final var previous = operation.getPreExecutionCallback();
					operation.setPreExecutionCallback(
							previous == null ? ownershipCheck : previous.and( ownershipCheck ) );
				}
			}
		}
	}

	private void decomposeUpdate(
			EntityUpdateAction action,
			int ordinalBase,
			SharedSessionContractImplementor session,
			DecompositionContext decompositionContext,
			Consumer<FlushOperation> operationConsumer) {
		final boolean vetoed = preUpdate( action, session );
		if ( vetoed ) {
			return;
		}
		final Object entity = action.getInstance();
		Object identifier = action.getId();
		if ( identifier == null && decompositionContext != null ) {
			identifier = decompositionContext.getGeneratedIdentifierHandle( entity );
		}
		final Object rowId = action.getRowId();
		Object[] state = action.getState();
		Object[] previousState = action.getPreviousState();
		final Object previousVersion = action.getPreviousVersion();

		// Capture the EntityEntry now, at decomposition time, because for entities being deleted
		// in the same flush, the DELETE operation may remove the entity from the persistence context
		// before the UPDATE's post-execution callback runs.
		final var entityEntry = session.getPersistenceContextInternal().getEntry( entity );
		final var filteredAssociations = FilteredAssociationMutation.forUpdate( entityEntry, state );
		previousState = filteredAssociations.physicalState( previousState );

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
			// Has dirty fields - must be a NULL-before-DELETE update, allow it to proceed.
			// But if the entity is also being inserted in this same flush, the row was never
			// written to the database (InsertDecomposer skips inserts for entities being deleted),
			// so there is nothing to update.
			if ( decompositionContext != null
					&& decompositionContext.isBeingInsertedInCurrentFlush( entity ) ) {
				return;
			}
		}

		action.handleNaturalIdLocalResolutions( identifier, entityPersister, session.getPersistenceContext() );
		final UpdateCacheHandling.CacheUpdate cacheUpdate = UpdateCacheHandling.lockItem( action, previousVersion, session );
		registerAfterTransactionCompletion( action, cacheUpdate, session );

		if ( hasOnlyInversePluralDirtiness( action ) ) {
			operationConsumer.accept( DecompositionSupport.createNoOpCallbackCarrier(
					entityPersister.getIdentifierTableDescriptor(),
					ordinalBase * 1_000,
					new PostUpdateHandling( action, cacheUpdate, previousVersion, null, entityEntry, true )
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
		state = filteredAssociations.physicalState( state );

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
				this,
				filteredAssociations
		);

		// Choose between static or dynamic update group.  Use dynamic update if:
		// 		1. Entity specifies dynamic-update
		// 		2. We need optimistic locking with DIRTY check
		//		3. The entity has any uninitialized state
		final boolean needsDynamicUpdate = filteredAssociations.isActive()
				|| entityPersister.isDynamicUpdate()
				|| rowId != null
				|| effectiveOptLockStyle.isAllOrDirty()
				|| preUpdateGeneratedAttributeIndexes.length > 0
				|| entityPersister.hasUninitializedLazyProperties( entity );

		CachedUpdates cachedUpdates = null;
		if ( filteredAssociations.isActive() && FilteredUpdateCache.supports( entityPersister )
				&& rowId == null && !entityPersister.hasUninitializedLazyProperties( entity )
				&& entityEntry != null && entityEntry.isModifiableEntity()
				&& ( !entityPersister.isVersioned() || previousVersion != null )
				&& getClass() == UpdateDecomposer.class ) {
			final var key = filteredUpdateKey( filteredAssociations, updateability, valuesAnalysis,
					state, previousVersion, session );
			final Object[] currentState = state;
			final Object[] oldState = previousState;
			final Object currentIdentifier = identifier;
			cachedUpdates = filteredUpdateCache.resolve( key, sessionFactory, () -> {
				final var updates = generateDynamicUpdateOperations( entity, currentIdentifier, null,
						currentState, oldState, previousVersion, updateability, valuesAnalysis, session );
				final var jdbcOperations = generateStaticJdbcOperations( updates );
				return new CachedUpdates( updates, jdbcOperations, generateStaticStatementShapeKeys( jdbcOperations ) );
			} );
		}

		final var effectiveGroup = cachedUpdates != null ? cachedUpdates.updates()
				: needsDynamicUpdate
						? generateDynamicUpdateOperations( entity, identifier, rowId, state, previousState,
								previousVersion, updateability, valuesAnalysis, session )
						: staticUpdateOperations;

		int localOrd = 0;
		FlushOperation previousOperation = null;

		// determine whether the entity we are about to update is being deleted in the same flush
		final boolean isBeingDeleted = decompositionContext != null
				&& decompositionContext.isBeingDeletedInCurrentFlush( entity );

		for ( var tableDescriptor : entityPersister.getTableDescriptors() ) {
			if ( tableDescriptor.isInverse() ) {
				continue;
			}
			final var tableUpdate = effectiveGroup.get( tableDescriptor.name() );
			if ( tableUpdate == null ) {
				continue;
			}
			var operation = cachedUpdates == null ? resolveJdbcUpdateOperation( tableDescriptor.name(), tableUpdate )
					: cachedUpdates.jdbcOperations().get( tableDescriptor.name() );
			var shapeKey = cachedUpdates == null ? resolveStatementShapeKey( tableDescriptor.name(), tableUpdate )
					: cachedUpdates.shapeKeys().get( tableDescriptor.name() );

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
			// DELETE -> DELETE edges ensure proper FK ordering, so UPDATEs are unnecessary.
			//
			// Technically we could also skip updates to the primary table if no foreign keys are being nullified.
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
					origin,
					shapeKey
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

	private FilteredUpdateCache.Key filteredUpdateKey(
			FilteredAssociationMutation filteredAssociations, boolean[] updateability,
			UpdateValuesAnalysis valuesAnalysis, Object[] state, Object previousVersion,
			SharedSessionContractImplementor session) {
		final var assignments = new BitSet();
		for ( int i = 0; i < updateability.length; i++ ) {
			if ( shouldIncludeInDynamicUpdate( entityPersister.getAttributeMapping( i ), updateability, valuesAnalysis ) ) {
				assignments.set( i );
			}
		}
		final var tables = new BitSet();
		final boolean versionChanged = isVersionChanged( state, previousVersion, session );
		for ( var table : entityPersister.getTableDescriptors() ) {
			if ( needsDynamicTableUpdate( table, valuesAnalysis, versionChanged ) ) {
				tables.set( table.getRelativePosition() );
			}
		}
		return filteredAssociations.updateCacheKey( assignments, tables );
	}

	private static Object getVersion(Object[] state, EntityVersionMapping versionMapping) {
		return state[versionMapping.getVersionAttribute().getStateArrayPosition()];
	}

	private boolean isVersionChanged(Object[] state, Object previousVersion, SharedSessionContractImplementor session) {
		final var versionMapping = entityPersister.getVersionMapping();
		if ( versionMapping == null ) {
			return false;
		}
		else {
			final Object version = getVersion( state, versionMapping );
			return !versionMapping.areEqual( version, previousVersion, session );
		}
	}

	private boolean needsDynamicTableUpdate(
			EntityTableDescriptor table, UpdateValuesAnalysis valuesAnalysis, boolean versionChanged) {
		return !table.isInverse()
			&& ( valuesAnalysis.needsUpdate( table ) || versionChanged && isVersionMappingTable( table ) );
	}

	private boolean isVersionMappingTable(EntityTableDescriptor table) {
		return table.name().equals( entityPersister.getVersionMapping().getContainingTableExpression() );
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
		if ( dirtyFields == null
				|| ( action.getNextVersion() != null && !entityPersister.isVersionPropertyGenerated() ) ) {
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
		final var completionHandling =
				new UpdateAfterTransactionCompletionHandling( action, cacheUpdate );
		if ( completionHandling.isNeeded( session ) ) {
			session.getTransactionCompletionCallbacks()
					.registerCallback( completionHandling );
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

		final var identifierTableDescriptor = entityPersister.getIdentifierTableDescriptor();
		var bindPlan = new ForceVersionBindPlan(
				identifierTableDescriptor,
				entityPersister,
				entity,
				identifier,
				previousVersion,
				newVersion
		);

		return new FlushOperation(
				identifierTableDescriptor,
				MutationKind.UPDATE,
				versionJdbcUpdate,
				bindPlan,
				ordinalBase * 1_000,
				"EntityUpdateAction(" + entityPersister.getEntityName() + ")"
		);
	}

	protected boolean preUpdate(EntityUpdateAction action, SharedSessionContractImplementor session) {
		final var listenerGroup =
				session.getFactory()
						.getEventListenerGroups().eventListenerGroup_PRE_UPDATE;
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

	private Map<String, MutationOperation> generateStaticJdbcOperations(
			Map<String, LogicalTableUpdate<?>> staticOperations) {
		final Map<String, MutationOperation> jdbcOperations = new HashMap<>();
		staticOperations.forEach( (name, operation) -> {
			jdbcOperations.put( name, operation.createMutationOperation( null, sessionFactory ) );
		} );
		return Collections.unmodifiableMap( jdbcOperations );
	}

	private Map<String, StatementShapeKey> generateStaticStatementShapeKeys(
			Map<String, MutationOperation> staticJdbcOperations) {
		final Map<String, StatementShapeKey> shapeKeys = new HashMap<>();
		staticJdbcOperations.forEach( (name, operation) -> {
			shapeKeys.put( name, StatementShapeKey.forMutation(
					name,
					MutationKind.UPDATE,
					findTableDescriptor( name ),
					operation
			) );
		} );
		return Collections.unmodifiableMap( shapeKeys );
	}

	private MutationOperation resolveJdbcUpdateOperation(
			String tableName,
			LogicalTableUpdate<?> tableUpdate) {
		if ( staticJdbcUpdateOperations != null && tableUpdate == staticUpdateOperations.get( tableName ) ) {
			return staticJdbcUpdateOperations.get( tableName );
		}
		return tableUpdate.createMutationOperation( null, sessionFactory );
	}

	private StatementShapeKey resolveStatementShapeKey(
			String tableName,
			LogicalTableUpdate<?> tableUpdate) {
		if ( staticStatementShapeKeys != null && tableUpdate == staticUpdateOperations.get( tableName ) ) {
			return staticStatementShapeKeys.get( tableName );
		}
		return null;
	}

	private TableUpdateBuilder<?> createTableUpdateBuilder(TableDescriptor tableDescriptor) {
		if ( tableDescriptor instanceof EntityTableDescriptor entityTableDescriptor
				&& entityTableDescriptor.isIdentifierTable() ) {
			final var delegate = entityPersister.getUpdateDelegate();
			if ( delegate != null ) {
				return (TableUpdateBuilder<?>) delegate.createTableMutationBuilder( null, sessionFactory );
			}
		}

		return new TableUpdateBuilderStandard<>(
				entityPersister,
				new MutatingTableReference( getTableMappingAdapter( tableDescriptor ) ),
				sessionFactory
		);
	}

	private TableDescriptorAsTableMapping getTableMappingAdapter(TableDescriptor tableDescriptor) {
		return tableMappingAdapters.computeIfAbsent( tableDescriptor, this::createTableMappingAdapter );
	}

	@Override
	public TableDescriptorAsTableMapping apply(EntityTableDescriptor tableDescriptor) {
		return getTableMappingAdapter( tableDescriptor );
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
		final var versionMapping = entityPersister.getVersionMapping();
		final boolean versionChanged = versionMapping != null
				&& !versionMapping.areEqual( getVersion( state, versionMapping ), version, session );

		// Process tables in forward order
		entityPersister.forEachMutableTableDescriptor( (tableDescriptor) -> {
			if (  tableDescriptor.isInverse() ) {
				// skip inverse tables
				return;
			}
			// A change confined to a secondary table may still increment the owner's version.
			if ( needsDynamicTableUpdate( tableDescriptor, valuesAnalysis, versionChanged ) ) {
				final var builder = createTableUpdateBuilder( tableDescriptor );
				valuesAnalysis.prepareUpdateBuilder( builder, tableDescriptor );
				operationBuilders.put( tableDescriptor.name(), builder );
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

		entityPersister.forEachMutableTableDescriptor( (tableDescriptor) -> {
			final var builder = builders.get( tableDescriptor.name() );

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
			applyKeyRestriction( tableDescriptor, builder );

			// Apply optimistic locking
			applyOptimisticLocking( tableDescriptor, builder, null, null, null );
		} );

		// Apply partitioned selection restrictions if needed
		if ( entityPersister.hasPartitionedSelectionMapping() ) {
			applyPartitionedSelectionRestrictions( builders );
		}
		for ( var builder : builders.values() ) {
			TenantIdHelper.applyTenantRestriction( entityPersister, builder );
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
		entityPersister.forEachMutableTableDescriptor( (tableDescriptor) -> {
			final var builder = builders.get( tableDescriptor.name() );
			if ( builder == null ) {
				return;
			}

			// Custom SQL has a fixed parameter layout even when generated values or hidden
			// associations require dynamic decomposition. Bind all its updateable attributes.
			for ( int i = 0; i < tableDescriptor.attributes().size(); i++ ) {
				var attribute = tableDescriptor.attributes().get( i );
				if ( tableDescriptor.updateDetails().getCustomSql() != null
						? updateable[attribute.getStateArrayPosition()]
						: shouldIncludeInDynamicUpdate( attribute, updateable, valuesAnalysis ) ) {
					if ( state[attribute.getStateArrayPosition()] == LazyPropertyInitializer.UNFETCHED_PROPERTY ) {
						// it was not fetched and so could not have changed, skip it
						continue;
					}
					applyValueAssignment( entity, entityPersister.getVersionMapping(), attribute, builder, session );
				}
			}

			// Apply WHERE clause - key restrictions
			applyKeyRestriction( tableDescriptor, builder, rowId );

			// Apply optimistic locking
			applyOptimisticLocking( tableDescriptor, builder, previousState, valuesAnalysis, session );
		} );

		// Apply partitioned selection restrictions if needed
		if ( entityPersister.hasPartitionedSelectionMapping() ) {
			applyPartitionedSelectionRestrictions( builders );
		}
		for ( var builder : builders.values() ) {
			TenantIdHelper.applyTenantRestriction( entityPersister, builder );
		}
	}

	private boolean shouldIncludeInDynamicUpdate(
			AttributeMapping attribute,
			boolean[] updateable,
			UpdateValuesAnalysis valuesAnalysis) {
		final var versionMapping = entityPersister.getVersionMapping();
		if ( versionMapping != null
				&& versionMapping.getVersionAttribute() == attribute ) {
			return true;
		}
		// First check if the attribute is updateable or has update-generated values
		else if ( !updateable[attribute.getStateArrayPosition()]
				&& !isValueGenerationOnUpdateInSql( attribute.getGenerator() ) ) {
			return false;
		}
		// If we have dirty fields, only include dirty ones
		else if ( valuesAnalysis.hasDirtyAttributes() ) {
			return valuesAnalysis.getDirtiness()[attribute.getStateArrayPosition()];
		}
		else {
			return true;
		}
	}

	private void applyKeyRestriction(EntityTableDescriptor tableDescriptor, TableUpdateBuilder<?> tableUpdateBuilder) {
		applyKeyRestriction( tableDescriptor, tableUpdateBuilder, null );
	}

	private void applyKeyRestriction(EntityTableDescriptor tableDescriptor, TableUpdateBuilder<?> tableUpdateBuilder, Object rowId) {
		if ( rowId != null
				&& tableDescriptor.isIdentifierTable()
				&& entityPersister.getRowIdMapping() != null ) {
			tableUpdateBuilder.addKeyRestrictionLeniently( entityPersister.getRowIdMapping() );
		}
		else {
			tableUpdateBuilder.addKeyRestrictions( tableDescriptor.keyDescriptor() );
		}
	}

	private void applyOptimisticLocking(
			EntityTableDescriptor tableDescriptor,
			TableUpdateBuilder<?> tableUpdateBuilder,
			Object[] previousState,
			UpdateValuesAnalysis valuesAnalysis,
			SharedSessionContractImplementor session) {
		final var optimisticLockStyle = entityPersister.optimisticLockStyle();

		if ( optimisticLockStyle.isVersion() && entityPersister.getVersionMapping() != null ) {
			applyVersionBasedOptLocking( tableDescriptor, tableUpdateBuilder );
		}
		else if ( optimisticLockStyle.isAllOrDirty() ) {
			applyNonVersionOptLocking( tableDescriptor, tableUpdateBuilder, previousState, optimisticLockStyle, valuesAnalysis, session );
		}
	}

	private void applyVersionBasedOptLocking(EntityTableDescriptor tableDescriptor, TableUpdateBuilder<?> tableUpdateBuilder) {
		final var versionMapping = entityPersister.getVersionMapping();
		if ( versionMapping != null
				&& tableDescriptor.name().equals(
					versionMapping.getContainingTableExpression() ) ) {
			tableUpdateBuilder.addOptimisticLockRestriction( versionMapping );
		}
	}

	private void applyNonVersionOptLocking(
			EntityTableDescriptor tableDescriptor,
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
				needsDynamicUpdate
						&& tableDescriptor.updateDetails().getCustomSql() == null,
				generatedValuesCollector
		);
	}
}
