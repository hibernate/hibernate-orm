/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.delta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.hibernate.action.queue.spi.CollectionTransition;
import org.hibernate.collection.internal.StandardArraySemantics;
import org.hibernate.collection.internal.StandardBagSemantics;
import org.hibernate.collection.internal.StandardIdentifierBagSemantics;
import org.hibernate.collection.internal.StandardListSemantics;
import org.hibernate.collection.internal.StandardMapSemantics;
import org.hibernate.collection.internal.StandardSetSemantics;
import org.hibernate.collection.internal.StandardSortedMapSemantics;
import org.hibernate.collection.internal.StandardSortedSetSemantics;
import org.hibernate.collection.spi.CollectionBaseline;
import org.hibernate.collection.spi.CollectionChangeSet;
import org.hibernate.collection.spi.CollectionInterpretationContext;
import org.hibernate.collection.spi.CollectionInterpretationProduction;
import org.hibernate.collection.spi.CollectionMutationInterpreter;
import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.collection.spi.PhysicalCollectionMutation;
import org.hibernate.collection.spi.SemanticCollectionChange;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Contract tests for semantics-owned collection mutation interpretation.
///
/// @author Steve Ebersole
public class CollectionMutationInterpreterContractTest {
	@Test
	void defaultInterpreterIsShared() {
		final var first = mock( CollectionSemantics.class, CALLS_REAL_METHODS );
		final var second = mock( CollectionSemantics.class, CALLS_REAL_METHODS );

		assertSame( first.getCollectionMutationInterpreter(), second.getCollectionMutationInterpreter() );
	}

	@ParameterizedTest
	@MethodSource("standardSemantics")
	void standardSemanticsUseCachedSpecializedInterpreter(CollectionSemantics<?, ?> semantics) {
		final var interpreter = semantics.getCollectionMutationInterpreter();

		assertSame( interpreter, semantics.getCollectionMutationInterpreter() );
		assertNotSame( CollectionMutationInterpreter.legacyCompatible(), interpreter );
	}

	@Test
	void createRetainsCurrentRowsWithoutSemanticAdditions() {
		final var collection = mock( PersistentCollection.class );
		final var persister = persister();
		when( collection.wasInitialized() ).thenReturn( true );
		when( collection.entries( persister ) ).thenReturn( List.of( "first", "second" ).iterator() );
		when( collection.includeInRecreate( any(), anyInt(), any(), any() ) ).thenReturn( true );

		final var interpretation = produced( StandardListSemantics.INSTANCE.getCollectionMutationInterpreter()
				.interpret( context( collection, persister, CollectionTransition.CREATE, CollectionBaseline.EMPTY ) ) );

		assertInstanceOf( SemanticCollectionChange.None.class, interpretation.semanticChange() );
		final var createAll = assertInstanceOf(
				PhysicalCollectionMutation.CreateAll.class,
				interpretation.physicalMutation()
		);
		final var rows = new ArrayList<Object>();
		createAll.currentRows().forEach( (entry, position) -> rows.add( entry ) );
		assertSame( "first", rows.get( 0 ) );
		assertSame( "second", rows.get( 1 ) );
		verify( collection, never() ).getChangeSet( persister );
	}

	@Test
	void indexedUpdateRetainsOriginalChangeSet() {
		final var collection = mock( PersistentCollection.class );
		final var persister = persister();
		final var changes = new CollectionChangeSet(
				List.of(),
				List.of( new CollectionChangeSet.Addition( "added", 1 ) ),
				List.of(),
				List.of()
		);
		when( collection.wasInitialized() ).thenReturn( true );
		when( collection.getChangeSet( persister ) ).thenReturn( changes );

		final var interpretation = produced( StandardListSemantics.INSTANCE.getCollectionMutationInterpreter()
				.interpret( context( collection, persister, CollectionTransition.UPDATE, CollectionBaseline.LOADED ) ) );

		final var rowChanges = assertInstanceOf(
				PhysicalCollectionMutation.RowChanges.class,
				interpretation.physicalMutation()
		);
		assertSame( changes, rowChanges.changes() );
		assertInstanceOf( SemanticCollectionChange.None.class, interpretation.semanticChange() );
	}

	@Test
	void identifierBagUpdateProducesPhysicalRowChangesDirectly() {
		final var collection = mock( PersistentCollection.class );
		final var persister = persister();
		when( collection.wasInitialized() ).thenReturn( true );
		when( collection.getStoredSnapshot() ).thenReturn( new HashMap<>( Map.of( 1L, "removed" ) ) );
		when( collection.getDeletes( persister, true ) ).thenReturn( List.of( 1L ).iterator() );
		when( collection.entries( persister ) ).thenReturn( List.of( "added" ).iterator() );
		when( collection.getIdentifier( "added", 0 ) ).thenReturn( null );
		when( collection.includeInInsert( any(), anyInt(), any(), any() ) ).thenReturn( true );
		when( collection.getElement( "added" ) ).thenReturn( "added" );

		final var interpretation = produced( StandardIdentifierBagSemantics.INSTANCE
				.getCollectionMutationInterpreter()
				.interpret( context(
						collection,
						persister,
						CollectionTransition.UPDATE,
						CollectionBaseline.LOADED
				) ) );

		final var rowChanges = assertInstanceOf(
				PhysicalCollectionMutation.RowChanges.class,
				interpretation.physicalMutation()
		);
		assertSame( "removed", rowChanges.changes().removals().get( 0 ).element() );
		assertSame( "added", rowChanges.changes().additions().get( 0 ).element() );
		assertInstanceOf(
				org.hibernate.collection.spi.DeferredCollectionIdentifier.class,
				rowChanges.changes().additions().get( 0 ).index()
		);
	}

	@Test
	void defaultInterpreterRequestsInitializationForUninitializedCustomWrapper() {
		final var collection = mock( PersistentCollection.class );
		when( collection.wasInitialized() ).thenReturn( false );

		final var production = CollectionMutationInterpreter.legacyCompatible().interpret(
				context( collection, persister(), CollectionTransition.UPDATE, CollectionBaseline.LOADED )
		);

		assertInstanceOf( CollectionInterpretationProduction.InitializationRequired.class, production );
	}

	private static org.hibernate.collection.spi.CollectionMutationInterpretation produced(
			CollectionInterpretationProduction production) {
		return assertInstanceOf( CollectionInterpretationProduction.Produced.class, production ).interpretation();
	}

	private static CollectionInterpretationContext context(
			PersistentCollection<?> collection,
			CollectionPersister persister,
			CollectionTransition transition,
			CollectionBaseline baseline) {
		return new CollectionInterpretationContext(
				collection,
				persister,
				transition,
				baseline,
				false,
				false,
				false,
				mock( SharedSessionContractImplementor.class )
		);
	}

	private static CollectionPersister persister() {
		final var persister = mock( CollectionPersister.class );
		when( persister.getAttributeMapping() ).thenReturn( mock( PluralAttributeMapping.class ) );
		return persister;
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
}
