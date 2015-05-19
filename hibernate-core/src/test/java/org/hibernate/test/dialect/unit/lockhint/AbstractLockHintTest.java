/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.dialect.unit.lockhint;

import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.dialect.Dialect;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.testing.junit4.BaseUnitTestCase;

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
			LockOptions lockOptions = new LockOptions(LockMode.UPGRADE);
			lockOptions.setAliasSpecificLockMode( aliasToLock, LockMode.UPGRADE );
			String actualProcessedSql = dialect.applyLocksToSql( rawSql, lockOptions, Collections.EMPTY_MAP );
			assertEquals( expectedProcessedSql, actualProcessedSql );
		}
	}
}
