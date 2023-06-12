/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

/**
 * A problem occurred translating a Hibernate query to SQL
 * due to invalid query syntax, or some similar problem.
 */
public class QueryException extends HibernateException {
	private final String queryString;

	/**
	 * Constructs a {@code QueryException} using the specified exception message.
	 *
	 * @param message A message explaining the exception condition
	 *
	 * @deprecated this constructor does not carry information
	 *             about the query which caused the failure
	 */
	@Deprecated(since = "6.3")
	public QueryException(String message) {
		this( message, null, null );
	}

	/**
	 * Constructs a {@code QueryException} using the specified exception message and cause.
	 *
	 * @param message A message explaining the exception condition
	 * @param cause The underlying cause
	 *
	 * @deprecated this constructor does not carry information
	 *             about the query which caused the failure
	 */
	@Deprecated(since = "6.3")
	public QueryException(String message, Exception cause) {
		this( message, null, cause );
	}

	/**
	 * Constructs a {@code QueryException} using the specified exception message and query string.
	 *
	 * @param message A message explaining the exception condition
	 * @param queryString The query being evaluated when the exception occurred
	 */
	public QueryException(String message, String queryString) {
		this( message, queryString, null );
	}

	/**
	 * Constructs a {@code QueryException} using the specified exception message and query string.
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
	 * Constructs a {@code QueryException} using the specified cause.
	 *
	 * @param cause The underlying cause
	 *
	 * @deprecated this constructor does not carry information
	 *             about the query which caused the failure
	 */
	@Deprecated(since = "6.3")
	public QueryException(Exception cause) {
		this( "A query exception occurred", null, cause );
	}

	/**
	 * Retrieve the query being evaluated when the exception occurred.
	 * May be null, but generally should not be.
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
	 * Wraps this exception with another, of same kind, with the specified query string.
	 * If this exception already has a query string defined, the same exception ({@code this})
	 * is returned. Otherwise, the protected {@link #generateQueryException(String)} is called,
	 * to allow subclasses to properly create the correct subclass for return.
	 *
	 * @param queryString The query string that led to the QueryException
	 *
	 * @return {@code this}, if {@code this} has {@code null} for {@link #getQueryString()};
	 *         otherwise a new {@code QueryException} (or subclass) is returned.
	 *
	 * @deprecated This method is no longer used
	 */
	@Deprecated(since = "6.2", forRemoval = true)
	public final QueryException wrapWithQueryString(String queryString) {
		if ( this.getQueryString() != null ) {
			return this;
		}

		return generateQueryException( queryString );
	}

	/**
	 * Called from {@link #wrapWithQueryString(String)} when we really need to
	 * generate a new {@code QueryException} (or subclass).
	 *
	 * @implNote implementors should take care to use {@link #getOriginalMessage()}
	 *           for the message, not {@link #getMessage()}
	 *
	 * @param queryString The query string
	 *
	 * @return The generated {@code QueryException} (or subclass)
	 *
	 * @see #getOriginalMessage()
	 */
	protected QueryException generateQueryException(String queryString) {
		return new QueryException( getOriginalMessage(), queryString, this );
	}
}
