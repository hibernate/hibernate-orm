/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
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
	/**
	 * Constructs a NoopLimitHandler
	 *
	 * @param sql The SQL
	 * @param selection The row selection options
	 */
	public NoopLimitHandler(String sql, RowSelection selection) {
		super( sql, selection );
	}

	@Override
	public String getProcessedSql() {
		return sql;
	}

	@Override
	public int bindLimitParametersAtStartOfQuery(PreparedStatement statement, int index) {
		return 0;
	}

	@Override
	public int bindLimitParametersAtEndOfQuery(PreparedStatement statement, int index) {
		return 0;
	}

	@Override
	public void setMaxRows(PreparedStatement statement) throws SQLException {
		if ( LimitHelper.hasMaxRows( selection ) ) {
			statement.setMaxRows( selection.getMaxRows() + convertToFirstRowValue( LimitHelper.getFirstRow( selection ) ) );
		}
	}
}
