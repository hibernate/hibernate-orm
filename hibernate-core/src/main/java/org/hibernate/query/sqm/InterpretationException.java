/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm;

import org.hibernate.QueryException;

/**
 * Represents a generic unhandled problem which occurred while translating
 * HQL/JPQL. This exception represents some sort of bug in the query
 * translator, whereas {@link org.hibernate.query.SemanticException} or
 * {@link org.hibernate.query.SyntaxException} indicate problems with the
 * query itself.
 *
 * @apiNote This exception type should not be used to report any expected
 *          kind of failure which could occur due to user error. It should
 *          only be used to assert that a condition should never occur. Of
 *          course, this exception usually occurs when a query has some sort
 *          of error. But its occurrence indicates that the query translator
 *          should have detected and reported that error earlier, in a more
 *          meaningful way, via a {@code SemanticException}.
 *
 * @author Steve Ebersole
 *
 * @see ParsingException
 */
public class InterpretationException extends QueryException {

	public InterpretationException(String query, String message) {
		super(
				"Error interpreting query [" + message + "]",
				query
		);
	}
	public InterpretationException(String query, Exception cause) {
		super(
				"Error interpreting query [" + cause.getMessage() + "]",
				query,
				cause
		);
	}

	/**
	 * @deprecated this constructor does not carry information
	 *             about the query which caused the failure
	 */
	@Deprecated(since = "6.3", forRemoval = true)
	public InterpretationException(String message) {
		super( "Error interpreting query [" + message + "]" );
	}
}
