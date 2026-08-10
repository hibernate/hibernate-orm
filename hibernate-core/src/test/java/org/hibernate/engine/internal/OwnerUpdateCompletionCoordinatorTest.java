/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.internal;

import java.util.ArrayList;

import org.hibernate.event.jpa.spi.EntityCallbacks;
import org.hibernate.event.spi.EventSource;
import org.hibernate.jpa.event.spi.CallbackType;
import org.hibernate.persister.entity.EntityPersister;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Tests flush-local owner update completion across entity and collection work.
///
/// @author Steve Ebersole
class OwnerUpdateCompletionCoordinatorTest {
	private final EventSource session = mock( EventSource.class );
	private final EntityCallbacks callbacks = mock( EntityCallbacks.class );
	private final EntityPersister persister = mock( EntityPersister.class );
	private final Object owner = new Object();

	@BeforeEach
	void setUp() {
		when( persister.getEntityCallbacks() ).thenReturn( callbacks );
		when( callbacks.hasRegisteredCallbacks( CallbackType.PRE_UPDATE ) ).thenReturn( true );
		when( callbacks.hasRegisteredCallbacks( CallbackType.POST_UPDATE ) ).thenReturn( true );
		doAnswer( invocation -> {
			((Runnable) invocation.getArgument( 0 )).run();
			return null;
		} ).when( session ).runEntityLifecycleCallback( any( Runnable.class ) );
	}

	@Test
	void waitsForEntityAndEveryCollectionParticipant() {
		final var coordinator = new OwnerUpdateCompletionCoordinator( session );
		coordinator.registerEntityMutation( owner, persister );
		coordinator.registerCollectionMutation( owner, false );
		coordinator.registerCollectionMutation( owner, false );
		coordinator.seal();

		coordinator.entityMutationCompleted( owner, () -> {} );
		coordinator.collectionMutationCompleted( owner, () -> {} );
		verify( callbacks, never() ).postUpdate( owner );

		coordinator.collectionMutationCompleted( owner, () -> {} );
		verify( callbacks ).postUpdate( owner );
	}

	@Test
	void failureSuppressesPostUpdate() {
		final var coordinator = new OwnerUpdateCompletionCoordinator( session );
		coordinator.registerEntityMutation( owner, persister );
		coordinator.registerCollectionMutation( owner, false );
		coordinator.seal();

		coordinator.entityMutationCompleted( owner, () -> {} );
		coordinator.mutationFailed( owner );
		coordinator.collectionMutationCompleted( owner, () -> {} );

		verify( callbacks, never() ).postUpdate( owner );
	}

	@Test
	void collectionWorkDoesNotEstablishUpdateApplicability() {
		final var coordinator = new OwnerUpdateCompletionCoordinator( session );
		coordinator.registerCollectionMutation( owner, false );
		coordinator.seal();

		verify( callbacks, never() ).postUpdate( owner );
	}

	@Test
	void inverseCollectionDoesNotParticipate() {
		final var coordinator = new OwnerUpdateCompletionCoordinator( session );
		coordinator.registerEntityMutation( owner, persister );
		coordinator.registerCollectionMutation( owner, true );
		coordinator.seal();
		coordinator.entityMutationCompleted( owner, () -> {} );

		verify( callbacks ).postUpdate( owner );
	}

	@Test
	void runsSuccessfulCompletionHandlersAfterOwnerPostUpdate() {
		final var order = new ArrayList<String>();
		doAnswer( invocation -> {
			order.add( "post-update" );
			return null;
		} ).when( callbacks ).postUpdate( owner );

		final var coordinator = new OwnerUpdateCompletionCoordinator( session );
		coordinator.registerEntityMutation( owner, persister );
		coordinator.registerCollectionMutation( owner, false );
		coordinator.seal();

		coordinator.entityMutationCompleted( owner, () -> order.add( "entity-statistics" ) );
		coordinator.collectionMutationCompleted( owner, () -> order.add( "collection-statistics" ) );

		org.assertj.core.api.Assertions.assertThat( order ).containsExactly(
				"post-update",
				"entity-statistics",
				"collection-statistics"
		);
	}
}
