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
			ConnectionLockTimeoutStrategy.NONE
	);

	public static final LockingSupport SYBASE_ASE = new TransactSQLLockingSupport(
			PessimisticLockStyle.NONE,
			LockTimeoutType.CONNECTION,
			LockTimeoutType.NONE,
			LockTimeoutType.NONE,
			RowLockStrategy.TABLE,
			OuterJoinLockingType.IDENTIFIED,
			ConnectionLockTimeoutStrategy.NONE
	);

	public static final LockingSupport SYBASE_LEGACY = new TransactSQLLockingSupport(
			PessimisticLockStyle.TABLE_HINT,
			LockTimeoutType.CONNECTION,
			LockTimeoutType.NONE,
			LockTimeoutType.NONE,
			RowLockStrategy.TABLE,
			OuterJoinLockingType.IDENTIFIED,
			ConnectionLockTimeoutStrategy.NONE
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
				ConnectionLockTimeoutStrategy.NONE
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
			return ConnectionLockTimeoutStrategy.Level.EXTENDED;
		}

		@Override
		public Timeout getLockTimeout(Connection connection, SessionFactoryImplementor factory) {
			return Helper.getLockTimeout(
					"select @@LOCK_TIMEOUT",
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
}
