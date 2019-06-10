/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.pagination;

import org.hibernate.engine.spi.RowSelection;

/**
 * A {@link LimitHandler} for HSQL prior to 2.0.
 */
public class LegacyHSQLLimitHandler extends AbstractLimitHandler {

	public static LegacyHSQLLimitHandler INSTANCE = new LegacyHSQLLimitHandler();

	@Override
	public String processSql(String sql, RowSelection selection) {
		if ( !hasMaxRows( selection) ) {
			return sql;
		}
		String limitOrTop = hasFirstRow( selection )
				? " limit ? ?"
				: " top ?";
		return insertAfterSelect( limitOrTop, sql );
	}

	@Override
	public final boolean supportsLimit() {
		return true;
	}

	@Override
	public final boolean bindLimitParametersFirst() {
		return true;
	}
}
