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

/**
 * A problem occurred translating a Hibernate query to SQL due to invalid query syntax, etc.
 */
public class QueryException extends HibernateException {
	private final String queryString;

	/**
	 * Constructs a QueryException using the specified exception message.
	 *
	 * @param message A message explaining the exception condition
	 */
	public QueryException(String message) {
		this( message, null, null );
	}

	/**
	 * Constructs a QueryException using the specified exception message and cause.
	 *
	 * @param message A message explaining the exception condition
	 * @param cause The underlying cause
	 */
	public QueryException(String message, Exception cause) {
		this( message, null, cause );
	}

	/**
	 * Constructs a QueryException using the specified exception message and query-string.
	 *
	 * @param message A message explaining the exception condition
	 * @param queryString The query being evaluated when the exception occurred
	 */
	public QueryException(String message, String queryString) {
		this( message, queryString, null );
	}

	/**
	 * Constructs a QueryException using the specified exception message and query-string.
	 *
	 * @param message A message explaining the exception condition
	 * @param queryString The query being evaluated when the exception occurred
	 * @param cause The underlying cause
	 */
	public QueryException(String message, String queryString, Exception cause) {
		super( message, cause );
		this.queryString = queryString;
	}

	/**
	 * Constructs a QueryException using the specified cause.
	 *
	 * @param cause The underlying cause
	 */
	public QueryException(Exception cause) {
		this( "A query exception occurred", null, cause );
	}

	/**
	 * Retrieve the query being evaluated when the exception occurred.  May be null, but generally should not.
	 *
	 * @return The query string
	 */
	public String getQueryString() {
		return queryString;
	}

	@Override
	public String getMessage() {
		String msg = getOriginalMessage();
		if ( queryString != null ) {
			msg += " [" + queryString + ']';
		}
		return msg;
	}

	protected final String getOriginalMessage() {
		return super.getMessage();
	}

	/**
	 *
	 * @param queryString
	 * @return
	 */
	public final QueryException wrapWithQueryString(String queryString) {
		if ( this.getQueryString() != null ) {
			return this;
		}

		return doWrapWithQueryString( queryString );
	}

	protected QueryException doWrapWithQueryString(String queryString) {
		return new QueryException( getOriginalMessage(), queryString, this );
	}
}
