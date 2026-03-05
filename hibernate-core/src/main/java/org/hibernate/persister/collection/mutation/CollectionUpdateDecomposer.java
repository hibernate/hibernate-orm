/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import org.hibernate.action.internal.CollectionUpdateAction;
import org.hibernate.action.queue.exec.PostExecutionCallback;
import org.hibernate.action.queue.graph.MutationDecomposer;
import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.PreCollectionUpdateEvent;
import org.hibernate.event.spi.PreCollectionUpdateEventListener;

import java.util.function.Consumer;
import org.hibernate.persister.collection.BasicCollectionPersister;
import org.hibernate.persister.collection.OneToManyPersister;

import java.util.ArrayList;
import java.util.List;

/// Decomposes a collection update action into planned operations.
///
/// Manages pre-execution phase and registering [PostCollectionUpdateHandling] callback
/// for post-execution phase.  Delegates to [DeleteRowsCoordinator], [UpdateRowsCoordinator],
///  and [InsertRowsCoordinator] to create the needed PlannedOperations.
///
/// @author Steve Ebersole
public class CollectionUpdateDecomposer implements MutationDecomposer<CollectionUpdateAction> {
	@Override
	public List<PlannedOperation> decompose(
			CollectionUpdateAction action,
			int ordinalBase,
			Consumer<PostExecutionCallback> postExecutionCallbackRegistry,
			SharedSessionContractImplementor session) {
		final var persister = action.getPersister();
		final var collection = action.getCollection();
		final var key = action.getKey();

		// ===================================================================
		// PRE-EXECUTION PHASE
		// ===================================================================

		// 1. Fire PRE_COLLECTION_UPDATE events
		preUpdate(action, session);

		// 2. Lock cache item
		final Object cacheKey = lockCacheItem(action, session);

		// ===================================================================
		// EXECUTION PHASE - Delegate to row coordinators
		// ===================================================================
		final List<PlannedOperation> operations = new ArrayList<>();

		// 1. DELETE operations for removed entries
		final DeleteRowsCoordinator deleteCoordinator = getDeleteRowsCoordinator(persister);
		if (deleteCoordinator != null) {
			operations.addAll(deleteCoordinator.decomposeDeleteRows(collection, key, ordinalBase, session));
		}

		// 2. UPDATE operations for modified entries
		final UpdateRowsCoordinator updateCoordinator = getUpdateRowsCoordinator(persister);
		if (updateCoordinator != null) {
			operations.addAll(updateCoordinator.decomposeUpdateRows(collection, key, ordinalBase + 1, session));
		}

		// 3. INSERT operations for new entries
		final InsertRowsCoordinator insertCoordinator = getInsertRowsCoordinator(persister);
		if (insertCoordinator != null) {
			operations.addAll(insertCoordinator.decomposeInsertRows(
					collection, key, collection::includeInInsert, ordinalBase + 2, session));
		}

		// ===================================================================
		// POST-EXECUTION PHASE - Register callback
		// ===================================================================
		postExecutionCallbackRegistry.accept(new PostCollectionUpdateHandling(action, cacheKey));

		return operations;
	}

	private void preUpdate(CollectionUpdateAction action, SharedSessionContractImplementor session) {
		session.getFactory()
				.getEventListenerGroups()
				.eventListenerGroup_PRE_COLLECTION_UPDATE
				.fireLazyEventOnEachListener(
						() -> new PreCollectionUpdateEvent(
								action.getPersister(),
								action.getCollection(),
								(org.hibernate.event.spi.EventSource) session
						),
						PreCollectionUpdateEventListener::onPreUpdateCollection
				);
	}

	private Object lockCacheItem(CollectionUpdateAction action, SharedSessionContractImplementor session) {
		if (!action.getPersister().hasCache()) {
			return null;
		}

		final CollectionDataAccess cache = action.getPersister().getCacheAccessStrategy();
		return cache.generateCacheKey(
				action.getKey(),
				action.getPersister(),
				session.getFactory(),
				session.getTenantIdentifier()
		);
		// Note: The actual lock is obtained in CollectionAction.beforeExecutions()
		// We just generate the cache key here for use in post-execution
	}

	private static DeleteRowsCoordinator getDeleteRowsCoordinator(
			org.hibernate.persister.collection.CollectionPersister persister) {
		if ( persister instanceof OneToManyPersister oneToMany ) {
			return oneToMany.getDeleteRowsCoordinator();
		}
		else if ( persister instanceof BasicCollectionPersister basic ) {
			return basic.getDeleteRowsCoordinator();
		}
		return null;
	}

	private static UpdateRowsCoordinator getUpdateRowsCoordinator(
			org.hibernate.persister.collection.CollectionPersister persister) {
		if ( persister instanceof OneToManyPersister oneToMany ) {
			return oneToMany.getUpdateRowsCoordinator();
		}
		else if ( persister instanceof BasicCollectionPersister basic ) {
			return basic.getUpdateRowsCoordinator();
		}
		return null;
	}

	private static InsertRowsCoordinator getInsertRowsCoordinator(
			org.hibernate.persister.collection.CollectionPersister persister) {
		if ( persister instanceof OneToManyPersister oneToMany ) {
			return oneToMany.getInsertRowsCoordinator();
		}
		else if ( persister instanceof BasicCollectionPersister basic ) {
			return basic.getInsertRowsCoordinator();
		}
		return null;
	}
}
