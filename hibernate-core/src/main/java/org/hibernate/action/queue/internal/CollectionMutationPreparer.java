/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.Internal;
import org.hibernate.action.queue.spi.CollectionEndpoint;
import org.hibernate.action.queue.spi.CollectionMutationInput;
import org.hibernate.action.queue.spi.CollectionTransition;
import org.hibernate.collection.spi.CollectionBaseline;
import org.hibernate.collection.spi.CollectionInterpretationContext;
import org.hibernate.collection.spi.CollectionInterpretationProduction;
import org.hibernate.collection.spi.CollectionMutationInterpretation;
import org.hibernate.collection.spi.PhysicalCollectionMutation;
import org.hibernate.collection.spi.SemanticCollectionChange;
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
		prepareAll( inputs, List.of(), session, prepared::add );
		return prepared;
	}

	/// Prepares two ordered input segments directly into a queue-native consumer.
	static void prepareAll(
			List<CollectionMutationInput> firstInputs,
			List<CollectionMutationInput> secondInputs,
			EventSource session,
			Consumer<PreparedCollectionMutation> mutationConsumer) {
		final var prepared = new ArrayList<PreparedCollectionMutation>(
				(firstInputs.size() + secondInputs.size()) * 2
		);
		prepareRemovals( firstInputs, session, prepared::add );
		prepareRemovals( secondInputs, session, prepared::add );
		prepareUpdates( firstInputs, session, prepared::add );
		prepareUpdates( secondInputs, session, prepared::add );
		prepareCreates( firstInputs, session, prepared::add );
		prepareCreates( secondInputs, session, prepared::add );
		prepareQueuedOperations( firstInputs, session, prepared::add );
		prepareQueuedOperations( secondInputs, session, prepared::add );
		for ( var mutation : prepared ) {
			mutationConsumer.accept( refreshInvalidInterpretation( mutation, session ) );
		}
	}

	/// Reinterprets collection state changed by a later pre-collection listener without
	/// delivering a second lifecycle callback for the same semantic mutation.
	private static PreparedCollectionMutation refreshInvalidInterpretation(
			PreparedCollectionMutation mutation,
			EventSource session) {
		if ( mutation.isInterpretationValid() ) {
			return mutation;
		}
		final var collection = mutation.collection();
		if ( collection == null ) {
			return mutation;
		}
		final boolean semanticDeltaRequired = mutation.kind() == PreparedCollectionMutation.Kind.QUEUED_OPERATIONS
				|| mutation.interpretation().semanticChange() instanceof SemanticCollectionChange.Delta;
		final var interpretation = interpret(
				mutation.kind(),
				collection,
				mutation.endpoint(),
				mutation.emptySnapshot(),
				semanticDeltaRequired,
				session
		);
		retainInterpretation( collection, interpretation, session );
		return new PreparedCollectionMutation(
				mutation.kind(),
				collection,
				mutation.endpoint(),
				mutation.emptySnapshot(),
				mutation.affectedOwner(),
				mutation.affectedOwnerId(),
				interpretation
		);
	}

	private static void prepareRemovals(
			List<CollectionMutationInput> inputs,
			EventSource session,
			Consumer<PreparedCollectionMutation> mutationConsumer) {
		for ( var input : inputs ) {
			if ( input.transition() == CollectionTransition.REMOVE
					|| input.transition() == CollectionTransition.REMOVE_AND_CREATE ) {
				prepareRemove( input, input.loadedEndpoint(), session, mutationConsumer );
			}
		}
	}

	private static void prepareUpdates(
			List<CollectionMutationInput> inputs,
			EventSource session,
			Consumer<PreparedCollectionMutation> mutationConsumer) {
		for ( var input : inputs ) {
			if ( input.transition() == CollectionTransition.UPDATE ) {
				prepareUpdate( input, input.currentEndpoint(), session, mutationConsumer );
			}
		}
	}

	private static void prepareCreates(
			List<CollectionMutationInput> inputs,
			EventSource session,
			Consumer<PreparedCollectionMutation> mutationConsumer) {
		for ( var input : inputs ) {
			if ( input.transition() == CollectionTransition.CREATE
					|| input.transition() == CollectionTransition.REMOVE_AND_CREATE ) {
				prepareCreate( input, input.currentEndpoint(), session, mutationConsumer );
			}
		}
	}

	private static void prepareQueuedOperations(
			List<CollectionMutationInput> inputs,
			EventSource session,
			Consumer<PreparedCollectionMutation> mutationConsumer) {
		for ( var input : inputs ) {
			if ( input.hasQueuedOperations() ) {
				final var endpoint = input.loadedEndpoint() != null
						? input.loadedEndpoint()
						: input.currentEndpoint();
				if ( endpoint == null ) {
					throw new IllegalArgumentException( "Queued collection operations have no endpoint" );
				}
				final var retainedInterpretation = retainedInterpretation( input.collection(), session );
				final var interpretation = retainedInterpretation == null
						? interpret(
								PreparedCollectionMutation.Kind.QUEUED_OPERATIONS,
								input.collection(),
								endpoint,
								input.emptySnapshot(),
								true,
								session
						)
						: retainedInterpretation;
				if ( retainedInterpretation == null ) {
					retainInterpretation( input.collection(), interpretation, session );
				}
				mutationConsumer.accept( new PreparedCollectionMutation(
						PreparedCollectionMutation.Kind.QUEUED_OPERATIONS,
						input.collection(),
						endpoint,
						input.emptySnapshot(),
						null,
						null,
						interpretation
				) );
			}
		}
	}

	private static void prepareCreate(
			CollectionMutationInput input,
			CollectionEndpoint endpoint,
			EventSource session,
			Consumer<PreparedCollectionMutation> mutationConsumer) {
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
		mutationConsumer.accept( preparedMutation(
				PreparedCollectionMutation.Kind.CREATE,
				collection,
				endpoint,
				input.emptySnapshot(),
				affectedOwner,
				affectedOwnerId,
				input.hasQueuedOperations(),
				session
		) );
	}

	private static void prepareRemove(
			CollectionMutationInput input,
			CollectionEndpoint endpoint,
			EventSource session,
			Consumer<PreparedCollectionMutation> mutationConsumer) {
		final var collection = input.collection();
		if ( collection == null ) {
			mutationConsumer.accept( new PreparedCollectionMutation(
					PreparedCollectionMutation.Kind.REMOVE,
					null,
					endpoint,
					input.emptySnapshot() || input.removalSkipped(),
					input.affectedOwner(),
					input.affectedOwnerId(),
					wrapperlessRemoveInterpretation( input.emptySnapshot() || input.removalSkipped() )
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
		mutationConsumer.accept( preparedMutation(
				PreparedCollectionMutation.Kind.REMOVE,
				collection,
				endpoint,
				input.emptySnapshot() || input.removalSkipped(),
				affectedOwner,
				affectedOwnerId,
				input.hasQueuedOperations(),
				session
		) );
	}

	private static void prepareUpdate(
			CollectionMutationInput input,
			CollectionEndpoint endpoint,
			EventSource session,
			Consumer<PreparedCollectionMutation> mutationConsumer) {
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
		mutationConsumer.accept( preparedMutation(
				PreparedCollectionMutation.Kind.UPDATE,
				collection,
				endpoint,
				input.emptySnapshot(),
				affectedOwner,
				affectedOwnerId,
				input.hasQueuedOperations(),
				session
		) );
	}

	private static PreparedCollectionMutation preparedMutation(
			PreparedCollectionMutation.Kind kind,
			PersistentCollection<?> collection,
			CollectionEndpoint endpoint,
			boolean emptySnapshot,
			Object affectedOwner,
			Object affectedOwnerId,
			boolean semanticDeltaRequired,
			EventSource session) {
		final var interpretation = interpret(
				kind,
				collection,
				endpoint,
				emptySnapshot,
				semanticDeltaRequired,
				session
		);
		retainInterpretation( collection, interpretation, session );
		return new PreparedCollectionMutation(
				kind,
				collection,
				endpoint,
				emptySnapshot,
				affectedOwner,
				affectedOwnerId,
				interpretation
		);
	}

	private static CollectionMutationInterpretation interpret(
			PreparedCollectionMutation.Kind kind,
			PersistentCollection<?> collection,
			CollectionEndpoint endpoint,
			boolean emptySnapshot,
			boolean semanticDeltaRequired,
			EventSource session) {
		final var persister = endpoint.persister();
		final var interpreter = persister.getCollectionSemantics().getCollectionMutationInterpreter();
		CollectionBaseline baseline = baseline( kind, collection );
		CollectionInterpretationProduction production = interpreter.interpret(
				new CollectionInterpretationContext(
						collection,
						persister,
						transition( kind ),
						baseline,
						emptySnapshot,
						false,
						semanticDeltaRequired,
						session
				)
		);
		if ( production instanceof CollectionInterpretationProduction.InitializationRequired ) {
			collection.forceInitialization();
			baseline = CollectionBaseline.LOADED;
			production = interpreter.interpret(
					new CollectionInterpretationContext(
							collection,
							persister,
							transition( kind ),
							baseline,
							emptySnapshot,
							false,
							semanticDeltaRequired,
							session
					)
			);
		}
		if ( !(production instanceof CollectionInterpretationProduction.Produced produced) ) {
			throw new IllegalStateException(
					"Collection mutation interpreter still requires initialization for initialized collection "
							+ persister.getRole()
			);
		}
		return produced.interpretation();
	}

	private static CollectionMutationInterpretation wrapperlessRemoveInterpretation(boolean removalSkipped) {
		return new CollectionMutationInterpretation(
				CollectionTransition.REMOVE,
				SemanticCollectionChange.bulkRemoval(),
				new PhysicalCollectionMutation.RemoveAll(
						removalSkipped
								? PhysicalCollectionMutation.RemovalMode.SKIP
								: PhysicalCollectionMutation.RemovalMode.EXECUTE
				),
				-1
		);
	}

	private static CollectionTransition transition(PreparedCollectionMutation.Kind kind) {
		return switch ( kind ) {
			case CREATE -> CollectionTransition.CREATE;
			case REMOVE -> CollectionTransition.REMOVE;
			case UPDATE -> CollectionTransition.UPDATE;
			case QUEUED_OPERATIONS -> CollectionTransition.NONE;
		};
	}

	private static CollectionBaseline baseline(
			PreparedCollectionMutation.Kind kind,
			PersistentCollection<?> collection) {
		return switch ( kind ) {
			case CREATE -> CollectionBaseline.EMPTY;
			case UPDATE -> CollectionBaseline.LOADED;
			case QUEUED_OPERATIONS -> collection.wasInitialized()
					? CollectionBaseline.LOADED
					: CollectionBaseline.UNINITIALIZED;
			case REMOVE -> CollectionBaseline.LOADED;
		};
	}

	private static CollectionMutationInterpretation retainedInterpretation(
			PersistentCollection<?> collection,
			EventSource session) {
		final var tracker = session.getPersistenceContextInternal().getCollectionFlushActionTracker();
		if ( tracker instanceof FlushProcessingContext flushProcessingContext ) {
			return flushProcessingContext.getValidCollectionInterpretation( collection );
		}
		return null;
	}

	private static void retainInterpretation(
			PersistentCollection<?> collection,
			CollectionMutationInterpretation interpretation,
			EventSource session) {
		final var tracker = session.getPersistenceContextInternal().getCollectionFlushActionTracker();
		if ( tracker instanceof FlushProcessingContext flushProcessingContext ) {
			flushProcessingContext.retainCollectionInterpretation( collection, interpretation );
		}
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
