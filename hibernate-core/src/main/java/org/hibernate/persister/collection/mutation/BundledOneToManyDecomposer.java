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
import org.hibernate.action.queue.exec.PostExecutionCallback;
import org.hibernate.action.queue.op.PlannedOperation;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.persister.collection.OneToManyPersister;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

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
			Consumer<PostExecutionCallback> postExecCallbackRegistry,
			SharedSessionContractImplementor session) {
		var collection = action.getCollection();
		var key = action.getKey();

		final CollectionJdbcOperations.InsertRowPlan insertRowPlan = jdbcOperations.getInsertRowPlan();
		final CollectionJdbcOperations.UpdateRowPlan updateRowPlan = jdbcOperations.getUpdateRowPlan();

		if ( insertRowPlan == null && updateRowPlan == null ) {
			return Collections.emptyList();
		}

		var operations = new ArrayList<PlannedOperation>();

		// Pre-insert callback once for the whole collection
		collection.preInsert( persister );

		final var entries = collection.entries( persister );
		if ( !entries.hasNext() ) {
			return List.of();
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
						MutationKind.INSERT,
						insertRowPlan.jdbcOperation(),
						bundledBindPlan,
						ordinalBase,
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
						jdbcOperations.getUpdateRowPlan().jdbcOperation().getTableDescriptor(),
						MutationKind.UPDATE,
						jdbcOperations.getUpdateRowPlan().jdbcOperation(),
						bundledBindPlan,
						ordinalBase * 2_000 + entryCount,
						"BundledWriteIndex[" + entryCount + "](" + persister.getRolePath() + ")"
				);

				operations.add( writeIndexPlannedOp );
			}
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

		// Fire PRE_COLLECTION_UPDATE events
		preUpdate( action, session );

		// Lock cache item
		final Object cacheKey = lockCacheItem(action, session);

		final List<PlannedOperation> operations = new ArrayList<>();

		// DELETE removed entries
		applyUpdateRemovals( collection, key, ordinalBase, session, operations::add );


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Create bundles for changes and additions at the same time to save iterations
		// since they both use the same set of elements based on PersistenceCollection.entires()
		var updateRowPlan = jdbcOperations.getUpdateRowPlan();
		var insertRowPlan = jdbcOperations.getInsertRowPlan();
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
		var deleteRowPlan = jdbcOperations.getDeleteRowPlan();
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
					MutationKind.DELETE,
					deleteRowPlan.jdbcOperation(),
					bundledBindPlan,
					ordinalBase,
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
				ordinalBase,
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
				MutationKind.INSERT,
				insertRowPlan.jdbcOperation(),
				bundledBindPlan,
				ordinalBase,
				"BundledInsertRows(" + persister.getRolePath() + ")"
		) );
	}

	@Override
	public List<PlannedOperation> decomposeRemove(
			CollectionRemoveAction action,
			int ordinalBase,
			Consumer<PostExecutionCallback> postExecCallbackRegistry,
			SharedSessionContractImplementor session) {
		return decomposeRemove( jdbcOperations, action, ordinalBase, postExecCallbackRegistry, session );
	}
}
