package org.hibernate.test.dialect.unit;

import junit.framework.TestSuite;

import org.hibernate.test.dialect.unit.lockhint.SybaseLockHintsTest;
import org.hibernate.test.dialect.unit.lockhint.SQLServerLockHintsTest;

/**
 * Suite of all unit tests of the Dialect(s).
 *
 * @author Steve Ebersole
 */
public class DialectUnitTestsSuite {
	public static TestSuite suite() {
		TestSuite suite = new TestSuite( "Dialect unit-tests" );
		suite.addTest( SybaseLockHintsTest.suite() );
		suite.addTest( SQLServerLockHintsTest.suite() );
		return suite;
	}
}
