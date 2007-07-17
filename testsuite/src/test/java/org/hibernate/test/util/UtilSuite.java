package org.hibernate.test.util;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.hibernate.test.util.dtd.EntityResolverTest;

/**
 * todo: describe UtilSuite
 *
 * @author Steve Ebersole
 */
public class UtilSuite {
	public static Test suite() {
		TestSuite suite = new TestSuite( "Utility package tests" );
		suite.addTest( PropertiesHelperTest.suite() );
		suite.addTest( EntityResolverTest.suite() );
		suite.addTest( StringHelperTest.suite() );
		return suite;
	}
}
