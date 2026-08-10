/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Internal;
import org.hibernate.action.queue.spi.CollectionEndpoint;
import org.hibernate.action.queue.spi.CollectionMutationInput;
import org.hibernate.action.queue.spi.CollectionTransition;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.PreCollectionRecreateEvent;
import org.hibernate.event.spi.PreCollectionRecreateEventListener;
import org.hibernate.event.spi.PreCollectionRemoveEvent;
import org.hibernate.event.spi.PreCollectionRemoveEventListener;
import org.hibernate.event.spi.PreCollectionUpdateEvent;
import org.hibernate.event.spi.PreCollectionUpdateEventListener;

/// Performs queue-independent collection lifecycle preparation after a positive flush decision.
///
/// @since 8.0
/// @author Steve Ebersole
@Internal
public final class CollectionMutationPreparer {
	public static List<PreparedCollectionMutation> prepare(
			CollectionMutationInput input,
			EventSource session) {
		return prepareAll( List.of( input ), session );
	}

	/// Prepares a flush's inputs in stable semantic phases shared by every queue implementation.
	public static List<PreparedCollectionMutation> prepareAll(
			List<CollectionMutationInput> inputs,
			EventSource session) {
		final var prepared = new ArrayList<PreparedCollectionMutation>( inputs.size() * 2 );
		for ( var input : inputs ) {
			if ( input.transition() == CollectionTransition.REMOVE
					|| input.transition() == CollectionTransition.REMOVE_AND_CREATE ) {
				prepareRemove( input, input.loadedEndpoint(), session, prepared );
			}
		}
		for ( var input : inputs ) {
			if ( input.transition() == CollectionTransition.UPDATE ) {
				prepareUpdate( input, input.currentEndpoint(), session, prepared );
			}
		}
		for ( var input : inputs ) {
			if ( input.transition() == CollectionTransition.CREATE
					|| input.transition() == CollectionTransition.REMOVE_AND_CREATE ) {
				prepareCreate( input, input.currentEndpoint(), session, prepared );
			}
		}

		for ( var input : inputs ) {
			if ( input.hasQueuedOperations() ) {
				final var endpoint = input.loadedEndpoint() != null
						? input.loadedEndpoint()
						: input.currentEndpoint();
				if ( endpoint == null ) {
					throw new IllegalArgumentException( "Queued collection operations have no endpoint" );
				}
				prepared.add( new PreparedCollectionMutation(
						PreparedCollectionMutation.Kind.QUEUED_OPERATIONS,
						input.collection(),
						endpoint,
						input.emptySnapshot(),
						null,
						null
				) );
			}
		}
		return List.copyOf( prepared );
	}

	private static void prepareCreate(
			CollectionMutationInput input,
			CollectionEndpoint endpoint,
			EventSource session,
			List<PreparedCollectionMutation> prepared) {
		final var collection = requireCollection( input );
		final Object key = endpoint.key();
		session.runInterceptorCallback( () -> session.getInterceptor().onCollectionRecreate( collection, key ) );
		final Object affectedOwner = collection.getOwner();
		final Object affectedOwnerId = ownerId( affectedOwner, session );
		session.getFactory().getEventListenerGroups().eventListenerGroup_PRE_COLLECTION_RECREATE
				.fireLazyEventOnEachListener(
						() -> new PreCollectionRecreateEvent( endpoint.persister(), collection, session ),
						PreCollectionRecreateEventListener::onPreRecreateCollection
				);
		prepared.add( new PreparedCollectionMutation(
				PreparedCollectionMutation.Kind.CREATE,
				collection,
				endpoint,
				input.emptySnapshot(),
				affectedOwner,
				affectedOwnerId
		) );
	}

	private static void prepareRemove(
			CollectionMutationInput input,
			CollectionEndpoint endpoint,
			EventSource session,
			List<PreparedCollectionMutation> prepared) {
		final var collection = input.collection();
		if ( collection == null ) {
			prepared.add( new PreparedCollectionMutation(
					PreparedCollectionMutation.Kind.REMOVE,
					null,
					endpoint,
					input.emptySnapshot() || input.removalSkipped(),
					input.affectedOwner(),
					input.affectedOwnerId()
			) );
			return;
		}
		final Object key = endpoint.key();
		session.runInterceptorCallback( () -> session.getInterceptor().onCollectionRemove( collection, key ) );
		final var persistenceContext = session.getPersistenceContextInternal();
		final Object affectedOwner = persistenceContext.getLoadedCollectionOwnerOrNull( collection );
		final Object affectedOwnerId = persistenceContext.getLoadedCollectionOwnerIdOrNull( collection );
		session.getFactory().getEventListenerGroups().eventListenerGroup_PRE_COLLECTION_REMOVE
				.fireLazyEventOnEachListener(
						() -> new PreCollectionRemoveEvent( endpoint.persister(), collection, session, affectedOwner ),
						PreCollectionRemoveEventListener::onPreRemoveCollection
				);
		prepared.add( new PreparedCollectionMutation(
				PreparedCollectionMutation.Kind.REMOVE,
				collection,
				endpoint,
				input.emptySnapshot() || input.removalSkipped(),
				affectedOwner,
				affectedOwnerId
		) );
	}

	private static void prepareUpdate(
			CollectionMutationInput input,
			CollectionEndpoint endpoint,
			EventSource session,
			List<PreparedCollectionMutation> prepared) {
		final var collection = requireCollection( input );
		final Object key = endpoint.key();
		session.runInterceptorCallback( () -> session.getInterceptor().onCollectionUpdate( collection, key ) );
		final var persistenceContext = session.getPersistenceContextInternal();
		final Object affectedOwner = persistenceContext.getLoadedCollectionOwnerOrNull( collection );
		final Object affectedOwnerId = persistenceContext.getLoadedCollectionOwnerIdOrNull( collection );
		session.getFactory().getEventListenerGroups().eventListenerGroup_PRE_COLLECTION_UPDATE
				.fireLazyEventOnEachListener(
						() -> new PreCollectionUpdateEvent( endpoint.persister(), collection, session ),
						PreCollectionUpdateEventListener::onPreUpdateCollection
				);
		prepared.add( new PreparedCollectionMutation(
				PreparedCollectionMutation.Kind.UPDATE,
				collection,
				endpoint,
				input.emptySnapshot(),
				affectedOwner,
				affectedOwnerId
		) );
	}

	private static Object ownerId(Object owner, EventSource session) {
		final var ownerEntry = session.getPersistenceContextInternal().getEntry( owner );
		return ownerEntry == null ? null : ownerEntry.getId();
	}

	private static PersistentCollection<?> requireCollection(
			CollectionMutationInput input) {
		final var collection = input.collection();
		if ( collection == null ) {
			throw new IllegalArgumentException( "Collection transition requires a collection wrapper" );
		}
		return collection;
	}

	private CollectionMutationPreparer() {
	}
}
