/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal;

import static org.hibernate.internal.util.Validator.checkNotNullIAE;

import org.hibernate.QueryException;

/**
 * Expecting to execute an illegal operation regarding the query type
 *
 * @author Emmanuel Bernard
 */
public class QueryExecutionRequestException extends QueryException {
	public QueryExecutionRequestException(String message, String queryString) {
		super( message, queryString );
		checkNotNullIAE( "queryString", queryString );
	}
}
