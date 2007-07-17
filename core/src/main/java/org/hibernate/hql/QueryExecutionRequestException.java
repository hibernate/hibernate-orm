//$Id: $
package org.hibernate.hql;

import org.hibernate.QueryException;

/**
 * Expecting to execute an illegal operation regarding the query type
 *
 * @author Emmanuel Bernard
 */
public class QueryExecutionRequestException extends QueryException {

	public QueryExecutionRequestException(String message, String queryString) {
		super( message, queryString );
	}
}
