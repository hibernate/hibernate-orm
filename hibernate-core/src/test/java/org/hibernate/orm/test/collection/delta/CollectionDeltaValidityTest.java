/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.delta;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.action.queue.spi.CollectionTransition;
import org.hibernate.collection.spi.CollectionBaseline;
import org.hibernate.collection.spi.CollectionChange;
import org.hibernate.collection.spi.CollectionDelta;
import org.hibernate.collection.spi.CollectionMutationInterpretation;
import org.hibernate.collection.spi.FrozenCollectionRows;
import org.hibernate.collection.spi.PhysicalCollectionMutation;
import org.hibernate.collection.spi.SemanticCollectionChange;
import org.hibernate.collection.spi.DeltaCoverage;
import org.hibernate.collection.spi.DeltaSource;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.internal.FlushProcessingContext;
import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.collection.CollectionPersister;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Tests validity-gated retention and orphan projection for frozen deltas.
///
/// @author Steve Ebersole
public class CollectionDeltaValidityTest {
	@Test
	void mutationGenerationInvalidatesRetainedDelta() {
		final var collection = mock( PersistentCollection.class );
		when( collection.getMutationGeneration() ).thenReturn( 3L );
		final var interpretation = new CollectionMutationInterpretation(
				CollectionTransition.UPDATE,
				new SemanticCollectionChange.Delta( delta() ),
				PhysicalCollectionMutation.noWork(),
				collection.getMutationGeneration()
		);
		final var context = new FlushProcessingContext( mock( EventSource.class ) );
		context.retainCollectionInterpretation( collection, interpretation );

		assertNotNull( context.getValidCollectionInterpretation( collection ) );

		when( collection.getMutationGeneration() ).thenReturn( 4L );

		assertNull( context.getValidCollectionInterpretation( collection ) );
	}

	@Test
	void orphanProjectionContainsOnlyKnownRemovedState() {
		assertEquals( List.of( "removed", "replaced" ), delta().orphanCandidates() );
	}

	@Test
	void segmentedRowsPreserveOrderAcrossBoundaries() {
		final var collection = mock( PersistentCollection.class );
		final var persister = mock( CollectionPersister.class );
		final var sourceEntries = new ArrayList<Integer>();
		for ( int index = 0; index < 257; index++ ) {
			sourceEntries.add( index );
		}
		when( collection.getMutationGeneration() ).thenReturn( 3L );
		when( collection.entries( persister ) ).thenReturn( sourceEntries.iterator() );
		when( collection.includeInRecreate( any(), anyInt(), any(), any() ) ).thenReturn( true );

		final FrozenCollectionRows rows = FrozenCollectionRows.from( collection, persister );

		assertEquals( 257, rows.size() );
		for ( int row = 0; row < rows.size(); row++ ) {
			assertEquals( row, rows.entry( row ) );
			assertEquals( row, rows.position( row ) );
		}

		final var visited = new ArrayList<Integer>();
		rows.forEach( (entry, position) -> {
			assertEquals( entry, position );
			visited.add( (Integer) entry );
		} );
		assertEquals( sourceEntries, visited );

		when( collection.getMutationGeneration() ).thenReturn( 4L );
		assertThrows( IllegalStateException.class, rows::size );
	}

	private static CollectionDelta delta() {
		return new CollectionDelta(
				CollectionBaseline.LOADED,
				DeltaCoverage.COMPLETE,
				List.of(
						new CollectionChange.Removal( "removed", 0 ),
						new CollectionChange.Addition( "added", 1 ),
						new CollectionChange.Replacement( "replaced", "replacement", 2, 2 ),
						new CollectionChange.PositionChange( "shifted", 3, 4 )
				),
				Set.of( DeltaSource.SNAPSHOT_COMPARISON )
		);
	}
}
