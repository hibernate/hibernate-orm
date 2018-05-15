/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.annotations.inheritance.Carrot;
import org.hibernate.test.annotations.inheritance.Tomato;
import org.hibernate.test.annotations.inheritance.Vegetable;
import org.hibernate.test.annotations.inheritance.VegetablePk;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Emmanuel Bernard
 */
public class JoinedSubclassTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testDefaultValues() {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		Ferry f = new Ferry();
		f.setSize( 2 );
		f.setSea( "Channel" );
		s.persist( f );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		f = (Ferry) s.get( Ferry.class, f.getId() );
		assertNotNull( f );
		assertEquals( "Channel", f.getSea() );
		assertEquals( 2, f.getSize() );
		s.delete( f );
		tx.commit();
		s.close();
	}

	@Test
	public void testDeclaredValues() {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		Country c = new Country();
		c.setName( "France" );
		AmericaCupClass f = new AmericaCupClass();
		f.setSize( 2 );
		f.setCountry( c );
		s.persist( c );
		s.persist( f );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		f = (AmericaCupClass) s.get( AmericaCupClass.class, f.getId() );
		assertNotNull( f );
		assertEquals( c, f.getCountry() );
		assertEquals( 2, f.getSize() );
		s.delete( f );
		s.delete( f.getCountry() );
		tx.commit();
		s.close();
	}

	@Test
	public void testCompositePk() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		Carrot c = new Carrot();
		VegetablePk pk = new VegetablePk();
		pk.setFarmer( "Bill" );
		pk.setHarvestDate( "2004-08-15" );
		c.setId( pk );
		c.setLength( 23 );
		s.persist( c );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		Vegetable v = (Vegetable) s.createCriteria( Vegetable.class ).uniqueResult();
		assertTrue( v instanceof Carrot );
		Carrot result = (Carrot) v;
		assertEquals( 23, result.getLength() );
		tx.commit();
		s.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				Boat.class,
				Ferry.class,
				AmericaCupClass.class,
				Country.class,
				Vegetable.class,
				Carrot.class,
				Tomato.class
		};
	}
}
