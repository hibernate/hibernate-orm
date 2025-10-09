/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.unit.lockhint;

import java.util.HashMap;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.dialect.Dialect;
import org.hibernate.internal.util.StringHelper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractLockHintTest {
	private Dialect dialect;

	protected abstract String getLockHintUsed();
	protected abstract Dialect getDialectUnderTest();

	@BeforeEach
	public void setUp() throws Exception {
		this.dialect = getDialectUnderTest();
	}

	@AfterEach
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
			final HashMap<String, String[]> aliasMap = new HashMap<>();
			aliasMap.put( aliasToLock, new String[] { "id" } );
			String actualProcessedSql = dialect.applyLocksToSql( rawSql, lockOptions( aliasToLock ), aliasMap );
			assertEquals( expectedProcessedSql, actualProcessedSql );
		}
	}
}
