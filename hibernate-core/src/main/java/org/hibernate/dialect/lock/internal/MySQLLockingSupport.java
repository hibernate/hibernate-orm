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
		// Making this configurable so TiDBDialect can re-use this, but with a lower value
		// TiDB v8.5.5 limits innodb_lock_wait_timeout to 3600s
		private final int foreverValue;

		public ConnectionLockTimeoutStrategyImpl() {
			// see https://dev.mysql.com/doc/refman/8.4/en/innodb-parameters.html#sysvar_innodb_lock_wait_timeout
			// unit: seconds, allowed values: [1, 1073741824]
			this( 100_000 );
		}

		public ConnectionLockTimeoutStrategyImpl(int foreverValue) {
			this.foreverValue = foreverValue;
		}

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
						// unit: seconds, allowed values: [1, 1073741824]
						final int seconds = resultSet.getInt( 1 );
						return seconds == foreverValue ? Timeouts.WAIT_FOREVER : Timeout.seconds( seconds );
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
						// unit: seconds, allowed values: [1, 1073741824]
						final int milliseconds = timeout.milliseconds();
						if ( milliseconds == SKIP_LOCKED_MILLI ) {
							throw new HibernateException( "Connection lock-timeout does not accept skip-locked" );
						}
						if ( milliseconds == WAIT_FOREVER_MILLI ) {
							return foreverValue;
						}
						return milliseconds / 1000;
					},
					"SET @@SESSION.innodb_lock_wait_timeout = %s",
					connection,
					factory
			);
		}
	}
}
