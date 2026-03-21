/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import org.hibernate.action.internal.CollectionRecreateAction;
import org.hibernate.action.internal.CollectionUpdateAction;
import org.hibernate.action.queue.MutationKind;
import org.hibernate.action.queue.bind.BindPlan;
import org.hibernate.action.queue.exec.PostExecutionCallback;
import org.hibernate.action.queue.op.PlannedOperation;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.OneToManyPersister;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

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
			Consumer<PostExecutionCallback> postExecCallbackRegistry,
			SharedSessionContractImplementor session) {
		var attribute = persister.getAttributeMapping();
		var collection = action.getCollection();
		var key = action.getKey();

		// Pre-insert callback once for the whole collection
		collection.preInsert( persister );

		final var entries = collection.entries( persister );
		if ( !entries.hasNext() ) {
			return List.of();
		}

		final List<PlannedOperation> operations = new ArrayList<>();

		int entryCount = 0;
		while ( entries.hasNext() ) {
			final Object entry = entries.next();
			var jdbcOperations = selectJdbcOperations( entry, session );
			var insertRowPlan = jdbcOperations.getInsertRowPlan();

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
						insertRowPlan.jdbcOperation().getTableDescriptor(),
						MutationKind.UPDATE,
						insertRowPlan.jdbcOperation(),
						bindPlan,
						ordinalBase * 1_000 + entryCount,
						"InsertRow[" + entryCount + "](" + persister.getRolePath() + ")"
				);

				operations.add( plannedOp );
			}

			if ( jdbcOperations.getUpdateRowPlan() != null ) {
				var writeIndexBindPlan = new SingleRowUpdateBindPlan(
						collection,
						key,
						entry,
						entryCount,
						jdbcOperations.getUpdateRowPlan().values(),
						jdbcOperations.getUpdateRowPlan().restrictions()
				);


				var  writeIndexPlannedOp = new PlannedOperation(
						jdbcOperations.getUpdateRowPlan().jdbcOperation().getTableDescriptor(),
						MutationKind.UPDATE,
						jdbcOperations.getUpdateRowPlan().jdbcOperation(),
						writeIndexBindPlan,
						ordinalBase * 2_000 + entryCount,
						"WriteIndex[" + entryCount + "](" + persister.getRolePath() + ")"
				);

				//postExecCallbackRegistry.accept(... );
				// on recreate we should be able to write the index as a normal planned-op
				operations.add( writeIndexPlannedOp );
			}

			entryCount++;
		}

		// Register callback to handle post-execution work (afterAction, cache, events, stats)
		final Object cacheKey = lockCacheItem( action, session );
		postExecCallbackRegistry.accept( new PostCollectionRecreateHandling( action, cacheKey ) );

		return operations;
	}

	@Override
	public final List<PlannedOperation> decomposeUpdate(
			CollectionUpdateAction action,
			int ordinalBase,
			Consumer<PostExecutionCallback> postExecCallbackRegistry,
			SharedSessionContractImplementor session) {
		final var collection = action.getCollection();
		final var key = action.getKey();

		// Lock cache item
		final Object cacheKey = lockCacheItem( action, session );

		final List<PlannedOperation> operations = new ArrayList<>();

		if ( !collection.wasInitialized() ) {
			// If the collection wasn't initialized, we cannot access entries/deletes
			// The collection should still be marked dirty for queued operations
			// We only need to notify the cache via the post-execution callback
		}
		else {
			// Fire PRE_COLLECTION_UPDATE events
			action.preUpdate();

			// DELETE removed entries
			applyUpdateRemovals( collection, key, ordinalBase, session, operations::add );

			// UPDATE modified entries
			applyUpdateChanges( collection, key, ordinalBase + 1, session, operations::add );

			// INSERT entries
			applyUpdateAdditions( collection, key, ordinalBase + 2, session, operations::add );
		}

		postExecCallbackRegistry.accept( new PostCollectionUpdateHandling(
				persister,
				collection,
				key,
				action.getAffectedOwner(),
				action.getAffectedOwnerId(),
				cacheKey
		) );

		return operations;
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

		int deletionCount = 0;
		while ( deletes.hasNext() ) {
			var removal = deletes.next();

			var jdbcOperations = selectJdbcOperations( removal, session );
			assert jdbcOperations != null;
			var deleteRowPlan = jdbcOperations.getDeleteRowPlan();

			final BindPlan bindPlan = new SingleRowDeleteBindPlan(
					collection,
					key,
					removal,
					deleteRowPlan.restrictions()
			);

			final PlannedOperation plannedOp = new PlannedOperation(
					deleteRowPlan.jdbcOperation().getTableDescriptor(),
					// technically an UPDATE
					MutationKind.UPDATE,
					deleteRowPlan.jdbcOperation(),
					bindPlan,
					ordinalBase * 1_000 + deletionCount,
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

		int entryCount = 0;
		while ( entries.hasNext() ) {
			final Object entry = entries.next();

			var jdbcOperations = selectJdbcOperations( entry, session );
			assert jdbcOperations != null;
			var updateRowPlan = jdbcOperations.getUpdateRowPlan();

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
						updateRowPlan.jdbcOperation().getTableDescriptor(),
						MutationKind.UPDATE,
						updateRowPlan.jdbcOperation(),
						bindPlan,
						ordinalBase * 1_000 + entryCount,
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

		int entryCount = 0;
		while ( entries.hasNext() ) {
			final Object entry = entries.next();
			if ( collection.includeInInsert( entry, entryCount, collection, persister.getAttributeMapping() ) ) {

				var jdbcOperations = selectJdbcOperations( entry, session );
				assert jdbcOperations != null;
				var insertRowPlan = jdbcOperations.getInsertRowPlan();

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
							insertRowPlan.jdbcOperation().getTableDescriptor(),
							// technically an UPDATE
							MutationKind.UPDATE,
							insertRowPlan.jdbcOperation(),
							bindPlan,
							ordinalBase * 1_000 + entryCount,
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
