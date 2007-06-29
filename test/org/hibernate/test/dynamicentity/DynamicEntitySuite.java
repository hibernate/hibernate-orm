package org.hibernate.test.dynamicentity;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.test.dynamicentity.interceptor.InterceptorDynamicEntityTest;
import org.hibernate.test.dynamicentity.tuplizer.TuplizerDynamicEntityTest;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class DynamicEntitySuite {
	public static Test suite() {
		TestSuite suite = new TestSuite( "dynamic entity suite" );
		suite.addTest( InterceptorDynamicEntityTest.suite() );
		suite.addTest( TuplizerDynamicEntityTest.suite() );
		return suite;
	}
}
