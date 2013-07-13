/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.test.annotations.access.jpa;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.annotations.access.Closet;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class AccessTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testDefaultConfigurationModeIsInherited() throws Exception {
		User john = new User();
		john.setFirstname( "John" );
		john.setLastname( "Doe" );
		List<User> friends = new ArrayList<User>();
		User friend = new User();
		friend.setFirstname( "Jane" );
		friend.setLastname( "Doe" );
		friends.add( friend );
		john.setFriends( friends );

		Session s = openSession();
		s.persist( john );
		Transaction tx = s.beginTransaction();
		tx.commit();
		s.clear();
		tx = s.beginTransaction();
		john = ( User ) s.get( User.class, john.getId() );
		assertEquals( "Wrong number of friends", 1, john.getFriends().size() );
		assertNull( john.firstname );

		s.delete( john );
		tx.commit();
		s.close();
	}

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

	@Test
	public void testEmbeddableUsesAccessStrategyOfContainingClass() throws Exception {
		Circle circle = new Circle();
		Color color = new Color( 5, 10, 15 );
		circle.setColor( color );
		Session s = openSession();
		s.persist( circle );
		Transaction tx = s.beginTransaction();
		tx.commit();
		s.clear();
		tx = s.beginTransaction();
		circle = ( Circle ) s.get( Circle.class, circle.getId() );
		assertEquals( 5, circle.getColor().r );
		try {
			circle.getColor().getR();
			fail();
		} catch (RuntimeException e) {
			// success		
		}
		s.delete( circle );
		tx.commit();
		s.close();
	}

	@Test
	public void testEmbeddableExplicitAccessStrategy() throws Exception {
		Square square = new Square();
		Position pos = new Position( 10, 15 );
		square.setPosition( pos );
		Session s = openSession();
		s.persist( square );
		Transaction tx = s.beginTransaction();
		tx.commit();
		s.clear();
		tx = s.beginTransaction();
		square = ( Square ) s.get( Square.class, square.getId() );
		assertEquals( 10, square.getPosition().x );
		try {
			square.getPosition().getX();
			fail();
		} catch (RuntimeException e) {
			// success
		}
		s.delete( square );
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
				Closet.class,
				Person.class,
				User.class,
				Shape.class,
				Circle.class,
				Color.class,
				Square.class,
				Position.class
		};
	}
}
