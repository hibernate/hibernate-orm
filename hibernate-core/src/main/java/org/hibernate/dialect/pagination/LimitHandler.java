/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.pagination;

import java.sql.PreparedStatement;
import java.sql.SQLException;

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

	String processSql(String sql, Limit limit);

	@Deprecated // Never called directly by Hibernate ORM
	default String processSql(String sql, Limit limit, QueryOptions queryOptions) {
		return processSql( sql, limit );
	}

	/**
	* Currently all Dialects with non-standard native parameter renderers invariably rely on {@link OffsetFetchLimitHandler},
	* so the method below is currently only overridden in it. However, if other {@link LimitHandler}s are involved in the future
	* (e.g. {@link Limit} related parameters need to be inserted after select), code refactoring is required (HHH-18624).
	*/
	// This is the one called directly by Hibernate ORM
	default String processSql(String sql, int jdbcParameterBindingsCnt, ParameterMarkerStrategy parameterMarkerStrategy, Limit limit, QueryOptions queryOptions) {
		return processSql( sql, limit );
	}

	int bindLimitParametersAtStartOfQuery(Limit limit, PreparedStatement statement, int index) throws SQLException;

	int bindLimitParametersAtEndOfQuery(Limit limit, PreparedStatement statement, int index) throws SQLException;

	void setMaxRows(Limit limit, PreparedStatement statement) throws SQLException;

}
