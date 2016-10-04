/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.internal.bytebuddy;

import org.hibernate.HibernateException;

public class BulkAccessorException extends HibernateException {
	private final int index;

	public BulkAccessorException(String message) {
		this( message, -1 );
	}

	public BulkAccessorException(String message, int index) {
		this( message, index, null );
	}

	public BulkAccessorException(String message, Exception cause) {
		this( message, -1, cause );
	}

	public BulkAccessorException(String message, int index, Exception cause) {
		super( message + " : @" + index, cause );
		this.index = index;
	}

	public int getIndex() {
		return this.index;
	}
}
