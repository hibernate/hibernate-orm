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
import org.hibernate.dialect.lock.PessimisticLockStyle;
import org.hibernate.dialect.lock.spi.ConnectionLockTimeoutStrategy;
import org.hibernate.dialect.lock.spi.LockTimeoutType;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.OuterJoinLockingType;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import java.sql.Connection;

/**
 * Locking support for TransactSQL based Dialects (SQL Server and Sybase).
 *
 * @author Steve Ebersole
 */
public class TransactSQLLockingSupport extends LockingSupportParameterized {
	public static final LockingSupport SQL_SERVER = new TransactSQLLockingSupport(
			PessimisticLockStyle.TABLE_HINT,
			LockTimeoutType.CONNECTION,
			LockTimeoutType.QUERY,
			LockTimeoutType.QUERY,
			RowLockStrategy.TABLE,
			OuterJoinLockingType.IDENTIFIED,
			SQLServerImpl.IMPL
	);

	public static final LockingSupport SYBASE = new TransactSQLLockingSupport(
			PessimisticLockStyle.TABLE_HINT,
			LockTimeoutType.CONNECTION,
			LockTimeoutType.QUERY,
			LockTimeoutType.NONE,
			RowLockStrategy.TABLE,
			OuterJoinLockingType.IDENTIFIED,
			SybaseImpl.IMPL
	);

	public static final LockingSupport SYBASE_ASE = new TransactSQLLockingSupport(
			PessimisticLockStyle.TABLE_HINT,
			LockTimeoutType.CONNECTION,
			LockTimeoutType.NONE,
			LockTimeoutType.NONE,
			RowLockStrategy.TABLE,
			OuterJoinLockingType.IDENTIFIED,
			SybaseImpl.IMPL
	);

	public static final LockingSupport SYBASE_LEGACY = new TransactSQLLockingSupport(
			PessimisticLockStyle.TABLE_HINT,
			LockTimeoutType.CONNECTION,
			LockTimeoutType.NONE,
			LockTimeoutType.NONE,
			RowLockStrategy.TABLE,
			OuterJoinLockingType.IDENTIFIED,
			SybaseImpl.IMPL
	);

	public static LockingSupport forSybaseAnywhere(DatabaseVersion version) {
		return new TransactSQLLockingSupport(
				version.isBefore( 10 )
						? PessimisticLockStyle.TABLE_HINT
						: PessimisticLockStyle.CLAUSE,
				LockTimeoutType.CONNECTION,
				LockTimeoutType.NONE,
				LockTimeoutType.NONE,
				version.isSameOrAfter( 10 )
						? RowLockStrategy.COLUMN
						: RowLockStrategy.TABLE,
				OuterJoinLockingType.IDENTIFIED,
				SybaseImpl.IMPL
		);
	}

	private final ConnectionLockTimeoutStrategy connectionLockTimeoutStrategy;

	public TransactSQLLockingSupport(
			PessimisticLockStyle pessimisticLockStyle,
			LockTimeoutType wait,
			LockTimeoutType noWait,
			LockTimeoutType skipLocked,
			RowLockStrategy rowLockStrategy,
			OuterJoinLockingType outerJoinLockingType,
			ConnectionLockTimeoutStrategy connectionLockTimeoutStrategy) {
		super(
				pessimisticLockStyle,
				rowLockStrategy,
				wait,
				noWait,
				skipLocked,
				outerJoinLockingType
		);
		this.connectionLockTimeoutStrategy = connectionLockTimeoutStrategy;
	}

	@Override
	public ConnectionLockTimeoutStrategy getConnectionLockTimeoutStrategy() {
		return connectionLockTimeoutStrategy;
	}

	public static class SQLServerImpl implements ConnectionLockTimeoutStrategy {
		public static final SQLServerImpl IMPL = new SQLServerImpl();

		@Override
		public Level getSupportedLevel() {
			return Level.EXTENDED;
		}

		@Override
		public Timeout getLockTimeout(Connection connection, SessionFactoryImplementor factory) {
			return Helper.getLockTimeout(
					"select @@lock_timeout",
					(resultSet) -> {
						final int timeoutInMilliseconds = resultSet.getInt( 1 );
						return switch ( timeoutInMilliseconds ) {
							case -1 -> Timeouts.WAIT_FOREVER;
							case 0 -> Timeouts.NO_WAIT;
							default -> Timeout.milliseconds( timeoutInMilliseconds );
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
						final int milliseconds = timeout.milliseconds();
						if ( milliseconds == Timeouts.SKIP_LOCKED_MILLI ) {
							throw new HibernateException( "Connection lock-timeout does not accept skip-locked" );
						}
						return milliseconds;
					},
					"set lock_timeout %s",
					connection,
					factory
			);
		}
	}

	public static class SybaseImpl implements ConnectionLockTimeoutStrategy {
		public static final SybaseImpl IMPL = new SybaseImpl();

		@Override
		public Level getSupportedLevel() {
			return Level.SUPPORTED;
		}

		@Override
		public Timeout getLockTimeout(Connection connection, SessionFactoryImplementor factory) {
			return Helper.getLockTimeout(
					"select @@lock_timeout",
					(resultSet) -> {
						final int timeoutInMilliseconds = resultSet.getInt( 1 );
						return switch ( timeoutInMilliseconds ) {
							case -1 -> Timeouts.WAIT_FOREVER;
							case 0 -> Timeouts.NO_WAIT;
							default -> Timeout.milliseconds( timeoutInMilliseconds );
						};
					},
					connection,
					factory
			);
		}

		@Override
		public void setLockTimeout(Timeout timeout, Connection connection, SessionFactoryImplementor factory) {
			final int milliseconds = timeout.milliseconds();

			if ( milliseconds == Timeouts.SKIP_LOCKED_MILLI ) {
				throw new HibernateException( "Sybase does not accept skip-locked for lock-timeout" );
			}

			// Sybase needs a special syntax for NO_WAIT rather than a number
			if ( milliseconds == Timeouts.NO_WAIT_MILLI ) {
				// NOTE: The docs say this is supported, and it does not fail when used,
				// but immediately after the setting value is still -1.  So it seems to
				// allow the call but ignore it.  Might just be jTDS.
				Helper.setLockTimeout( "set lock nowait", connection, factory );
			}
			else if ( milliseconds == Timeouts.WAIT_FOREVER_MILLI ) {
				// Even though Sybase's wait-forever (and default) value is -1, it won't accept
				// -1 as a value because, well, of course it won't.  Need to set max value instead
				// because, well, of course you do.
				Helper.setLockTimeout( 2147483647, "set lock wait %s", connection, factory );
			}
			else {
				Helper.setLockTimeout( milliseconds, "set lock wait %s", connection, factory );
			}
		}
	}
}
