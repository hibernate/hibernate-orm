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
 * LIMIT clause handler compatible with SQL Server 2012 and later.
 *
 * @author Chris Cranford
 */
public class SQLServer2012LimitHandler extends SQLServer2005LimitHandler {
	// determines whether the limit handler used offset/fetch or 2005 behavior.
	private boolean usedOffsetFetch;

	public SQLServer2012LimitHandler() {

	}

	@Override
	public boolean supportsLimit() {
		return true;
	}

	@Override
	public boolean supportsVariableLimit() {
		return true;
	}

	@Override
	public String processSql(String sql, RowSelection selection) {
		// SQLServer mandates the following rules to use OFFSET/LIMIT
		//  * An 'ORDER BY' is required
		//  * The 'OFFSET ...' clause is mandatory, cannot use 'FETCH ...' by itself.
		//  * The 'TOP' clause isn't permitted with LIMIT/OFFSET.
		if ( hasOrderBy( sql ) ) {
			if ( !LimitHelper.useLimit( this, selection ) ) {
				return sql;
			}
			return applyOffsetFetch( selection, sql, getInsertPosition( sql ) );
		}
		return super.processSql( sql, selection );
	}

	@Override
	public boolean useMaxForLimit() {
		// when using the offset fetch clause, the max value is passed as-is.
		// SQLServer2005LimitHandler uses start + max values.
		return usedOffsetFetch ? false : super.useMaxForLimit();
	}

	@Override
	public int convertToFirstRowValue(int zeroBasedFirstResult) {
		// When using the offset/fetch clause, the first row is passed as-is
		// SQLServer2005LimitHandler uses zeroBasedFirstResult + 1
		if ( usedOffsetFetch ) {
			return zeroBasedFirstResult;
		}
		return super.convertToFirstRowValue( zeroBasedFirstResult );
	}

	@Override
	public int bindLimitParametersAtEndOfQuery(RowSelection selection, PreparedStatement statement, int index)
	throws SQLException {
		if ( usedOffsetFetch && !LimitHelper.hasFirstRow( selection ) ) {
			// apply just the max value when offset fetch applied
			statement.setInt( index, getMaxOrLimit( selection ) );
			return 1;
		}
		return super.bindLimitParametersAtEndOfQuery( selection, statement, index );
	}

	private String getOffsetFetch(RowSelection selection) {
		if ( !LimitHelper.hasFirstRow( selection ) ) {
			return " offset 0 rows fetch next ? rows only";
		}
		return " offset ? rows fetch next ? rows only";
	}

	private int getInsertPosition(String sql) {
		int position = sql.length() - 1;
		for ( ; position > 0; --position ) {
			char ch = sql.charAt( position );
			if ( ch != ';' && ch != ' ' && ch != '\r' && ch != '\n' ) {
				break;
			}
		}
		return position + 1;
	}

	private String applyOffsetFetch(RowSelection selection, String sql, int position) {
		usedOffsetFetch = true;

		StringBuilder sb = new StringBuilder();
		sb.append( sql.substring( 0, position ) );
		sb.append( getOffsetFetch( selection ) );
		if ( position > sql.length() ) {
			sb.append( sql.substring( position - 1 ) );
		}

		return sb.toString();
	}

	private boolean hasOrderBy(String sql) {
		int depth = 0;

		String lowerCaseSQL = sql.toLowerCase();

		for ( int i = lowerCaseSQL.length() - 1; i >= 0; --i ) {
			char ch = lowerCaseSQL.charAt( i );
			if ( ch == '(' ) {
				depth++;
			}
			else if ( ch == ')' ) {
				depth--;
			}
			if ( depth == 0 ) {
				if ( lowerCaseSQL.startsWith( "order by ", i ) ) {
					return true;
				}
			}
		}
		return false;
	}
}
