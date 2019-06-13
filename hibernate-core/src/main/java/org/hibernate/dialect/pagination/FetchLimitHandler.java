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
 * A {@link LimitHandler} for databases which support the ANSI
 * SQL standard syntax {@code FETCH FIRST m ROWS ONLY} but not
 * {@code OFFSET n ROWS}.
 *
 * @author Gavin King
 */
public class FetchLimitHandler extends AbstractLimitHandler {

	public static final FetchLimitHandler INSTANCE = new FetchLimitHandler(false);

	private boolean variableLimit;

	public FetchLimitHandler(boolean variableLimit) {
		this.variableLimit = variableLimit;
	}

	@Override
	public String processSql(String sql, RowSelection selection) {
		if ( !hasMaxRows( selection) ) {
			return sql;
		}
		String fetch = " fetch first ? rows only";
		if ( variableLimit ) {
			fetch = fetch.replace( "?", valueOf( getMaxOrLimit( selection ) ) );
		}
		return insertBeforeForUpdate( fetch, sql );
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

}
