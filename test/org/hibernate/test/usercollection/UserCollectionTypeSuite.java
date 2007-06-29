package org.hibernate.test.usercollection;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.test.usercollection.basic.UserCollectionTypeTest;
import org.hibernate.test.usercollection.parameterized.ParameterizedUserCollectionTypeTest;

/**
 * Suite for testing various aspects of user collection types.
 *
 * @author Steve Ebersole
 */
public class UserCollectionTypeSuite {
	public static Test suite() {
		TestSuite suite = new TestSuite( "user collection type tests" );
		suite.addTest( UserCollectionTypeTest.suite() );
		suite.addTest( ParameterizedUserCollectionTypeTest.suite() );
		return suite;
	}
}
