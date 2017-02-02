/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import javax.persistence.PersistenceException;

/**
 * The base exception type for Hibernate exceptions.
 * <p/>
 * Note that all {@link java.sql.SQLException SQLExceptions} will be wrapped in some form of 
 * {@link JDBCException}.
 * 
 * @author Gavin King
 */
public class HibernateException extends PersistenceException {
	/**
	 * Constructs a HibernateException using the given exception message.
	 *
	 * @param message The message explaining the reason for the exception
	 */
	public HibernateException(String message) {
		super( message );
	}

	/**
	 * Constructs a HibernateException using the given message and underlying cause.
	 *
	 * @param cause The underlying cause.
	 */
	public HibernateException(Throwable cause) {
		super( cause );
	}

	/**
	 * Constructs a HibernateException using the given message and underlying cause.
	 *
	 * @param message The message explaining the reason for the exception.
	 * @param cause The underlying cause.
	 */
	public HibernateException(String message, Throwable cause) {
		super( message, cause );
	}
}
