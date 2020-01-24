/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.pagination;

import org.hibernate.engine.spi.RowSelection;

import static java.lang.String.valueOf;

/**
 * Superclass for {@link LimitHandler}s that don't support
 * offsets at all.
 *
 * @author Gavin King
 */
public abstract class AbstractNoOffsetLimitHandler extends AbstractLimitHandler {

	private boolean variableLimit;

	public AbstractNoOffsetLimitHandler(boolean variableLimit) {
		this.variableLimit = variableLimit;
	}

	/**
	 * The SQL fragment to insert, with a ? placeholder
	 * for the actual numerical limit.
	 */
	protected abstract String limitClause();

	protected abstract String insert(String limitClause, String sql);

	@Override
	public String processSql(String sql, RowSelection selection) {
		if ( !hasMaxRows( selection) ) {
			return sql;
		}
		String limitClause = limitClause();
		if ( !supportsVariableLimit() ) {
			String limit = valueOf( getMaxOrLimit(selection) );
			limitClause = limitClause.replace( "?", limit );
		}
		return insert( limitClause, sql );
	}

	@Override
	public final boolean supportsLimit() {
		return true;
	}

	@Override
	public final boolean supportsLimitOffset() {
		return false;
	}

	@Override
	public final boolean supportsVariableLimit() {
		return variableLimit;
	}

	@Override
	public abstract boolean bindLimitParametersFirst();

}
