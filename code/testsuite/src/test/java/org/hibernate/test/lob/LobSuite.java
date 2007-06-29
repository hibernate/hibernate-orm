package org.hibernate.test.lob;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class LobSuite {
	public static Test suite() {
		TestSuite suite = new TestSuite( "LOB handling tests" );
		suite.addTest( SerializableTypeTest.suite() );
		suite.addTest( BlobTest.suite() );
		suite.addTest( ClobTest.suite() );
		return suite;
	}
}
