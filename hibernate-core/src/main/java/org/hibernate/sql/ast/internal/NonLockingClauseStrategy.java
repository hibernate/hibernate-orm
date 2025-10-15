/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.internal;

import org.hibernate.sql.ast.spi.LockingClauseStrategy;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;

import java.util.Collection;
import java.util.List;

/**
 * LockingClauseStrategy implementation for cases when a dialect
 * applies locking in the {@code FROM clause} (e.g., SQL Server).
 * It is also used for cases where no locking was requested.
 *
 * @author Steve Ebersole
 */
public class NonLockingClauseStrategy implements LockingClauseStrategy {
	public static final NonLockingClauseStrategy NON_CLAUSE_STRATEGY = new NonLockingClauseStrategy();

	@Override
	public void registerRoot(TableGroup root) {
		// nothing to do
	}

	@Override
	public void registerJoin(TableGroupJoin join) {
		// nothing to do
	}

	@Override
	public boolean containsOuterJoins() {
		return false;
	}

	@Override
	public void render(SqlAppender sqlAppender) {
		// nothing to do
	}

	@Override
	public Collection<TableGroup> getRootsToLock() {
		return List.of();
	}

	@Override
	public Collection<TableGroupJoin> getJoinsToLock() {
		return List.of();
	}
}
