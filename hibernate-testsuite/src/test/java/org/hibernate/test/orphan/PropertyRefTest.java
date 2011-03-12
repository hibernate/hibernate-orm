// $Id: PropertyRefTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.orphan;

import junit.framework.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit.functional.FunctionalTestCase;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;


/**
 * Test for HHH-565
 *
 * @author Steve Ebersole
 */
public class PropertyRefTest extends FunctionalTestCase {

	public PropertyRefTest(String name) {
		super( name );
	}

	public String[] getMappings() {
		return new String[] { "orphan/User.hbm.xml", "orphan/Mail.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( PropertyRefTest.class );
	}

	public void testDeleteParentWithBidirOrphanDeleteCollectionBasedOnPropertyRef() {
		Session session = openSession();
		Transaction txn = session.beginTransaction();
		User user = new User( "test" );
		user.addMail( "test" );
		user.addMail( "test" );
		session.save( user );
		txn.commit();
		session.close();

		session = openSession();
		txn = session.beginTransaction();
		user = ( User ) session.load( User.class, user.getId() );
		session.delete( user );
		txn.commit();
		session.close();

		session = openSession();
		txn = session.beginTransaction();
		session.createQuery( "delete from Mail where alias = :alias" ).setString( "alias", "test" ).executeUpdate();
		session.createQuery( "delete from User where userid = :userid" ).setString( "userid", "test" ).executeUpdate();
		txn.commit();
		session.close();
	}
	
}
