/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.decompose.collection;


import org.hibernate.action.internal.CollectionAction;
import org.hibernate.action.queue.spi.decompose.DecompositionContext;
import org.hibernate.action.queue.spi.bind.ChainedPostExecutionCallback;
import org.hibernate.action.queue.spi.bind.PostExecutionCallback;
import org.hibernate.action.queue.spi.meta.TableDescriptor;
import org.hibernate.action.queue.spi.plan.FlushOperation;
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
import org.hibernate.jpa.event.spi.CallbackType;
import org.hibernate.persister.collection.CollectionPersister;

import static org.hibernate.engine.internal.CacheHelper.usingCache;

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

	public static PostExecutionCallback withOwnerUpdateCallbacks(
			CollectionPersister persister,
			Object affectedOwner,
			DecompositionContext decompositionContext,
			PostExecutionCallback callback) {
		if ( persister.isInverse() || affectedOwner == null ) {
			return callback;
		}

		final var ownerPersister = persister.getOwnerEntityPersister();
		final var ownerCallbacks = ownerPersister.getEntityCallbacks();
		final boolean hasPreUpdate = ownerCallbacks.hasRegisteredCallbacks( CallbackType.PRE_UPDATE );
		final boolean hasPostUpdate = ownerCallbacks.hasRegisteredCallbacks( CallbackType.POST_UPDATE );
		if ( !hasPreUpdate && !hasPostUpdate ) {
			return callback;
		}
		if ( decompositionContext != null && !decompositionContext.registerOwnerUpdateCallbacks( affectedOwner ) ) {
			return callback;
		}

		if ( hasPreUpdate ) {
			ownerCallbacks.preUpdate( affectedOwner );
		}

		if ( !hasPostUpdate ) {
			return callback;
		}

		return new ChainedPostExecutionCallback(
				callback,
				session -> ownerCallbacks.postUpdate( affectedOwner )
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

	/// Creates a no-op FlushOperation to carry a post-execution callback.
	/// Used when decomposition produces no SQL operations but needs to defer
	/// the POST event callback until after all decompositions complete.
	public static FlushOperation createNoOpCallbackCarrier(
			TableDescriptor tableDescriptor,
			int ordinal,
			PostExecutionCallback callback) {
		// Create FlushOperation with NO_OP kind
		// Executor will skip SQL execution but still run the callback
		final var noOp = new FlushOperation(
				tableDescriptor,
				org.hibernate.action.queue.spi.MutationKind.NO_OP,
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
		return usingCache( action.getPersister(),
				cache -> {
					final Object key = action.getKey();
					//TODO: Highly suspicious assertion.
					//      This method does sometimes
					//      get called with an action
					//      with a null key!
					assert key != null;
					return cache.generateCacheKey(
							key,
							action.getPersister(),
							session.getFactory(),
							session.getTenantIdentifier()
					);
				},
				null );
	}

	public static void syncOwnerCollectionLoadedState(
			CollectionPersister persister,
			Object affectedOwner,
			SharedSessionContractImplementor session) {
		if ( affectedOwner != null ) {
			final var ownerEntry = session.getPersistenceContextInternal().getEntry( affectedOwner );
			if ( ownerEntry != null && ownerEntry.getLoadedState() != null ) {
				final String role = persister.getRole();
				final String ownerEntityName = persister.getOwnerEntityPersister().getEntityName();
				if ( role.startsWith( ownerEntityName + "." ) ) {
					final String propertyName = role.substring( ownerEntityName.length() + 1 );
					if ( propertyName.indexOf( '.' ) < 0 ) {
						final Object currentValue =
								persister.getOwnerEntityPersister()
										.getPropertyValue( affectedOwner, propertyName );
						if ( currentValue == null ) {
							ownerEntry.overwriteLoadedStateCollectionValue( propertyName, null );
						}
						else if ( currentValue instanceof PersistentCollection<?> collection ) {
							ownerEntry.overwriteLoadedStateCollectionValue( propertyName, collection );
						}
					}
				}
			}
		}
	}
}
