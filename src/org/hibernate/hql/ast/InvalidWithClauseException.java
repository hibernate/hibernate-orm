package org.hibernate.hql.ast;

import org.hibernate.QueryException;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class InvalidWithClauseException extends QuerySyntaxException {
	public InvalidWithClauseException(String message) {
		super( message );
	}

	public InvalidWithClauseException(String message, String queryString) {
		super( message, queryString );
	}
}
