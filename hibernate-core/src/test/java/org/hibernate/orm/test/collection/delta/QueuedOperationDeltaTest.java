/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.delta;

import java.util.List;
import java.util.Set;

import org.hibernate.collection.internal.StandardBagSemantics;
import org.hibernate.collection.internal.StandardListSemantics;
import org.hibernate.collection.internal.StandardMapSemantics;
import org.hibernate.collection.internal.StandardSetSemantics;
import org.hibernate.collection.spi.CollectionBaseline;
import org.hibernate.collection.spi.CollectionChange;
import org.hibernate.collection.spi.CollectionDelta;
import org.hibernate.collection.spi.CollectionDeltaProducer;
import org.hibernate.collection.spi.CollectionDeltaProduction;
import org.hibernate.collection.spi.CollectionDeltaProductionContext;
import org.hibernate.collection.spi.DeferredCollectionPosition;
import org.hibernate.collection.spi.DeltaCoverage;
import org.hibernate.collection.spi.DeltaSource;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.collection.spi.QueuedCollectionOperation;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.junit.jupiter.api.Test;

import static org.hibernate.collection.spi.QueuedCollectionOperation.Kind.ADD;
import static org.hibernate.collection.spi.QueuedCollectionOperation.Kind.CLEAR;
import static org.hibernate.collection.spi.QueuedCollectionOperation.Kind.PUT;
import static org.hibernate.collection.spi.QueuedCollectionOperation.Kind.REMOVE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Tests normalization of an immutable queued-command view into a collection delta.
///
/// @author Steve Ebersole
public class QueuedOperationDeltaTest {
	@Test
	void unindexedCommandsRetainOrderAndClearBarrier() {
		final var delta = produce(
				StandardBagSemantics.INSTANCE.getCollectionDeltaProducer(),
				List.of(
						operation( ADD, "before-clear", null, null, 0 ),
						operation( CLEAR, null, null, null, 1 ),
						operation( ADD, "after-clear", null, null, 2 ),
						operation( REMOVE, null, "removed", null, 3 )
				)
		);

		assertPartialQueuedDelta( delta );
		assertEquals( List.of(
				new CollectionChange.Addition( "before-clear", null ),
				new CollectionChange.Clear(),
				new CollectionChange.Addition( "after-clear", null ),
				new CollectionChange.Removal( "removed", null )
		), delta.changes() );
	}

	@Test
	void setUsesItsSemanticsOwnedProducer() {
		final var delta = produce(
				StandardSetSemantics.INSTANCE.getCollectionDeltaProducer(),
				List.of( operation( ADD, "added", null, null, 0 ) )
		);

		assertPartialQueuedDelta( delta );
		assertEquals( List.of( new CollectionChange.Addition( "added", null ) ), delta.changes() );
	}

	@Test
	void mapCommandsRetainKnownKeysAndOldValues() {
		final var delta = produce(
				StandardMapSemantics.INSTANCE.getCollectionDeltaProducer(),
				List.of(
						operation( PUT, "new", "old", "key", 0 ),
						operation( REMOVE, null, "removed", "other-key", 1 )
				)
		);

		assertEquals( List.of(
				new CollectionChange.Replacement( "old", "new", "key", "key" ),
				new CollectionChange.Removal( "removed", "other-key" )
		), delta.changes() );
	}

	@Test
	void listAppendsShareOnePersistedSizeHandle() {
		final var delta = produce(
				StandardListSemantics.INSTANCE.getCollectionDeltaProducer(),
				List.of(
						operation( ADD, "first", null, null, 0 ),
						operation( ADD, "second", null, null, 1 )
				)
		);

		final var first = assertInstanceOf( CollectionChange.Addition.class, delta.changes().get( 0 ) );
		final var second = assertInstanceOf( CollectionChange.Addition.class, delta.changes().get( 1 ) );
		final var firstPosition = assertInstanceOf( DeferredCollectionPosition.class, first.currentPosition() );
		final var secondPosition = assertInstanceOf( DeferredCollectionPosition.class, second.currentPosition() );

		assertSame( firstPosition.persistedSize(), secondPosition.persistedSize() );
		assertFalse( firstPosition.persistedSize().isResolved() );
		firstPosition.persistedSize().resolve( 3 );
		assertTrue( secondPosition.persistedSize().isResolved() );
		assertEquals( 3, firstPosition.resolve() );
		assertEquals( 4, secondPosition.resolve() );
	}

	@Test
	void clearMakesFollowingAppendPositionAbsolute() {
		final var delta = produce(
				StandardListSemantics.INSTANCE.getCollectionDeltaProducer(),
				List.of(
						operation( CLEAR, null, null, null, 0 ),
						operation( ADD, "first", null, null, 1 ),
						operation( ADD, "second", null, null, 2 )
				)
		);

		assertEquals( List.of(
				new CollectionChange.Clear(),
				new CollectionChange.Addition( "first", 0 ),
				new CollectionChange.Addition( "second", 1 )
		), delta.changes() );
	}

	private static void assertPartialQueuedDelta(CollectionDelta delta) {
		assertEquals( CollectionBaseline.UNINITIALIZED, delta.baseline() );
		assertEquals( DeltaCoverage.EXPLICIT_CHANGES_ONLY, delta.coverage() );
		assertEquals( Set.of( DeltaSource.QUEUED_OPERATION_LOG ), delta.sources() );
	}

	private static CollectionDelta produce(
			CollectionDeltaProducer producer,
			List<QueuedCollectionOperation> operations) {
		final var collection = mock( PersistentCollection.class );
		when( collection.wasInitialized() ).thenReturn( false );
		when( collection.hasQueuedOperations() ).thenReturn( true );
		when( collection.getQueuedOperations() ).thenReturn( operations );
		final var production = producer.produceDelta( new CollectionDeltaProductionContext(
				collection,
				mock( CollectionPersister.class ),
				CollectionBaseline.UNINITIALIZED,
				mock( SharedSessionContractImplementor.class )
		) );
		return assertInstanceOf( CollectionDeltaProduction.Produced.class, production ).delta();
	}

	private static QueuedCollectionOperation operation(
			QueuedCollectionOperation.Kind kind,
			Object addedValue,
			Object orphan,
			Object position,
			int order) {
		return new QueuedCollectionOperation( kind, addedValue, orphan, position, order );
	}
}
