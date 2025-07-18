/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.LockOptions;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.RowLockStrategy;
import org.hibernate.sql.ast.internal.PessimisticLockKind;
import org.hibernate.sql.ast.internal.StandardLockingClauseStrategy;
import org.hibernate.sql.ast.spi.SqlAppender;

/**
 * StandardLockingClauseStrategy subclass, specific for Derby.
 *
 * @author Steve Ebersole
 */
public class DerbyLockingClauseStrategy extends StandardLockingClauseStrategy {
	public DerbyLockingClauseStrategy(
			Dialect dialect,
			PessimisticLockKind lockKind,
			RowLockStrategy rowLockStrategy,
			LockOptions lockOptions) {
		super( dialect, lockKind, rowLockStrategy, lockOptions );
	}

	@Override
	protected void renderResultSetOptions(SqlAppender sqlAppender) {
		sqlAppender.append( " with rs" );
	}
}
