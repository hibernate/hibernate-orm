package org.hibernate.test.dialect.functional;

import junit.framework.TestSuite;

import org.hibernate.test.dialect.functional.cache.SQLFunctionsInterSystemsTest;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class DialectFunctionalTestsSuite {
	public static TestSuite suite() {
		TestSuite suite = new TestSuite( "Dialect tests" );
		suite.addTest( SQLFunctionsInterSystemsTest.suite() );
		return suite;
	}
}
