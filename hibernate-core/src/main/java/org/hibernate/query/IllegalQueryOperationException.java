/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.query;

import org.hibernate.QueryException;

/**
 * Thrown when an operation of the {@link Query} interface
 * is called which is unsupported due to the nature of the
 * query itself. For example, this exception is throw if
 * {@link Query#executeUpdate executeUpdate()} is invoked
 * on an instance of {@code Query} representing a JPQL or
 * SQL {@code SELECT} query.
 *
 * @author Steve Ebersole
 */
public class IllegalQueryOperationException extends QueryException {
	public IllegalQueryOperationException(String message) {
		super( message );
	}

	public IllegalQueryOperationException(String message, String queryString, Exception cause) {
		super( message, queryString, cause );
	}
}
