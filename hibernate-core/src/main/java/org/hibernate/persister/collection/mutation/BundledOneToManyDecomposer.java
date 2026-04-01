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
import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.persister.collection.OneToManyPersister;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.hibernate.action.queue.CollectionOrdinalSupport.Slot;
import static org.hibernate.action.queue.CollectionOrdinalSupport.calculateOrdinal;
import static org.hibernate.sql.model.ModelMutationLogging.MODEL_MUTATION_LOGGER;

/// Specialized `one-to-many` decomposer which produces bundled operations - one
/// operation per collection per operation kind, as opposed to one operation
/// be collection element per operation kind.
///
/// @see org.hibernate.cfg.FlushSettings#BUNDLE_COLLECTION_OPERATIONS
///
/// @author Steve Ebersole
public class BundledOneToManyDecomposer extends AbstractOneToManyDecomposer {
	private final CollectionJdbcOperations jdbcOperations;

	public BundledOneToManyDecomposer(
			OneToManyPersister persister,
			SessionFactoryImplementor factory) {
		super( persister, factory );

		jdbcOperations = buildJdbcOperations( persister.getCollectionTableDescriptor(), factory );
	}

	@Override
	public List<PlannedOperation> decomposeRecreate(
			CollectionRecreateAction action,
			int ordinalBase,
			SharedSessionContractImplementor session) {
		var collection = action.getCollection();

		// Always fire PRE event, even if no SQL operations will be needed
		DecompositionSupport.firePreRecreate( persister, collection, session );

		// Create callback to handle post-execution work (afterAction, cache, events, stats)
		var postRecreateHandling = new PostCollectionRecreateHandling(
				persister,
				action.getCollection(),
				action.getAffectedOwner(),
				action.getAffectedOwnerId(),
				DecompositionSupport.generateCacheKey( action, session )
		);

		final CollectionJdbcOperations.InsertRowPlan insertRowPlan = jdbcOperations.insertRowPlan();
		final CollectionJdbcOperations.UpdateRowPlan updateRowPlan = jdbcOperations.updateRowPlan();

		if ( insertRowPlan == null && updateRowPlan == null ) {
			// No plans - create no-op to defer POST callback
			return List.of( DecompositionSupport.createNoOpCallbackCarrier(
					persister.getCollectionTableDescriptor(),
					calculateOrdinal( ordinalBase, Slot.INSERT ),
					postRecreateHandling
			) );
		}

		var operations = new ArrayList<PlannedOperation>();

		// Pre-insert callback once for the whole collection
		collection.preInsert( persister );

		final var entries = collection.entries( persister );
		if ( !entries.hasNext() ) {
			// No entries - create no-op to defer POST callback
			return List.of( DecompositionSupport.createNoOpCallbackCarrier(
					persister.getCollectionTableDescriptor(),
					calculateOrdinal( ordinalBase, Slot.INSERT ),
					postRecreateHandling
			) );
		}

		final List<BundledBindPlanEntry> entryList = new ArrayList<>();
		int entryCount = 0;

		while ( entries.hasNext() ) {
			final Object entry = entries.next();
			boolean include = collection.includeInRecreate(
					entry,
					entryCount,
					collection,
					persister.getAttributeMapping()
			);

			if ( include ) {
				entryList.add( new BundledBindPlanEntry( entry, entryCount ) );
			}

			entryCount++;
		}

		if ( !entryList.isEmpty() ) {
			if ( insertRowPlan != null ) {
				final BindPlan bundledBindPlan = new BundledCollectionInsertBindPlan(
						insertRowPlan.values(),
						collection,
						action.getKey(),
						entryList
				);

				operations.add( new PlannedOperation(
						persister.getCollectionTableDescriptor(),
						// technically an UPDATE
						MutationKind.UPDATE,
						insertRowPlan.jdbcOperation(),
						bundledBindPlan,
						calculateOrdinal( ordinalBase, Slot.INSERT ),
						"BundledInsertRows(" + persister.getRolePath() + ")"
				) );
			}

			if ( updateRowPlan != null ) {
				final BindPlan bundledBindPlan = new BundledCollectionUpdateBindPlan(
						collection,
						action.getKey(),
						updateRowPlan.values(),
						updateRowPlan.restrictions(),
						entryList
				);

				var  writeIndexPlannedOp = new PlannedOperation(
						persister.getCollectionTableDescriptor(),
						MutationKind.UPDATE,
						jdbcOperations.updateRowPlan().jdbcOperation(),
						bundledBindPlan,
						calculateOrdinal( ordinalBase, Slot.WRITEINDEX ),
						"BundledWriteIndex[" + entryCount + "](" + persister.getRolePath() + ")"
				);

				operations.add( writeIndexPlannedOp );
			}
		}

		// Attach post-execution callback to the last operation
		if ( !operations.isEmpty() ) {
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
		final PostCollectionUpdateHandling postCollectionUpdateHandling = new PostCollectionUpdateHandling(
				persister,
				collection,
				key,
				action.getAffectedOwner(),
				action.getAffectedOwnerId(),
				DecompositionSupport.generateCacheKey(action, session)
		);

		final List<PlannedOperation> operations = new ArrayList<>();

		if ( !collection.wasInitialized() ) {
			// If the collection wasn't initialized, we cannot access entries/deletes
			// The collection should still be marked dirty for queued operations
			// We only need to notify the cache via the post-execution callback
		}
		else {
			// DELETE removed entries
			applyUpdateRemovals( collection, key, ordinalBase, session, operations::add );

			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// Create bundles for changes and additions at the same time to save iterations
			// since they both use the same set of elements based on PersistenceCollection.entires()
			var updateRowPlan = jdbcOperations.updateRowPlan();
			var insertRowPlan = jdbcOperations.insertRowPlan();
			var entries = collection.entries( persister );

			if ( (updateRowPlan != null || insertRowPlan != null) && entries.hasNext() ) {
				var changeEntries = updateRowPlan == null ? null : new ArrayList<BundledBindPlanEntry>();
				var additionEntries = insertRowPlan == null ? null : new ArrayList<BundledBindPlanEntry>();
				int entryCount = 0;

				while ( entries.hasNext() ) {
					final Object entry = entries.next();

					var isAddition = collection.needsInserting( entry, entryCount, persister.getElementType() );
					var isChange = collection.needsUpdating( entry, entryCount, persister.getAttributeMapping() );

					if ( isAddition && isChange ) {
						// Log a warning?  This typically means bad equals/hashCode, though can happen I guess
						// with UserCollectionType too...
					}
					if ( updateRowPlan != null && isChange ) {
						changeEntries.add( new BundledBindPlanEntry( entry, entryCount ) );
					}
					if ( insertRowPlan != null && isAddition ) {
						additionEntries.add( new BundledBindPlanEntry( entry, entryCount ) );
					}

					entryCount++;
				}

				// UPDATE modified entries
				applyUpdateChanges( collection, key, ordinalBase + 1, changeEntries, updateRowPlan, operations::add );

				// INSERT entries
				applyUpdateAdditions( collection, key, ordinalBase + 2, additionEntries, insertRowPlan, operations::add );
			}
		}

		// Attach post-execution callback to the last operation
		if ( !operations.isEmpty() ) {
			operations.get( operations.size() - 1 ).setPostExecutionCallback( postCollectionUpdateHandling );
			return operations;
		}
		else {
			// Operations empty (e.g., uninitialized dirty collection) - create no-op to defer POST callback
			return List.of( DecompositionSupport.createNoOpCallbackCarrier(
					persister.getCollectionTableDescriptor(),
					calculateOrdinal( ordinalBase, Slot.UPDATE ),
					postCollectionUpdateHandling
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
		var deleteRowPlan = jdbcOperations.deleteRowPlan();
		final var deletes = collection.getDeletes( persister, !persister.hasPhysicalIndexColumn() );
		if ( deleteRowPlan == null || !deletes.hasNext() ) {
			MODEL_MUTATION_LOGGER.noRowsToDelete();
			return;
		}

		// Bundle all rows into a single PlannedOperation with a bundled BindPlan
		final List<Object> deletionList = new ArrayList<>();

		while ( deletes.hasNext() ) {
			deletionList.add( deletes.next() );
		}

		if ( !deletionList.isEmpty() ) {
			final BindPlan bundledBindPlan = new BundledCollectionDeleteBindPlan(
					collection,
					key,
					deleteRowPlan.restrictions(),
					deletionList
			);

			operationConsumer.accept( new PlannedOperation(
					persister.getCollectionTableDescriptor(),
					// technically an UPDATE
					MutationKind.UPDATE,
					deleteRowPlan.jdbcOperation(),
					bundledBindPlan,
					calculateOrdinal( ordinalBase, Slot.DELETE ),
					"BundledDeleteRows(" + persister.getRolePath() + ")"
			) );
		}
	}

	protected void applyUpdateChanges(
			PersistentCollection<?> collection,
			Object key,
			int ordinalBase,
			List<BundledBindPlanEntry> changeEntries,
			CollectionJdbcOperations.UpdateRowPlan updateRowPlan,
			Consumer<PlannedOperation> operationConsumer) {
		if ( CollectionHelper.isEmpty( changeEntries ) ) {
			return;
		}

		final BindPlan bundledBindPlan = new BundledCollectionUpdateBindPlan(
				collection,
				key,
				updateRowPlan.values(),
				updateRowPlan.restrictions(),
				changeEntries
		);

		operationConsumer.accept( new PlannedOperation(
				persister.getCollectionTableDescriptor(),
				MutationKind.UPDATE,
				updateRowPlan.jdbcOperation(),
				bundledBindPlan,
				calculateOrdinal( ordinalBase, Slot.UPDATE ),
				"BundledUpdateRows(" + persister.getRolePath() + ")"
		) );
	}

	protected void applyUpdateAdditions(
			PersistentCollection<?> collection,
			Object key,
			int ordinalBase,
			List<BundledBindPlanEntry> additionEntries,
			CollectionJdbcOperations.InsertRowPlan insertRowPlan,
			Consumer<PlannedOperation> operationConsumer) {
		if ( CollectionHelper.isEmpty( additionEntries ) ) {
			return;
		}

		// Pre-insert callback once for the whole collection
		collection.preInsert( persister );

		final BindPlan bundledBindPlan = new BundledCollectionInsertBindPlan(
				insertRowPlan.values(),
				collection,
				key,
				additionEntries
		);

		operationConsumer.accept( new PlannedOperation(
				persister.getCollectionTableDescriptor(),
				// technically an UPDATE
				MutationKind.UPDATE,
				insertRowPlan.jdbcOperation(),
				bundledBindPlan,
				calculateOrdinal( ordinalBase, Slot.INSERT ),
				"BundledInsertRows(" + persister.getRolePath() + ")"
		) );
	}

	@Override
	public List<PlannedOperation> decomposeRemove(
			CollectionRemoveAction action,
			int ordinalBase,
			SharedSessionContractImplementor session) {
		return decomposeRemove( jdbcOperations, action, ordinalBase, session );
	}
}
