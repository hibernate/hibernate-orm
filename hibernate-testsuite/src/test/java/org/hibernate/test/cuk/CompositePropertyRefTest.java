//$Id: CompositePropertyRefTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.cuk;

import java.util.List;
import java.util.Set;

import junit.framework.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.junit.functional.FunctionalTestCase;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;

/**
 * @author Gavin King
 */
public class CompositePropertyRefTest extends FunctionalTestCase {
	
	public CompositePropertyRefTest(String str) {
		super(str);
	}

	public String[] getMappings() {
		return new String[] { "cuk/Person.hbm.xml" };
	}

	public void configure(Configuration cfg) {
		cfg.setProperty(Environment.DEFAULT_BATCH_FETCH_SIZE, "1");
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( CompositePropertyRefTest.class );
	}
	
	public void testOneToOnePropertyRef() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Person p = new Person();
		p.setName("Steve");
		p.setUserId("steve");
		Address a = new Address();
		a.setAddress("Texas");
		a.setCountry("USA");
		p.setAddress(a);
		a.setPerson(p);
		s.save(p);
		Person p2 = new Person();
		p2.setName("Max");
		p2.setUserId("max");
		s.save(p2);
		Account act = new Account();
		act.setType('c');
		act.setUser(p2);
		p2.getAccounts().add(act);
		s.save(act);
		s.flush();
		s.clear();
		
		p = (Person) s.get( Person.class, p.getId() ); //get address reference by outer join
		p2 = (Person) s.get( Person.class, p2.getId() ); //get null address reference by outer join
		assertNull( p2.getAddress() );
		assertNotNull( p.getAddress() );
		List l = s.createQuery("from Person").list(); //pull address references for cache
		assertEquals( l.size(), 2 );
		assertTrue( l.contains(p) && l.contains(p2) );
		s.clear();
		
		l = s.createQuery("from Person p order by p.name").list(); //get address references by sequential selects
		assertEquals( l.size(), 2 );
		assertNull( ( (Person) l.get(0) ).getAddress() );
		assertNotNull( ( (Person) l.get(1) ).getAddress() );
		s.clear();
		
		l = s.createQuery("from Person p left join fetch p.address a order by a.country").list(); //get em by outer join
		assertEquals( l.size(), 2 );
		if ( ( (Person) l.get(0) ).getName().equals("Max") ) {
			assertNull( ( (Person) l.get(0) ).getAddress() );
			assertNotNull( ( (Person) l.get(1) ).getAddress() );
		}
		else {
			assertNull( ( (Person) l.get(1) ).getAddress() );
			assertNotNull( ( (Person) l.get(0) ).getAddress() );
		}
		s.clear();
		
		l = s.createQuery("from Person p left join p.accounts").list();
		for ( int i=0; i<2; i++ ) {
			Object[] row = (Object[]) l.get(i);
			Person px = (Person) row[0];
			Set accounts = px.getAccounts();
			assertFalse( Hibernate.isInitialized(accounts) );
			assertTrue( px.getAccounts().size()>0 || row[1]==null );
		}
		s.clear();

		l = s.createQuery("from Person p left join fetch p.accounts a order by p.name").list();
		Person p0 = (Person) l.get(0);
		assertTrue( Hibernate.isInitialized( p0.getAccounts() ) );
		assertEquals( p0.getAccounts().size(), 1 );
		assertSame( ( (Account) p0.getAccounts().iterator().next() ).getUser(), p0 );
		Person p1 = (Person) l.get(1);
		assertTrue( Hibernate.isInitialized( p1.getAccounts() ) );
		assertEquals( p1.getAccounts().size(), 0 );
		s.clear();
		
		l = s.createQuery("from Account a join fetch a.user").list();
		
		s.clear();
		
		l = s.createQuery("from Person p left join fetch p.address").list();
		
		s.clear();
		s.createQuery( "delete Address" ).executeUpdate();
		s.createQuery( "delete Account" ).executeUpdate();
		s.createQuery( "delete Person" ).executeUpdate();
		t.commit();
		s.close();
	}

}

