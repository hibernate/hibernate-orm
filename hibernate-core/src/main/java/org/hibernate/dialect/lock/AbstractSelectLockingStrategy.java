/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
		return timeout == LockOptions.WAIT_FOREVER
				? waitForeverSql
				: timeout == LockOptions.NO_WAIT
						? getNoWaitSql()
						: generateLockString( timeout );
	}

	private String noWaitSql;

	public String getNoWaitSql() {
		if ( noWaitSql == null ) {
			noWaitSql = generateLockString( LockOptions.NO_WAIT );
		}
		return noWaitSql;
	}
}
