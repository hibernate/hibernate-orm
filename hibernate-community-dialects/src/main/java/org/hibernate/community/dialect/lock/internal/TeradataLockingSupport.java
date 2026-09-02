/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.lock.internal;

import jakarta.persistence.Timeout;
import org.hibernate.Timeouts;
import org.hibernate.dialect.lock.spi.ConnectionLockTimeoutStrategy;
import org.hibernate.dialect.lock.spi.FollowOnLockingPolicy;
import org.hibernate.dialect.lock.spi.LockTimeoutType;
import org.hibernate.dialect.lock.spi.LockingClauseRenderer;
import org.hibernate.dialect.lock.spi.LockingClauseRequest;
import org.hibernate.dialect.lock.spi.LockingSqlRewriter;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.OuterJoinLockingType;
import org.hibernate.dialect.lock.spi.PessimisticLockKind;
import org.hibernate.dialect.lock.spi.StandardLockingSqlRewriters;

/**
 * @author Steve Ebersole
 */
public class TeradataLockingSupport implements LockingSupport, LockingSupport.Metadata, LockingClauseRenderer {
	private final LockingSqlRewriter lockingSqlRewriter;
	private final FollowOnLockingPolicy followOnLockingPolicy;

	public TeradataLockingSupport() {
		this( true );
	}

	public TeradataLockingSupport(boolean statementPrefix) {
		lockingSqlRewriter = statementPrefix
				? StandardLockingSqlRewriters.statementPrefix( this )
				: StandardLockingSqlRewriters.statementSuffix( this );
		followOnLockingPolicy = statementPrefix
				? FollowOnLockingPolicy.ALWAYS
				: FollowOnLockingPolicy.NEVER;
	}

	@Override
	public Metadata getMetadata() {
		return this;
	}

	@Override
	public LockingClauseRenderer getLockingClauseRenderer() {
		return this;
	}

	@Override
	public LockingSqlRewriter getLockingSqlRewriter() {
		return lockingSqlRewriter;
	}

	@Override
	public FollowOnLockingPolicy getFollowOnLockingPolicy() {
		return followOnLockingPolicy;
	}

	@Override
	public String render(LockingClauseRequest request) {
		final StringBuilder fragment = new StringBuilder(
				request.lockKind() == PessimisticLockKind.SHARE
						? " Locking row for read  "
						: " Locking row for write "
		);
		if ( request.timeout().milliseconds() == Timeouts.NO_WAIT_MILLI ) {
			fragment.append( " nowait " );
		}
		return fragment.toString();
	}

	@Override
	public LockTimeoutType getLockTimeoutType(Timeout timeout) {
		if ( timeout.milliseconds() == Timeouts.NO_WAIT_MILLI ) {
			return LockTimeoutType.QUERY;
		}
		// todo (db-locking) : maybe getConnectionLockTimeoutStrategy?
		return LockTimeoutType.NONE;
	}

	@Override
	public OuterJoinLockingType getOuterJoinLockingType() {
		return OuterJoinLockingType.UNSUPPORTED;
	}

	@Override
	public ConnectionLockTimeoutStrategy getConnectionLockTimeoutStrategy() {
		// todo (db-locking) : not sure about this for Teradata...
		return ConnectionLockTimeoutStrategy.NONE;
	}
}
