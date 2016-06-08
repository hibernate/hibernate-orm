/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.LockOptions;

/**
 * An SQL dialect for Postgres 9.5 and later. Adds support for SKIP LOCKED.
 */
public class PostgreSQL95Dialect extends PostgreSQL94Dialect {

	@Override
	public String getWriteLockString(int timeout) {
		if ( timeout == LockOptions.SKIP_LOCKED ) {
			return getForUpdateSkipLockedString();
		}
		else {
			return super.getWriteLockString( timeout );
		}
	}

	@Override
	public String getWriteLockString(String aliases, int timeout) {
		if ( timeout == LockOptions.SKIP_LOCKED ) {
			return getForUpdateSkipLockedString( aliases );
		}
		else {
			return super.getWriteLockString( aliases, timeout );
		}
	}

	@Override
	public String getReadLockString(int timeout) {
		if ( timeout == LockOptions.SKIP_LOCKED ) {
			return " for share skip locked";
		}
		else {
			return super.getReadLockString( timeout );
		}
	}

	@Override
	public String getReadLockString(String aliases, int timeout) {
		if ( timeout == LockOptions.SKIP_LOCKED ) {
			return String.format( " for share of %s skip locked", aliases );
		}
		else {
			return super.getReadLockString( aliases, timeout );
		}
	}

	@Override
	public String getForUpdateSkipLockedString() {
		return " for update skip locked";
	}

	@Override
	public String getForUpdateSkipLockedString(String aliases) {
		return getForUpdateString() + " of " + aliases + " skip locked";
	}
}
