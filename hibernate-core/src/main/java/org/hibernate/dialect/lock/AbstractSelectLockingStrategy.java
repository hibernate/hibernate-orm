/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.lock;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.persister.entity.Lockable;

/**
 * Base {@link LockingStrategy} implementation to support implementations
 * based on issuing <tt>SQL</tt> <tt>SELECT</tt> statements
 *
 * @author Steve Ebersole
 */
public abstract class AbstractSelectLockingStrategy implements LockingStrategy {
	private final Lockable lockable;
	private final LockMode lockMode;
	private final String waitForeverSql;

	protected AbstractSelectLockingStrategy(Lockable lockable, LockMode lockMode) {
		this.lockable = lockable;
		this.lockMode = lockMode;
		this.waitForeverSql = generateLockString( LockOptions.WAIT_FOREVER );
	}

	protected Lockable getLockable() {
		return lockable;
	}

	protected LockMode getLockMode() {
		return lockMode;
	}

	protected abstract String generateLockString(int lockTimeout);

	protected String determineSql(int timeout) {
		if ( timeout == LockOptions.WAIT_FOREVER) {
			return waitForeverSql;
		}
		else if ( timeout == LockOptions.NO_WAIT) {
			return getNoWaitSql();
		}
		else if ( timeout == LockOptions.SKIP_LOCKED) {
			return getSkipLockedSql();
		}
		else {
			return generateLockString( timeout );
		}
	}

	private String noWaitSql;

	protected String getNoWaitSql() {
		if ( noWaitSql == null ) {
			noWaitSql = generateLockString( LockOptions.NO_WAIT );
		}
		return noWaitSql;
	}

	private String skipLockedSql;

	protected String getSkipLockedSql() {
		if ( skipLockedSql == null ) {
			skipLockedSql = generateLockString( LockOptions.SKIP_LOCKED );
		}
		return skipLockedSql;
	}
}
