/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

import java.util.List;

import jakarta.annotation.Nonnull;

/**
 * Used as part of circularity detection
 * <p>
 * Uniquely distinguishes a side of the foreign-key, using
 * that side's table and column(s)
 *
 * @see Association#resolveCircularFetch
 *
 * @author Andrea Boriero
 */
public record AssociationKey(@Nonnull String table, @Nonnull List<String> columns) {
}
