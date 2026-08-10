/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

import org.hibernate.Internal;
import org.hibernate.action.queue.spi.CollectionEndpoint;
import org.hibernate.action.queue.spi.CollectionMutationInput;
import org.hibernate.action.queue.spi.CollectionTransition;
import org.hibernate.collection.spi.CollectionBaseline;
import org.hibernate.collection.spi.CollectionDeltaProduction;
import org.hibernate.collection.spi.CollectionDeltaProductionContext;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.internal.FlushProcessingContext;
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
						null,
						null
				) );
			}
		}
		return freezeDeltas( prepared, session );
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
				affectedOwnerId,
				null
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
					input.affectedOwnerId(),
					null
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
				affectedOwnerId,
				null
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
				affectedOwnerId,
				null
		) );
	}

	private static List<PreparedCollectionMutation> freezeDeltas(
			List<PreparedCollectionMutation> mutations,
			EventSource session) {
		final var frozenByCollection = new IdentityHashMap<PersistentCollection<?>, FrozenCollectionDelta>();
		final var result = new ArrayList<PreparedCollectionMutation>( mutations.size() );
		for ( var mutation : mutations ) {
			final var collection = mutation.collection();
			if ( mutation.kind() == PreparedCollectionMutation.Kind.REMOVE || collection == null ) {
				result.add( mutation );
				continue;
			}

			var frozen = frozenByCollection.get( collection );
			if ( frozen == null ) {
				frozen = produceDelta( mutation, collection, session );
				frozenByCollection.put( collection, frozen );
				final var tracker = session.getPersistenceContextInternal().getCollectionFlushActionTracker();
				if ( tracker instanceof FlushProcessingContext flushProcessingContext ) {
					flushProcessingContext.retainFrozenDelta( collection, frozen );
				}
			}
			result.add( new PreparedCollectionMutation(
					mutation.kind(),
					collection,
					mutation.endpoint(),
					mutation.emptySnapshot(),
					mutation.affectedOwner(),
					mutation.affectedOwnerId(),
					frozen
			) );
		}
		return List.copyOf( result );
	}

	private static FrozenCollectionDelta produceDelta(
			PreparedCollectionMutation mutation,
			PersistentCollection<?> collection,
			EventSource session) {
		final var persister = mutation.endpoint().persister();
		final var producer = persister.getCollectionSemantics().getCollectionDeltaProducer();
		CollectionBaseline baseline = baseline( mutation, collection );
		CollectionDeltaProduction production = producer.produceDelta(
				new CollectionDeltaProductionContext( collection, persister, baseline, session )
		);
		if ( production instanceof CollectionDeltaProduction.InitializationRequired ) {
			collection.forceInitialization();
			baseline = CollectionBaseline.LOADED;
			production = producer.produceDelta(
					new CollectionDeltaProductionContext( collection, persister, baseline, session )
			);
		}
		if ( !(production instanceof CollectionDeltaProduction.Produced produced) ) {
			throw new IllegalStateException(
					"Collection delta producer still requires initialization for initialized collection "
							+ persister.getRole()
			);
		}
		return FrozenCollectionDelta.freeze( produced.delta(), collection );
	}

	private static CollectionBaseline baseline(
			PreparedCollectionMutation mutation,
			PersistentCollection<?> collection) {
		return switch ( mutation.kind() ) {
			case CREATE -> CollectionBaseline.EMPTY;
			case UPDATE -> CollectionBaseline.LOADED;
			case QUEUED_OPERATIONS -> collection.wasInitialized()
					? CollectionBaseline.LOADED
					: CollectionBaseline.UNINITIALIZED;
			case REMOVE -> throw new IllegalArgumentException( "Remove uses a bulk strategy, not a delta" );
		};
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
