/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Indicates an attempt to create a {@linkplain SelectionQuery} instance
 * with a non-selection query (generally a mutation query)
 *
 * @author Steve Ebersole
 */
public class IllegalSelectQueryException extends IllegalQueryOperationException {
	public IllegalSelectQueryException(@Nonnull String message) {
		super( message );
	}

	public IllegalSelectQueryException(@Nonnull String message, @Nullable String queryString) {
		super( message, queryString, null );
	}
}
