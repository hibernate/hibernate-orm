/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

import org.hibernate.HibernateException;

/**
 * Indicates a mismatch between the expected and actual result types of a query.
 *
 * @author Steve Ebersole
 */
public class QueryTypeMismatchException extends HibernateException {
	public QueryTypeMismatchException(String message) {
		super( message );
	}

	public QueryTypeMismatchException(String message, Throwable cause) {
		super( message, cause );
	}
}
