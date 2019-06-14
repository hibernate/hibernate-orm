/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.pagination;

import org.hibernate.engine.spi.RowSelection;

/**
 * A {@link LimitHandler} for Informix which supports the syntax
 * {@code SKIP m FIRST n}.
 */
public class SkipFirstLimitHandler extends AbstractLimitHandler {

	private boolean variableLimit;

	public SkipFirstLimitHandler(boolean variableLimit) {
		this.variableLimit = variableLimit;
	}

	@Override
	public String processSql(String sql, RowSelection selection) {

		boolean hasFirstRow = hasFirstRow( selection );
		boolean hasMaxRows = hasMaxRows( selection );

		if ( !hasFirstRow && !hasMaxRows ) {
			return sql;
		}

		StringBuilder skipFirst = new StringBuilder();

		if ( supportsVariableLimit() ) {
			if ( hasFirstRow ) {
				skipFirst.append( " skip ?" );
			}
			if ( hasMaxRows ) {
				skipFirst.append( " first ?" );
			}
		}
		else {
			if ( hasFirstRow ) {
				skipFirst.append( " skip " )
						.append( selection.getFirstRow() );
			}
			if ( hasMaxRows ) {
				skipFirst.append( " first " )
						.append( getMaxOrLimit( selection ) );
			}
		}

		return insertAfterSelect( sql, skipFirst.toString() );
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
	public final boolean bindLimitParametersFirst() {
		return true;
	}

	@Override
	public final boolean supportsVariableLimit() {
		return variableLimit;
	}
}
