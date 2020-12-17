/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.pagination;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.engine.spi.RowSelection;
import org.hibernate.query.Limit;
import org.hibernate.query.spi.QueryOptions;

/**
 * Contract defining dialect-specific limit and offset handling.
 * Most implementations extend {@link AbstractLimitHandler}.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
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

	default String processSql(String sql, Limit limit) {
		return processSql(
				sql,
				limit == null ? null : new RowSelection(
						limit.getFirstRow(),
						limit.getMaxRows(),
						null,
						null
				)
		);
	}

	default int bindLimitParametersAtStartOfQuery(Limit limit, PreparedStatement statement, int index)
			throws SQLException {
		return bindLimitParametersAtStartOfQuery(
				limit == null ? null : new RowSelection(
						limit.getFirstRow(),
						limit.getMaxRows(),
						null,
						null
				),
				statement,
				index
		);
	}

	default int bindLimitParametersAtEndOfQuery(Limit limit, PreparedStatement statement, int index)
			throws SQLException {
		return bindLimitParametersAtEndOfQuery(
				limit == null ? null : new RowSelection(
						limit.getFirstRow(),
						limit.getMaxRows(),
						null,
						null
				),
				statement,
				index
		);
	}

	default void setMaxRows(Limit limit, PreparedStatement statement) throws SQLException {
		setMaxRows(
				limit == null ? null : new RowSelection(
						limit.getFirstRow(),
						limit.getMaxRows(),
						null,
						null
				),
				statement
		);
	}

	/**
	 * Return processed SQL query.
	 *
     * @param sql       the SQL query to process.
     * @param selection the selection criteria for rows.
     *
	 * @return Query statement with LIMIT clause applied.
	 * @deprecated todo (6.0): remove in favor of Limit version?
	 */
	@Deprecated
	String processSql(String sql, RowSelection selection);

	/**
	 * Bind parameter values needed by the limit and offset clauses
	 * right at the start of the original query statement, before all
	 * the other query parameters.
	 *
     * @param selection the selection criteria for rows.
	 * @param statement Statement to which to bind limit parameter values.
	 * @param index Index from which to start binding.
	 * @return The number of parameter values bound.
	 * @throws SQLException Indicates problems binding parameter values.
	 * @deprecated todo (6.0): remove in favor of Limit version?
	 */
	@Deprecated
	int bindLimitParametersAtStartOfQuery(RowSelection selection, PreparedStatement statement, int index)
			throws SQLException;

	/**
	 * Bind parameter values needed by the limit and offset clauses
	 * right at the end of the original query statement, after all
	 * the other query parameters.
	 *
     * @param selection the selection criteria for rows.
	 * @param statement Statement to which to bind limit parameter values.
	 * @param index Index from which to start binding.
	 * @return The number of parameter values bound.
	 * @throws SQLException Indicates problems binding parameter values.
	 * @deprecated todo (6.0): remove in favor of Limit version?
	 */
	@Deprecated
	int bindLimitParametersAtEndOfQuery(RowSelection selection, PreparedStatement statement, int index)
			throws SQLException;

	/**
	 * Use JDBC APIs to limit the number of rows returned by the SQL query.
	 * Handlers that do not support a SQL limit clause should implement this
	 * method.
	 *
     * @param selection the selection criteria for rows.
	 * @param statement Statement which number of returned rows shall be limited.
	 * @throws SQLException Indicates problems while limiting maximum rows returned.
	 * @deprecated todo (6.0): remove in favor of Limit version?
	 */
	@Deprecated
	void setMaxRows(RowSelection selection, PreparedStatement statement) throws SQLException;
}
