/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.unit.lockhint;

import java.util.Collections;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.dialect.Dialect;
import org.hibernate.internal.util.StringHelper;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractLockHintTest extends BaseUnitTestCase {
	private Dialect dialect;

	protected abstract String getLockHintUsed();
	protected abstract Dialect getDialectUnderTest();

	@Before
	public void setUp() throws Exception {
		this.dialect = getDialectUnderTest();
	}

	@After
	public void tearDown() throws Exception {
		this.dialect = null;
	}

	@Test
	public void testBasicLocking() {
		new SyntaxChecker( "select xyz from ABC $HOLDER$", "a" ).verify();
		new SyntaxChecker( "select xyz from ABC $HOLDER$ join DEF d", "a" ).verify();
		new SyntaxChecker( "select xyz from ABC $HOLDER$, DEF d", "a" ).verify();
	}

	protected LockOptions lockOptions(String aliasToLock) {
		LockOptions lockOptions = new LockOptions(LockMode.PESSIMISTIC_WRITE);
		lockOptions.setAliasSpecificLockMode( aliasToLock, LockMode.PESSIMISTIC_WRITE );
		return lockOptions;
	}

	protected class SyntaxChecker {
		private final String aliasToLock;
		private final String rawSql;
		private final String expectedProcessedSql;

		public SyntaxChecker(String template) {
			this( template, "" );
		}

		public SyntaxChecker(String template, String aliasToLock) {
			this.aliasToLock = aliasToLock;
			rawSql = StringHelper.replace( template, "$HOLDER$", aliasToLock );
			expectedProcessedSql = StringHelper.replace( template, "$HOLDER$", aliasToLock + " " + getLockHintUsed() );
		}

		public void verify() {
			String actualProcessedSql = dialect.applyLocksToSql( rawSql, lockOptions( aliasToLock ), Collections.EMPTY_MAP );
			assertEquals( expectedProcessedSql, actualProcessedSql );
		}
	}
}
