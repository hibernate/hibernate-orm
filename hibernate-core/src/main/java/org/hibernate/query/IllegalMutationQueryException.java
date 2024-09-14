/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query;

/**
 * Indicates an attempt to call {@link QueryProducer#createMutationQuery(String)},
 * {@link QueryProducer#createNamedMutationQuery(String)} or
 * {@link QueryProducer#createNativeMutationQuery(String)} with a non-mutation
 * query (generally a select query)
 *
 * @author Steve Ebersole
 */
public class IllegalMutationQueryException extends IllegalQueryOperationException {
	public IllegalMutationQueryException(String message) {
		super( message );
	}

	public IllegalMutationQueryException(String message, String queryString) {
		super( message, queryString, null );
	}
}
