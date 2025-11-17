/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import org.hibernate.query.Query;

/**
 * Thrown when the application calls {@link Query#getSingleResult()} or
 * {@link Query#uniqueResult()} and the query returns more than one row
 * from the database. Unlike every other exception thrown by Hibernate,
 * this one is recoverable!
 *
 * @author Gavin King
 *
 * @see jakarta.persistence.Query#getSingleResult
 * @see org.hibernate.query.SelectionQuery#getSingleResult
 * @see jakarta.persistence.NonUniqueResultException
 */
public class NonUniqueResultException extends HibernateException {

	private final int resultCount;

	/**
	 * Constructs a {@code NonUniqueResultException}.
	 *
	 * @param resultCount The number of actual results.
	 */
	public NonUniqueResultException(int resultCount) {
		super( "Query did not return a unique result: " + resultCount + " results were returned" );
		this.resultCount = resultCount;
	}

	/**
	 * Get the number of actual results.
	 * @return number of actual results
	 */
	public int getResultCount() {
		return this.resultCount;
	}

}
