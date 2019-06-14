/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.pagination;

import org.hibernate.engine.spi.RowSelection;

/**
 * A {@link LimitHandler} for databases like PostgreSQL, H2,
 * and HSQL that support the syntax {@code LIMIT n OFFSET m}.
 */
public class LimitOffsetLimitHandler extends AbstractLimitHandler {

	public static LimitOffsetLimitHandler INSTANCE = new LimitOffsetLimitHandler();

	@Override
	public String processSql(String sql, RowSelection selection) {
		String limitOffset = hasFirstRow( selection )
				? " limit ? offset ?"
				: " limit ?";
		//limit/offset comes before 'for update' on Postgres and H2
		return insertBeforeForUpdate( limitOffset, sql );
	}

	@Override
	public final boolean supportsLimit() {
		return true;
	}

	@Override
	public final boolean supportsVariableLimit() {
		return true;
	}

	@Override
	public final boolean bindLimitParametersInReverseOrder() {
		return true;
	}
}
