//$Id: EmptyIterator.java 3890 2004-06-03 16:31:32Z steveebersole $
package org.hibernate.util;

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
