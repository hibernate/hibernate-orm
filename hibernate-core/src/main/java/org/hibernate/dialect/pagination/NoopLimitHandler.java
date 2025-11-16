/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.pagination;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.query.spi.Limit;


/**
 * Handler not supporting query LIMIT clause. JDBC API is used to set maximum number of returned rows.
 *
 * @author Lukasz Antoniak
 */
public class NoopLimitHandler extends AbstractLimitHandler {

	public static final NoopLimitHandler INSTANCE = new NoopLimitHandler();

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
		if ( hasMaxRows( limit ) ) {
			final Integer firstRow = limit.getFirstRow();
			final int convertedMaxRows =
					limit.getMaxRows()
					+ convertToFirstRowValue( firstRow == null ? 0 : firstRow );
			// Use Integer.MAX_VALUE on overflow
			statement.setMaxRows( convertedMaxRows < 0 ? Integer.MAX_VALUE : convertedMaxRows );
		}
	}

	@Override
	public boolean processSqlMutatesState() {
		return false;
	}
}
