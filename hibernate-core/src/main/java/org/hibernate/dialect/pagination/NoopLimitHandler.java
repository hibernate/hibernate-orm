/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.pagination;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.regex.Pattern;

import org.hibernate.engine.spi.RowSelection;
import org.hibernate.query.Limit;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.compile;

/**
 * Handler not supporting query LIMIT clause. JDBC API is used to set maximum number of returned rows.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class NoopLimitHandler extends AbstractLimitHandler {

	public static final NoopLimitHandler INSTANCE = new NoopLimitHandler();

	@Override
	public String processSql(String sql, RowSelection selection) {
		return sql;
	}

	@Override
	public int bindLimitParametersAtStartOfQuery(RowSelection selection, PreparedStatement statement, int index) {
		return 0;
	}

	@Override
	public int bindLimitParametersAtEndOfQuery(RowSelection selection, PreparedStatement statement, int index) {
		return 0;
	}

	@Override
	public void setMaxRows(RowSelection selection, PreparedStatement statement) throws SQLException {
		if ( selection != null && selection.getMaxRows() != null && selection.getMaxRows() > 0 ) {
			final int maxRows = selection.getMaxRows() + convertToFirstRowValue(
					selection.getFirstRow() == null ? 0 : selection.getFirstRow()
			);
			// Use Integer.MAX_VALUE on overflow
			if ( maxRows < 0 ) {
				statement.setMaxRows( Integer.MAX_VALUE );
			}
			else {
				statement.setMaxRows( maxRows );
			}
		}
	}

	@Override
	public String processSql(String sql, Limit limit) {
		return sql;
	}

	@Override
	public int bindLimitParametersAtStartOfQuery(Limit limit, PreparedStatement statement, int index) {
		return 0;
	}

	@Override
	public int bindLimitParametersAtEndOfQuery(Limit limit, PreparedStatement statement, int index) {
		return 0;
	}

	@Override
	public void setMaxRows(Limit limit, PreparedStatement statement) throws SQLException {
		if ( limit != null && limit.getMaxRows() != null && limit.getMaxRows() > 0 ) {
			final int maxRows = limit.getMaxRows() + convertToFirstRowValue(
					limit.getFirstRow() == null ? 0 : limit.getFirstRow()
			);
			// Use Integer.MAX_VALUE on overflow
			if ( maxRows < 0 ) {
				statement.setMaxRows( Integer.MAX_VALUE );
			}
			else {
				statement.setMaxRows( maxRows );
			}
		}
	}
}
