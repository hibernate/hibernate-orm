/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.util.collections;

import java.util.Iterator;

/**
 * @author Gavin King
 */
public final class SingletonIterator<T> implements Iterator<T> {
	private T value;
	private boolean hasNext = true;

	public boolean hasNext() {
		return hasNext;
	}

	public T next() {
		if (hasNext) {
			hasNext = false;
			return value;
		}
		else {
			throw new IllegalStateException();
		}
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}

	public SingletonIterator(T value) {
		this.value = value;
	}

}
