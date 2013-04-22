/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
