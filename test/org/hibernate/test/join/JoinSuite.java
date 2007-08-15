//$Id: $
package org.hibernate.test.join;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author Gail Badner
 */

/**
 * Implementation of JoinSuite.
 */
public class JoinSuite {

	public static Test suite() {
		TestSuite suite = new TestSuite( "Join tests");
		suite.addTest( JoinTest.suite() );
		suite.addTest( OptionalJoinTest.suite() );
		return suite;
	}
}
