package org.hibernate.test.sql.check;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Suite for testing custom SQL result checking strategies.
 * <p/>
 * Yes, currently there is only one actual test...
 *
 * @author Steve Ebersole
 */
public class CheckSuite {
	public static Test suite() {
		TestSuite suite = new TestSuite( "native sql result checking" );
		suite.addTest( OracleCheckStyleTest.suite() );
		return suite;
	}
}
