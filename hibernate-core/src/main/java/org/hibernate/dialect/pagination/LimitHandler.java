package org.hibernate.dialect.pagination;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.engine.spi.RowSelection;

/**
 * Contract defining dialect-specific LIMIT clause handling. Typically implementers might consider extending
 * {@link AbstractLimitHandler} class.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public interface LimitHandler {
	/**
	 * Does this handler support some form of limiting query results
	 * via a SQL clause?
	 *
	 * @return True if this handler supports some form of LIMIT.
	 */
	public boolean supportsLimit();

	/**
	 * Does this handler's LIMIT support (if any) additionally
	 * support specifying an offset?
	 *
	 * @return True if the handler supports an offset within the limit support.
	 */
	public boolean supportsLimitOffset();

	/**
	 * Return processed SQL query.
	 *
	 * @return Query statement with LIMIT clause applied.
	 */
	public String getProcessedSql();

	/**
	 * Bind parameter values needed by the LIMIT clause before original SELECT statement.
	 *
	 * @param statement Statement to which to bind limit parameter values.
	 * @param index Index from which to start binding.
	 * @return The number of parameter values bound.
	 * @throws SQLException Indicates problems binding parameter values.
	 */
	public int bindLimitParametersAtStartOfQuery(PreparedStatement statement, int index) throws SQLException;

	/**
	 * Bind parameter values needed by the LIMIT clause after original SELECT statement.
	 *
	 * @param statement Statement to which to bind limit parameter values.
	 * @param index Index from which to start binding.
	 * @return The number of parameter values bound.
	 * @throws SQLException Indicates problems binding parameter values.
	 */
	public int bindLimitParametersAtEndOfQuery(PreparedStatement statement, int index) throws SQLException;

	/**
	 * Use JDBC API to limit the number of rows returned by the SQL query. Typically handlers that do not
	 * support LIMIT clause should implement this method.
	 *
	 * @param statement Statement which number of returned rows shall be limited.
	 * @throws SQLException Indicates problems while limiting maximum rows returned.
	 */
	public void setMaxRows(PreparedStatement statement) throws SQLException;
}
