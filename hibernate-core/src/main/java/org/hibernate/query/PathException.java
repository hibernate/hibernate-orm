/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

/**
 * Indicates a problem with a path expression in HQL/JPQL.
 *
 * @author Steve Ebersole
 *
 * @see org.hibernate.query.sqm.UnknownPathException
 */
public class PathException extends SemanticException {
	public PathException(String message) {
		super( message );
	}

	public PathException(String message, Exception cause) {
		super( message, cause );
	}

	public PathException(String message, String hql, Exception cause) {
		super(message, hql, cause);
	}
}
