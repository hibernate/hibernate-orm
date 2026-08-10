/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.delta;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.hibernate.collection.internal.StandardArraySemantics;
import org.hibernate.collection.internal.StandardBagSemantics;
import org.hibernate.collection.spi.CollectionBaseline;
import org.hibernate.collection.spi.CollectionChange;
import org.hibernate.collection.spi.CollectionChangeSet;
import org.hibernate.collection.spi.CollectionDelta;
import org.hibernate.collection.spi.CollectionDeltaProducer;
import org.hibernate.collection.spi.CollectionDeltaProduction;
import org.hibernate.collection.spi.CollectionDeltaProductionContext;
import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.collection.internal.StandardIdentifierBagSemantics;
import org.hibernate.collection.internal.StandardListSemantics;
import org.hibernate.collection.internal.StandardMapSemantics;
import org.hibernate.collection.internal.StandardSetSemantics;
import org.hibernate.collection.internal.StandardSortedMapSemantics;
import org.hibernate.collection.internal.StandardSortedSetSemantics;
import org.hibernate.collection.spi.DeferredCollectionIdentifier;
import org.hibernate.collection.spi.DeltaCoverage;
import org.hibernate.collection.spi.DeltaSource;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.collection.spi.PersistentIdentifierBag;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Contract tests for the conservative semantics-owned collection delta producer.
///
/// @author Steve Ebersole
public class CollectionDeltaProducerContractTest {
	@Test
	void defaultProducerIsShared() {
		final var first = mock( CollectionSemantics.class, CALLS_REAL_METHODS );
		final var second = mock( CollectionSemantics.class, CALLS_REAL_METHODS );

		assertSame( first.getCollectionDeltaProducer(), second.getCollectionDeltaProducer() );
	}

	@ParameterizedTest
	@MethodSource("standardSemantics")
	void standardSemanticsUseCachedSpecializedProducer(CollectionSemantics<?, ?> semantics) {
		final var producer = semantics.getCollectionDeltaProducer();

		assertSame( producer, semantics.getCollectionDeltaProducer() );
		assertNotSame( CollectionDeltaProducer.legacyCompatible(), producer );
	}

	@ParameterizedTest
	@MethodSource("standardSnapshotSemantics")
	void initializedStandardSemanticsProduceCompleteDelta(CollectionSemantics<?, ?> semantics) {
		final var collection = mock( PersistentCollection.class );
		final var persister = mock( CollectionPersister.class );
		when( collection.wasInitialized() ).thenReturn( true );
		when( collection.getChangeSet( persister ) ).thenReturn( new CollectionChangeSet(
				List.of(),
				List.of( new CollectionChangeSet.Addition( "added", 1 ) ),
				List.of(),
				List.of()
		) );

		final var produced = assertInstanceOf(
				CollectionDeltaProduction.Produced.class,
				semantics.getCollectionDeltaProducer()
						.produceDelta( context( collection, persister, CollectionBaseline.LOADED ) )
		);

		assertEquals( DeltaCoverage.COMPLETE, produced.delta().coverage() );
		assertEquals(
				List.of( new CollectionChange.Addition( "added", 1 ) ),
				produced.delta().changes()
		);
	}

	@Test
	void requestsInitializationForUninitializedCustomWrapper() {
		final var collection = mock( PersistentCollection.class );
		when( collection.wasInitialized() ).thenReturn( false );

		final var production = producer().produceDelta( context( collection, CollectionBaseline.LOADED ) );

		assertInstanceOf( CollectionDeltaProduction.InitializationRequired.class, production );
	}

	@Test
	void adaptsExistingInitializedChangeSet() {
		final var collection = mock( PersistentCollection.class );
		final var persister = mock( CollectionPersister.class );
		when( collection.wasInitialized() ).thenReturn( true );
		when( collection.getChangeSet( persister ) ).thenReturn( new CollectionChangeSet(
				List.of( new CollectionChangeSet.Removal( "removed", 1 ) ),
				List.of( new CollectionChangeSet.Addition( "added", 2 ) ),
				List.of( new CollectionChangeSet.Shift( "shifted", 3, 4 ) ),
				List.of( new CollectionChangeSet.ValueChange( "old", "new", 5 ) )
		) );

		final var produced = assertInstanceOf(
				CollectionDeltaProduction.Produced.class,
				producer().produceDelta( context( collection, persister, CollectionBaseline.LOADED ) )
		);

		assertEquals( CollectionBaseline.LOADED, produced.delta().baseline() );
		assertEquals( DeltaCoverage.COMPLETE, produced.delta().coverage() );
		assertEquals( Set.of( DeltaSource.SNAPSHOT_COMPARISON ), produced.delta().sources() );
		assertEquals( List.of(
				new CollectionChange.Removal( "removed", 1 ),
				new CollectionChange.Addition( "added", 2 ),
				new CollectionChange.PositionChange( "shifted", 3, 4 ),
				new CollectionChange.Replacement( "old", "new", 5, 5 )
		), produced.delta().changes() );
	}

	@Test
	void deltaMakesDefensiveStructuralCopies() {
		final var mutableChanges = new ArrayList<CollectionChange>();
		mutableChanges.add( new CollectionChange.Addition( "first", 0 ) );
		final var delta = new CollectionDelta(
				CollectionBaseline.EMPTY,
				DeltaCoverage.COMPLETE,
				mutableChanges,
				Set.of( DeltaSource.SNAPSHOT_COMPARISON )
		);

		mutableChanges.add( new CollectionChange.Addition( "second", 1 ) );

		assertEquals( 1, delta.changes().size() );
		assertThrows(
				UnsupportedOperationException.class,
				() -> delta.changes().add( new CollectionChange.Addition( "third", 2 ) )
		);
	}

	@Test
	void identifierBagRetainsRowIdentifiersAndDefersGeneratedOnes() {
		final var existing = new Object();
		final var added = new Object();
		final var collection = mock( PersistentIdentifierBag.class );
		final var persister = mock( CollectionPersister.class );
		final var attributeMapping = mock( PluralAttributeMapping.class );
		when( persister.getAttributeMapping() ).thenReturn( attributeMapping );
		when( collection.wasInitialized() ).thenReturn( true );
		when( collection.getStoredSnapshot() ).thenReturn( new java.util.HashMap<>( java.util.Map.of(
				10, "removed",
				11, "old"
		) ) );
		when( collection.getDeletes( persister, true ) ).thenReturn( List.of( 10 ).iterator() );
		when( collection.entries( persister ) ).thenReturn( List.of( existing, added ).iterator() );
		when( collection.getIdentifier( existing, 0 ) ).thenReturn( 11 );
		when( collection.getIdentifier( added, 1 ) ).thenReturn( null );
		when( collection.getElement( existing ) ).thenReturn( "new" );
		when( collection.getElement( added ) ).thenReturn( "added" );
		when( collection.includeInInsert( any(), anyInt(), any(), any() ) )
				.thenAnswer( invocation -> invocation.getArgument( 0 ) == added );
		when( collection.needsUpdating( any(), anyInt(), any( PluralAttributeMapping.class ) ) )
				.thenAnswer( invocation -> invocation.getArgument( 0 ) == existing );

		final var produced = assertInstanceOf(
				CollectionDeltaProduction.Produced.class,
				StandardIdentifierBagSemantics.INSTANCE.getCollectionDeltaProducer()
						.produceDelta( context( collection, persister, CollectionBaseline.LOADED ) )
		);

		assertEquals( new CollectionChange.Removal( "removed", 10 ), produced.delta().changes().get( 0 ) );
		assertEquals( new CollectionChange.Replacement( "old", "new", 11, 11 ),
				produced.delta().changes().get( 1 ) );
		final var addition = assertInstanceOf(
				CollectionChange.Addition.class,
				produced.delta().changes().get( 2 )
		);
		assertEquals( "added", addition.element() );
		assertInstanceOf( DeferredCollectionIdentifier.class, addition.currentPosition() );
	}

	private static org.hibernate.collection.spi.CollectionDeltaProducer producer() {
		return mock( CollectionSemantics.class, CALLS_REAL_METHODS ).getCollectionDeltaProducer();
	}

	private static Stream<CollectionSemantics<?, ?>> standardSemantics() {
		return Stream.of(
				StandardArraySemantics.INSTANCE,
				StandardBagSemantics.INSTANCE,
				StandardIdentifierBagSemantics.INSTANCE,
				StandardListSemantics.INSTANCE,
				StandardMapSemantics.INSTANCE,
				StandardSetSemantics.INSTANCE,
				StandardSortedMapSemantics.INSTANCE,
				StandardSortedSetSemantics.INSTANCE
		);
	}

	private static Stream<CollectionSemantics<?, ?>> standardSnapshotSemantics() {
		return standardSemantics().filter( semantics -> semantics != StandardIdentifierBagSemantics.INSTANCE );
	}

	private static CollectionDeltaProductionContext context(
			PersistentCollection<?> collection,
			CollectionBaseline baseline) {
		return context( collection, mock( CollectionPersister.class ), baseline );
	}

	private static CollectionDeltaProductionContext context(
			PersistentCollection<?> collection,
			CollectionPersister persister,
			CollectionBaseline baseline) {
		return new CollectionDeltaProductionContext(
				collection,
				persister,
				baseline,
				mock( SharedSessionContractImplementor.class )
		);
	}
}
