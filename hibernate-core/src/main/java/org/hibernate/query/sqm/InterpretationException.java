/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm;

import org.hibernate.QueryException;

/**
 * Represents a general uncaught problem performing the interpretation.
 * This usually indicate a semantic error in the query.
 *
 * @author Steve Ebersole
 */
public class InterpretationException extends QueryException {

	public InterpretationException(String query, String message) {
		super(
				"Error interpreting query [" + message + "] [" + query + "]",
				query
		);
	}
	public InterpretationException(String query, Exception cause) {
		super(
				"Error interpreting query [" + cause.getMessage() + "] [" + query + "]",
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
