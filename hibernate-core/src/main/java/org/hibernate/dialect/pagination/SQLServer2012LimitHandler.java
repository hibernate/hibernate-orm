/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.pagination;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.dialect.pagination.SQLServer2005LimitHandler.Keyword;
import org.hibernate.engine.spi.RowSelection;

/**
 * A {@link LimitHandler} compatible with SQL Server 2012 and later.
 * <p>
 * SQL Server 2012 introduced support for ANSI SQL-style
 * {@code OFFSET m ROWS FETCH NEXT n ROWS ONLY}, though this syntax
 * is considered part of the {@code ORDER BY} clause.
 *
 * @author Chris Cranford
 * @author Gavin King
 */
public class SQLServer2012LimitHandler extends AbstractLimitHandler {

	@Override
	public final boolean supportsLimit() {
		return true;
	}

	@Override
	public final boolean supportsVariableLimit() {
		return true;
	}

	/**
	 * {@code OFFSET} and {@code FETCH} have to come right at the end
	 * of the {@code ORDER BY} clause, and {@code OFFSET} is required
	 * in order to have a {@code FETCH}:
	 * <pre>order by ... offset m rows [fetch next n rows only]</pre>
	 */
	@Override
	public String processSql(String sql, RowSelection selection) {
		//see https://docs.microsoft.com/en-us/sql/t-sql/queries/select-order-by-clause-transact-sql?view=sql-server-ver15
		String offsetFetch = hasFirstRow( selection )
				? " offset ? rows fetch next ? rows only"
				: " offset 0 rows fetch next ? rows only";
		if ( Keyword.ORDER_BY.rootOffset( sql ) <= 0 ) {
			//we need to add a whole 'order by' clause
			offsetFetch = " order by 1" + offsetFetch;
		}
		return insertAtEnd( offsetFetch, sql );
	}

	@Override
	public int bindLimitParametersAtEndOfQuery(RowSelection selection, PreparedStatement statement, int index)
	throws SQLException {
		if ( !hasFirstRow( selection ) ) {
			// apply just the max value when offset fetch applied
			statement.setInt( index, getMaxOrLimit( selection ) );
			return 1;
		}
		else {
			return super.bindLimitParametersAtEndOfQuery( selection, statement, index );
		}
	}

}
