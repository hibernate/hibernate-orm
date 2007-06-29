//$Id$
package org.hibernate.util;

import java.util.Iterator;

/**
 * @author Gavin King
 */
public final class SingletonIterator implements Iterator {

	private Object value;
	private boolean hasNext = true;

	public boolean hasNext() {
		return hasNext;
	}

	public Object next() {
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

	public SingletonIterator(Object value) {
		this.value = value;
	}

}
