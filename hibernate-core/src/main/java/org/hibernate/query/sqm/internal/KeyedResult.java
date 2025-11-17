/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.internal;

import org.hibernate.AssertionFailure;
import org.hibernate.query.KeyedPage.KeyInterpretation;

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

	static <R> List<R> collectResults(List<KeyedResult<R>> executed, int pageSize, KeyInterpretation interpretation) {
		//note: given list probably has one more result than needed
		final int size = executed.size();
		final List<R> resultList = new ArrayList<>( size );
		switch ( interpretation ) {
			case NO_KEY:
			case KEY_OF_LAST_ON_PREVIOUS_PAGE:
				for (int i = 0; i < size && i < pageSize; i++) {
					resultList.add( executed.get(i).getResult() );
				}
				break;
			case KEY_OF_FIRST_ON_NEXT_PAGE:
				// the results come in reverse order
				for (int i = pageSize-1; i>=0; i--) {
					if (i < size) {
						resultList.add( executed.get(i).getResult() );
					}
				}
				break;
			default:
				throw new AssertionFailure("Unrecognized KeyInterpretation");
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
