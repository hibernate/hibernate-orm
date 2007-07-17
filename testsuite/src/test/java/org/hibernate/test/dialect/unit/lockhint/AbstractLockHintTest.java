package org.hibernate.test.dialect.unit.lockhint;

import java.util.HashMap;
import java.util.Collections;

import org.hibernate.junit.UnitTestCase;
import org.hibernate.dialect.Dialect;
import org.hibernate.util.StringHelper;
import org.hibernate.LockMode;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public abstract class AbstractLockHintTest extends UnitTestCase {
	public AbstractLockHintTest(String string) {
		super( string );
	}

	private Dialect dialect;

	protected abstract String getLockHintUsed();
	protected abstract Dialect getDialectUnderTest();

	protected void setUp() throws Exception {
		super.setUp();
		this.dialect = getDialectUnderTest();
	}

	protected void tearDown() throws Exception {
		this.dialect = null;
		super.tearDown();
	}

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
			HashMap lockModes = new HashMap();
			lockModes.put( aliasToLock, LockMode.UPGRADE );
			String actualProcessedSql = dialect.applyLocksToSql( rawSql, lockModes, Collections.EMPTY_MAP );
			assertEquals( expectedProcessedSql, actualProcessedSql );
		}
	}
}
