/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Locking;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.RowLockStrategy;
import org.hibernate.sql.ast.internal.PessimisticLockKind;
import org.hibernate.sql.ast.internal.StandardForUpdateClauseStrategy;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.select.QuerySpec;

/**
 * @author Steve Ebersole
 */
public class DerbyForUpdateStrategy extends StandardForUpdateClauseStrategy {
	public DerbyForUpdateStrategy(
			Dialect dialect,
			RowLockStrategy rowLockStrategy,
			LockMode lockMode,
			PessimisticLockKind lockKind,
			Locking.Scope lockingScope,
			int timeout) {
		super( dialect, rowLockStrategy, lockMode, lockKind, lockingScope, timeout );
	}

	@Override
	protected void renderResultSetOptions(Dialect dialect, SqlAppender sqlAppender) {
		sqlAppender.append( " with rs" );
	}

	public static DerbyForUpdateStrategy strategy(
			Dialect dialect,
			QuerySpec querySpec,
			LockOptions lockOptions) {
		return (DerbyForUpdateStrategy) StandardForUpdateClauseStrategy.strategy(
				dialect,
				querySpec,
				lockOptions,
				(dialect1, rowLockStrategy, lockMode, lockKind, lockScope, timeout) -> new StandardForUpdateClauseStrategy(
						dialect,
						rowLockStrategy,
						lockMode,
						lockKind,
						lockScope,
						timeout
				)
		);
	}
}
