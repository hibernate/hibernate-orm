/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.fetch;

import java.util.Date;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;

import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Emmanuel Bernard
 */
public class FetchingTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testLazy() {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		Person p = new Person( "Gavin", "King", "JBoss Inc" );
		Stay stay = new Stay( p, new Date(), new Date(), "A380", "Blah", "Blah" );
		p.addStay( stay );
		s.persist( p );
		tx.commit();
		s.clear();
		tx = s.beginTransaction();
		p = (Person) s.createQuery( "from Person p where p.firstName = :name" )
				.setParameter( "name", "Gavin" ).uniqueResult();
		assertFalse( Hibernate.isInitialized( p.getStays() ) );
		s.remove( p );
		tx.commit();
		s.close();
	}

	@Test
	public void testHibernateFetchingLazy() {
		try(Session s = openSession()) {
			Transaction tx = s.beginTransaction();
			try {
				Person p = new Person( "Gavin", "King", "JBoss Inc" );
				Stay stay = new Stay( null, new Date(), new Date(), "A380", "Blah", "Blah" );
				Stay stay2 = new Stay( null, new Date(), new Date(), "A320", "Blah", "Blah" );
				Stay stay3 = new Stay( null, new Date(), new Date(), "A340", "Blah", "Blah" );
				stay.setOldPerson( p );
				stay2.setVeryOldPerson( p );
				stay3.setVeryOldPerson( p );
				p.addOldStay( stay );
				p.addVeryOldStay( stay2 );
				p.addVeryOldStay( stay3 );
				s.persist( p );
				tx.commit();
				s.clear();
				tx = s.beginTransaction();
				p = (Person) s.createQuery( "from Person p where p.firstName = :name" )
						.setParameter( "name", "Gavin" ).uniqueResult();
				assertFalse( Hibernate.isInitialized( p.getOldStays() ) );
				assertEquals( 1, p.getOldStays().size() );
				assertTrue( Hibernate.isInitialized( p.getOldStays() ) );
				s.clear();
				stay = (Stay) s.get( Stay.class, stay.getId() );
				assertTrue( !Hibernate.isInitialized( stay.getOldPerson() ) );
				s.clear();
				stay3 = (Stay) s.get( Stay.class, stay3.getId() );
				assertTrue(
						"FetchMode.JOIN should overrides lazy options",
						Hibernate.isInitialized( stay3.getVeryOldPerson() )
				);
				s.remove( stay3.getVeryOldPerson() );
				tx.commit();
			}finally {
				if ( tx.isActive() ) {
					tx.rollback();
				}
			}
		}
	}

	@Test
	public void testOneToManyFetchEager() {
		Branch b = new Branch();
		Session s = openSession( );
		try {
			s.getTransaction().begin();
			s.persist( b );
			s.flush();
			Leaf l = new Leaf();
			l.setBranch( b );
			s.persist( l );
			s.flush();

			s.clear();

			CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
			CriteriaQuery<Branch> criteria = criteriaBuilder.createQuery( Branch.class );
			criteria.from( Branch.class );
			s.createQuery( criteria ).list();
//			s.createCriteria( Branch.class ).list();

		}
		finally {
			if ( s.getTransaction().isActive() ) {
				s.getTransaction().rollback();
			}
			s.close();
		}
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				Person.class,
				Stay.class,
				Branch.class,
				Leaf.class
		};
	}
}
