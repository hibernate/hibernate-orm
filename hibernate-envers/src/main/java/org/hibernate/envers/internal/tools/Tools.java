/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.envers.tools.Pair;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author HernпїЅn Chanfreau
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 * @author Chris Cranford
 */
public abstract class Tools {
	public static <K, V> Map<K, V> newHashMap() {
		return new HashMap<>();
	}

	public static <E> Set<E> newHashSet() {
		return new HashSet<>();
	}

	public static <K, V> Map<K, V> newLinkedHashMap() {
		return new LinkedHashMap<>();
	}

	public static <T> List<T> iteratorToList(Iterator<T> iter) {
		final List<T> ret = new ArrayList<>();
		while ( iter.hasNext() ) {
			ret.add( iter.next() );
		}

		return ret;
	}

	public static <X> List<X> collectionToList(Collection<X> collection) {
		if ( collection instanceof List ) {
			return (List<X>) collection;
		}
		else {
			return new ArrayList<>( collection );
		}
	}

	/**
	 * Transforms a list of arbitrary elements to a list of index-element pairs.
	 *
	 * @param list List to transform.
	 *
	 * @return A list of pairs: ((0, element_at_index_0), (1, element_at_index_1), ...)
	 */
	public static <T> List<Pair<Integer, T>> listToIndexElementPairList(List<T> list) {
		final List<Pair<Integer, T>> ret = new ArrayList<>();
		final Iterator<T> listIter = list.iterator();
		for ( int i = 0; i < list.size(); i++ ) {
			ret.add( Pair.make( i, listIter.next() ) );
		}

		return ret;
	}
}
