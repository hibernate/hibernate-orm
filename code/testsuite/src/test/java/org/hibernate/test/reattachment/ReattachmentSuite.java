package org.hibernate.test.reattachment;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Suite of reattachment specific tests.
 *
 * @author Steve Ebersole
 */
public class ReattachmentSuite {
	public static Test suite() {
		TestSuite suite = new TestSuite( "reattachment semantics" );
		suite.addTest( CollectionReattachmentTest.suite() );
		suite.addTest( ProxyReattachmentTest.suite() );
		return suite;
	}
}
