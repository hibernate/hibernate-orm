package org.hibernate.test.cascade;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Implementation of CascadeSuite.
 */
public class CascadeSuite {

	public static Test suite() {
		TestSuite suite = new TestSuite( "Cascade tests" );
		suite.addTest( BidirectionalOneToManyCascadeTest.suite() );
		suite.addTest( RefreshTest.suite() );
		return suite;
	}
}
