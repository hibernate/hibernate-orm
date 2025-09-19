/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.internal;

import jakarta.persistence.Timeout;
import org.hibernate.dialect.lock.spi.ConnectionLockTimeoutStrategy;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.PostAction;
import org.hibernate.sql.exec.spi.PreAction;
import org.hibernate.sql.exec.spi.StatementAccess;

import java.sql.Connection;

/**
 * Handles lock timeouts using setting on the JDBC Connection.
 *
 * @see ConnectionLockTimeoutStrategy
 *
 * @author Steve Ebersole
 */
public class LockTimeoutHandler implements PreAction, PostAction {
	private final ConnectionLockTimeoutStrategy lockTimeoutStrategy;
	private final Timeout timeout;

	private Timeout baseline;
	private boolean setTimeout;

	public LockTimeoutHandler(Timeout timeout, ConnectionLockTimeoutStrategy lockTimeoutStrategy) {
		this.timeout = timeout;
		this.lockTimeoutStrategy = lockTimeoutStrategy;
	}

	public Timeout getBaseline() {
		return baseline;
	}

	@Override
	public void performPreAction(StatementAccess jdbcStatementAccess, Connection jdbcConnection, ExecutionContext executionContext) {
		final SessionFactoryImplementor factory = executionContext.getSession().getFactory();

		// first, get the baseline (for post-action)
		baseline = lockTimeoutStrategy.getLockTimeout( jdbcConnection, factory );

		// now set the timeout
		lockTimeoutStrategy.setLockTimeout( timeout, jdbcConnection, factory );
		setTimeout = true;
	}

	@Override
	public void performPostAction(StatementAccess jdbcStatementAccess, Connection jdbcConnection, ExecutionContext executionContext) {
		final SessionFactoryImplementor factory = executionContext.getSession().getFactory();

		// reset the timeout
		lockTimeoutStrategy.setLockTimeout( baseline, jdbcConnection, factory );
	}

	@Override
	public boolean shouldRunAfterFail() {
		// if we set the timeout in the pre-action, we should always reset it in post-action
		return setTimeout;
	}
}
