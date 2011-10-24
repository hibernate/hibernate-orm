/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
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
