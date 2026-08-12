/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cascade.internal;

import java.util.List;
import java.util.Set;

import org.hibernate.collection.spi.CollectionBaseline;
import org.hibernate.collection.spi.CollectionChange;
import org.hibernate.collection.spi.CollectionDelta;
import org.hibernate.collection.spi.CollectionMutationInterpretation;
import org.hibernate.collection.spi.DeltaCoverage;
import org.hibernate.collection.spi.DeltaSource;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.collection.spi.PhysicalCollectionMutation;
import org.hibernate.collection.spi.SemanticCollectionChange;
import org.hibernate.engine.internal.FlushProcessingContext;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.event.spi.EventSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Tests cascade orphan discovery against a valid flush-time collection delta.
///
/// @author Steve Ebersole
class CascadeCollectionDeltaOrphanTest {
	@Test
	void reusesValidFlushDeltaInsteadOfRepeatingLoadedStateComparison() {
		final var fixture = fixture();
		final var orphan = new Object();
		when( fixture.collection().getMutationGeneration() ).thenReturn( 1L );
		fixture.flushContext().retainCollectionInterpretation(
				fixture.collection(),
				interpretation( orphan, 1L )
		);

		assertEquals(
				List.of( orphan ),
				List.copyOf( Cascade.getOrphans( fixture.session(), "Child", fixture.collection() ) )
		);
		verify( fixture.collectionEntry(), never() ).getOrphans( "Child", fixture.collection() );
	}

	@Test
	void staleFlushDeltaFallsBackToStandaloneLoadedStateComparison() {
		final var fixture = fixture();
		final var staleOrphan = new Object();
		final var currentOrphan = new Object();
		when( fixture.collection().getMutationGeneration() ).thenReturn( 2L );
		when( fixture.collection().wasInitialized() ).thenReturn( true );
		doReturn( List.of( currentOrphan ) )
				.when( fixture.collectionEntry() ).getOrphans( "Child", fixture.collection() );
		fixture.flushContext().retainCollectionInterpretation(
				fixture.collection(),
				interpretation( staleOrphan, 1L )
		);

		assertEquals(
				List.of( currentOrphan ),
				List.copyOf( Cascade.getOrphans( fixture.session(), "Child", fixture.collection() ) )
		);
		verify( fixture.collectionEntry() ).getOrphans( "Child", fixture.collection() );
	}

	@Test
	void absentFlushDeltaUsesQueuedOrphansWithoutInitialization() {
		final var session = mock( EventSource.class );
		final var persistenceContext = mock( PersistenceContext.class );
		final var collection = mock( PersistentCollection.class );
		final var orphan = new Object();
		when( session.getPersistenceContextInternal() ).thenReturn( persistenceContext );
		when( collection.wasInitialized() ).thenReturn( false );
		doReturn( List.of( orphan ) ).when( collection ).getQueuedOrphans( "Child" );

		assertEquals( List.of( orphan ), List.copyOf( Cascade.getOrphans( session, "Child", collection ) ) );
		verify( collection, never() ).forceInitialization();
	}

	private static CollectionMutationInterpretation interpretation(Object orphan, long generation) {
		return new CollectionMutationInterpretation(
				org.hibernate.action.queue.spi.CollectionTransition.UPDATE,
				new SemanticCollectionChange.Delta( new CollectionDelta(
						CollectionBaseline.LOADED,
						DeltaCoverage.COMPLETE,
						List.of( new CollectionChange.Removal( orphan, null ) ),
						Set.of( DeltaSource.SNAPSHOT_COMPARISON )
				) ),
				PhysicalCollectionMutation.noWork(),
				generation
		);
	}

	private static Fixture fixture() {
		final var session = mock( EventSource.class );
		final var persistenceContext = mock( PersistenceContext.class );
		final var collection = mock( PersistentCollection.class );
		final var collectionEntry = mock( CollectionEntry.class );
		final var flushContext = new FlushProcessingContext( session );
		when( session.getPersistenceContextInternal() ).thenReturn( persistenceContext );
		when( persistenceContext.getCollectionFlushActionTracker() ).thenReturn( flushContext );
		when( persistenceContext.getCollectionEntry( collection ) ).thenReturn( collectionEntry );
		return new Fixture( session, collection, collectionEntry, flushContext );
	}

	private record Fixture(
			EventSource session,
			PersistentCollection<?> collection,
			CollectionEntry collectionEntry,
			FlushProcessingContext flushContext) {
	}
}
