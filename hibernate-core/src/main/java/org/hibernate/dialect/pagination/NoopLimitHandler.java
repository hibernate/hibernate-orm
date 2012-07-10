package org.hibernate.dialect.pagination;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.engine.spi.RowSelection;

/**
 * Handler not supporting query LIMIT clause. JDBC API is used to set maximum number of returned rows.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class NoopLimitHandler extends AbstractLimitHandler {
	public NoopLimitHandler(String sql, RowSelection selection) {
		super( sql, selection );
	}

	public String getProcessedSql() {
		return sql;
	}

	public int bindLimitParametersAtStartOfQuery(PreparedStatement statement, int index) {
		return 0;
	}

	public int bindLimitParametersAtEndOfQuery(PreparedStatement statement, int index) {
		return 0;
	}

	public void setMaxRows(PreparedStatement statement) throws SQLException {
		if ( LimitHelper.hasMaxRows( selection ) ) {
			statement.setMaxRows( selection.getMaxRows() + convertToFirstRowValue( LimitHelper.getFirstRow( selection ) ) );
		}
	}
}
