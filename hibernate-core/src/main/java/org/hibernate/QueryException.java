/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A problem occurred translating a Hibernate query to SQL due to illegal query
 * syntax, an operation which is not well-typed, an unresolvable reference to
 * an entity or attribute, an unknown named query, or any similar problem. This
 * exception type is not used to represent failures that occur while executing
 * a query or reading the result set of a query.
 * <p>
 * The two most important subtypes are:
 * <ul>
 * <li>{@link org.hibernate.query.SyntaxException}, which represents a syntax
 *     error in the HQL query, and
 * <li>{@link org.hibernate.query.SemanticException}, which represents an error
 *     in the semantics of the query.
 * </ul>
 *
 * @see org.hibernate.query.SemanticException
 * @see org.hibernate.query.SyntaxException
 */
public class QueryException extends HibernateException {
	private final @Nullable String queryString;

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
	public QueryException(String message, @Nullable String queryString) {
		this( message, queryString, null );
	}

	/**
	 * Constructs a {@code QueryException} using the specified exception message and query string.
	 *
	 * @param message A message explaining the exception condition
	 * @param queryString The query being evaluated when the exception occurred
	 * @param cause The underlying cause
	 */
	public QueryException(String message, @Nullable String queryString, Exception cause) {
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
	public @Nullable String getQueryString() {
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
}
