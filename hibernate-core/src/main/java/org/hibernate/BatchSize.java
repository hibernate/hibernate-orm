/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import jakarta.persistence.FindOption;

import java.util.List;

/**
 * Specify a batch size, that is, how many entities should be
 * fetched in each request to the database, for an invocation of
 * {@link Session#findMultiple(Class, List, FindOption...)}.
 * <ul>
 * <li>By default, the batch sizing strategy is determined by the
 *     {@linkplain org.hibernate.dialect.Dialect#getBatchLoadSizingStrategy
 *    SQL dialect}, but
 * <li>if some {@code batchSize>1} is specified as an
 *     argument to this method, then that batch size will be used.
 * </ul>
 * <p>
 * If an explicit batch size is set manually, care should be taken
 * to not exceed the capabilities of the underlying database.
 * <p>
 * A batch size is considered a hint. This option has no effect
 * on {@link Session#find(Class, Object, FindOption...)}.
 *
 * @param batchSize The batch size
 *
 * @see Session#findMultiple
 * @see MultiIdentifierLoadAccess#withBatchSize
 *
 * @since 7.0
 *
 * @author Gavin King
 */
public record BatchSize(int batchSize) implements FindOption {
}
