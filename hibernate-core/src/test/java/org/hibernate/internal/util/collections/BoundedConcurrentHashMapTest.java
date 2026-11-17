package org.hibernate.internal.util.collections;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("removal")
class BoundedConcurrentHashMapTest {
	@Test
	void removingAnEntryDiscardsOnlyItsQueuedLruHits() {
		final int batchSize = BoundedConcurrentHashMap.EvictionPolicy.MAX_BATCH_SIZE;
		final var map = new BoundedConcurrentHashMap<String, Integer>( batchSize, 1, BoundedConcurrentHashMap.Eviction.LRU );
		final var eviction = map.segments[0].eviction;
		map.put( "keep", 0 );
		map.put( "remove", 0 );
		map.get( "keep" );
		map.get( "remove" );
		map.get( "remove" );
		map.remove( "remove" );

		// One hit remains. Check the threshold before the segment drains the next batch.
		final var nextEntry = eviction.createNewEntry( "next", 0, null, 0 );
		for ( int hits = 1; hits < batchSize; hits++ ) {
			assertFalse( eviction.thresholdExpired() );
			eviction.onEntryHit( nextEntry );
		}
		assertTrue( eviction.thresholdExpired() );
	}
}
