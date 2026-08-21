/*
 * SPDX-License-Identifier: Apache-2.0
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
 * The performance impact of setting a batch size depends on whether
 * a SQL array may be used to pass the list of identifiers to the
 * database:
 * <ul>
 * <li>for databases which support standard SQL arrays, a smaller
 *     batch size might be extremely inefficient compared to a very
 *     large batch size or no batching at all, but
 * <li>on the other hand, for databases with no SQL array type, a
 *     large batch size results in long SQL statements with many JDBC
 *     parameters.
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
