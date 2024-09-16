/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
 */
public class NonUniqueResultException extends HibernateException {
	/**
	 * Constructs a {@code NonUniqueResultException}.
	 *
	 * @param resultCount The number of actual results.
	 */
	public NonUniqueResultException(int resultCount) {
		super( "Query did not return a unique result: " + resultCount + " results were returned" );
	}

}
