/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import jakarta.persistence.Timeout;
import org.hibernate.HibernateException;
import org.hibernate.Internal;
import org.hibernate.Timeouts;
import org.hibernate.dialect.lock.spi.RowLockStrategy;
import org.hibernate.dialect.lock.spi.ConnectionLockTimeoutStrategy;
import org.hibernate.dialect.lock.spi.ConnectionLockTimeoutOperations;
import org.hibernate.dialect.lock.spi.LockTimeoutType;
import org.hibernate.dialect.lock.spi.LockingClauseRenderer;
import org.hibernate.dialect.lock.spi.LockingClauseRequest;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.OuterJoinLockingType;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import java.sql.Connection;

import static org.hibernate.Timeouts.NO_WAIT_MILLI;
import static org.hibernate.Timeouts.SKIP_LOCKED_MILLI;
import static org.hibernate.Timeouts.WAIT_FOREVER_MILLI;

@Internal
public class InformixLockingSupport
		implements LockingSupport, LockingSupport.Metadata, ConnectionLockTimeoutStrategy, LockingClauseRenderer {
	public static final LockingSupport LOCKING_SUPPORT = new InformixLockingSupport();

	public InformixLockingSupport() {
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
		return " for update";
	}

	@Override
	public RowLockStrategy getWriteRowLockStrategy() {
		return RowLockStrategy.COLUMN;
	}

	@Override
	public LockTimeoutType getLockTimeoutType(Timeout timeout) {
		return switch ( timeout.milliseconds() ) {
			case SKIP_LOCKED_MILLI -> LockTimeoutType.NONE;
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
		return Level.EXTENDED;
	}

	@Override
	public Timeout getLockTimeout(Connection connection, SessionFactoryImplementor factory) {
		return ConnectionLockTimeoutOperations.query(
				"select scs_lockmode from sysmaster:syssqlcurses where scs_sessionid = dbinfo('sessionid')",
				(resultSet) -> {
					final int seconds = resultSet.getInt( 1 );
					return switch ( seconds ) {
						case -1 -> Timeouts.WAIT_FOREVER;
						case 0 -> Timeouts.NO_WAIT;
						default -> Timeout.seconds( seconds );
					};
				},
				connection,
				factory
		);
	}

	@Override
	public void setLockTimeout(Timeout timeout, Connection connection, SessionFactoryImplementor factory) {
		final int milliseconds = timeout.milliseconds();
		if ( milliseconds == SKIP_LOCKED_MILLI ) {
			throw new HibernateException( "Connection lock-timeout does not accept skip-locked" );
		}
		if ( milliseconds == WAIT_FOREVER_MILLI ) {
			ConnectionLockTimeoutOperations.execute(
					"set lock mode to wait",
					connection,
					factory
			);
		}
		else if ( milliseconds == NO_WAIT_MILLI ) {
			ConnectionLockTimeoutOperations.execute(
					"set lock mode to not wait",
					connection,
					factory
			);
		}
		else {
			ConnectionLockTimeoutOperations.execute(
					(int) Math.ceil( (double) milliseconds / 1000),
					"set lock mode to wait %s",
					connection,
					factory
			);
		}
	}
}
