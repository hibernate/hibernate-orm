package org.hibernate.internal.util.collections;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("removal")
class BoundedConcurrentHashMapTest {
	@Test
	void lateLruHitAfterClearCannotEvictALiveEntry() {
		final var map = new BoundedConcurrentHashMap<String, Integer>( 2, 1 );
		final var segment = map.segments[0];
		segment.put( "old", 0, 0, false );
		final var entrySeenByReader = segment.getFirst( 0 );
		map.clear();
		map.put( "a", 1 );
		map.put( "b", 2 );

		// Resume the reader after both replacement entries have been installed.
		segment.eviction.onEntryHit( entrySeenByReader );
		for ( int i = 0; i < BoundedConcurrentHashMap.EvictionPolicy.MAX_BATCH_SIZE; i++ ) {
			map.put( "b", i );
		}
		assertEquals( 2, map.size() );
		assertEquals( 1, map.get( "a" ) );
	}

	@Test
	void lateLruHitDoesNotRefreshAReinsertedKey() {
		final var map = new BoundedConcurrentHashMap<String, Integer>( 2, 1 );
		final var segment = map.segments[0];
		segment.put( "a", 0, 1, false );
		final var original = segment.getFirst( 0 );
		segment.remove( "a", 0, null );
		segment.put( "a", 0, 2, false );
		segment.put( "b", 1, 3, false );
		segment.eviction.onEntryHit( original );
		segment.put( "c", 2, 4, false );
		assertFalse( segment.containsKey( "a", 0 ) );
		assertTrue( segment.containsKey( "b", 1 ) );
	}

	@Test
	void firstTimeLirsMissDoesNotDisplaceAHotEntry() {
		final var map = new BoundedConcurrentHashMap<Integer, Integer>( 4, 1, BoundedConcurrentHashMap.Eviction.LIRS );
		for ( int key = 0; key < 5; key++ ) {
			map.put( key, key );
		}
		// Three warmup entries fit in the hot set. The fifth key replaces the fourth.
		assertEquals( Set.of( 0, 1, 2, 4 ), map.keySet() );
	}

	@Test
	void lirsAdaptsWhenTheWorkingSetChanges() {
		final var map = new BoundedConcurrentHashMap<Integer, Integer>( 4, 1, BoundedConcurrentHashMap.Eviction.LIRS );
		for ( int key = 0; key < 3; key++ ) {
			map.put( key, key );
		}
		// Only one slot is cold. Both new keys must become resident when they recur.
		for ( int i = 0; i < 10; i++ ) {
			final int key = 3 + i % 2;
			if ( map.get( key ) == null ) {
				map.put( key, key );
			}
		}
		assertEquals( 3, map.get( 3 ) );
		assertEquals( 4, map.get( 4 ) );
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void emptyLirsCacheForgetsEvictedKeys(boolean removeIndividually) {
		final var map = new BoundedConcurrentHashMap<Integer, Integer>( 4, 1, BoundedConcurrentHashMap.Eviction.LIRS );
		for ( int key = 0; key < 5; key++ ) {
			map.put( key, key );
		}
		if ( removeIndividually ) {
			Set.copyOf( map.keySet() ).forEach( map::remove );
		}
		else {
			map.clear();
		}
		// Key 3 was evicted before the reset and should now be treated as a first-time miss.
		for ( int key : List.of( 10, 11, 12, 3, 13 ) ) {
			map.put( key, key );
		}
		assertEquals( Set.of( 10, 11, 12, 13 ), map.keySet() );
	}

	@ParameterizedTest
	@EnumSource
	void updatingAnExistingEntryDrainsFullBatches(BoundedConcurrentHashMap.Eviction strategy) {
		final int batchSize = BoundedConcurrentHashMap.EvictionPolicy.MAX_BATCH_SIZE;
		final var map = new BoundedConcurrentHashMap<String, Integer>( batchSize, 1, strategy );
		map.put( "key", 0 );
		for ( int i = 1; i <= 2 * batchSize; i++ ) {
			assertEquals( i - 1, map.put( "key", i ) );
		}
		assertFalse( map.segments[0].eviction.thresholdExpired() );
		assertEquals( 2 * batchSize, map.get( "key" ) );
	}

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
