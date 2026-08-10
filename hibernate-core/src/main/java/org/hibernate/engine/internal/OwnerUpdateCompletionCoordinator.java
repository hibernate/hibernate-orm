/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.internal;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.AssertionFailure;
import org.hibernate.event.jpa.spi.EntityCallbacks;
import org.hibernate.event.spi.EventSource;
import org.hibernate.jpa.event.spi.CallbackType;
import org.hibernate.persister.entity.EntityPersister;

/// Coordinates one Jakarta Persistence update-callback lifecycle for all entity
/// and collection mutation work belonging to an owner during a flush.
///
/// Entity update scheduling remains the authority for callback applicability.
/// Collection work may participate in an established owner update lifecycle but
/// cannot turn owner insert or delete work into an update lifecycle.
///
/// @since 8.0
/// @author Steve Ebersole
final class OwnerUpdateCompletionCoordinator {
	private static final class OwnerState {
		private final EntityCallbacks callbacks;
		private final boolean hasPostUpdate;
		private int entityMutations;
		private int completedEntityMutations;
		private int collectionMutations;
		private int completedCollectionMutations;
		private final List<Runnable> successfulCompletionHandlers = new ArrayList<>();
		private boolean sealed;
		private boolean failed;
		private boolean postUpdateInvoked;

		private OwnerState(EntityCallbacks callbacks) {
			this.callbacks = callbacks;
			hasPostUpdate = callbacks.hasRegisteredCallbacks( CallbackType.POST_UPDATE );
		}
	}

	private final EventSource session;
	private final Map<Object, OwnerState> ownerStates = new IdentityHashMap<>();

	OwnerUpdateCompletionCoordinator(EventSource session) {
		this.session = session;
	}

	void registerEntityMutation(Object owner, EntityPersister persister) {
		final var callbacks = persister.getEntityCallbacks();
		if ( !callbacks.hasRegisteredCallbacks( CallbackType.PRE_UPDATE )
				&& !callbacks.hasRegisteredCallbacks( CallbackType.POST_UPDATE ) ) {
			return;
		}
		final var state = ownerStates.computeIfAbsent( owner, ignored -> new OwnerState( callbacks ) );
		checkNotSealed( state );
		state.entityMutations++;
	}

	void registerCollectionMutation(Object owner, boolean inverse) {
		if ( owner == null || inverse ) {
			return;
		}
		final var state = ownerStates.get( owner );
		if ( state != null ) {
			checkNotSealed( state );
			state.collectionMutations++;
		}
	}

	boolean handles(Object owner) {
		return ownerStates.containsKey( owner );
	}

	void seal() {
		for ( var entry : ownerStates.entrySet() ) {
			entry.getValue().sealed = true;
			completeIfReady( entry.getKey(), entry.getValue() );
		}
	}

	boolean entityMutationCompleted(Object owner, Runnable successfulCompletionHandler) {
		final var state = ownerStates.get( owner );
		if ( state == null ) {
			return false;
		}
		state.successfulCompletionHandlers.add( successfulCompletionHandler );
		state.completedEntityMutations++;
		if ( state.completedEntityMutations > state.entityMutations ) {
			throw new AssertionFailure( "Owner entity update completed more than once" );
		}
		completeIfReady( owner, state );
		return true;
	}

	boolean collectionMutationCompleted(Object owner, Runnable successfulCompletionHandler) {
		final var state = ownerStates.get( owner );
		if ( state == null ) {
			return false;
		}
		state.successfulCompletionHandlers.add( successfulCompletionHandler );
		state.completedCollectionMutations++;
		if ( state.completedCollectionMutations > state.collectionMutations ) {
			throw new AssertionFailure( "Owner collection update completed more than once" );
		}
		completeIfReady( owner, state );
		return true;
	}

	void mutationFailed(Object owner) {
		final var state = ownerStates.get( owner );
		if ( state != null ) {
			state.failed = true;
		}
	}

	private void completeIfReady(Object owner, OwnerState state) {
		if ( state.sealed
				&& !state.failed
				&& !state.postUpdateInvoked
				&& state.completedEntityMutations == state.entityMutations
				&& state.completedCollectionMutations == state.collectionMutations ) {
			state.postUpdateInvoked = true;
			if ( state.hasPostUpdate ) {
				session.runEntityLifecycleCallback( () -> state.callbacks.postUpdate( owner ) );
			}
			for ( var successfulCompletionHandler : state.successfulCompletionHandlers ) {
				successfulCompletionHandler.run();
			}
			state.successfulCompletionHandlers.clear();
		}
	}

	private static void checkNotSealed(OwnerState state) {
		if ( state.sealed ) {
			throw new AssertionFailure( "Owner update participation registered after sealing" );
		}
	}
}
