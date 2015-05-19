/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.LockOptions;
import org.hibernate.sql.ANSIJoinFragment;
import org.hibernate.sql.JoinFragment;

/**
 * A dialect specifically for use with Oracle 10g.
 * <p/>
 * The main difference between this dialect and {@link Oracle9iDialect}
 * is the use of "ANSI join syntax".
 *
 * @author Steve Ebersole
 */
public class Oracle10gDialect extends Oracle9iDialect {
	/**
	 * Constructs a Oracle10gDialect
	 */
	public Oracle10gDialect() {
		super();
	}

	@Override
	public JoinFragment createOuterJoinFragment() {
		return new ANSIJoinFragment();
	}

	@Override
	public String getCrossJoinSeparator() {
		return " cross join ";
	}

	@Override
	public String getWriteLockString(int timeout) {
		if ( timeout == LockOptions.SKIP_LOCKED ) {
			return  getForUpdateSkipLockedString();
		}
		else {
			return super.getWriteLockString( timeout );
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
