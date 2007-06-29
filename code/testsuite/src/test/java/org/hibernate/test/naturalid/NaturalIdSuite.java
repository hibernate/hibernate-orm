package org.hibernate.test.naturalid;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.test.naturalid.immutable.ImmutableNaturalIdTest;
import org.hibernate.test.naturalid.mutable.MutableNaturalIdTest;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class NaturalIdSuite {
	public static Test suite() {
		TestSuite suite = new TestSuite( "natural ids" );
		suite.addTest( MutableNaturalIdTest.suite() );
		suite.addTest( ImmutableNaturalIdTest.suite() );
		return suite;
	}
}
