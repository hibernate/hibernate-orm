/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.internal;

import java.util.ArrayList;
import java.util.List;

/**
 * Intermediate holder class for results of queries
 * executed using key-based pagination.
 *
 * @author Gavin King
 */
class KeyedResult<R> {
	final R result;
	final List<Comparable<?>> key;

	public KeyedResult(R result, List<Comparable<?>> key) {
		this.result = result;
		this.key = key;
	}

	public R getResult() {
		return result;
	}

	public List<Comparable<?>> getKey() {
		return key;
	}

	static <R> List<R> collectResults(List<KeyedResult<R>> executed, int pageSize) {
		final int size = executed.size();
		final List<R> resultList = new ArrayList<>( size );
		for (int i = 0; i < size && i < pageSize; i++) {
			resultList.add( executed.get(i).getResult() );
		}
		return resultList;
	}

	static List<List<?>> collectKeys(List<? extends KeyedResult<?>> executed, int pageSize) {
		final int size = executed.size();
		final List<List<?>> resultList = new ArrayList<>( size );
		for ( int i = 0; i < size && i < pageSize; i++ ) {
			final List<Comparable<?>> key = executed.get(i).getKey();
			if ( key == null ) {
				throw new IllegalArgumentException("Null keys in key-based pagination are not yet supported");
			}
			resultList.add( key );
		}
		return resultList;
	}
}
