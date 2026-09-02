/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.internal;

import jakarta.persistence.Timeout;
import org.hibernate.dialect.lock.spi.ConnectionLockTimeoutStrategy;
import org.hibernate.dialect.lock.spi.LockTimeoutType;
import org.hibernate.dialect.lock.spi.LockingClauseRenderer;
import org.hibernate.dialect.lock.spi.LockingClauseRequest;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.OuterJoinLockingType;

/**
 * LockingSupport for HSQLDialect
 *
 * @author Steve Ebersole
 */
public class HSQLLockingSupport implements LockingSupport, LockingSupport.Metadata, LockingClauseRenderer {
	public static final HSQLLockingSupport LOCKING_SUPPORT = new HSQLLockingSupport( true );
	public static final HSQLLockingSupport NO_CLAUSE_LOCKING_SUPPORT = new HSQLLockingSupport( false );

	private final boolean supportsLockingClause;

	private HSQLLockingSupport(boolean supportsLockingClause) {
		this.supportsLockingClause = supportsLockingClause;
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
	public String render(LockingClauseRequest request) {
		return supportsLockingClause ? " for update" : "";
	}

	@Override
	public LockTimeoutType getLockTimeoutType(Timeout timeout) {
		return LockTimeoutType.NONE;
	}

	@Override
	public OuterJoinLockingType getOuterJoinLockingType() {
		return OuterJoinLockingType.IGNORED;
	}

	@Override
	public ConnectionLockTimeoutStrategy getConnectionLockTimeoutStrategy() {
		return ConnectionLockTimeoutStrategy.NONE;
	}
}
