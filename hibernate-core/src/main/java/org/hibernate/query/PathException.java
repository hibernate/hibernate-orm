/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Indicates a problem with a path expression in HQL/JPQL.
 *
 * @author Steve Ebersole
 *
 * @see org.hibernate.query.sqm.UnknownPathException
 */
public class PathException extends SemanticException {
	public PathException(@Nonnull String message) {
		super( message );
	}

	public PathException(@Nonnull String message, @Nullable Exception cause) {
		super( message, cause );
	}

	public PathException(@Nonnull String message, @Nullable String hql, @Nullable Exception cause) {
		super(message, hql, cause);
	}
}
