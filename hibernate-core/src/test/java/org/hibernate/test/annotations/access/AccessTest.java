//$Id$
package org.hibernate.test.annotations.access;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.annotations.TestCase;

/**
 * @author Emmanuel Bernard
 */
public class AccessTest extends TestCase {

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
