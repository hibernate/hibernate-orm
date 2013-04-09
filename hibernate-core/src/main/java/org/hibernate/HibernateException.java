/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007,2011, 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate;

/**
 * The base exception type for Hibernate exceptions.
 * <p/>
 * Note that all {@link java.sql.SQLException SQLExceptions} will be wrapped in some form of 
 * {@link JDBCException}.
 * 
 * @see JDBCException
 * 
 * @author Gavin King
 */
public class HibernateException extends RuntimeException {
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






