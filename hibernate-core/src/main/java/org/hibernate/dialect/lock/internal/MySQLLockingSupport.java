/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.internal;

import jakarta.persistence.Timeout;
import org.hibernate.HibernateException;
import org.hibernate.Timeouts;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.RowLockStrategy;
import org.hibernate.dialect.lock.spi.ConnectionLockTimeoutStrategy;
import org.hibernate.dialect.lock.spi.LockTimeoutType;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.OuterJoinLockingType;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import java.sql.Connection;

import static org.hibernate.Timeouts.NO_WAIT_MILLI;
import static org.hibernate.Timeouts.SKIP_LOCKED_MILLI;
import static org.hibernate.Timeouts.WAIT_FOREVER_MILLI;
import static org.hibernate.dialect.lock.spi.LockTimeoutType.NONE;
import static org.hibernate.dialect.lock.spi.LockTimeoutType.QUERY;

/**
 * LockingSupport for MySQLDialect
 *
 * @author Steve Ebersole
 */
public class MySQLLockingSupport implements LockingSupport, LockingSupport.Metadata {
	public static final ConnectionLockTimeoutStrategy MYSQL_CONN_LOCK_TIMEOUT_STRATEGY = new ConnectionLockTimeoutStrategyImpl();
	public static final LockingSupport MYSQL_LOCKING_SUPPORT = new MySQLLockingSupport();

	private final boolean laterThanVersion8;

	public MySQLLockingSupport() {
		laterThanVersion8 = true;
	}

	public MySQLLockingSupport(DatabaseVersion version) {
		laterThanVersion8 = version.isSameOrAfter( 8 );
	}

	@Override
	public Metadata getMetadata() {
		return this;
	}

	@Override
	public LockTimeoutType getLockTimeoutType(Timeout timeout) {
		return switch ( timeout.milliseconds() ) {
			case SKIP_LOCKED_MILLI, NO_WAIT_MILLI -> laterThanVersion8 ? QUERY : NONE;
			case WAIT_FOREVER_MILLI -> NONE;
			// For MySQL real lock timeouts need to be applied on the Connection
			//default -> CONNECTION;
			// todo (db-locking) : however, I have not yet integrated that stuff - so report NONE
			default -> NONE;
		};
	}

	@Override
	public boolean supportsWait() {
		return false;
	}

	@Override
	public boolean supportsNoWait() {
		return laterThanVersion8;
	}

	@Override
	public boolean supportsSkipLocked() {
		return laterThanVersion8;
	}

	@Override
	public RowLockStrategy getWriteRowLockStrategy() {
		return RowLockStrategy.TABLE;
	}

	@Override
	public OuterJoinLockingType getOuterJoinLockingType() {
		return OuterJoinLockingType.IDENTIFIED;
	}

	@Override
	public ConnectionLockTimeoutStrategy getConnectionLockTimeoutStrategy() {
		return MYSQL_CONN_LOCK_TIMEOUT_STRATEGY;
	}

	public static class ConnectionLockTimeoutStrategyImpl implements ConnectionLockTimeoutStrategy {
		@Override
		public Level getSupportedLevel() {
			return ConnectionLockTimeoutStrategy.Level.EXTENDED;
		}

		@Override
		public Timeout getLockTimeout(Connection connection, SessionFactoryImplementor factory) {
			return Helper.getLockTimeout(
					"SELECT @@SESSION.innodb_lock_wait_timeout",
					(resultSet) -> {
						// see https://dev.mysql.com/doc/refman/8.4/en/innodb-parameters.html#sysvar_innodb_lock_wait_timeout
						final int millis = resultSet.getInt( 1 );
						return switch ( millis ) {
							case 0 -> Timeouts.NO_WAIT;
							case 100000000 -> Timeouts.WAIT_FOREVER;
							default -> Timeout.milliseconds( millis );
						};
					},
					connection,
					factory
			);
		}

		@Override
		public void setLockTimeout(Timeout timeout, Connection connection, SessionFactoryImplementor factory) {
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
					"SET @@SESSION.innodb_lock_wait_timeout = %s",
					connection,
					factory
			);
		}
	}
}
