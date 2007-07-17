package org.hibernate.test.naturalid.immutable;

import junit.framework.Test;

import org.hibernate.Session;
import org.hibernate.junit.functional.FunctionalTestCase;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class ImmutableNaturalIdTest extends FunctionalTestCase {
	public ImmutableNaturalIdTest(String string) {
		super( string );
	}

	public String[] getMappings() {
		return new String[] { "naturalid/immutable/User.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( ImmutableNaturalIdTest.class );
	}

	public void testUpdate() {
		// prepare some test data...
		Session session = openSession();
    	session.beginTransaction();
	  	User user = new User();
    	user.setUserName( "steve" );
    	user.setEmail( "steve@hibernate.org" );
    	user.setFirstName( "Steve" );
    	user.setInitial( null);
    	user.setLastName( "Ebersole" );
    	user.setPassword( "brewhaha" );
		session.save( user );
    	session.getTransaction().commit();
    	session.close();

		// 'user' is now a detached entity, so lets change a property and reattch...
		user.setPassword( "homebrew" );
		session = openSession();
		session.beginTransaction();
		session.update( user );
		session.getTransaction().commit();
		session.close();

		// clean up
		session = openSession();
		session.beginTransaction();
		session.delete( user );
		session.getTransaction().commit();
		session.close();
	}
}
