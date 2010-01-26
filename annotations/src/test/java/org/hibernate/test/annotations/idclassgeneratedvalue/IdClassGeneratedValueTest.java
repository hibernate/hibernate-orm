// $Id$
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.annotations.idclassgeneratedvalue;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.junit.FailureExpected;
import org.hibernate.test.annotations.TestCase;

/**
 * A test.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Stale W. Pedersen</a>
 */
public class IdClassGeneratedValueTest extends TestCase {

	@SuppressWarnings({ "unchecked" })
	public void testBaseLine() {
		Session s = openSession();
		s.beginTransaction();
		Simple s1 = new Simple( 1L, 2L, 10 );
		s.persist( s1 );
		Simple s2 = new Simple( 2L, 3L, 20 );
		s.persist( s2 );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		List<Simple> simpleList = s.createQuery( "select s from Simple s" ).list();
		assertEquals( simpleList.size(), 2 );
		s1 = ( Simple ) s.load( Simple.class, new SimplePK( 1L, 2L ) );
		assertEquals( s1.getQuantity(), 10 );
		s.clear();
		s.createQuery( "delete Simple" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

	@FailureExpected(message = "Not yet implemented", jiraKey = "HHH-4552")
	@SuppressWarnings({ "unchecked" })
	public void testSingleGeneratedValue() {
		Session s = openSession();
		s.beginTransaction();
		Simple2 s1 = new Simple2( 200L, 10 );
		s.persist( s1 );
		Long s1Id1 = s1.getId1();
		Simple2 s2 = new Simple2( 300L, 20 );
		s.persist( s2 );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		List<Simple2> simpleList = s.createQuery( "select s from Simple2 s" ).list();
		assertEquals( simpleList.size(), 2 );
		s1 = ( Simple2 ) s.load( Simple2.class, new SimplePK( s1Id1, 2L ) );
		assertEquals( s1.getQuantity(), 10 );
		s.clear();
		s.createQuery( "delete Simple2" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

	@FailureExpected(message = "Not yet implemented", jiraKey = "HHH-4552")
	@SuppressWarnings({ "unchecked" })
	public void testMultipleGeneratedValue() {
		Session s = openSession();
		s.beginTransaction();
		Multiple m1 = new Multiple( 1000L, 10 );
		s.persist( m1 );
		Long m1Id1 = m1.getId1();
		Long m1Id2 = m1.getId2();
		Multiple m2 = new Multiple( 2000L, 20 );
		s.persist( m2 );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		List<Multiple> simpleList = s.createQuery( "select m from Multiple m" ).list();
		assertEquals( simpleList.size(), 2 );
		m1 = ( Multiple ) s.load( Multiple.class, new MultiplePK( m1Id1, m1Id2, 2L ) );
		assertEquals( m1.getQuantity(), 10 );
		s.clear();
		s.createQuery( "delete Multiple" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

//	public void testComplexIdClass() {
//		Session s = openSession();
//		Transaction tx = s.beginTransaction();
//
//		Customer c1 = new Customer(
//				"foo", "bar", "contact1", "100", new BigDecimal( 1000 ), new BigDecimal( 1000 ), new BigDecimal( 1000 )
//		);
//
//		s.persist( c1 );
//		Item boat = new Item();
//		boat.setId( "1" );
//		boat.setName( "cruiser" );
//		boat.setPrice( new BigDecimal( 500 ) );
//		boat.setDescription( "a boat" );
//		boat.setCategory( 42 );
//
//		s.persist( boat );
//		s.flush();
//		s.clear();
//
//		c1.addInventory( boat, 10, new BigDecimal( 5000 ) );
//		s.merge( c1 );
//		s.flush();
//		s.clear();
//
//		Customer c2 = (Customer) s.createQuery( "select c from Customer c" ).uniqueResult();
//
//		List<CustomerInventory> inventory = c2.getInventories();
//
//		assertEquals( 1, inventory.size() );
//		assertEquals( 10, inventory.get( 0 ).getQuantity() );
//
//		tx.rollback();
//		s.close();
//
//		assertTrue( true );
//	}

	protected Class[] getAnnotatedClasses() {
		return new Class[] {
//				Customer.class,
//				CustomerInventory.class,
//				Item.class,
				Simple.class,
				Simple2.class,
				Multiple.class

		};
	}
}
