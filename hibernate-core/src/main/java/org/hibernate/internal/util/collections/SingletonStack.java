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
public class SingletonStack<T> implements Stack<T> {
	private final T instance;

	public SingletonStack(T instance) {
		this.instance = instance;
	}

	@Override
	public void push(T newCurrent) {
		throw new UnsupportedOperationException( "Cannot push to a singleton Stack" );
	}

	@Override
	public T pop() {
		throw new UnsupportedOperationException( "Cannot pop from a singleton Stack" );
	}

	@Override
	public T getCurrent() {
		return instance;
	}

	@Override
	public T getPrevious() {
		return null;
	}

	@Override
	public int depth() {
		return 1;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public void clear() {
	}

	@Override
	public void visitCurrentFirst(Consumer<T> action) {
		action.accept( instance );
	}

	@Override
	public <X> X findCurrentFirst(Function<T, X> action) {
		return action.apply( instance );
	}
}
