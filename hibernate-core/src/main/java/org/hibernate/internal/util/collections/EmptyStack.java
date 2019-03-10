/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.internal.util.collections;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author Steve Ebersole
 */
public class EmptyStack<T> implements Stack<T> {
	/**
	 * Singleton access
	 */
	public static final EmptyStack INSTANCE = new EmptyStack();

	@SuppressWarnings("unchecked")
	public static <T> EmptyStack<T> instance() {
		return INSTANCE;
	}

	@Override
	public void push(T newCurrent) {
	}

	@Override
	public T pop() {
		return null;
	}

	@Override
	public T getCurrent() {
		return null;
	}

	@Override
	public T getPrevious() {
		return null;
	}

	@Override
	public int depth() {
		return 0;
	}

	@Override
	public boolean isEmpty() {
		return true;
	}

	@Override
	public void clear() {
	}

	@Override
	public void visitCurrentFirst(Consumer<T> action) {
	}

	@Override
	public <X> X findCurrentFirst(Function<T, X> action) {
		return null;
	}
}
