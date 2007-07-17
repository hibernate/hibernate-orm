//$Id: UnconstrainedTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.unconstrained;

import junit.framework.Test;

import org.hibernate.FetchMode;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.junit.functional.FunctionalTestCase;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;

/**
 * @author Gavin King
 */
public class UnconstrainedTest extends FunctionalTestCase {
	
	public UnconstrainedTest(String str) {
		super(str);
	}

	public String[] getMappings() {
		return new String[] { "unconstrained/Person.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( UnconstrainedTest.class );
	}

	public void testUnconstrainedNoCache() {
		Session session = openSession();
		Transaction tx = session.beginTransaction();
		Person p = new Person("gavin");
		p.setEmployeeId("123456");
		session.persist(p);
		tx.commit();
		session.close();
		
		getSessions().evict(Person.class);
		
		session = openSession();
		tx = session.beginTransaction();
		p = (Person) session.get(Person.class, "gavin");
		assertNull( p.getEmployee() );
		p.setEmployee( new Employee("123456") );
		tx.commit();
		session.close();

		getSessions().evict(Person.class);
		
		session = openSession();
		tx = session.beginTransaction();
		p = (Person) session.get(Person.class, "gavin");
		assertTrue( Hibernate.isInitialized( p.getEmployee() ) );
		assertNotNull( p.getEmployee() );
		session.delete(p);
		tx.commit();
		session.close();
	}

	public void testUnconstrainedOuterJoinFetch() {
		Session session = openSession();
		Transaction tx = session.beginTransaction();
		Person p = new Person("gavin");
		p.setEmployeeId("123456");
		session.persist(p);
		tx.commit();
		session.close();
		
		getSessions().evict(Person.class);
		
		session = openSession();
		tx = session.beginTransaction();
		p = (Person) session.createCriteria(Person.class)
			.setFetchMode("employee", FetchMode.JOIN)
			.add( Restrictions.idEq("gavin") )
			.uniqueResult();
		assertNull( p.getEmployee() );
		p.setEmployee( new Employee("123456") );
		tx.commit();
		session.close();

		getSessions().evict(Person.class);
		
		session = openSession();
		tx = session.beginTransaction();
		p = (Person) session.createCriteria(Person.class)
			.setFetchMode("employee", FetchMode.JOIN)
			.add( Restrictions.idEq("gavin") )
			.uniqueResult();
		assertTrue( Hibernate.isInitialized( p.getEmployee() ) );
		assertNotNull( p.getEmployee() );
		session.delete(p);
		tx.commit();
		session.close();
	}

	public void testUnconstrained() {
		Session session = openSession();
		Transaction tx = session.beginTransaction();
		Person p = new Person("gavin");
		p.setEmployeeId("123456");
		session.persist(p);
		tx.commit();
		session.close();
		
		session = openSession();
		tx = session.beginTransaction();
		p = (Person) session.get(Person.class, "gavin");
		assertNull( p.getEmployee() );
		p.setEmployee( new Employee("123456") );
		tx.commit();
		session.close();

		session = openSession();
		tx = session.beginTransaction();
		p = (Person) session.get(Person.class, "gavin");
		assertTrue( Hibernate.isInitialized( p.getEmployee() ) );
		assertNotNull( p.getEmployee() );
		session.delete(p);
		tx.commit();
		session.close();
	}

}

