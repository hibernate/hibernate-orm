//$Id: LazyOneToOneTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.lazyonetoone;

import java.util.Date;

import junit.framework.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.intercept.FieldInterceptionHelper;
import org.hibernate.testing.junit.functional.FunctionalTestCase;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;

/**
 * @author Gavin King
 */
public class LazyOneToOneTest extends FunctionalTestCase {
	
	public LazyOneToOneTest(String str) {
		super(str);
	}

	public String[] getMappings() {
		return new String[] { "lazyonetoone/Person.hbm.xml" };
	}

	public void configure(Configuration cfg) {
		cfg.setProperty(Environment.MAX_FETCH_DEPTH, "2");
		cfg.setProperty(Environment.USE_SECOND_LEVEL_CACHE, "false");
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( LazyOneToOneTest.class );
	}

	public static boolean isRunnable() {
		return FieldInterceptionHelper.isInstrumented( new Person() );
	}

	public void testLazy() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Person p = new Person("Gavin");
		Person p2 = new Person("Emmanuel");
		Employee e = new Employee(p);
		new Employment(e, "JBoss");
		Employment old = new Employment(e, "IFA");
		old.setEndDate( new Date() );
		s.persist(p);
		s.persist(p2);
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		p = (Person) s.createQuery("from Person where name='Gavin'").uniqueResult();
		//assertFalse( Hibernate.isPropertyInitialized(p, "employee") );
		assertSame( p.getEmployee().getPerson(), p );
		assertTrue( Hibernate.isInitialized( p.getEmployee().getEmployments() ) );
		assertEquals( p.getEmployee().getEmployments().size(), 1 );
		p2 = (Person) s.createQuery("from Person where name='Emmanuel'").uniqueResult();
		assertNull( p2.getEmployee() );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		p = (Person) s.get(Person.class, "Gavin");
		//assertFalse( Hibernate.isPropertyInitialized(p, "employee") );
		assertSame( p.getEmployee().getPerson(), p );
		assertTrue( Hibernate.isInitialized( p.getEmployee().getEmployments() ) );
		assertEquals( p.getEmployee().getEmployments().size(), 1 );
		p2 = (Person) s.get(Person.class, "Emmanuel");
		assertNull( p2.getEmployee() );
		s.delete(p2);
		s.delete(old);
		s.delete(p);
		t.commit();
		s.close();
	}
}

