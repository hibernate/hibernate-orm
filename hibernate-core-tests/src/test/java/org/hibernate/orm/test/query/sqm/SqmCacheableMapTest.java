package org.hibernate.orm.test.query.sqm;

import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.query.sqm.tree.spi.SqmCacheable;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The structural comparison of two maps of SQM nodes, as used for the
 * query plan cache, must take every key and every value into account.
 */
@JiraKey("HHH-21159")
public class SqmCacheableMapTest {

	@Test
	public void sameKeysAndCompatibleValues() {
		assertTrue( SqmCacheable.areCompatible( map( "a", "x", "b", "y" ), map( "b", "y", "a", "x" ) ) );
	}

	@Test
	public void differentKeys() {
		assertFalse( SqmCacheable.areCompatible( map( "a", "x" ), map( "b", "x" ) ) );
		assertFalse( SqmCacheable.areCompatible( map( "a", "x", "b", "y" ), map( "a", "x", "c", "y" ) ) );
	}

	@Test
	public void incompatibleValues() {
		assertFalse( SqmCacheable.areCompatible( map( "a", "x", "b", "y" ), map( "a", "x", "b", "z" ) ) );
	}

	@Test
	public void differentSizes() {
		assertFalse( SqmCacheable.areCompatible( map( "a", "x" ), map( "a", "x", "b", "y" ) ) );
	}

	@Test
	public void hashCodeIgnoresIterationOrder() {
		assertEquals(
				SqmCacheable.cacheHashCode( map( "a", "x", "b", "y" ) ),
				SqmCacheable.cacheHashCode( map( "b", "y", "a", "x" ) )
		);
	}

	@Test
	public void hashCodeUsesEveryEntry() {
		assertNotEquals(
				SqmCacheable.cacheHashCode( map( "a", "x", "b", "y" ) ),
				SqmCacheable.cacheHashCode( map( "a", "z", "b", "y" ) )
		);
		assertNotEquals(
				SqmCacheable.cacheHashCode( map( "a", "x", "b", "y" ) ),
				SqmCacheable.cacheHashCode( map( "c", "x", "b", "y" ) )
		);
	}

	private static Map<String, Node> map(String... keysAndValues) {
		final Map<String, Node> map = new LinkedHashMap<>();
		for ( int i = 0; i < keysAndValues.length; i += 2 ) {
			map.put( keysAndValues[i], new Node( keysAndValues[i + 1] ) );
		}
		return map;
	}

	private record Node(String name) implements SqmCacheable {
		@Override
		public boolean isCompatible(Object object) {
			return object instanceof Node that && name.equals( that.name );
		}

		@Override
		public int cacheHashCode() {
			return name.hashCode();
		}
	}
}
