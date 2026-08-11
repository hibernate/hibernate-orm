/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.delta;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.hibernate.action.queue.internal.CollectionMutationPreparer;
import org.hibernate.action.queue.spi.CollectionEndpoint;
import org.hibernate.action.queue.spi.CollectionMutationInput;
import org.hibernate.action.queue.spi.CollectionTransition;
import org.hibernate.collection.spi.CollectionBaseline;
import org.hibernate.collection.spi.CollectionChange;
import org.hibernate.collection.spi.CollectionDelta;
import org.hibernate.collection.spi.CollectionInterpretationProduction;
import org.hibernate.collection.spi.CollectionMutationInterpretation;
import org.hibernate.collection.spi.CollectionMutationInterpreter;
import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.collection.spi.DeltaCoverage;
import org.hibernate.collection.spi.DeltaSource;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.collection.spi.PhysicalCollectionMutation;
import org.hibernate.collection.spi.SemanticCollectionChange;
import org.hibernate.engine.internal.FlushProcessingContext;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.collection.CollectionPersister;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Tests custom interpreter selection through shared collection mutation preparation.
///
/// @author Steve Ebersole
public class CustomCollectionMutationInterpreterTest {
	@Test
	void specializedInterpreterCanDescribeUninitializedStateWithoutInitialization() {
		final var expectedDelta = new CollectionDelta(
				CollectionBaseline.UNINITIALIZED,
				DeltaCoverage.EXPLICIT_CHANGES_ONLY,
				List.of( new CollectionChange.Addition( "queued", null ) ),
				Set.of( DeltaSource.QUEUED_OPERATION_LOG )
		);
		final var observedBaseline = new AtomicReference<CollectionBaseline>();
		final CollectionMutationInterpreter interpreter = context -> {
			observedBaseline.set( context.baseline() );
			return CollectionInterpretationProduction.produced( new CollectionMutationInterpretation(
					context.transition(),
					new SemanticCollectionChange.Delta( expectedDelta ),
					PhysicalCollectionMutation.noWork(),
					context.collection().getMutationGeneration()
			) );
		};
		final var fixture = fixture( interpreter );
		when( fixture.collection().wasInitialized() ).thenReturn( false );

		final var prepared = CollectionMutationPreparer.prepare( fixture.input(), fixture.session() );

		assertEquals( CollectionBaseline.UNINITIALIZED, observedBaseline.get() );
		assertSame( expectedDelta, ( (SemanticCollectionChange.Delta)
				prepared.get( 0 ).interpretation().semanticChange() ).delta() );
		assertSame(
				prepared.get( 0 ).interpretation(),
				fixture.flushContext().getValidCollectionInterpretation( fixture.collection() )
		);
		verify( fixture.collection(), never() ).forceInitialization();
	}

	private static Fixture fixture(CollectionMutationInterpreter interpreter) {
		final var collection = mock( PersistentCollection.class );
		final var semantics = mock( CollectionSemantics.class, CALLS_REAL_METHODS );
		final var persister = mock( CollectionPersister.class );
		final var session = mock( EventSource.class );
		final var persistenceContext = mock( PersistenceContext.class );
		final var flushContext = new FlushProcessingContext( session );
		when( semantics.getCollectionMutationInterpreter() ).thenReturn( interpreter );
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
