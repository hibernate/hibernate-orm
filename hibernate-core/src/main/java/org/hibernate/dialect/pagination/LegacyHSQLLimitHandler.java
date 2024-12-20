/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
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
