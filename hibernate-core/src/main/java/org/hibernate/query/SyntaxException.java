/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
