package org.hibernate.test.generated;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class GeneratedPropertySuite {
	public static Test suite() {
		TestSuite suite = new TestSuite( "generated property suite" );
		suite.addTest( TimestampGeneratedValuesWithCachingTest.suite() );
		suite.addTest( TriggerGeneratedValuesWithCachingTest.suite() );
		suite.addTest( TriggerGeneratedValuesWithoutCachingTest.suite() );
		suite.addTest( PartiallyGeneratedComponentTest.suite() );
		return suite;
	}
}
