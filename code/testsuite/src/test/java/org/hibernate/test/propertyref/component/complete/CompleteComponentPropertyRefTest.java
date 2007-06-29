package org.hibernate.test.propertyref.component.complete;

import junit.framework.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.junit.functional.FunctionalTestCase;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;

/**
 * @author Gavin King
 */
public class CompleteComponentPropertyRefTest extends FunctionalTestCase {

	public CompleteComponentPropertyRefTest(String name) {
		super( name );
	}

	public String[] getMappings() {
		return new String[] { "propertyref/component/complete/Mapping.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( CompleteComponentPropertyRefTest.class );
	}

	public void testComponentPropertyRef() {
		Person p = new Person();
		p.setIdentity( new Identity() );
		Account a = new Account();
		a.setNumber("123-12345-1236");
		a.setOwner(p);
		p.getIdentity().setName("Gavin");
		p.getIdentity().setSsn("123-12-1234");
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist(p);
		s.persist(a);
		s.flush();
		s.clear();

		a = (Account) s.createQuery("from Account a left join fetch a.owner").uniqueResult();
		assertTrue( Hibernate.isInitialized( a.getOwner() ) );
		assertNotNull( a.getOwner() );
		assertEquals( "Gavin", a.getOwner().getIdentity().getName() );
		s.clear();

		a = (Account) s.get(Account.class, "123-12345-1236");
		assertFalse( Hibernate.isInitialized( a.getOwner() ) );
		assertNotNull( a.getOwner() );
		assertEquals( "Gavin", a.getOwner().getIdentity().getName() );
		assertTrue( Hibernate.isInitialized( a.getOwner() ) );

		s.clear();

		getSessions().evict(Account.class);
		getSessions().evict(Person.class);

		a = (Account) s.get(Account.class, "123-12345-1236");
		assertTrue( Hibernate.isInitialized( a.getOwner() ) );
		assertNotNull( a.getOwner() );
		assertEquals( "Gavin", a.getOwner().getIdentity().getName() );
		assertTrue( Hibernate.isInitialized( a.getOwner() ) );

		s.delete( a );
		s.delete( a.getOwner() );
		tx.commit();
		s.close();
	}
}
