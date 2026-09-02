/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import org.hibernate.dialect.lock.PessimisticLockStyle;
import org.hibernate.dialect.lock.spi.ConnectionLockTimeoutStrategy;
import org.hibernate.dialect.lock.spi.FollowOnLockingPolicy;
import org.hibernate.dialect.lock.spi.LockingClauseRenderer;
import org.hibernate.dialect.lock.spi.LockTimeoutType;
import org.hibernate.dialect.lock.spi.LockingSqlRewriteResult;
import org.hibernate.dialect.lock.spi.LockingSqlRewriter;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.OuterJoinLockingType;
import org.hibernate.dialect.lock.spi.PessimisticLockKind;
import org.hibernate.dialect.lock.spi.RowLockStrategy;
import org.hibernate.dialect.lock.spi.StandardLockingSupports;
import org.hibernate.dialect.lock.spi.StandardLockingSqlRewriters;
import org.hibernate.dialect.lock.spi.TableLockHintRenderer;

/// Provider implementation exercising the supported locking-family boundary.
///
/// @since 8.0
/// @author Steve Ebersole
public final class ExampleLockingSupport implements LockingSupport, LockingSupport.Metadata {
	public static final ExampleLockingSupport INSTANCE = new ExampleLockingSupport();

	private static final LockingSupport STANDARD_PROFILE = StandardLockingSupports.simple(
			PessimisticLockStyle.TABLE_HINT,
			RowLockStrategy.NONE,
			LockTimeoutType.NONE,
			OuterJoinLockingType.IDENTIFIED,
			ConnectionLockTimeoutStrategy.NONE
	);
	private static final LockingClauseRenderer LOCKING_CLAUSE_RENDERER = request -> " for fixture update";
	private static final TableLockHintRenderer TABLE_LOCK_HINT_RENDERER = request ->
			request.lockKind() == PessimisticLockKind.NONE ? "" : " with (fixture_lock)";
	private static final LockingSqlRewriter LOCKING_SQL_REWRITER =
			StandardLockingSqlRewriters.tableHints( TABLE_LOCK_HINT_RENDERER );
	private static final FollowOnLockingPolicy FOLLOW_ON_LOCKING_POLICY = request ->
			request.statementShape().setOperation()
					|| request.rawSqlRewriteOutcome() == LockingSqlRewriteResult.Outcome.UNSUPPORTED;

	private ExampleLockingSupport() {
	}

	@Override
	public LockingClauseRenderer getLockingClauseRenderer() {
		return LOCKING_CLAUSE_RENDERER;
	}

	@Override
	public TableLockHintRenderer getTableLockHintRenderer() {
		return TABLE_LOCK_HINT_RENDERER;
	}

	@Override
	public LockingSqlRewriter getLockingSqlRewriter() {
		return LOCKING_SQL_REWRITER;
	}

	@Override
	public FollowOnLockingPolicy getFollowOnLockingPolicy() {
		return FOLLOW_ON_LOCKING_POLICY;
	}

	@Override
	public Metadata getMetadata() {
		return this;
	}

	@Override
	public ConnectionLockTimeoutStrategy getConnectionLockTimeoutStrategy() {
		return STANDARD_PROFILE.getConnectionLockTimeoutStrategy();
	}

	@Override
	public PessimisticLockStyle getPessimisticLockStyle() {
		return STANDARD_PROFILE.getMetadata().getPessimisticLockStyle();
	}

	@Override
	public OuterJoinLockingType getOuterJoinLockingType() {
		return STANDARD_PROFILE.getMetadata().getOuterJoinLockingType();
	}
}
