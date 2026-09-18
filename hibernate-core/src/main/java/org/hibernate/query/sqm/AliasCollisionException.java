/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.query.SemanticException;

/**
 * Occurs when the HQL query declares a duplicate identification variable
 * in the {@code from} clause, or a duplicate result column alias in the
 * {@code select} clause.
 *
 * @author Andrea Boriero
 */
public class AliasCollisionException extends SemanticException {
	public AliasCollisionException(@Nonnull String message) {
		super( message );
	}

	public AliasCollisionException(@Nonnull String message, @Nullable Exception cause) {
		super( message, cause );
	}
}
