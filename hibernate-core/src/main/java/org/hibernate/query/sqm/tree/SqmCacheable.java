/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

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

	static boolean areCompatible(@Nullable Collection<? extends SqmCacheable> collection1, @Nullable Collection<? extends SqmCacheable> collection2) {
		if ( collection1 == null ) {
			return collection2 == null;
		}
		if ( collection2 != null ) {
			if ( collection1.size() == collection2.size() ) {
				OUTER: for ( SqmCacheable node1 : collection1 ) {
					for ( SqmCacheable node2 : collection2 ) {
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

	static boolean areCompatible(@Nullable List<? extends SqmCacheable> collection1, @Nullable List<? extends SqmCacheable> collection2) {
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

	static <K> boolean areCompatible(@Nullable Map<@NonNull K, @NonNull ? extends SqmCacheable> collection1, @Nullable Map<@NonNull K, @NonNull ? extends SqmCacheable> collection2) {
		if ( collection1 == null ) {
			return collection2 == null;
		}
		if ( collection2 != null ) {
			final int size = collection1.size();
			if ( size == collection2.size() ) {
				for ( Map.Entry<@NonNull K, @NonNull ? extends SqmCacheable> entry : collection1.entrySet() ) {
					final SqmCacheable otherValue = collection2.get( entry.getKey() );
					if ( otherValue != null && !entry.getValue().isCompatible( otherValue ) ) {
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

	static int cacheHashCode(@Nullable Collection<@NonNull ? extends SqmCacheable> collection) {
		if ( collection == null ) {
			return 0;
		}
		int result = 1;
		for ( var node : collection ) {
			result = 31 * result + node.cacheHashCode();
		}
		return result;
	}

	static int cacheHashCode(@Nullable Map<@NonNull ?, @NonNull ? extends SqmCacheable> map) {
		if ( map == null ) {
			return 0;
		}
		int result = 0;
		for ( Map.Entry<@NonNull ?, @NonNull ? extends SqmCacheable> entry : map.entrySet() ) {
			result = entry.getKey().hashCode() ^ entry.getValue().cacheHashCode();
		}
		return result;
	}
}
