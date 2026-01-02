/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.internal;

import jakarta.persistence.Timeout;
import org.hibernate.HibernateException;
import org.hibernate.Timeouts;
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
import static org.hibernate.dialect.lock.spi.LockTimeoutType.QUERY;

/**
 * @author Steve Ebersole
 */
public class PostgreSQLLockingSupport implements LockingSupport, LockingSupport.Metadata, ConnectionLockTimeoutStrategy {
	public static final LockingSupport LOCKING_SUPPORT = new PostgreSQLLockingSupport();
	private final boolean supportsNoWait;
	private final boolean supportsSkipLocked;

	public PostgreSQLLockingSupport() {
		this( true, true );
	}

	public PostgreSQLLockingSupport(boolean supportsNoWait, boolean supportsSkipLocked) {
		this.supportsNoWait = supportsNoWait;
		this.supportsSkipLocked = supportsSkipLocked;
	}

	@Override
	public Metadata getMetadata() {
		return this;
	}

	@Override
	public RowLockStrategy getWriteRowLockStrategy() {
		return RowLockStrategy.TABLE;
	}

	@Override
	public LockTimeoutType getLockTimeoutType(Timeout timeout) {
		return switch ( timeout.milliseconds() ) {
			case NO_WAIT_MILLI -> supportsNoWait ? QUERY : LockTimeoutType.NONE;
			case SKIP_LOCKED_MILLI -> supportsSkipLocked ? QUERY : LockTimeoutType.NONE;
			case WAIT_FOREVER_MILLI -> LockTimeoutType.NONE;
			// we can apply a timeout via the connection
			default -> LockTimeoutType.CONNECTION;
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
				"select current_setting('lock_timeout', true)",
				(resultSet) -> {
					// Although lock_timeout is stored internally in milliseconds,
					// `current_setting` returns a String in a canonical, human-readable form
					// that varies depending on the value:
					//   * "0" is returned for no timeout (WAIT_FOREVER)
					//   * Non-zero values may be returned with units such as:
					//       - milliseconds: "500ms"
					//       - seconds:      "3s"
					//       - minutes:      "1min"
					//       - hours:        "1h"
					// Therefore, we need to parse this String carefully to reconstruct the correct Timeout.
					String value = resultSet.getString( 1 );
					if ( "0".equals( value ) ) {
						return Timeouts.WAIT_FOREVER;
					}
					final var unitStartIndex = findUnitStartIndex( value );
					final var amount = Integer.parseInt( value, 0, unitStartIndex, 10 );
					return switch ( unitStartIndex == -1 ? "ms" : value.substring( unitStartIndex ) ) {
						case "ms" -> Timeout.milliseconds( amount );
						case "s" -> Timeout.seconds( amount );
						case "min" -> Timeout.seconds( amount * 60 );
						case "h" -> Timeout.seconds( amount * 3600 );
						case "d" -> Timeout.seconds( amount * 3600 * 24 );
						default -> throw new IllegalArgumentException(
							"Unexpected PostgreSQL lock_timeout format: " + value );
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
					if ( milliseconds == SKIP_LOCKED_MILLI ) {
						throw new HibernateException( "Connection lock-timeout does not accept skip-locked" );
					}

					if ( milliseconds == NO_WAIT_MILLI ) {
						throw new HibernateException( "Connection lock-timeout does not accept no-wait" );
					}
					return milliseconds == WAIT_FOREVER_MILLI
							? 0
							: milliseconds;
				},
				"set local lock_timeout = %s",
				connection,
				factory
		);
	}

	private static int findUnitStartIndex(String value) {
		for ( int i = value.length() - 1; i > 0; i-- ) {
			if ( Character.isDigit( value.charAt( i ) ) ) {
				return i + 1;
			}
		}
		return -1;
	}
}
