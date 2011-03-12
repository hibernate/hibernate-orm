// $Id: CurrentSessionConnectionTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.connections;

import junit.framework.Test;

import org.hibernate.Session;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;

/**
 * Implementation of CurrentSessionConnectionTest.
 *
 * @author Steve Ebersole
 */
public class CurrentSessionConnectionTest extends AggressiveReleaseTest {

	public CurrentSessionConnectionTest(String name) {
		super( name );
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( CurrentSessionConnectionTest.class );
	}

	protected Session getSessionUnderTest() throws Throwable {
		return getSessions().getCurrentSession();
	}

	protected void release(Session session) {
		// do nothing, txn synch should release session as part of current-session definition
	}
}
