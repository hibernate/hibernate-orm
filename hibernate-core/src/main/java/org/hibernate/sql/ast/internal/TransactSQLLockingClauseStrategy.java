/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.internal;

import org.hibernate.Locking;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAppender;

import java.util.Set;

/// LockingClauseStrategy implementation for T-SQL (SQL Server and Sybase)
///
/// @author Steve Ebersole
public class TransactSQLLockingClauseStrategy extends AbstractLockingClauseStrategy {

	public TransactSQLLockingClauseStrategy(Locking.Scope lockingScope, Set<NavigablePath> rootsForLocking) {
		super( lockingScope, rootsForLocking );
	}

	@Override
	public boolean containsOuterJoins() {
		// not used for T-SQL dialects
		return false;
	}

	@Override
	public void render(SqlAppender sqlAppender) {
		// not used for T-SQL dialects
	}
}
