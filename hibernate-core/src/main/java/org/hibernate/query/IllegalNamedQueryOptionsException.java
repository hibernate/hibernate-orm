/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

import org.hibernate.QueryException;

/**
 * Indicates a named-query has specified options that are not legal
 *
 * @author Steve Ebersole
 */
public class IllegalNamedQueryOptionsException extends QueryException {
	public IllegalNamedQueryOptionsException(String message) {
		super( message );
	}
}
