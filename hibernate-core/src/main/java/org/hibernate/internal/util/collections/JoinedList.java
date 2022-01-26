/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.util.collections;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * @author Gavin King
 */
public class JoinedList<E> extends AbstractList<E> {
	private final List<List<E>> lists;
	private final int size;

	public JoinedList(List<List<E>> lists) {
		this.lists = lists;
		size = lists.stream().map(List::size).reduce(0, Integer::sum);
	}

	@SafeVarargs
	public JoinedList(List<E>... lists) {
		this( Arrays.asList(lists) );
	}

	@Override
	public E get(int index) {
		for (List<E> list: lists) {
			if ( list.size() > index ) {
				return list.get(index);
			}
			index -= list.size();
		}
		return null;
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public Iterator<E> iterator() {
		return lists.stream().flatMap(List::stream).iterator();
	}
}
