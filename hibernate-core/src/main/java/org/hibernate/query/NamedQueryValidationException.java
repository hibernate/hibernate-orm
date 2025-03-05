/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

import org.hibernate.HibernateException;
import org.hibernate.QueryException;

import java.util.Map;

/**
 * Indicates that validation and translation of one or more named
 * queries failed at initialization time. This exception packages
 * every {@link org.hibernate.QueryException} that occurred for an
 * invalid HQL/JPQL query, together with any exceptions that indicate
 * problems with named native SQL queries.
 *
 * @author Gavin King
 */
public class NamedQueryValidationException extends QueryException {
	private final Map<String, HibernateException> errors;

	public NamedQueryValidationException(String message, Map<String, HibernateException> errors) {
		super( message );
		this.errors = errors;
	}

	/**
	 * A map from query name to the error that occurred while
	 * interpreting or translating the named query.
	 */
	public Map<String, HibernateException> getErrors() {
		return errors;
	}
}
