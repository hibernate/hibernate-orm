/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.LockOptions;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.type.StandardBasicTypes;

/**
 * @author Vlad Mihalcea
 */
public class MySQL8Dialect extends MySQL57Dialect {

	public MySQL8Dialect() {
		// MySQL doesn't add the new reserved keywords to their JDBC driver to preserve backward compatibility.

		registerKeyword("CUME_DIST");
		registerKeyword("DENSE_RANK");
		registerKeyword("EMPTY");
		registerKeyword("EXCEPT");
		registerKeyword("FIRST_VALUE");
		registerKeyword("GROUPS");
		registerKeyword("JSON_TABLE");
		registerKeyword("LAG");
		registerKeyword("LAST_VALUE");
		registerKeyword("LEAD");
		registerKeyword("NTH_VALUE");
		registerKeyword("NTILE");
		registerKeyword("PERSIST");
		registerKeyword("PERCENT_RANK");
		registerKeyword("PERSIST_ONLY");
		registerKeyword("RANK");
		registerKeyword("ROW_NUMBER");

		registerFunction( "regexp_replace", new StandardSQLFunction( "regexp_replace", StandardBasicTypes.STRING ) );
		registerFunction( "regexp_instr", new StandardSQLFunction( "regexp_instr", StandardBasicTypes.INTEGER ) );
		registerFunction( "regexp_substr", new StandardSQLFunction( "regexp_substr", StandardBasicTypes.STRING ) );
	}

	@Override
	public String getWriteLockString(int timeout) {
		if ( timeout == LockOptions.NO_WAIT ) {
			return getForUpdateNowaitString();
		}
		else if ( timeout == LockOptions.SKIP_LOCKED ) {
			return getForUpdateSkipLockedString();
		}
		return super.getWriteLockString( timeout );
	}

	@Override
	public String getWriteLockString(String aliases, int timeout) {
		if ( timeout == LockOptions.NO_WAIT ) {
			return getForUpdateNowaitString(aliases);
		}
		else if ( timeout == LockOptions.SKIP_LOCKED ) {
			return getForUpdateSkipLockedString(aliases);
		}
		return super.getWriteLockString( aliases, timeout );
	}

	@Override
	public String getReadLockString(int timeout) {
		String readLockString =  " for share";
		if ( timeout == LockOptions.NO_WAIT ) {
			return readLockString + " nowait ";
		}
		else if ( timeout == LockOptions.SKIP_LOCKED ) {
			return readLockString + " skip locked ";
		}
		return readLockString;
	}

	@Override
	public String getReadLockString(String aliases, int timeout) {
		String readLockString = String.format( " for share of %s ", aliases );
		if ( timeout == LockOptions.NO_WAIT ) {
			return readLockString + " nowait ";
		}
		else if ( timeout == LockOptions.SKIP_LOCKED ) {
			return readLockString + " skip locked ";
		}
		return readLockString;
	}

	@Override
	public String getForUpdateSkipLockedString() {
		return " for update skip locked";
	}

	@Override
	public String getForUpdateSkipLockedString(String aliases) {
		return getForUpdateString() + " of " + aliases + " skip locked";
	}

	@Override
	public String getForUpdateNowaitString() {
		return getForUpdateString() + " nowait ";
	}

	@Override
	public String getForUpdateNowaitString(String aliases) {
		return getForUpdateString( aliases ) + " nowait ";
	}

	@Override
	public String getForUpdateString(String aliases) {
		return getForUpdateString() + " of " + aliases;
	}

	@Override
	public boolean supportsSkipLocked() {
		return true;
	}

	public boolean supportsNoWait() {
		return true;
	}
}
