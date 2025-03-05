/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
