/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.internal;

import jakarta.persistence.Timeout;
import org.hibernate.Timeouts;
import org.hibernate.dialect.lock.spi.ConnectionLockTimeoutStrategy;
import org.hibernate.dialect.lock.spi.LockTimeoutType;
import org.hibernate.dialect.lock.spi.LockingClauseRenderer;
import org.hibernate.dialect.lock.spi.LockingClauseRequest;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.OuterJoinLockingType;

import static org.hibernate.Timeouts.NO_WAIT_MILLI;
import static org.hibernate.Timeouts.SKIP_LOCKED_MILLI;
import static org.hibernate.dialect.lock.spi.LockTimeoutType.QUERY;

/**
 * LockingSupport for H2Dialect
 *
 * @author Steve Ebersole
 */
public class H2LockingSupport implements LockingSupport, LockingSupport.Metadata, LockingClauseRenderer {
	public static final H2LockingSupport LEGACY_INSTANCE = new H2LockingSupport( false );
	public static final H2LockingSupport INSTANCE = new H2LockingSupport( true );

	private final boolean supportsForUpdateOptions;

	private H2LockingSupport(boolean supportsForUpdateOptions) {
		this.supportsForUpdateOptions = supportsForUpdateOptions;
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
		final StringBuilder fragment = new StringBuilder( " for update" );
		if ( supportsForUpdateOptions ) {
			switch ( request.timeout().milliseconds() ) {
				case NO_WAIT_MILLI -> fragment.append( " nowait" );
				case SKIP_LOCKED_MILLI -> fragment.append( " skip locked" );
				case Timeouts.WAIT_FOREVER_MILLI -> {
				}
				default -> fragment.append( " wait " ).append( Timeouts.getTimeoutInSeconds( request.timeout() ) );
			}
		}
		return fragment.toString();
	}

	@Override
	public OuterJoinLockingType getOuterJoinLockingType() {
		return OuterJoinLockingType.IGNORED;
	}

	@Override
	public LockTimeoutType getLockTimeoutType(Timeout timeout) {
		return switch ( timeout.milliseconds() ) {
			case NO_WAIT_MILLI -> supportsForUpdateOptions ? QUERY : LockTimeoutType.NONE;
			case SKIP_LOCKED_MILLI -> supportsForUpdateOptions ? QUERY : LockTimeoutType.NONE;
			case Timeouts.WAIT_FOREVER_MILLI -> LockTimeoutType.QUERY;
			default -> supportsForUpdateOptions ? QUERY : LockTimeoutType.NONE;
		};
	}

	@Override
	public ConnectionLockTimeoutStrategy getConnectionLockTimeoutStrategy() {
		// while we can set the `LOCK_TIMEOUT` setting, there seems to be
		// no corollary to read that value - thus we'd not be able to reset
		// is after
		return ConnectionLockTimeoutStrategy.NONE;
	}

}
