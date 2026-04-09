/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import org.hibernate.action.internal.CollectionRecreateAction;
import org.hibernate.action.internal.CollectionRemoveAction;
import org.hibernate.action.internal.CollectionUpdateAction;
import org.hibernate.action.queue.MutationKind;
import org.hibernate.action.queue.bind.BindPlan;
import org.hibernate.action.queue.bind.JdbcValueBindings;
import org.hibernate.action.queue.graph.DecompositionContext;
import org.hibernate.action.queue.meta.TableDescriptor;
import org.hibernate.action.queue.meta.TableDescriptorAsTableMapping;
import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilderStandard;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.internal.EntityCollectionPart;
import org.hibernate.persister.collection.OneToManyPersister;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.hibernate.action.queue.CollectionOrdinalSupport.Slot;
import static org.hibernate.action.queue.CollectionOrdinalSupport.calculateOrdinal;
import static org.hibernate.sql.model.ModelMutationLogging.MODEL_MUTATION_LOGGER;
import static org.hibernate.sql.model.ast.builder.TableMutationBuilder.NULL;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractOneToManyDecomposer implements OneToManyDecomposer {
	protected final OneToManyPersister persister;
	protected final SessionFactoryImplementor factory;

	public AbstractOneToManyDecomposer(
			OneToManyPersister persister,
			SessionFactoryImplementor factory) {
		this.persister = persister;
		this.factory = factory;
	}


	protected abstract CollectionJdbcOperations selectJdbcOperations(Object entry, SharedSessionContractImplementor session);

	@Override
	public List<PlannedOperation> decomposeRecreate(
			CollectionRecreateAction action,
			int ordinalBase,
			SharedSessionContractImplementor session, DecompositionContext decompositionContext) {
		var collection = action.getCollection();
		var key = action.getKey();

		var attribute = persister.getAttributeMapping();

		// Always fire PRE event, even if no SQL operations will be needed
		DecompositionSupport.firePreRecreate( persister, collection, session );
		collection.preInsert( persister );

		// Create post-execution callback to handle post-execution work (afterAction, cache, events, stats)
		var postRecreateHandling = new PostCollectionRecreateHandling(
				persister,
				collection,
				action.getAffectedOwner(),
				action.getAffectedOwnerId(),
				DecompositionSupport.generateCacheKey( action, session )
		);

		final var entries = collection.entries( persister );
		if ( !entries.hasNext() ) {
			// No entries - create no-op to defer POST callback
			return List.of( DecompositionSupport.createNoOpCallbackCarrier(
					persister.getCollectionTableDescriptor(),
					calculateOrdinal( ordinalBase, Slot.INSERT ),
					postRecreateHandling
			) );
		}

		final List<PlannedOperation> operations = new ArrayList<>();

		var insertOrdinal = calculateOrdinal( ordinalBase, Slot.INSERT );
		var writeIndexOrdinal = calculateOrdinal( ordinalBase, Slot.WRITEINDEX );

		int entryCount = 0;
		while ( entries.hasNext() ) {
			final Object entry = entries.next();
			var jdbcOperations = selectJdbcOperations( entry, session );
			var insertRowPlan = jdbcOperations.insertRowPlan();

			// For inverse one-to-many collections, insertRowPlan will be null (inserts are managed by the owning side)
			if ( insertRowPlan != null && collection.includeInRecreate( entry, entryCount, collection, attribute ) ) {
				final BindPlan bindPlan = new SingleRowInsertBindPlan(
						persister,
						insertRowPlan.values(),
						collection,
						key,
						entry,
						entryCount
				);

				// For one-to-many collections, the "insert" is actually an UPDATE that sets the FK
				// Use MutationKind.UPDATE so it's ordered AFTER entity INSERTs via FK edges
				final PlannedOperation plannedOp = new PlannedOperation(
						persister.getCollectionTableDescriptor(),
						MutationKind.UPDATE,
						insertRowPlan.jdbcOperation(),
						bindPlan,
						insertOrdinal,
						"InsertRow[" + entryCount + "](" + persister.getRolePath() + ")"
				);

				operations.add( plannedOp );
			}

			if ( jdbcOperations.updateIndexPlan() != null ) {
				var writeIndexBindPlan = new SingleRowUpdateBindPlan(
						collection,
						key,
						entry,
						entryCount,
						jdbcOperations.updateIndexPlan().values(),
						jdbcOperations.updateIndexPlan().restrictions()
				);


				var  writeIndexPlannedOp = new PlannedOperation(
						persister.getCollectionTableDescriptor(),
						MutationKind.UPDATE_ORDER,
						jdbcOperations.updateIndexPlan().jdbcOperation(),
						writeIndexBindPlan,
						writeIndexOrdinal,
						"WriteIndex[" + entryCount + "](" + persister.getRolePath() + ")"
				);

				//postExecCallbackRegistry.accept(... );
				// on recreate we should be able to write the index as a normal planned-op
				operations.add( writeIndexPlannedOp );
			}

			entryCount++;
		}

		if ( !operations.isEmpty() ) {
			// Attach it to the last operation
			operations.get( operations.size() - 1 ).setPostExecutionCallback( postRecreateHandling );
			return operations;
		}
		else {
			// Operations unexpectedly empty - create no-op to defer POST callback
			return List.of( DecompositionSupport.createNoOpCallbackCarrier(
					persister.getCollectionTableDescriptor(),
					calculateOrdinal( ordinalBase, Slot.INSERT ),
					postRecreateHandling
			) );
		}
	}

	@Override
	public final List<PlannedOperation> decomposeUpdate(
			CollectionUpdateAction action,
			int ordinalBase,
			SharedSessionContractImplementor session, DecompositionContext decompositionContext) {
		final var collection = action.getCollection();
		final var key = action.getKey();

		// Always fire PRE event, even if collection is not initialized
		DecompositionSupport.firePreUpdate( persister, collection, session );

		// Create callback to handle post-execution work (afterAction, cache, events, stats)
		var postUpdateHandling = new PostCollectionUpdateHandling(
				persister,
				collection,
				key,
				action.getAffectedOwner(),
				action.getAffectedOwnerId(),
				DecompositionSupport.generateCacheKey( action, session )
		);

		final List<PlannedOperation> operations = new ArrayList<>();

		if ( !collection.wasInitialized() ) {
			// If the collection wasn't initialized, we cannot access entries/deletes
			// The collection should still be marked dirty for queued operations
			// We only need to notify the cache via the post-execution callback
		}
		else {
			applyUpdateRemovals( collection, key, ordinalBase, session, operations::add );
			applyUpdateChanges( collection, key, ordinalBase + 1, session, operations::add );
			applyUpdateAdditions( collection, key, ordinalBase + 2, session, operations::add );
		}

		if ( !operations.isEmpty() ) {
			// Attach post-execution callback to the last operation
			operations.get( operations.size() - 1 ).setPostExecutionCallback( postUpdateHandling );
			return operations;
		}
		else {
			// Operations empty (e.g., uninitialized dirty collection) - create no-op to defer POST callback
			return List.of( DecompositionSupport.createNoOpCallbackCarrier(
					persister.getCollectionTableDescriptor(),
					calculateOrdinal( ordinalBase, Slot.UPDATE ),
					postUpdateHandling
			) );
		}
	}

	protected void applyUpdateRemovals(
			PersistentCollection<?> collection,
			Object key,
			int ordinalBase,
			SharedSessionContractImplementor session,
			Consumer<PlannedOperation> operationConsumer) {
		if ( !persister.needsRemove() ) {
			// EARLY EXIT!!
			return;
		}

		final var deletes = collection.getDeletes( persister, true );
		if ( !deletes.hasNext() ) {
			MODEL_MUTATION_LOGGER.noRowsToDelete();
			// EARLY EXIT!!
			return;
		}

		var deleteOrdinal = calculateOrdinal( ordinalBase, Slot.DELETE );

		int deletionCount = 0;
		while ( deletes.hasNext() ) {
			var removal = deletes.next();

			var jdbcOperations = selectJdbcOperations( removal, session );
			assert jdbcOperations != null;
			var deleteRowPlan = jdbcOperations.deleteRowPlan();

			final BindPlan bindPlan = new SingleRowDeleteBindPlan(
					collection,
					key,
					removal,
					deleteRowPlan.restrictions()
			);

			final PlannedOperation plannedOp = new PlannedOperation(
					persister.getCollectionTableDescriptor(),
					// technically an UPDATE
					MutationKind.UPDATE,
					deleteRowPlan.jdbcOperation(),
					bindPlan,
					deleteOrdinal,
					"DeleteRow[" + deletionCount + "](" + persister.getRole() + ")"
			);

			operationConsumer.accept( plannedOp );
			deletionCount++;
		}
	}


	protected List<PlannedOperation> decomposeRemove(
			CollectionJdbcOperations jdbcOperations,
			CollectionRemoveAction action,
			int ordinalBase,
			SharedSessionContractImplementor session) {
		// Always fire PRE event, even if no SQL operations will be needed
		DecompositionSupport.firePreRemove( persister, action.getCollection(), action.getAffectedOwner(), session );

		// Create callback to handle post-execution work (afterAction, cache, events, stats)
		var postRemoveHandling = new PostCollectionRemoveHandling(
				persister,
				action.getCollection(),
				action.getAffectedOwner(),
				action.getAffectedOwnerId(),
				DecompositionSupport.generateCacheKey( action, session )
		);

		if ( !persister.needsRemove() ) {
			return List.of( DecompositionSupport.createNoOpCallbackCarrier(
					persister.getCollectionTableDescriptor(),
					calculateOrdinal( ordinalBase, Slot.DELETE ),
					postRemoveHandling
			) );
		}

		// Create callback to handle post-execution work (afterAction, cache, events, stats)
		var removeOperation = jdbcOperations.removeOperation();
		var operation = new PlannedOperation(
				persister.getCollectionTableDescriptor(),
				// technically an UPDATE
				MutationKind.UPDATE,
				removeOperation,
				new RemoveBindPlan( action.getKey(), persister ),
				calculateOrdinal( ordinalBase, Slot.DELETE ),
				"RemoveAllRows(" + persister.getRolePath() + ")"
		);

		// Attach post-execution callback to the operation
		operation.setPostExecutionCallback( postRemoveHandling );

		return List.of( operation );
	}

	protected void applyUpdateChanges(
			PersistentCollection<?> collection,
			Object key,
			int ordinalBase,
			SharedSessionContractImplementor session,
			Consumer<PlannedOperation> operationConsumer) {
		if ( !persister.isDoWriteEvenWhenInverse() ) {
			// EARLY EXIT!!
			return;
		}

		final var entries = collection.entries( persister);

		var updateOrdinal = calculateOrdinal( ordinalBase, Slot.UPDATE );

		int entryCount = 0;
		while ( entries.hasNext() ) {
			final Object entry = entries.next();

			var jdbcOperations = selectJdbcOperations( entry, session );
			assert jdbcOperations != null;
			var updateIndexPlan = jdbcOperations.updateIndexPlan();

			// For inverse collections, updateIndexPlan will be null
			if ( updateIndexPlan != null && collection.needsUpdating( entry, entryCount, persister.getAttributeMapping() ) ) {
				final BindPlan bindPlan = new SingleRowUpdateBindPlan(
						collection,
						key,
						entry,
						entryCount,
						updateIndexPlan.values(),
						updateIndexPlan.restrictions()
				);

				final PlannedOperation plannedOp = new PlannedOperation(
						persister.getCollectionTableDescriptor(),
						MutationKind.UPDATE_ORDER,
						updateIndexPlan.jdbcOperation(),
						bindPlan,
						updateOrdinal,
						"UpdateRow[" + entryCount + "](" + persister.getRolePath() + ")"
				);

				operationConsumer.accept( plannedOp );
			}

			entryCount++;
		}
	}

	protected void applyUpdateAdditions(
			PersistentCollection<?> collection,
			Object key,
			int ordinalBase,
			SharedSessionContractImplementor session,
			Consumer<PlannedOperation> operationConsumer) {
		// todo (ActionQueue2) : where should this come from?
		var allowInserts = true;
		if ( !allowInserts ) {
			// EARLY EXIT!!
			return;
		}

		final var entries = collection.entries( persister );
		if ( !entries.hasNext() ) {
			MODEL_MUTATION_LOGGER.noCollectionRowsToInsert( persister.getRolePath(), key );
			return;
		}

		collection.preInsert( persister );

		var insertOrdinal = calculateOrdinal( ordinalBase, Slot.INSERT );

		int entryCount = 0;
		while ( entries.hasNext() ) {
			final Object entry = entries.next();
			if ( collection.includeInInsert( entry, entryCount, collection, persister.getAttributeMapping() ) ) {

				var jdbcOperations = selectJdbcOperations( entry, session );
				assert jdbcOperations != null;
				var insertRowPlan = jdbcOperations.insertRowPlan();

				// For inverse one-to-many collections, insertRowPlan will be null
				// (inserts are managed by the owning side)
				if ( insertRowPlan != null ) {
					final BindPlan bindPlan = new SingleRowInsertBindPlan(
							persister,
							insertRowPlan.values(),
							collection,
							key,
							entry,
							entryCount
					);

					// For one-to-many collections, the "insert" is actually an UPDATE that sets the FK
					// Use MutationKind.UPDATE so it's ordered AFTER entity INSERTs via FK edges
					final PlannedOperation plannedOp = new PlannedOperation(
							persister.getCollectionTableDescriptor(),
							// technically an UPDATE
							MutationKind.UPDATE,
							insertRowPlan.jdbcOperation(),
							bindPlan,
							insertOrdinal,
							"InsertRow[" + entryCount + "](" + persister.getRolePath() + ")"
					);

					operationConsumer.accept( plannedOp );
				}
			}

			entryCount++;
		}

		MODEL_MUTATION_LOGGER.doneInsertingCollectionRows( entryCount, persister.getRolePath() );
	}


	protected static class RemoveBindPlan implements BindPlan {
		private final Object key;
		private final OneToManyPersister mutationTarget;

		public RemoveBindPlan(Object key, OneToManyPersister mutationTarget) {
			this.key = key;
			this.mutationTarget = mutationTarget;
		}

		@Override
		public void execute(
				org.hibernate.action.queue.exec.ExecutionContext context,
				PlannedOperation plannedOperation,
				SharedSessionContractImplementor session) {
			context.executeRow(
					plannedOperation,
					(valueBindings, s) -> {
						var fkDescriptor = mutationTarget.getAttributeMapping().getKeyDescriptor();
						fkDescriptor.getKeyPart().decompose(
								key,
								(valueIndex, value, jdbcValueMapping) -> {
									valueBindings.bindValue(
											value,
											jdbcValueMapping.getSelectionExpression(),
											ParameterUsage.RESTRICT
									);
								},
								session
						);
					},
					null
			);
		}
	}


	protected CollectionJdbcOperations buildJdbcOperations(
			TableDescriptor tableDescriptor,
			SessionFactoryImplementor factory) {
		final CollectionJdbcOperations.InsertRowPlan insertRowPlan = buildInsertRowPlan( tableDescriptor );
		final CollectionJdbcOperations.UpdateRowPlan updateIndexPlan = buildUpdateIndexPlan( tableDescriptor );
		final CollectionJdbcOperations.DeleteRowPlan deleteRowPlan = buildDeleteRowPlan( tableDescriptor );

		return new CollectionJdbcOperations(
				persister,
				insertRowPlan,
				null, // one-to-many doesn't update element values, only FK/ORDER
				updateIndexPlan,  // ORDER column updates for inverse @OrderColumn collections
				deleteRowPlan,
				buildRemoveOperation( tableDescriptor )
		);
	}

	/// Generates operation to perform "insert" SQL in the form -
	///
	/// ```sql
	/// update orders set customer_fk = ?, order_number = ? where id = ?
	/// ```
	///
	/// Which, in the case of a unidirectional one-to-many, actually "adds the collection row"
	private CollectionJdbcOperations.InsertRowPlan buildInsertRowPlan(TableDescriptor tableDescriptor) {
		if ( persister.isInverse() || !persister.isRowInsertEnabled() ) {
			return null;
		}

		final TableDescriptorAsTableMapping tableMapping = new TableDescriptorAsTableMapping(
				tableDescriptor,
				0, // relativePosition
				false, // isIdentifierTable
				false // isInverse
		);
		var updateBuilder = new TableUpdateBuilderStandard<>(
				persister,
				new MutatingTableReference(tableMapping),
				factory,
				persister.getSqlWhereString()
		);

		final var attributeMapping = persister.getAttributeMapping();
		attributeMapping.getKeyDescriptor().getKeyPart().forEachUpdatable( (index, selectableMapping) -> {
			updateBuilder.addColumnAssignment( selectableMapping );
		} );

		final var indexDescriptor = attributeMapping.getIndexDescriptor();
		if ( indexDescriptor != null ) {
			indexDescriptor.forEachUpdatable( (index, selectableMapping) -> {
				updateBuilder.addColumnAssignment( selectableMapping );
			} );
		}

		final var elementDescriptor = (EntityCollectionPart) attributeMapping.getElementDescriptor();
		final var elementType = elementDescriptor.getAssociatedEntityMappingType();
		elementType.getIdentifierMapping().forEachColumn( (index, selectableMapping) -> {
			updateBuilder.addKeyRestriction( selectableMapping );
		} );


		return new CollectionJdbcOperations.InsertRowPlan(
				updateBuilder.buildMutation().createMutationOperation(null, factory),
				this::applyInsertRowValues
		);
	}

	private void applyInsertRowValues(
			PersistentCollection<?> collection,
			Object keyValue,
			Object rowValue,
			int rowPosition,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings) {
		final var attributeMapping = persister.getAttributeMapping();
		attributeMapping.getKeyDescriptor().getKeyPart().decompose(
				keyValue,
				jdbcValueBindings::bindAssignment,
				session
		);
		final var indexDescriptor = attributeMapping.getIndexDescriptor();
		if ( indexDescriptor != null ) {
			indexDescriptor.decompose(
					persister.incrementIndexByBase( collection.getIndex( rowValue, rowPosition, persister ) ),
					jdbcValueBindings::bindAssignment,
					session
			);
		}

		final Object elementValue = collection.getElement( rowValue );
		final var elementDescriptor = (EntityCollectionPart) attributeMapping.getElementDescriptor();
		final var identifierMapping = elementDescriptor.getAssociatedEntityMappingType().getIdentifierMapping();
		identifierMapping.decompose(
				identifierMapping.getIdentifier( elementValue ),
				jdbcValueBindings::bindRestriction,
				session
		);
	}

	private CollectionJdbcOperations.UpdateRowPlan buildUpdateIndexPlan(TableDescriptor tableDescriptor) {
		if ( !persister.isDoWriteEvenWhenInverse() ) {
			return null;
		}

		final TableDescriptorAsTableMapping tableMapping = new TableDescriptorAsTableMapping(
				tableDescriptor,
				0, // relativePosition
				false, // isIdentifierTable
				false // isInverse
		);
		final var updateBuilder = new TableUpdateBuilderStandard<>(
				persister,
				new MutatingTableReference(tableMapping),
				factory,
				persister.getSqlWhereString()
		);

		final var attributeMapping = persister.getAttributeMapping();

		attributeMapping.getIndexDescriptor().forEachUpdatable( (index, selectableMapping) -> {
			updateBuilder.addColumnAssignment( selectableMapping );
		} );

		// Add element identifier restrictions (WHERE clause for element's PK)
		final var elementDescriptor = (EntityCollectionPart) attributeMapping.getElementDescriptor();
		final var elementType = elementDescriptor.getAssociatedEntityMappingType();
		elementType.getIdentifierMapping().forEachColumn( (index, selectableMapping) -> {
			updateBuilder.addKeyRestriction( selectableMapping );
		} );

		return new CollectionJdbcOperations.UpdateRowPlan(
				updateBuilder.buildMutation().createMutationOperation(null, factory),
				this::applyWriteIndexValues,
				this::applyWriteIndexRestrictions
		);
	}

	private void applyWriteIndexValues(
			PersistentCollection<?> collection,
			Object key,
			Object entry,
			int entryPosition,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings) {
		persister.getAttributeMapping().getIndexDescriptor().decompose(
				persister.incrementIndexByBase( collection.getIndex( entry, entryPosition, persister ) ),
				(valueIndex, jdbcValue, jdbcValueMapping) -> {
					if ( jdbcValueMapping.isUpdateable() ) {
						jdbcValueBindings.bindValue(
								jdbcValue,
								jdbcValueMapping.getSelectionExpression(),
								ParameterUsage.SET
						);
					}
				},
				session
		);
	}

	private void applyWriteIndexRestrictions(
			PersistentCollection<?> collection,
			Object key,
			Object entry,
			int entryPosition,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings) {
		final var attributeMapping = persister.getAttributeMapping();
		final var elementDescriptor = (EntityCollectionPart) attributeMapping.getElementDescriptor();
		final var associatedType = elementDescriptor.getAssociatedEntityMappingType();
		final Object element = collection.getElement( entry );
		final var identifierMapping = associatedType.getIdentifierMapping();
		identifierMapping.decompose(
				identifierMapping.getIdentifier( element ),
				jdbcValueBindings::bindRestriction,
				session
		);
	}

	/// Generates operation to perform "delete" SQL in the form -
	///
	/// ```sql
	/// update orders set customer_fk = null, order_number = null where id = ?
	/// ```
	///
	/// Which, in the case of a unidirectional one-to-many, actually "removes the collection row"
	private CollectionJdbcOperations.DeleteRowPlan buildDeleteRowPlan(TableDescriptor collectionTableDescriptor) {
		if ( !persister.needsRemove() ) {
			return null;
		}

		final TableDescriptorAsTableMapping tableMapping = new TableDescriptorAsTableMapping(
				collectionTableDescriptor,
				0, // relativePosition
				false, // isIdentifierTable
				false // isInverse
		);
		final var updateBuilder = new TableUpdateBuilderStandard<>(
				persister,
				new MutatingTableReference(tableMapping),
				factory,
				persister.getSqlWhereString()
		);

		final var foreignKeyDescriptor = persister.getAttributeMapping().getKeyDescriptor();
		foreignKeyDescriptor.getKeyPart().forEachUpdatable( (index, selectableMapping) -> {
			// set null
			updateBuilder.addColumnAssignment( selectableMapping, NULL );
		} );

		// set the value for each index column to null
		if ( persister.hasPhysicalIndexColumn() ) {
			final var indexDescriptor = persister.getAttributeMapping().getIndexDescriptor();
			assert indexDescriptor != null;
			indexDescriptor.forEachUpdatable( (index, selectableMapping) -> {
				updateBuilder.addColumnAssignment( selectableMapping, NULL );
			} );
		}

		// for one-to-many, we know the element is an entity and need to restrict the update
		// based on the element's id
		final var entityPart = (EntityCollectionPart) persister.getAttributeMapping().getElementDescriptor();
		entityPart.getAssociatedEntityMappingType().getIdentifierMapping().forEachColumn(  (index, selectableMapping) -> {
			updateBuilder.addKeyRestriction( selectableMapping );
		} );

		return new CollectionJdbcOperations.DeleteRowPlan(
				updateBuilder.buildMutation().createMutationOperation(null, factory),
				this::applyDeleteRowRestrictions
		);
	}

	private void applyDeleteRowRestrictions(
			PersistentCollection<?> collection,
			Object keyValue,
			Object rowValue,
			int rowPosition,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings) {
		final var entityPart = (EntityCollectionPart) persister.getAttributeMapping().getElementDescriptor();
		entityPart.getAssociatedEntityMappingType().getIdentifierMapping().decompose(
				keyValue,
				(index, val, jdbcMapping) -> jdbcValueBindings.bindValue(
						val,
						( jdbcMapping.getSelectionExpression() ),
						ParameterUsage.RESTRICT
				),
				session
		);
	}

	private MutationOperation buildRemoveOperation(TableDescriptor tableDescriptor) {
		if ( !persister.needsRemove() ) {
			return null;
		}

		final TableDescriptorAsTableMapping tableMapping = new TableDescriptorAsTableMapping(
				tableDescriptor,
				0, // relativePosition
				false, // isIdentifierTable
				false // isInverse
		);
		var builder = new TableUpdateBuilderStandard<>(
				persister,
				new MutatingTableReference(tableMapping),
				factory,
				persister.getSqlWhereString()
		);

		final var attributeMapping = persister.getAttributeMapping();
		assert attributeMapping != null;

		final var foreignKeyDescriptor = attributeMapping.getKeyDescriptor();
		assert foreignKeyDescriptor != null;

		foreignKeyDescriptor.getKeyPart().forEachSelectable( (selectionIndex, selectableMapping) -> {
			builder.addColumnAssignment( selectableMapping, NULL );
			builder.addKeyRestrictionLeniently( selectableMapping );
		} );

		if ( persister.hasPhysicalIndexColumn() ) {
			attributeMapping.getIndexDescriptor().forEachColumn( (selectionIndex, selectableMapping) -> {
				if ( selectableMapping.isUpdateable() ) {
					builder.addColumnAssignment( selectableMapping, NULL );
				}
			} );
		}

		return builder.buildMutation().createMutationOperation(null, factory);
	}

}
