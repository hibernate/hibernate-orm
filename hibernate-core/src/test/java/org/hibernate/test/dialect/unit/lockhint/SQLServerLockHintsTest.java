package org.hibernate.test.dialect.unit.lockhint;

import junit.framework.TestSuite;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SQLServerDialect;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class SQLServerLockHintsTest extends AbstractLockHintTest {
	public static final Dialect DIALECT = new SQLServerDialect();

	public SQLServerLockHintsTest(String string) {
		super( string );
	}

	protected String getLockHintUsed() {
		return "with (updlock, rowlock)";
	}

	protected Dialect getDialectUnderTest() {
		return DIALECT;
	}

	public static TestSuite suite() {
		return new TestSuite( SQLServerLockHintsTest.class );
	}
}
