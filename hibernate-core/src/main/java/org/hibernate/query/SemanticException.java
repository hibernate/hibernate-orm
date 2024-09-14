/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
