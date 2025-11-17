/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

import org.hibernate.QueryException;

/**
 * Represents an error in the semantics (meaning) of a HQL/JPQL query.
 *
 * @author Steve Ebersole
 *
 * @see SyntaxException
 */
public class SemanticException extends QueryException {

	/**
	 * @deprecated this constructor does not carry information
	 *             about the query which caused the failure
	 */
	@Deprecated(since = "6.3")
	public SemanticException(String message) {
		super( message );
	}

	/**
	 * @deprecated this constructor does not carry information
	 *             about the query which caused the failure
	 */
	@Deprecated(since = "6.3")
	public SemanticException(String message, Exception cause) {
		super( message, cause );
	}

	public SemanticException(String message, String queryString) {
		super( message, queryString );
	}

	public SemanticException(String message, String queryString, Exception cause) {
		super( message, queryString, cause );
	}
}
