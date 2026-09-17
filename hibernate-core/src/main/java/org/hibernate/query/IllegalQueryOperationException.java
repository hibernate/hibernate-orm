/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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
	public IllegalQueryOperationException(@Nonnull String message) {
		super( message );
	}

	public IllegalQueryOperationException(@Nonnull String message, @Nullable String queryString, @Nullable Exception cause) {
		super( message, queryString, cause );
	}
}
