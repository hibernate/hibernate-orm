//$Id: OneToOneFormulaTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.onetoone.formula;

import junit.framework.Test;

import org.hibernate.FetchMode;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.criterion.Property;
import org.hibernate.testing.junit.functional.FunctionalTestCase;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;

/**
 * @author Gavin King
 */
public class OneToOneFormulaTest extends FunctionalTestCase {
	
	public OneToOneFormulaTest(String str) {
		super(str);
	}

	public String[] getMappings() {
		return new String[] { "onetoone/formula/Person.hbm.xml" };
	}

	public void configure(Configuration cfg) {
		cfg.setProperty(Environment.USE_SECOND_LEVEL_CACHE, "false");
		cfg.setProperty(Environment.GENERATE_STATISTICS, "true");
		cfg.setProperty(Environment.DEFAULT_BATCH_FETCH_SIZE, "2");
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( OneToOneFormulaTest.class );
	}
	
	public void testOneToOneFormula() {
		Person p = new Person();
		p.setName("Gavin King");
		Address a = new Address();
		a.setPerson(p);
		a.setType("HOME");
		a.setZip("3181");
		a.setState("VIC");
		a.setStreet("Karbarook Ave");
		p.setAddress(a);
		
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist(p);
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		p = (Person) s.createQuery("from Person").uniqueResult();
		
		assertNotNull( p.getAddress() );
		assertTrue( Hibernate.isInitialized( p.getAddress() ) );
		assertNull( p.getMailingAddress() );

		s.clear();

		p = (Person) s.createQuery("from Person p left join fetch p.mailingAddress left join fetch p.address").uniqueResult();

		assertNotNull( p.getAddress() );
		assertTrue( Hibernate.isInitialized( p.getAddress() ) );
		assertNull( p.getMailingAddress() );

		s.clear();

		p = (Person) s.createQuery("from Person p left join fetch p.address").uniqueResult();

		assertNotNull( p.getAddress() );
		assertTrue( Hibernate.isInitialized( p.getAddress() ) );
		assertNull( p.getMailingAddress() );

		s.clear();

		p = (Person) s.createCriteria(Person.class)
			.createCriteria("address")
				.add( Property.forName("zip").eq("3181") )
			.uniqueResult();
		assertNotNull(p);
		
		s.clear();

		p = (Person) s.createCriteria(Person.class)
			.setFetchMode("address", FetchMode.JOIN)
			.uniqueResult();

		assertNotNull( p.getAddress() );
		assertTrue( Hibernate.isInitialized( p.getAddress() ) );
		assertNull( p.getMailingAddress() );
		
		s.clear();

		p = (Person) s.createCriteria(Person.class)
			.setFetchMode("mailingAddress", FetchMode.JOIN)
			.uniqueResult();

		assertNotNull( p.getAddress() );
		assertTrue( Hibernate.isInitialized( p.getAddress() ) );
		assertNull( p.getMailingAddress() );
		
		s.delete(p);
		
		t.commit();
		s.close();
		
	}
	
	public void testOneToOneEmbeddedCompositeKey() {
		Person p = new Person();
		p.setName("Gavin King");
		Address a = new Address();
		a.setPerson(p);
		a.setType("HOME");
		a.setZip("3181");
		a.setState("VIC");
		a.setStreet("Karbarook Ave");
		p.setAddress(a);
		
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist(p);
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		
		a = new Address();
		a.setType("HOME");
		a.setPerson(p);
		a = (Address) s.load(Address.class, a);
		assertFalse( Hibernate.isInitialized(a) );
		a.getPerson();
		a.getType();
		assertFalse( Hibernate.isInitialized(a) );
		assertEquals(a.getZip(), "3181");
		
		s.clear();
		
		a = new Address();
		a.setType("HOME");
		a.setPerson(p);
		Address a2 = (Address) s.get(Address.class, a);
		assertTrue( Hibernate.isInitialized(a) );
		assertSame(a2, a);
		assertSame(a2.getPerson(), p); //this is a little bit desirable
		assertEquals(a.getZip(), "3181");
		
		s.delete(a2);
		s.delete( s.get( Person.class, p.getName() ) ); //this is certainly undesirable! oh well...
		
		t.commit();
		s.close();
		
	}

}
