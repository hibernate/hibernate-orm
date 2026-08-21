/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.spi;

import org.hibernate.dialect.Dialect;

/**
 * Strategy for determining an optimal size for loading by multiple keys.  The
 * optimal size is defined as the most appropriate number of key values to load
 * in any single SQL query.
 *
 * @apiNote This is used with IN-list style loading to determine the number
 * of keys to encode into the SQL restriction to make sure we do not exceed
 * database/driver limits on the number of JDBC parameters.  Generally, prefer
 * using a SQL ARRAY parameter for the keys instead if the database/driver
 * supports it.
 *
 * @see Dialect#getMultiKeyLoadSizingStrategy()
 * @see org.hibernate.annotations.BatchSize
 * @see org.hibernate.Session#byMultipleIds
 * @see org.hibernate.Session#byMultipleNaturalId
 *
 * @author Steve Ebersole
 */
@FunctionalInterface
public interface MultiKeyLoadSizingStrategy {
	/**
	 * Determine the optimal batch size (number of key values) to load at a time.
	 * <p/>
	 * The return can be less than the total {@code numberOfKeys} to be loaded indicating
	 * that the load should be split across multiple SQL queries.  E.g. if we are loading
	 * 7 keys and the strategy says the optimal size is 5, we will perform 2 queries.
	 * <p/>
	 * @apiNote
	 *
	 * @param numberOfKeyColumns The number of columns to which the key is mapped
	 * @param numberOfKeys The total number of keys we need to load
	 * @param inClauseParameterPaddingEnabled See {@link org.hibernate.cfg.AvailableSettings#IN_CLAUSE_PARAMETER_PADDING}
	 *
	 * @return The number of keys to load at once.  The total number of JDBC parameters needed for that load is
	 * defined by {@code numberOfKeys} * {@code numberOfKeyColumns}. The strategy should take care to ensure that
	 * {@code numberOfKeys} * {@code numberOfKeyColumns} does not exceed any database/driver limits on the number
	 * of parameters allowed in a {@linkplain java.sql.PreparedStatement}.
	 */
	int determineOptimalBatchLoadSize(int numberOfKeyColumns, int numberOfKeys, boolean inClauseParameterPaddingEnabled);
}
