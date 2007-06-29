// $Id$
package org.hibernate.test.connections;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Implementation of ConnectionsSuite.
 *
 * @author Steve Ebersole
 */
public class ConnectionsSuite {

	public static Test suite() {
		TestSuite suite = new TestSuite( "Connection-management tests");
		suite.addTest( AggressiveReleaseTest.suite() );
		suite.addTest( BasicConnectionProviderTest.suite() );
		suite.addTest( CurrentSessionConnectionTest.suite() );
		suite.addTest( SuppliedConnectionTest.suite() );
		suite.addTest( ThreadLocalCurrentSessionTest.suite() );
		return suite;
	}
}
