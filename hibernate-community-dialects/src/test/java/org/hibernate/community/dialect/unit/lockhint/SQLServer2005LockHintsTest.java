/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.unit.lockhint;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.community.dialect.SQLServerLegacyDialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.orm.test.dialect.unit.lockhint.AbstractLockHintTest;

/**
 * @author Vlad Mihalcea
 */
public class SQLServer2005LockHintsTest extends AbstractLockHintTest {
	public static final Dialect DIALECT = new SQLServerLegacyDialect( DatabaseVersion.make( 9 ) );

	protected String getLockHintUsed() {
		return "with (updlock,holdlock,rowlock,nowait)";
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
