//$Id: LazyIterator.java 7699 2005-07-30 04:56:09Z oneovthafew $
package org.hibernate.util;

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
