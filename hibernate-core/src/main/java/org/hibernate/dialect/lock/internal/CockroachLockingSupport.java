/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.internal;

import jakarta.persistence.Timeout;
import org.hibernate.HibernateException;
import org.hibernate.dialect.RowLockStrategy;
import org.hibernate.dialect.lock.spi.ConnectionLockTimeoutStrategy;
import org.hibernate.dialect.lock.spi.LockTimeoutType;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.OuterJoinLockingType;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import java.sql.Connection;

import static org.hibernate.Timeouts.NO_WAIT;
import static org.hibernate.Timeouts.NO_WAIT_MILLI;
import static org.hibernate.Timeouts.SKIP_LOCKED_MILLI;
import static org.hibernate.Timeouts.WAIT_FOREVER;
import static org.hibernate.Timeouts.WAIT_FOREVER_MILLI;
import static org.hibernate.dialect.lock.spi.LockTimeoutType.CONNECTION;
import static org.hibernate.dialect.lock.spi.LockTimeoutType.QUERY;

/**
 * @author Steve Ebersole
 */
public class CockroachLockingSupport implements LockingSupport, LockingSupport.Metadata, ConnectionLockTimeoutStrategy {
	public static final CockroachLockingSupport COCKROACH_LOCKING_SUPPORT = new CockroachLockingSupport( false );
	public static final CockroachLockingSupport LEGACY_COCKROACH_LOCKING_SUPPORT = new CockroachLockingSupport( true );

	private final boolean supportsNoWait;
	private final RowLockStrategy rowLockStrategy;

	public CockroachLockingSupport(boolean isLegacy) {
		rowLockStrategy = isLegacy ? RowLockStrategy.NONE : RowLockStrategy.TABLE;
		supportsNoWait = !isLegacy;
	}

	@Override
	public Metadata getMetadata() {
		return this;
	}

	@Override
	public RowLockStrategy getWriteRowLockStrategy() {
		return rowLockStrategy;
	}

	@Override
	public LockTimeoutType getLockTimeoutType(Timeout timeout) {
		// [1] See https://www.cockroachlabs.com/docs/stable/select-for-update.html#wait-policies
		// todo (db-locking) : to me, reading that doc, Cockroach *does* support skip-locked.
		//		figure out why we report false here.  version?
		return switch (timeout.milliseconds()) {
			case WAIT_FOREVER_MILLI -> QUERY;
			case NO_WAIT_MILLI -> supportsNoWait ? QUERY : LockTimeoutType.NONE;
			case SKIP_LOCKED_MILLI -> LockTimeoutType.NONE;
			// it does not, however, support WAIT as part of for-update, but does support a connection-level lock_timeout setting
			default -> CONNECTION;
		};
	}

	@Override
	public OuterJoinLockingType getOuterJoinLockingType() {
		return OuterJoinLockingType.UNSUPPORTED;
	}

	@Override
	public ConnectionLockTimeoutStrategy getConnectionLockTimeoutStrategy() {
		return this;
	}

	@Override
	public Level getSupportedLevel() {
		return ConnectionLockTimeoutStrategy.Level.SUPPORTED;
	}

	@Override
	public Timeout getLockTimeout(Connection connection, SessionFactoryImplementor factory) {
		return Helper.getLockTimeout(
				"show lock_timeout",
				(resultSet) -> {
					// see https://dev.mysql.com/doc/refman/8.4/en/innodb-parameters.html#sysvar_innodb_lock_wait_timeout
					final int millis = resultSet.getInt( 1 );
					return switch ( millis ) {
						case 0 -> NO_WAIT;
						case 100000000 -> WAIT_FOREVER;
						default -> Timeout.milliseconds( millis );
					};
				},
				connection,
				factory
		);
	}

	@Override
	public void setLockTimeout(
			Timeout timeout,
			Connection connection,
			SessionFactoryImplementor factory) {
		Helper.setLockTimeout(
				timeout,
				(t) -> {
					// see https://dev.mysql.com/doc/refman/8.4/en/innodb-parameters.html#sysvar_innodb_lock_wait_timeout
					final int milliseconds = timeout.milliseconds();
					if ( milliseconds == SKIP_LOCKED_MILLI ) {
						throw new HibernateException( "Connection lock-timeout does not accept skip-locked" );
					}
					if ( milliseconds == WAIT_FOREVER_MILLI ) {
						return 100000000;
					}
					return milliseconds;
				},
				"set lock_timeout = %s",
				connection,
				factory
		);
	}

}
