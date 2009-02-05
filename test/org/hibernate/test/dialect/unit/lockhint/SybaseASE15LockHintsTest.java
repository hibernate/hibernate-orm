//$Id $
package org.hibernate.test.dialect.unit.lockhint;

import junit.framework.TestSuite;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SybaseASE15Dialect;

/**
 * {@inheritDoc}
 *
 * @author Gail Badner
 */
public class SybaseASE15LockHintsTest extends AbstractLockHintTest {
	public static final Dialect DIALECT = new SybaseASE15Dialect();

	public SybaseASE15LockHintsTest(String string) {
		super( string );
	}

	protected String getLockHintUsed() {
		return "holdlock";
	}

	protected Dialect getDialectUnderTest() {
		return DIALECT;
	}

	public static TestSuite suite() {
		return new TestSuite( SybaseASE15LockHintsTest.class );
	}
}
