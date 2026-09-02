/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.internal;

import org.hibernate.Timeouts;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.lock.PessimisticLockStyle;
import org.hibernate.dialect.lock.spi.ConnectionLockTimeoutStrategy;
import org.hibernate.dialect.lock.spi.LockingClauseRenderer;
import org.hibernate.dialect.lock.spi.LockingClauseRequest;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.OuterJoinLockingType;
import org.hibernate.dialect.lock.spi.RowLockStrategy;

import static org.hibernate.Timeouts.NO_WAIT_MILLI;
import static org.hibernate.Timeouts.SKIP_LOCKED_MILLI;
import static org.hibernate.Timeouts.WAIT_FOREVER_MILLI;

/**
 * LockingSupport for HANADialect
 *
 * @author Steve Ebersole
 */
public class HANALockingSupport extends LockingSupportParameterized implements LockingClauseRenderer {
	public static final HANALockingSupport HANA_LOCKING_SUPPORT = new HANALockingSupport( true, true	);

	public static LockingSupport forDialectVersion(DatabaseVersion version) {
		final boolean supportsWait = version.isSameOrAfter( 2, 0, 10 );
		final boolean supportsSkipLocked = version.isSameOrAfter(2, 0, 30);
		return new HANALockingSupport( supportsWait, supportsSkipLocked );
	}

	public HANALockingSupport(boolean supportsSkipLocked) {
		this( false, supportsSkipLocked );
	}

	private final boolean supportsWait;
	private final boolean supportsSkipLocked;

	private HANALockingSupport(boolean supportsWait, boolean supportsSkipLocked) {
		super(
				PessimisticLockStyle.CLAUSE,
				RowLockStrategy.COLUMN,
				supportsWait,
				supportsWait,
				supportsSkipLocked,
				OuterJoinLockingType.IDENTIFIED
		);
		this.supportsWait = supportsWait;
		this.supportsSkipLocked = supportsSkipLocked;
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
		if ( !request.targets().isEmpty() ) {
			fragment.append( " of " );
			LockingClauseRendererSupport.appendTargets( fragment, request.targets() );
		}

		switch ( request.timeout().milliseconds() ) {
			case NO_WAIT_MILLI -> {
				if ( supportsWait ) {
					fragment.append( " nowait" );
				}
			}
			case SKIP_LOCKED_MILLI -> {
				if ( supportsSkipLocked ) {
					fragment.append( " ignore locked" );
				}
			}
			case WAIT_FOREVER_MILLI -> {
			}
			default -> {
				if ( supportsWait ) {
					fragment.append( " wait " ).append( Timeouts.getTimeoutInSeconds( request.timeout() ) );
				}
			}
		}
		return fragment.toString();
	}

	@Override
	public RowLockStrategy getWriteRowLockStrategy() {
		return RowLockStrategy.COLUMN;
	}

	@Override
	public OuterJoinLockingType getOuterJoinLockingType() {
		return OuterJoinLockingType.IDENTIFIED;
	}

	@Override
	public ConnectionLockTimeoutStrategy getConnectionLockTimeoutStrategy() {
		return ConnectionLockTimeoutStrategy.NONE;
	}
}
