/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Indicates an attempt to create a {@linkplain MutationQuery} with a non-mutation
 * query (generally a select query).
 *
 * @author Steve Ebersole
 */
public class IllegalMutationQueryException extends IllegalQueryOperationException {
	public IllegalMutationQueryException(@Nonnull String message) {
		super( message );
	}

	public IllegalMutationQueryException(@Nonnull String message, @Nullable String queryString) {
		super( message, queryString, null );
	}
}
