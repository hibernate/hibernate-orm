/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.annotations.fetch;

import java.util.Date;

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
	public void testLazy() throws Exception {
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
		s.delete( p );
		tx.commit();
		s.close();
	}

	@Test
	public void testExtraLazy() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		Person p = new Person( "Gavin", "King", "JBoss Inc" );
		Stay stay = new Stay( p, new Date(), new Date(), "A380", "Blah", "Blah" );
		p.getOrderedStay().add( stay );
		s.persist( p );
		tx.commit();
		s.clear();
		tx = s.beginTransaction();
		p = (Person) s.createQuery( "from Person p where p.firstName = :name" )
				.setParameter( "name", "Gavin" ).uniqueResult();
		assertFalse( Hibernate.isInitialized( p.getOrderedStay() ) );
		assertEquals( 1, p.getOrderedStay().size() );
		assertFalse( Hibernate.isInitialized( p.getOrderedStay() ) );
		assertEquals( "A380", p.getOrderedStay().get(0).getVessel() );
		assertFalse( Hibernate.isInitialized( p.getOrderedStay() ) );
		s.delete( p );
		tx.commit();
		s.close();
	}

	@Test
	public void testHibernateFetchingLazy() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
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
		assertFalse( "lazy extra is failing", Hibernate.isInitialized( p.getOldStays() ) );
		s.clear();
		stay = (Stay) s.get( Stay.class, stay.getId() );
		assertTrue( ! Hibernate.isInitialized( stay.getOldPerson() ) );
		s.clear();
		stay3 = (Stay) s.get( Stay.class, stay3.getId() );
		assertTrue( "FetchMode.JOIN should overrides lazy options", Hibernate.isInitialized( stay3.getVeryOldPerson() ) );
		s.delete( stay3.getVeryOldPerson() );
		tx.commit();
		s.close();
	}

	@Test
	public void testOneToManyFetchEager() throws Exception {
		Branch b = new Branch();
		Session s = openSession( );
		s.getTransaction().begin();
		s.persist( b );
		s.flush();
		Leaf l = new Leaf();
		l.setBranch( b );
		s.persist( l );
		s.flush();

		s.clear();

		s.createCriteria( Branch.class ).list();

		s.getTransaction().rollback();
		s.close();
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
