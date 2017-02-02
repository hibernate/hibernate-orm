/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.access;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Emmanuel Bernard
 */
public class AccessTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testSuperclassOverriding() throws Exception {
		Furniture fur = new Furniture();
		fur.setColor( "Black" );
		fur.setName( "Beech" );
		fur.isAlive = true;
		Session s = openSession();
		s.persist( fur );
		Transaction tx = s.beginTransaction();
		tx.commit();
		s.clear();
		tx = s.beginTransaction();
		fur = ( Furniture ) s.get( Furniture.class, fur.getId() );
		assertFalse( fur.isAlive );
		assertNotNull( fur.getColor() );
		s.delete( fur );
		tx.commit();
		s.close();
	}

	@Test
	public void testSuperclassNonOverriding() throws Exception {
		Furniture fur = new Furniture();
		fur.setGod( "Buddha" );
		Session s = openSession();
		s.persist( fur );
		Transaction tx = s.beginTransaction();
		tx.commit();
		s.clear();
		tx = s.beginTransaction();
		fur = ( Furniture ) s.get( Furniture.class, fur.getId() );
		assertNotNull( fur.getGod() );
		s.delete( fur );
		tx.commit();
		s.close();
	}
    @Test
	public void testPropertyOverriding() throws Exception {
		Furniture fur = new Furniture();
		fur.weight = 3;
		Session s = openSession();
		s.persist( fur );
		Transaction tx = s.beginTransaction();
		tx.commit();
		s.clear();
		tx = s.beginTransaction();
		fur = ( Furniture ) s.get( Furniture.class, fur.getId() );
		assertEquals( 5, fur.weight );
		s.delete( fur );
		tx.commit();
		s.close();
	}
    @Test
	public void testNonOverridenSubclass() throws Exception {
		Chair chair = new Chair();
		chair.setPillow( "Blue" );
		Session s = openSession();
		s.persist( chair );
		Transaction tx = s.beginTransaction();
		tx.commit();
		s.clear();
		tx = s.beginTransaction();
		chair = ( Chair ) s.get( Chair.class, chair.getId() );
		assertNull( chair.getPillow() );
		s.delete( chair );
		tx.commit();
		s.close();
	}
    @Test
	public void testOverridenSubclass() throws Exception {
		BigBed bed = new BigBed();
		bed.size = 5;
		bed.setQuality( "good" );
		Session s = openSession();
		s.persist( bed );
		Transaction tx = s.beginTransaction();
		tx.commit();
		s.clear();
		tx = s.beginTransaction();
		bed = ( BigBed ) s.get( BigBed.class, bed.getId() );
		assertEquals( 5, bed.size );
		assertNull( bed.getQuality() );
		s.delete( bed );
		tx.commit();
		s.close();
	}
    @Test
	public void testFieldsOverriding() throws Exception {
		Gardenshed gs = new Gardenshed();
		gs.floors = 4;
		Session s = openSession();
		s.persist( gs );
		Transaction tx = s.beginTransaction();
		tx.commit();
		s.clear();
		tx = s.beginTransaction();
		gs = ( Gardenshed ) s.get( Gardenshed.class, gs.getId() );
		assertEquals( 4, gs.floors );
		assertEquals( 6, gs.getFloors() );
		s.delete( gs );
		tx.commit();
		s.close();
	}
    @Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Bed.class,
				Chair.class,
				Furniture.class,
				BigBed.class,
				Gardenshed.class,
		};
	}
}
