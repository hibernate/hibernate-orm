/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.delta;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.hibernate.action.queue.internal.CollectionMutationPreparer;
import org.hibernate.action.queue.spi.CollectionEndpoint;
import org.hibernate.action.queue.spi.CollectionMutationInput;
import org.hibernate.action.queue.spi.CollectionTransition;
import org.hibernate.collection.spi.CollectionBaseline;
import org.hibernate.collection.spi.CollectionChange;
import org.hibernate.collection.spi.CollectionChangeSet;
import org.hibernate.collection.spi.CollectionDelta;
import org.hibernate.collection.spi.CollectionDeltaProducer;
import org.hibernate.collection.spi.CollectionDeltaProduction;
import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.collection.spi.DeltaCoverage;
import org.hibernate.collection.spi.DeltaSource;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.internal.FlushProcessingContext;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.collection.CollectionPersister;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Tests custom-semantics producer selection through shared mutation preparation.
///
/// @author Steve Ebersole
public class CustomCollectionDeltaProducerTest {
	@Test
	void specializedProducerCanDescribeUninitializedStateWithoutInitialization() {
		final var expectedDelta = new CollectionDelta(
				CollectionBaseline.UNINITIALIZED,
				DeltaCoverage.EXPLICIT_CHANGES_ONLY,
				List.of( new CollectionChange.Addition( "queued", null ) ),
				Set.of( DeltaSource.QUEUED_OPERATION_LOG )
		);
		final var observedBaseline = new AtomicReference<CollectionBaseline>();
		final CollectionDeltaProducer producer = context -> {
			observedBaseline.set( context.baseline() );
			return CollectionDeltaProduction.produced( expectedDelta );
		};
		final var fixture = fixture( producer );
		when( fixture.collection().wasInitialized() ).thenReturn( false );

		final var prepared = CollectionMutationPreparer.prepare( fixture.input(), fixture.session() );

		assertEquals( CollectionBaseline.UNINITIALIZED, observedBaseline.get() );
		assertSame( expectedDelta, prepared.get( 0 ).frozenDelta().delta() );
		assertSame(
				prepared.get( 0 ).frozenDelta(),
				fixture.flushContext().getValidFrozenDelta( fixture.collection() )
		);
		verify( fixture.collection(), never() ).forceInitialization();
	}

	@Test
	void conservativeProducerRequestsCoordinatedInitialization() {
		final var initialized = new AtomicBoolean();
		final var fixture = fixture( CollectionDeltaProducer.legacyCompatible() );
		when( fixture.collection().wasInitialized() ).thenAnswer( invocation -> initialized.get() );
		doAnswer( invocation -> {
			initialized.set( true );
			return null;
		} ).when( fixture.collection() ).forceInitialization();
		when( fixture.collection().getChangeSet( fixture.persister() ) )
				.thenReturn( CollectionChangeSet.EMPTY );

		final var prepared = CollectionMutationPreparer.prepare( fixture.input(), fixture.session() );

		assertTrue( initialized.get() );
		verify( fixture.collection() ).forceInitialization();
		assertEquals( CollectionBaseline.LOADED, prepared.get( 0 ).frozenDelta().delta().baseline() );
		assertEquals( DeltaCoverage.COMPLETE, prepared.get( 0 ).frozenDelta().delta().coverage() );
	}

	private static Fixture fixture(CollectionDeltaProducer producer) {
		final var collection = mock( PersistentCollection.class );
		final var semantics = mock( CollectionSemantics.class, CALLS_REAL_METHODS );
		final var persister = mock( CollectionPersister.class );
		final var session = mock( EventSource.class );
		final var persistenceContext = mock( PersistenceContext.class );
		final var flushContext = new FlushProcessingContext( session );
		when( semantics.getCollectionDeltaProducer() ).thenReturn( producer );
		when( persister.getCollectionSemantics() ).thenReturn( semantics );
		when( session.getPersistenceContextInternal() ).thenReturn( persistenceContext );
		when( persistenceContext.getCollectionFlushActionTracker() ).thenReturn( flushContext );
		final var endpoint = new CollectionEndpoint( persister, 1 );
		final var input = new CollectionMutationInput(
				collection,
				CollectionTransition.NONE,
				endpoint,
				null,
				false,
				true
		);
		return new Fixture( collection, persister, session, flushContext, input );
	}

	private record Fixture(
			PersistentCollection<?> collection,
			CollectionPersister persister,
			EventSource session,
			FlushProcessingContext flushContext,
			CollectionMutationInput input) {
	}
}
