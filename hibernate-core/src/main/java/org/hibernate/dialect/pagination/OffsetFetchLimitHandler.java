/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.pagination;

import org.hibernate.engine.spi.RowSelection;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * A {@link LimitHandler} for databases support the
 * ANSI SQL standard syntax {@code FETCH FIRST m ROWS ONLY}
 * and {@code OFFSET n ROWS FETCH NEXT m ROWS ONLY}.
 *
 * @author Gavin King
 */
public class OffsetFetchLimitHandler extends AbstractLimitHandler {

	public static final OffsetFetchLimitHandler INSTANCE = new OffsetFetchLimitHandler(true);

	private boolean variableLimit;

	public OffsetFetchLimitHandler(boolean variableLimit) {
		this.variableLimit = variableLimit;
	}

	@Override
	public String processSql(String sql, RowSelection selection) {

		boolean hasFirstRow = hasFirstRow(selection);
		boolean hasMaxRows = hasMaxRows(selection);

		if ( !hasFirstRow && !hasMaxRows ) {
			return sql;
		}

		StringBuilder offsetFetch = new StringBuilder();

		begin(sql, offsetFetch, hasFirstRow, hasMaxRows);

		if ( hasFirstRow ) {
			offsetFetch.append( " offset " );
			if ( supportsVariableLimit() ) {
				offsetFetch.append( "?" );
			}
			else {
				offsetFetch.append( selection.getFirstRow() );
			}
			if ( !isIngres() ) {
				offsetFetch.append( " rows" );
			}

		}
		if ( hasMaxRows ) {
			if ( hasFirstRow ) {
				offsetFetch.append( " fetch next " );
			}
			else {
				offsetFetch.append( " fetch first " );
			}
			if ( supportsVariableLimit() ) {
				offsetFetch.append( "?" );
			}
			else {
				offsetFetch.append( getMaxOrLimit( selection ) );
			}
			offsetFetch.append( " rows only" );
		}

		return insert( offsetFetch.toString(), sql );
	}

	void begin(String sql, StringBuilder offsetFetch, boolean hasFirstRow, boolean hasMaxRows) {}

	String insert(String offsetFetch, String sql) {
		return insertBeforeForUpdate( offsetFetch, sql );
	}

	@Override
	public final boolean supportsLimit() {
		return true;
	}

	@Override
	public boolean supportsOffset() {
		return true;
	}

	@Override
	public final boolean supportsVariableLimit() {
		return variableLimit;
	}

	boolean isIngres() {
		return false;
	}
}
