/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.pagination;

import org.hibernate.engine.spi.RowSelection;

/**
 * A {@link LimitHandler} for databases like Oracle, Ingres,
 * and Apache Derby that support the ANSI SQL standard syntax
 * {@code FETCH FIRST m ROWS ONLY} and
 * {@code OFFSET n ROWS FETCH NEXT m ROWS ONLY}.
 *
 * @author Gavin King
 */
public class OffsetFetchLimitHandler extends AbstractLimitHandler {

	private boolean variableLimit;

	public OffsetFetchLimitHandler(boolean variableLimit) {
		this.variableLimit = variableLimit;
	}

	@Override
	public String processSql(String sql, RowSelection selection) {

		StringBuilder offsetFetch = new StringBuilder();
		boolean hasFirstRow = hasFirstRow(selection);
		boolean hasMaxRows = hasMaxRows(selection);

		if ( !hasFirstRow && !hasMaxRows ) {
			return sql;
		}

		if ( hasFirstRow ) {
			offsetFetch.append( " offset " );
			if ( variableLimit ) {
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
			if ( variableLimit ) {
				offsetFetch.append( "?" );
			}
			else {
				offsetFetch.append( getMaxOrLimit( selection ) );
			}
			offsetFetch.append( " rows only" );
		}

		return insertBeforeForUpdate( offsetFetch.toString(), sql );
	}

	@Override
	public final boolean supportsLimit() {
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
