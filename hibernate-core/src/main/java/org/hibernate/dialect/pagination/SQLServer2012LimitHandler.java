/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.pagination;

import org.hibernate.engine.spi.RowSelection;

/**
 * LIMIT clause handler compatible with SQL Server 2012 and later.
 *
 * @author Chris Cranford
 */
public class SQLServer2012LimitHandler extends SQLServer2005LimitHandler {
	public static final SQLServer2012LimitHandler INSTANCE = new SQLServer2012LimitHandler();

	private SQLServer2012LimitHandler() {

	}

	@Override
	public boolean supportsLimit() {
		return true;
	}

	@Override
	public boolean supportsVariableLimit() {
		return false;
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
			int firstRow = LimitHelper.hasFirstRow( selection ) ? selection.getFirstRow() : 0;
			return sql + String.format( " offset %d rows fetch next %d rows only", firstRow, selection.getMaxRows() );
		}
		return super.processSql( sql, selection );
	}

	private boolean hasOrderBy(String sql) {
		int depth = 0;
		for ( int i = 0; i < sql.length(); ++i ) {
			char ch = sql.charAt( i );
			if ( ch == '(' ) {
				depth++;
			}
			else if ( ch == ')' ) {
				depth--;
			}
			if ( depth == 0 ) {
				if ( sql.substring( i ).toLowerCase().startsWith( "order by " ) ) {
					return true;
				}
			}
		}
		return false;
	}
}
