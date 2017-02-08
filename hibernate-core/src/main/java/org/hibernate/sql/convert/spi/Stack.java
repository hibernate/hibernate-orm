/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.convert.spi;

import java.util.ArrayDeque;

/**
 * A general-purpose stack impl for use in parsing.
 *
 * @param <T> The type of things stored in the stack
 *
 * @author Steve Ebersole
 */
public class Stack<T> {
	private ArrayDeque<T> internalStack = new ArrayDeque<>();

	public Stack() {
	}

	public Stack(T initial) {
		push( initial );
	}

	public void push(T newCurrent) {
		internalStack.addFirst( newCurrent );
	}

	public T pop() {
		return internalStack.removeFirst();
	}

	public T getCurrent() {
		return internalStack.getFirst();
	}

	public boolean isEmpty() {
		return internalStack.isEmpty();
	}

	public void clear() {
		internalStack.clear();
	}
}
