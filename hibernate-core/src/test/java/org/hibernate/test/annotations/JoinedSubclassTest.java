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
package org.hibernate.test.annotations;

import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.test.annotations.inheritance.Carrot;
import org.hibernate.test.annotations.inheritance.Tomato;
import org.hibernate.test.annotations.inheritance.Vegetable;
import org.hibernate.test.annotations.inheritance.VegetablePk;
import org.junit.Test;

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
				VegetablePk.class,
				Carrot.class,
				Tomato.class
		};
	}
}
