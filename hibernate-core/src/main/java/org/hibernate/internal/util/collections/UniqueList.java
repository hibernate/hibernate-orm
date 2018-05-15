/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.internal.util.collections;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;

/**
 * A collection of values that is simultaneously a
 * List (ordered) and a Set (unique)
 *
 * @author Steve Ebersole
 */
@SuppressWarnings({"NullableProblems", "unused", "WeakerAccess"})
public class UniqueList<E> extends AbstractList<E> implements Set<E>, List<E> {

	// todo (6.0) : probably better performance-wise to fully implement the contract rather than wrap/delegate

	private final List<E> elements;

	public UniqueList() {
		this( new ArrayList<>() );
	}

	public UniqueList(List<E> elements) {
		this.elements = elements;
	}

	public UniqueList(int size) {
		this.elements = CollectionHelper.arrayList( size );
	}

	@Override
	public E get(int index) {
		return elements.get( index );
	}

	@Override
	public void add(int index, E element) {
		if ( elements.contains( element ) ) {
			return;
		}
		elements.add( index, element );
	}

	@Override
	public int size() {
		return elements.size();
	}

	@Override
	public Iterator<E> iterator() {
		return elements.iterator();
	}

	@Override
	public ListIterator<E> listIterator() {
		return elements.listIterator();
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		return elements.listIterator( index );
	}

	@Override
	public Object[] toArray() {
		return elements.toArray();
	}

	@Override
	@SuppressWarnings("SuspiciousToArrayCall")
	public <T> T[] toArray(T[] a) {
		return elements.toArray( a );
	}

	@Override
	public Spliterator<E> spliterator() {
		return Spliterators.spliterator( this, Spliterator.ORDERED );
	}
}
