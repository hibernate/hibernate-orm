/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.pagination;

import org.hibernate.engine.spi.RowSelection;

/**
 * A {@link LimitHandler} for databases which support the ANSI
 * SQL standard syntax {@code FETCH FIRST m ROWS ONLY} but not
 * {@code OFFSET n ROWS}.
 *
 * @author Gavin King
 */
public class FetchLimitHandler extends AbstractLimitHandler {

	public static final FetchLimitHandler INSTANCE = new FetchLimitHandler();

	@Override
	public String processSql(String sql, RowSelection selection) {
		if ( !hasMaxRows( selection) ) {
			return sql;
		}
		String fetch = supportsVariableLimit()
				? " fetch first ? rows only"
				: " fetch first " + getMaxOrLimit( selection ) + " rows only";
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
	public boolean supportsVariableLimit() {
		return false;
	}

}
