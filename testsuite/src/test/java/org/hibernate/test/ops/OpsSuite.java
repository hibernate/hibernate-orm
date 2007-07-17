package org.hibernate.test.ops;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class OpsSuite {
	public static Test suite() {
		TestSuite suite = new TestSuite( "Operations tests" );
		suite.addTest( CreateTest.suite() );
		suite.addTest( DeleteTest.suite() );
		suite.addTest( GetLoadTest.suite() );
		suite.addTest( MergeTest.suite() );
		suite.addTest( SaveOrUpdateTest.suite() );
		return suite;
	}
}
