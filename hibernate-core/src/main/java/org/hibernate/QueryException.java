/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
	 * Wraps this exception with another, of same kind, with the specified queryString.  If this exception already
	 * has a queryString defined, the same exception ({@code this}) is returned.  Otherwise the protected
	 * {@link #generateQueryException(String)} is called, to allow subclasses to properly create the correct
	 * subclass for return.
	 *
	 * @param queryString The query string that led to the QueryException
	 *
	 * @return {@code this}, if {@code this} has {@code null} for {@link #getQueryString()}; otherwise a new
	 * QueryException (or subclass) is returned.
	 */
	public final QueryException wrapWithQueryString(String queryString) {
		if ( this.getQueryString() != null ) {
			return this;
		}

		return generateQueryException( queryString );
	}

	/**
	 * Called from {@link #wrapWithQueryString(String)} when we really need to generate a new QueryException
	 * (or subclass).
	 * <p/>
	 * NOTE : implementors should take care to use {@link #getOriginalMessage()} for the message, not
	 * {@link #getMessage()}
	 *
	 * @param queryString The query string
	 *
	 * @return The generated QueryException (or subclass)
	 *
	 * @see #getOriginalMessage()
	 */
	protected QueryException generateQueryException(String queryString) {
		return new QueryException( getOriginalMessage(), queryString, this );
	}
}
