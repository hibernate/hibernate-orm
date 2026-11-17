package org.hibernate.internal.util.collections;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("removal")
class BoundedConcurrentHashMapTest {
	@Test
	void lirsCanRefillAfterClear() {
		final var map = new BoundedConcurrentHashMap<String, Integer>( 4, 1, BoundedConcurrentHashMap.Eviction.LIRS );
		final var keys = List.of( "a", "b", "c", "d" );
		for ( var key : keys ) {
			map.put( key, 1 );
			map.get( key );
		}
		map.clear();
		assertTrue( map.isEmpty() );
		for ( var key : keys ) {
			map.put( key, 2 );
			assertEquals( 2, map.get( key ) );
		}
		assertEquals( keys.size(), map.size() );
	}

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
