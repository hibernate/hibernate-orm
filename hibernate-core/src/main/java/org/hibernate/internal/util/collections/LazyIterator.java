/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.util.collections;

import java.util.Iterator;
import java.util.Map;

public final class LazyIterator implements Iterator {
	
	private final Map map;
	private Iterator iterator;
	
	private Iterator getIterator() {
		if (iterator==null) {
			iterator = map.values().iterator();
		}
		return iterator;
	}

	public LazyIterator(Map map) {
		this.map = map;
	}
	
	public boolean hasNext() {
		return getIterator().hasNext();
	}

	public Object next() {
		return getIterator().next();
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}

}
