/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.pagination;

import org.hibernate.dialect.pagination.LimitLimitHandler;

/**
 * Limit handler for {@link org.hibernate.community.dialect.AltibaseDialect}.
 *
 * @author Geoffrey park
 */
public class AltibaseLimitHandler extends LimitLimitHandler {
	public static final AltibaseLimitHandler INSTANCE = new AltibaseLimitHandler();

	@Override
	protected String limitClause(boolean hasFirstRow) {
		return hasFirstRow ? " limit 1+?,?" : " limit ?";
	}

	@Override
	protected String offsetOnlyClause() {
		return " limit 1+?," + Integer.MAX_VALUE;
	}
}
