/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.decompose.collection;

import org.hibernate.action.internal.CollectionAction;
import org.hibernate.action.queue.exec.PostExecutionCallback;
import org.hibernate.action.queue.meta.TableDescriptor;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.service.spi.EventListenerGroups;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.PostCollectionRecreateEvent;
import org.hibernate.event.spi.PostCollectionRecreateEventListener;
import org.hibernate.event.spi.PostCollectionRemoveEvent;
import org.hibernate.event.spi.PostCollectionRemoveEventListener;
import org.hibernate.event.spi.PostCollectionUpdateEvent;
import org.hibernate.event.spi.PostCollectionUpdateEventListener;
import org.hibernate.event.spi.PreCollectionRecreateEvent;
import org.hibernate.event.spi.PreCollectionRecreateEventListener;
import org.hibernate.event.spi.PreCollectionRemoveEvent;
import org.hibernate.event.spi.PreCollectionRemoveEventListener;
import org.hibernate.event.spi.PreCollectionUpdateEvent;
import org.hibernate.event.spi.PreCollectionUpdateEventListener;
import org.hibernate.persister.collection.CollectionPersister;

/// Manages listener/callack events for collection actions when decomposed and performed
/// through the graph-based action queue.
///
/// @author Steve Ebersole
public class DecompositionSupport {

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Recreate events

	public static void firePreRecreate(
			CollectionPersister persister,
			PersistentCollection<?> collection,
			SharedSessionContractImplementor session) {
		getListenerGroups( session ).eventListenerGroup_PRE_COLLECTION_RECREATE.fireLazyEventOnEachListener(
				() -> new PreCollectionRecreateEvent( persister, collection, (EventSource) session ),
				PreCollectionRecreateEventListener::onPreRecreateCollection
		);
	}

	public static void firePostRecreate(
			CollectionPersister persister,
			PersistentCollection<?> collection,
			Object affectedOwner,
			Object affectedOwnerId,
			SharedSessionContractImplementor session) {
		getListenerGroups( session ).eventListenerGroup_POST_COLLECTION_RECREATE.fireLazyEventOnEachListener(
				() -> new PostCollectionRecreateEvent( persister, collection, (EventSource) session, affectedOwner, affectedOwnerId ),
				PostCollectionRecreateEventListener::onPostRecreateCollection
		);
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Update events

	public static void firePreUpdate(
			CollectionPersister persister,
			PersistentCollection<?> collection,
			SharedSessionContractImplementor session) {
		getListenerGroups( session ).eventListenerGroup_PRE_COLLECTION_UPDATE.fireLazyEventOnEachListener(
				() -> new PreCollectionUpdateEvent( persister, collection, (EventSource) session ),
				PreCollectionUpdateEventListener::onPreUpdateCollection
		);
	}

	public static void firePostUpdate(
			CollectionPersister persister,
			PersistentCollection<?> collection,
			Object affectedOwner,
			Object affectedOwnerId,
			SharedSessionContractImplementor session) {
		getListenerGroups( session ).eventListenerGroup_POST_COLLECTION_UPDATE.fireLazyEventOnEachListener(
				() -> new PostCollectionUpdateEvent( persister, collection, (EventSource) session, affectedOwner, affectedOwnerId ),
				PostCollectionUpdateEventListener::onPostUpdateCollection
		);
	}

	public static void firePreRemove(
			CollectionPersister persister,
			PersistentCollection<?> collection,
			Object affectedOwner,
			SharedSessionContractImplementor session) {
		getListenerGroups( session ).eventListenerGroup_PRE_COLLECTION_REMOVE.fireLazyEventOnEachListener(
				() -> new PreCollectionRemoveEvent( persister, collection, (EventSource) session, affectedOwner ),
				PreCollectionRemoveEventListener::onPreRemoveCollection
		);
	}

	public static void firePostRemove(
			CollectionPersister persister,
			PersistentCollection<?> collection,
			Object affectedOwner,
			Object affectedOwnerId,
			SharedSessionContractImplementor session) {
		getListenerGroups( session ).eventListenerGroup_POST_COLLECTION_REMOVE.fireLazyEventOnEachListener(
				() -> new PostCollectionRemoveEvent( persister, collection, (EventSource) session, affectedOwner, affectedOwnerId ),
				PostCollectionRemoveEventListener::onPostRemoveCollection
		);

	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// No-op operations

	/// Creates a no-op PlannedOperation to carry a post-execution callback.
	/// Used when decomposition produces no SQL operations but needs to defer
	/// the POST event callback until after all decompositions complete.
	public static org.hibernate.action.queue.plan.PlannedOperation createNoOpCallbackCarrier(
			TableDescriptor tableDescriptor,
			int ordinal,
			PostExecutionCallback callback) {
		// Create PlannedOperation with NO_OP kind
		// Executor will skip SQL execution but still run the callback
		var noOp = new org.hibernate.action.queue.plan.PlannedOperation(
				tableDescriptor,
				org.hibernate.action.queue.MutationKind.NO_OP,
				null,  // jdbcOperation - not needed for no-op
				null,  // bindPlan - not needed for no-op
				ordinal,
				"no-op-callback-carrier"
		);
		noOp.setPostExecutionCallback( callback );
		return noOp;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Internals

	private static EventListenerGroups getListenerGroups(SharedSessionContractImplementor session) {
		return session.getFactory().getEventListenerGroups();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Caching

	public static Object generateCacheKey(CollectionAction action, SharedSessionContractImplementor session) {
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
	}
}
