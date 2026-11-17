package org.hibernate.query.sqm.tree.spi;

import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Base contract for any SQM AST node caching.
 */
public interface SqmCacheable {

	boolean isCompatible(Object object);

	int cacheHashCode();

	static boolean areCompatible(@Nullable SqmCacheable e1, @Nullable SqmCacheable e2) {
		return e1 == null ? e2 == null : e2 != null && e1.isCompatible( e2 );
	}

	static boolean areCompatible(
			@Nullable Collection<? extends SqmCacheable> collection1,
			@Nullable Collection<? extends SqmCacheable> collection2) {
		if ( collection1 == null ) {
			return collection2 == null;
		}
		if ( collection2 != null ) {
			if ( collection1.size() == collection2.size() ) {
				OUTER: for ( var node1 : collection1 ) {
					for ( var node2 : collection2 ) {
						if ( node1.isCompatible( node2 ) ) {
							continue OUTER;
						}
					}
					return false;
				}
				return true;
			}
		}
		return false;
	}

	static boolean areCompatible(
			@Nullable List<? extends SqmCacheable> collection1,
			@Nullable List<? extends SqmCacheable> collection2) {
		if ( collection1 == null ) {
			return collection2 == null;
		}
		if ( collection2 != null ) {
			final int size = collection1.size();
			if ( size == collection2.size() ) {
				for ( int i = 0; i < size; i++ ) {
					if ( !collection1.get( i ).isCompatible( collection2.get( i ) ) ) {
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}

	static <K> boolean areCompatible(
			@Nullable Map<K, ? extends SqmCacheable> collection1,
			@Nullable Map<K, ? extends SqmCacheable> collection2) {
		if ( collection1 == null ) {
			return collection2 == null;
		}
		if ( collection2 != null ) {
			final int size = collection1.size();
			if ( size == collection2.size() ) {
				for ( var entry : collection1.entrySet() ) {
					final K key = entry.getKey();
					if ( !collection2.containsKey( key )
							|| !areCompatible( entry.getValue(), collection2.get( key ) ) ) {
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}

	static int cacheHashCode(@Nullable SqmCacheable e1) {
		return e1 == null ? 0 : e1.cacheHashCode();
	}

	static int cacheHashCode(@Nullable Collection<? extends SqmCacheable> collection) {
		if ( collection == null ) {
			return 0;
		}
		int result = 1;
		for ( var node : collection ) {
			result = 31 * result + node.cacheHashCode();
		}
		return result;
	}

	static int cacheHashCode(@Nullable Map<?, ? extends SqmCacheable> map) {
		if ( map == null ) {
			return 0;
		}
		// the sum does not depend on the iteration order of the map
		int result = 0;
		for ( var entry : map.entrySet() ) {
			result += entry.getKey().hashCode() ^ cacheHashCode( entry.getValue() );
		}
		return result;
	}
}
