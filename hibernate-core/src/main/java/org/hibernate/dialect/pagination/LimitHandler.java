/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.pagination;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.query.spi.Limit;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.spi.ParameterMarkerStrategy;

/**
 * Contract defining dialect-specific limit and offset handling.
 * Most implementations extend {@link AbstractLimitHandler}.
 *
 * @author Lukasz Antoniak
 */
public interface LimitHandler {
	/**
	 * Does this handler support limiting query results?
	 *
	 * @return True if this handler supports limit alone.
	 */
	boolean supportsLimit();

	/**
	 * Does this handler support offsetting query results without
	 * also specifying a limit?
	 *
	 * @return True if this handler supports offset alone.
	 */
	boolean supportsOffset();

	/**
	 * Does this handler support combinations of limit and offset?
	 *
	 * @return True if the handler supports an offset within the limit support.
	 */
	boolean supportsLimitOffset();

	@Deprecated(forRemoval = true)
	String processSql(String sql, Limit limit);

	@Deprecated(forRemoval = true)
	default String processSql(String sql, Limit limit, QueryOptions queryOptions) {
		return processSql( sql, limit );
	}

	/**
	 * Applies the limit from the {@link QueryOptions} to the SQL with the given {@link ParameterMarkerStrategy}.
	 *
	 * @since 7.1
	 */
	default String processSql(String sql, int jdbcParameterCount, @Nullable ParameterMarkerStrategy parameterMarkerStrategy, QueryOptions queryOptions) {
		return processSql( sql, queryOptions.getLimit(), queryOptions );
	}

	int bindLimitParametersAtStartOfQuery(Limit limit, PreparedStatement statement, int index) throws SQLException;

	int bindLimitParametersAtEndOfQuery(Limit limit, PreparedStatement statement, int index) throws SQLException;

	/**
	 * Returns whether {@link #processSql(String, int, ParameterMarkerStrategy, QueryOptions)}  mutates the state of this limit handler and
	 * needs to be called for certain other methods to work correctly.
	 *
	 * @since 7.1
	 */
	default boolean processSqlMutatesState() {
		return true;
	}

	/**
	 * Returns the position at which to start binding parameters after {@link #bindLimitParametersAtStartOfQuery(Limit, PreparedStatement, int)}.
	 *
	 * @since 7.1
	 */
	default int getParameterPositionStart(Limit limit) {
		return 1;
	}

	void setMaxRows(Limit limit, PreparedStatement statement) throws SQLException;

}
