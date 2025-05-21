/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi;

import org.hibernate.sql.ast.tree.from.TableGroup;

/**
 * Strategy for dealing with locking via a SQL {@code FOR UPDATE (OF)}
 * clause.
 * <p/>
 * Some dialects do not use a {@code FOR UPDATE (OF)} to apply
 * locks - e.g., they apply locks in the {@code FROM} clause.  Such
 * dialects would return a no-op version of this contract.
 * <p/>
 * Some dialects support an additional {@code FOR SHARE (OF)} clause
 * as well to acquire non-exclusive locks.  That is also handled here,
 * varied by the requested {@linkplain org.hibernate.LockMode LockMode}.
 *
 * @see org.hibernate.dialect.Dialect#getForUpdateClauseStrategy
 *
 * @author Steve Ebersole
 */
public interface ForUpdateClauseStrategy {
	void register(TableGroup tableGroup, boolean isRoot);
	void render(SqlAppender sqlAppender);
}
