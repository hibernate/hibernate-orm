/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.dialect.unit.lockhint;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SQLServer2005Dialect;

/**
 * @author Vlad Mihalcea
 */
public class SQLServer2005LockHintsTest extends AbstractLockHintTest {
	public static final Dialect DIALECT = new SQLServer2005Dialect();

	protected String getLockHintUsed() {
		return "with (updlock, rowlock, nowait)";
	}

	protected Dialect getDialectUnderTest() {
		return DIALECT;
	}

	@Override
	protected LockOptions lockOptions(String aliasToLock) {
		LockOptions lockOptions = new LockOptions( LockMode.PESSIMISTIC_WRITE).setTimeOut( LockOptions.NO_WAIT );
		lockOptions.setAliasSpecificLockMode( aliasToLock, LockMode.PESSIMISTIC_WRITE );
		return lockOptions;
	}
}
