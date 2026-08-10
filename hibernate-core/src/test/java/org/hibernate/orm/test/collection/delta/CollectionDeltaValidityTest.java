/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.delta;

import java.util.List;
import java.util.Set;

import org.hibernate.action.queue.internal.FrozenCollectionDelta;
import org.hibernate.collection.spi.CollectionBaseline;
import org.hibernate.collection.spi.CollectionChange;
import org.hibernate.collection.spi.CollectionDelta;
import org.hibernate.collection.spi.DeltaCoverage;
import org.hibernate.collection.spi.DeltaSource;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.internal.FlushProcessingContext;
import org.hibernate.event.spi.EventSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
		final var frozen = FrozenCollectionDelta.freeze( delta(), collection );
		final var context = new FlushProcessingContext( mock( EventSource.class ) );
		context.retainFrozenDelta( collection, frozen );

		assertNotNull( context.getValidFrozenDelta( collection ) );

		when( collection.getMutationGeneration() ).thenReturn( 4L );

		assertNull( context.getValidFrozenDelta( collection ) );
	}

	@Test
	void orphanProjectionContainsOnlyKnownRemovedState() {
		assertEquals( List.of( "removed", "replaced" ), delta().orphanCandidates() );
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
