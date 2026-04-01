/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import org.hibernate.action.internal.CollectionRecreateAction;
import org.hibernate.action.internal.CollectionUpdateAction;
import org.hibernate.action.queue.MutationKind;
import org.hibernate.action.queue.bind.BindPlan;
import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.OneToManyPersister;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.hibernate.action.queue.CollectionOrdinalSupport.Slot;
import static org.hibernate.action.queue.CollectionOrdinalSupport.calculateOrdinal;
import static org.hibernate.sql.model.ModelMutationLogging.MODEL_MUTATION_LOGGER;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractNonBundledOneToManyDecomposer extends AbstractOneToManyDecomposer {
	public AbstractNonBundledOneToManyDecomposer(OneToManyPersister persister, SessionFactoryImplementor factory) {
		super( persister, factory );
	}

	protected abstract CollectionJdbcOperations selectJdbcOperations(Object entry, SharedSessionContractImplementor session);

	@Override
	public List<PlannedOperation> decomposeRecreate(
			CollectionRecreateAction action,
			int ordinalBase,
			SharedSessionContractImplementor session) {
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

			if ( jdbcOperations.updateRowPlan() != null ) {
				var writeIndexBindPlan = new SingleRowUpdateBindPlan(
						collection,
						key,
						entry,
						entryCount,
						jdbcOperations.updateRowPlan().values(),
						jdbcOperations.updateRowPlan().restrictions()
				);


				var  writeIndexPlannedOp = new PlannedOperation(
						persister.getCollectionTableDescriptor(),
						MutationKind.UPDATE,
						jdbcOperations.updateRowPlan().jdbcOperation(),
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
			SharedSessionContractImplementor session) {
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

	@Override
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
			var updateRowPlan = jdbcOperations.updateRowPlan();

			// For inverse collections, updateRowPlan will be null
			if ( updateRowPlan != null && collection.needsUpdating( entry, entryCount, persister.getAttributeMapping() ) ) {
				final BindPlan bindPlan = new SingleRowUpdateBindPlan(
						collection,
						key,
						entry,
						entryCount,
						updateRowPlan.values(),
						updateRowPlan.restrictions()
				);

				final PlannedOperation plannedOp = new PlannedOperation(
						persister.getCollectionTableDescriptor(),
						MutationKind.UPDATE,
						updateRowPlan.jdbcOperation(),
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

}
