/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.pagination;

import org.hibernate.dialect.pagination.AbstractSimpleLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;

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
