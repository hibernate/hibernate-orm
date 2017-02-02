/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.internal.javassist;

import org.hibernate.HibernateException;

/**
 * An exception thrown while generating a bulk accessor.
 *
 * @author Muga Nishizawa
 * @author modified by Shigeru Chiba
 */
public class BulkAccessorException extends HibernateException {
	private final int index;

	/**
	 * Constructs an exception.
	 *
	 * @param message Message explaining the exception condition
	 */
	public BulkAccessorException(String message) {
		this( message, -1 );
	}

	/**
	 * Constructs an exception.
	 *
	 * @param message Message explaining the exception condition
	 * @param index The index of the property that causes an exception.
	 */
	public BulkAccessorException(String message, int index) {
		this( message, index, null );
	}

	/**
	 * Constructs an exception.
	 *
	 * @param message Message explaining the exception condition
	 * @param cause The underlying cause
	 */
	public BulkAccessorException(String message, Exception cause) {
		this( message, -1, cause );
	}

	/**
	 * Constructs an exception.
	 *
	 * @param message Message explaining the exception condition
	 * @param index The index of the property that causes an exception.
	 * @param cause The underlying cause
	 */
	public BulkAccessorException(String message, int index, Exception cause) {
		super( message + " : @" + index, cause );
		this.index = index;
	}

	/**
	 * Returns the index of the property that causes this exception.
	 *
	 * @return -1 if the index is not specified.
	 */
	public int getIndex() {
		return this.index;
	}
}
