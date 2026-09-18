/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.QueryException;

/**
 * Represents a syntax error in a HQL/JPQL query.
 *
 * @author Gavin King
 *
 * @see SemanticException
 *
 * @since 6.3
 */
public class SyntaxException extends QueryException {
	public SyntaxException(@Nonnull String message, @Nullable String queryString) {
		super( message, queryString );
	}
	public SyntaxException(@Nonnull String message) {
		super( message );
	}
}
