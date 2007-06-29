package org.hibernate.test.dialect.unit.lockhint;

import junit.framework.TestSuite;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SybaseDialect;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class SybaseLockHintsTest extends AbstractLockHintTest {
	public static final Dialect DIALECT = new SybaseDialect();

	public SybaseLockHintsTest(String string) {
		super( string );
	}

	protected String getLockHintUsed() {
		return "holdlock";
	}

	protected Dialect getDialectUnderTest() {
		return DIALECT;
	}

	public static TestSuite suite() {
		return new TestSuite( SybaseLockHintsTest.class );
	}
}
