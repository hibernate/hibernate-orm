/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.unconstrained;

import org.junit.Test;

import org.hibernate.FetchMode;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Gavin King
 */
public class UnconstrainedTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "unconstrained/Person.hbm.xml" };
	}

	@Test
	public void testUnconstrainedNoCache() {
		Session session = openSession();
		Transaction tx = session.beginTransaction();
		Person p = new Person("gavin");
		p.setEmployeeId("123456");
		session.persist(p);
		tx.commit();
		session.close();
		
		sessionFactory().getCache().evictEntityRegion( Person.class );
		
		session = openSession();
		tx = session.beginTransaction();
		p = (Person) session.get(Person.class, "gavin");
		assertNull( p.getEmployee() );
		p.setEmployee( new Employee("123456") );
		tx.commit();
		session.close();

		sessionFactory().getCache().evictEntityRegion( Person.class );
		
		session = openSession();
		tx = session.beginTransaction();
		p = (Person) session.get(Person.class, "gavin");
		assertTrue( Hibernate.isInitialized( p.getEmployee() ) );
		assertNotNull( p.getEmployee() );
		session.delete(p);
		tx.commit();
		session.close();
	}

	@Test
	public void testUnconstrainedOuterJoinFetch() {
		Session session = openSession();
		Transaction tx = session.beginTransaction();
		Person p = new Person("gavin");
		p.setEmployeeId("123456");
		session.persist(p);
		tx.commit();
		session.close();
		
		sessionFactory().getCache().evictEntityRegion( Person.class );
		
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

		sessionFactory().getCache().evictEntityRegion( Person.class );
		
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

	@Test
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

