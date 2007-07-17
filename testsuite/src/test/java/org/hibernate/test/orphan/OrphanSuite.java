package org.hibernate.test.orphan;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class OrphanSuite {
	public static Test suite() {
		TestSuite suite = new TestSuite( "orphan delete suite" );
		suite.addTest( OrphanTest.suite() );
		suite.addTest( PropertyRefTest.suite() );
		return suite;
	}
}
