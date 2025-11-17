/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.lock.internal;

import jakarta.persistence.Timeout;
import org.hibernate.HibernateException;
import org.hibernate.Timeouts;
import org.hibernate.dialect.RowLockStrategy;
import org.hibernate.dialect.lock.internal.Helper;
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
 * @author chen zhida
 *
 * Notes: Original code of this class is based on PostgreSQLLockingSupport.
 */
public class GaussDBLockingSupport implements LockingSupport, LockingSupport.Metadata, ConnectionLockTimeoutStrategy {
	public static final LockingSupport LOCKING_SUPPORT = new GaussDBLockingSupport();
	private final boolean supportsNoWait;
	private final boolean supportsSkipLocked;

	public GaussDBLockingSupport() {
		this( true, true );
	}

	public GaussDBLockingSupport(boolean supportsNoWait, boolean supportsSkipLocked) {
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
	public OuterJoinLockingType getOuterJoinLockingType() {
		return OuterJoinLockingType.UNSUPPORTED;
	}

	@Override
	public ConnectionLockTimeoutStrategy getConnectionLockTimeoutStrategy() {
		return this;
	}

	@Override
	public Level getSupportedLevel() {
		return Level.SUPPORTED;
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
	public Timeout getLockTimeout(Connection connection, SessionFactoryImplementor factory) {
		return Helper.getLockTimeout(
				"select current_setting('lockwait_timeout')",
				(resultSet) -> {
					// even though lock_timeout is "in milliseconds", `current_setting`
					// returns a String form which unfortunately varies depending on
					// the actual value:
					//		* for zero (no timeout), "0" is returned
					//		* for non-zero, `{timeout-in-seconds}s` is returned (e.g. "4s")
					// so we need to "parse" that form here
					final String value = resultSet.getString( 1 );
					if ( "0".equals( value ) ) {
						return Timeouts.WAIT_FOREVER;
					}
					if ( value.endsWith( "min" ) ) {
						final int minute = getTimeout( value, 3 );
						return Timeout.milliseconds( minute * 60 * 1000);
					}
					else if ( value.endsWith( "s" ) ) {
						final int seconds  = getTimeout( value, 1 );
						return Timeout.seconds(seconds);
					}
					final int milliseconds  = getTimeout( value, 2 );
					return Timeout.milliseconds(milliseconds);
				},
				connection,
				factory
		);
	}

	private static int getTimeout(String value, int unitLength) {
		final int number;
		try {
			number = Integer.parseInt( value.substring( 0, value.length() - unitLength ) );
		}
		catch (NumberFormatException e) {
			throw new RuntimeException( e );
		}
		return number;
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
				"set local lockwait_timeout = %s",
				connection,
				factory
		);
	}
}
