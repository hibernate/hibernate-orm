//$Id: OneToOneLinkTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.onetoone.link;

import java.util.Date;

import junit.framework.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit.functional.FunctionalTestCase;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;

/**
 * @author Gavin King
 */
public class OneToOneLinkTest extends FunctionalTestCase {
	
	public OneToOneLinkTest(String str) {
		super(str);
	}

	public String[] getMappings() {
		return new String[] { "onetoone/link/Person.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( OneToOneLinkTest.class );
	}
	
	public void testOneToOneViaAssociationTable() {
		Person p = new Person();
		p.setName("Gavin King");
		p.setDob( new Date() );
		Employee e = new Employee();
		p.setEmployee(e);
		e.setPerson(p);
		
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist(p);
		t.commit();
		s.close();
	
		s = openSession();
		t = s.beginTransaction();
		e = (Employee) s.createQuery("from Employee e where e.person.name like 'Gavin%'").uniqueResult();
		assertEquals( e.getPerson().getName(), "Gavin King" );
		assertFalse( Hibernate.isInitialized( e.getPerson() ) );
		assertNull( e.getPerson().getCustomer() );
		s.clear();

		e = (Employee) s.createQuery("from Employee e where e.person.dob = :date")
			.setDate("date", new Date() )
			.uniqueResult();
		assertEquals( e.getPerson().getName(), "Gavin King" );
		assertFalse( Hibernate.isInitialized( e.getPerson() ) );
		assertNull( e.getPerson().getCustomer() );
		s.clear();
		
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();

		e = (Employee) s.createQuery("from Employee e join fetch e.person p left join fetch p.customer").uniqueResult();
		assertTrue( Hibernate.isInitialized( e.getPerson() ) );
		assertNull( e.getPerson().getCustomer() );
		Customer c = new Customer();
		e.getPerson().setCustomer(c);
		c.setPerson( e.getPerson() );
		
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();

		e = (Employee) s.createQuery("from Employee e join fetch e.person p left join fetch p.customer").uniqueResult();
		assertTrue( Hibernate.isInitialized( e.getPerson() ) );
		assertTrue( Hibernate.isInitialized( e.getPerson().getCustomer() ) );
		assertNotNull( e.getPerson().getCustomer() );
		s.delete(e);
		t.commit();
		s.close();
		
	}

}

