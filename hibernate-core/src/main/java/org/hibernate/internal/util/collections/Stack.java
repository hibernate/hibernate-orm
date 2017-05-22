/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.internal.util.collections;

import java.util.LinkedList;

/**
 * A general-purpose stack impl for use in parsing.
 *
 * @param <T> The type of things stored in the stack
 *
 * @author Steve Ebersole
 */
public class Stack<T> {
	private LinkedList<T> internalStack = new LinkedList<>();

	public Stack() {
	}

	public Stack(T initial) {
		internalStack.add( initial );
	}

	public void push(T newCurrent) {
		internalStack.addFirst( newCurrent );
	}

	public T pop() {
		return internalStack.removeFirst();
	}

	public T getCurrent() {
		return internalStack.peek();
	}

	public T getPrevious() {
		if ( internalStack.size() < 2 ) {
			return null;
		}
		return internalStack.get( internalStack.size() - 2 );
	}

	public int depth() {
		return internalStack.size();
	}

	public boolean isEmpty() {
		return internalStack.isEmpty();
	}

	public void clear() {
		internalStack.clear();
	}
}
