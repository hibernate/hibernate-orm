/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2013, Red Hat Inc. or third-party contributors as
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

import org.jboss.logging.Logger;

/**
 * A problem occurred translating a Hibernate query to SQL due to invalid query syntax, etc.
 */
public class QueryException extends HibernateException {
	private static final Logger log = Logger.getLogger( QueryException.class );

	private String queryString;

	/**
	 * Constructs a QueryException using the specified exception message.
	 *
	 * @param message A message explaining the exception condition
	 */
	public QueryException(String message) {
		super( message );
	}

	/**
	 * Constructs a QueryException using the specified exception message and cause.
	 *
	 * @param message A message explaining the exception condition
	 * @param cause The underlying cause
	 */
	public QueryException(String message, Throwable cause) {
		super( message, cause );
	}

	/**
	 * Constructs a QueryException using the specified exception message and query-string.
	 *
	 * @param message A message explaining the exception condition
	 * @param queryString The query being evaluated when the exception occurred
	 */
	public QueryException(String message, String queryString) {
		super( message );
		this.queryString = queryString;
	}

	/**
	 * Constructs a QueryException using the specified cause.
	 *
	 * @param cause The underlying cause
	 */
	public QueryException(Exception cause) {
		super( cause );
	}

	/**
	 * Retrieve the query being evaluated when the exception occurred.  May be null, but generally should not.
	 *
	 * @return The query string
	 */
	public String getQueryString() {
		return queryString;
	}

	/**
	 * Set the query string.  Even an option since often the part of the code generating the exception does not
	 * have access to the query overall.
	 *
	 * @param queryString The query string.
	 */
	public void setQueryString(String queryString) {
		if ( this.queryString != null ) {
			log.debugf(
					"queryString overriding non-null previous value [%s] : %s",
					this.queryString,
					queryString
			);
		}
		this.queryString = queryString;
	}

	@Override
	public String getMessage() {
		String msg = super.getMessage();
		if ( queryString!=null ) {
			msg += " [" + queryString + ']';
		}
		return msg;
	}

}
