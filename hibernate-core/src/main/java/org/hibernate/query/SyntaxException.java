/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

import org.hibernate.QueryException;

/**
 * Represents a syntax error in a HQL/JPQL query.
 *
 * @author Gavin King
 *
 * @see SemanticException
 *
 * @since 6.3
 */
public class SyntaxException extends QueryException {
	public SyntaxException(String message, String queryString) {
		super( message, queryString );
	}
	public SyntaxException(String message) {
		super( message );
	}
}
