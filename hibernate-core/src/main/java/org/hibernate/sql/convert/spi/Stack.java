/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.convert.spi;

import java.util.LinkedList;

/**
 * A general-purpose stack impl for use in parsing.
 *
 * @param <T> The type of things stored in the stack
 *
 * @author Steve Ebersole
 */
public class Stack<T> {
	private LinkedList<T> stack = new LinkedList<>();

	public Stack() {
	}

	public Stack(T initial) {
		push( initial );
	}

	public void push(T newCurrent) {
		stack.addFirst( newCurrent );
	}

	public T pop() {
		return stack.removeFirst();
	}

	public T getCurrent() {
		return stack.getFirst();
	}
}
