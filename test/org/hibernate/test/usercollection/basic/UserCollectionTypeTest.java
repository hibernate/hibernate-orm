//$Id$
package org.hibernate.test.usercollection.basic;

import junit.framework.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.junit.functional.FunctionalTestCase;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;

/**
 * @author Max Rydahl Andersen
 */
public class UserCollectionTypeTest extends FunctionalTestCase {
	
	public UserCollectionTypeTest(String str) {
		super(str);
	}

	public String[] getMappings() {
		return new String[] { "usercollection/basic/UserPermissions.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( UserCollectionTypeTest.class );
	}

	public void testBasicOperation() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		User u = new User("max");
		u.getEmailAddresses().add( new Email("max@hibernate.org") );
		u.getEmailAddresses().add( new Email("max.andersen@jboss.com") );
		s.persist(u);
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		User u2 = (User) s.createCriteria(User.class).uniqueResult();
		assertTrue( Hibernate.isInitialized( u2.getEmailAddresses() ) );
		assertEquals( u2.getEmailAddresses().size(), 2 );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		u2 = ( User ) s.get( User.class, u.getUserName() );
		u2.getEmailAddresses().size();
		assertEquals( 2, MyListType.lastInstantiationRequest );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		s.delete( u );
		t.commit();
		s.close();
	}

}

