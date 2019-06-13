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

/**
 * A {@link LimitHandler} compatible with SQL Server 2012 and later.
 * <p>
 * SQL Server 2012 introduced support for ANSI SQL-style
 * {@code OFFSET m ROWS FETCH NEXT n ROWS ONLY}, though it's not
 * mentioned in the online documentation.
 *
 * @author Chris Cranford
 */
public class SQLServer2012LimitHandler extends SQLServer2005LimitHandler {

	// records whether the limit handler used offset/fetch or 2005 behavior
	private boolean usedOffsetFetch;

	/**
	 * SQLServer mandates the following rules to use
	 * 'OFFSET'/'FETCH':
	 * <ul>
	 *     <li>an 'ORDER BY' is required</li>
	 *     <li>The 'OFFSET' clause is mandatory,
	 *         cannot use 'FETCH ...' by itself</li>
	 *     <li>The 'TOP' clause isn't permitted</li>
	 * </ul>
	 */
	@Override
	public String processSql(String sql, RowSelection selection) {
		if ( Keyword.ORDER_BY.rootOffset( sql ) > 0 ) {
			//if it has an 'order by' clause, we can use offset/fetch
			usedOffsetFetch = true;
			String offsetFetch = hasFirstRow( selection )
					? " offset ? rows fetch next ? rows only"
					: " offset 0 rows fetch next ? rows only";
			return insertAtEnd( offsetFetch, sql );
		}
		else {
			//otherwise do it the hard way
			return super.processSql( sql, selection );
		}
	}

	@Override
	public boolean useMaxForLimit() {
		// when using the offset fetch clause, the max value is passed as-is.
		// SQLServer2005LimitHandler uses start + max values.
		return !usedOffsetFetch && super.useMaxForLimit();
	}

	@Override
	public int convertToFirstRowValue(int zeroBasedFirstResult) {
		// When using the offset/fetch clause, the first row is passed as-is
		// SQLServer2005LimitHandler uses zeroBasedFirstResult + 1
		return usedOffsetFetch
				? zeroBasedFirstResult
				: super.convertToFirstRowValue( zeroBasedFirstResult );
	}

	@Override
	public int bindLimitParametersAtEndOfQuery(RowSelection selection, PreparedStatement statement, int index)
	throws SQLException {
		if ( usedOffsetFetch && !hasFirstRow( selection ) ) {
			// apply just the max value when offset fetch applied
			statement.setInt( index, getMaxOrLimit( selection ) );
			return 1;
		}
		else {
			return super.bindLimitParametersAtEndOfQuery( selection, statement, index );
		}
	}

}
