/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.collection.spi;

import org.hibernate.Incubating;
import org.hibernate.persister.collection.CollectionPersister;

/// A validity-guarded projection of the current rows of a persistent collection.
///
/// The projection uses immutable segmented parallel arrays instead of allocating a
/// second object for every collection row. Segments let production retain a single
/// pass over the live collection without repeatedly copying a growing array. It
/// retains the collection comparison generation and enumerates each included row
/// directly into the physical consumer. Access fails if the collection changes after
/// the projection is created.
///
/// @since 8.0
/// @author Steve Ebersole
@Incubating
public final class FrozenCollectionRows {
	private static final int INITIAL_SEGMENT_CAPACITY = 16;
	private static final int MAX_SEGMENT_CAPACITY = 64;

	private final PersistentCollection<?> collection;
	private final long mutationGeneration;
	private final Object[][] entrySegments;
	private final int[][] positionSegments;
	private final int size;

	private FrozenCollectionRows(
			PersistentCollection<?> collection,
			long mutationGeneration,
			Object[][] entrySegments,
			int[][] positionSegments,
			int size) {
		this.collection = collection;
		this.mutationGeneration = mutationGeneration;
		this.entrySegments = entrySegments;
		this.positionSegments = positionSegments;
		this.size = size;
	}

	public static FrozenCollectionRows from(
			PersistentCollection<?> collection,
			CollectionPersister persister) {
		final long mutationGeneration = collection.getMutationGeneration();
		verifyValidity( collection, mutationGeneration );
		Object[][] entrySegments = new Object[4][];
		int[][] positionSegments = new int[4][];
		int segment = -1;
		int segmentPosition = 0;
		int size = 0;
		final var iterator = collection.entries( persister );
		int position = 0;
		while ( iterator.hasNext() ) {
			final Object entry = iterator.next();
			if ( collection.includeInRecreate(
					entry,
					position,
					collection,
					persister.getAttributeMapping() ) ) {
				if ( segment < 0 || segmentPosition == entrySegments[segment].length ) {
					segment++;
					if ( segment == entrySegments.length ) {
						entrySegments = growSegments( entrySegments );
						positionSegments = growSegments( positionSegments );
					}
					final int segmentCapacity = segmentCapacity( segment );
					entrySegments[segment] = new Object[segmentCapacity];
					positionSegments[segment] = new int[segmentCapacity];
					segmentPosition = 0;
				}
				entrySegments[segment][segmentPosition] = entry;
				positionSegments[segment][segmentPosition] = position;
				segmentPosition++;
				size++;
			}
			position++;
		}
		verifyValidity( collection, mutationGeneration );
		return new FrozenCollectionRows(
				collection,
				mutationGeneration,
				entrySegments,
				positionSegments,
				size
		);
	}

	public int size() {
		verifyValidity();
		return size;
	}

	public Object entry(int row) {
		verifyRow( row );
		return entrySegments[segment( row )][segmentPosition( row )];
	}

	public int position(int row) {
		verifyRow( row );
		return positionSegments[segment( row )][segmentPosition( row )];
	}

	public void forEach(RowConsumer consumer) {
		verifyValidity();
		int remaining = size;
		for ( int segment = 0; remaining > 0; segment++ ) {
			final Object[] entries = entrySegments[segment];
			final int[] positions = positionSegments[segment];
			final int segmentSize = Math.min( remaining, entries.length );
			for ( int row = 0; row < segmentSize; row++ ) {
				consumer.accept( entries[row], positions[row] );
			}
			remaining -= segmentSize;
		}
		verifyValidity();
	}

	@FunctionalInterface
	public interface RowConsumer {
		void accept(Object entry, int position);
	}

	private static Object[][] growSegments(Object[][] segments) {
		final Object[][] grown = new Object[segments.length * 2][];
		System.arraycopy( segments, 0, grown, 0, segments.length );
		return grown;
	}

	private static int[][] growSegments(int[][] segments) {
		final int[][] grown = new int[segments.length * 2][];
		System.arraycopy( segments, 0, grown, 0, segments.length );
		return grown;
	}

	private static int segmentCapacity(int segment) {
		return segment < 2 ? INITIAL_SEGMENT_CAPACITY << segment : MAX_SEGMENT_CAPACITY;
	}

	private static int segment(int row) {
		if ( row < INITIAL_SEGMENT_CAPACITY ) {
			return 0;
		}
		if ( row < INITIAL_SEGMENT_CAPACITY * 3 ) {
			return 1;
		}
		return 2 + (row - INITIAL_SEGMENT_CAPACITY * 3) / MAX_SEGMENT_CAPACITY;
	}

	private static int segmentPosition(int row) {
		if ( row < INITIAL_SEGMENT_CAPACITY ) {
			return row;
		}
		if ( row < INITIAL_SEGMENT_CAPACITY * 3 ) {
			return row - INITIAL_SEGMENT_CAPACITY;
		}
		return (row - INITIAL_SEGMENT_CAPACITY * 3) % MAX_SEGMENT_CAPACITY;
	}

	private void verifyRow(int row) {
		verifyValidity();
		if ( row < 0 || row >= size ) {
			throw new IndexOutOfBoundsException( row );
		}
	}

	private void verifyValidity() {
		verifyValidity( collection, mutationGeneration );
	}

	private static void verifyValidity(PersistentCollection<?> collection, long mutationGeneration) {
		if ( collection.getMutationGeneration() != mutationGeneration ) {
			throw new IllegalStateException( "Collection changed after its rows were frozen" );
		}
	}
}
