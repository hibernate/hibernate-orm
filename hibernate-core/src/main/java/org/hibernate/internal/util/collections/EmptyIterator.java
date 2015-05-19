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
public final class EmptyIterator implements Iterator {

	public static final Iterator INSTANCE = new EmptyIterator();

	public boolean hasNext() {
		return false;
	}

	public Object next() {
		throw new UnsupportedOperationException();
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}

	private EmptyIterator() {}

}
