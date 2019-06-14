/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.pagination;

/**
 * A {@link LimitHandler} for HSQL prior to 2.0.
 */
public class LegacyHSQLLimitHandler extends AbstractSimpleLimitHandler {

	public static LegacyHSQLLimitHandler INSTANCE = new LegacyHSQLLimitHandler();

	@Override
	protected String limitClause(boolean hasFirstRow) {
		return hasFirstRow ? " limit ? ?" : " top ?";
	}

	@Override
	protected String insert(String limitOrTop, String sql) {
		return insertAfterSelect( limitOrTop, sql );
	}

	@Override
	public final boolean bindLimitParametersFirst() {
		return true;
	}
}
