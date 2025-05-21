/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.internal;

import org.hibernate.sql.ast.spi.ForUpdateClauseStrategy;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.from.TableGroup;

/**
 * ForUpdateClauseStrategy implementation for cases when a dialect
 * applies locking in the {@code FROM clause} (e.g., SQL Server).
 * It is also used for cases where no locking was requested.
 *
 * @author Steve Ebersole
 */
public class NoOpForUpdateClauseStrategy implements ForUpdateClauseStrategy {
	public static final NoOpForUpdateClauseStrategy NO_OP_STRATEGY = new NoOpForUpdateClauseStrategy();

	@Override
	public void register(TableGroup tableGroup, boolean isRoot) {
		// nothing to do
	}

	@Override
	public void render(SqlAppender sqlAppender) {
		// nothing to do
	}
}
