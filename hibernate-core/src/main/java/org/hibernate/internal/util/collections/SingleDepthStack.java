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
public class SingleDepthStack<T> implements Stack<T> {
	private T entry;

	@Override
	public void push(T newCurrent) {
		if ( entry != null ) {
			throw new IllegalStateException( "Attempt to push new entry to single-depth stack when an entry already set" );
		}
		this.entry = newCurrent;
	}

	@Override
	public T pop() {
		if ( entry == null ) {
			throw new IllegalStateException( "Cannot pop from single-depth stack with no entry currently set" );
		}
		final T rtn = entry;
		this.entry = null;
		return rtn;
	}

	@Override
	public T getCurrent() {
		return entry;
	}

	@Override
	public T getPrevious() {
		return null;
	}

	@Override
	public int depth() {
		return entry == null ? 0 : 1;
	}

	@Override
	public boolean isEmpty() {
		return entry == null;
	}

	@Override
	public void clear() {
		entry = null;
	}

	@Override
	public void visitCurrentFirst(Consumer<T> action) {

	}

	@Override
	public <X> X findCurrentFirst(Function<T, X> action) {
		return null;
	}
}
