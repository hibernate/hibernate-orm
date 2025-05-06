/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.access.jpa;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Transaction;
import org.hibernate.orm.test.bootstrap.binding.annotations.access.Closet;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
@DomainModel(
		annotatedClasses = {
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
		}
)
@SessionFactory
@ServiceRegistry
public class AccessTest {

	@Test
	public void testDefaultConfigurationModeIsInherited(SessionFactoryScope scope) {
		User john = new User();
		john.setFirstname( "John" );
		john.setLastname( "Doe" );

		List<User> friends = new ArrayList<>();
		User friend = new User();
		friend.setFirstname( "Jane" );
		friend.setLastname( "Doe" );
		friends.add( friend );
		john.setFriends( friends );

		scope.inSession(
				session -> {
					session.persist( john );
					Transaction tx = session.beginTransaction();
					tx.commit();

					session.clear();

					tx = session.beginTransaction();
					User user = session.get( User.class, john.getId() );
					assertEquals( 1, user.getFriends().size(), "Wrong number of friends" );
					assertNull( user.firstname );
					session.remove( user );
					tx.commit();
				}
		);
	}

	@Test
	public void testSuperclassOverriding(SessionFactoryScope scope) {
		Furniture fur = new Furniture();
		fur.setColor( "Black" );
		fur.setName( "Beech" );
		fur.isAlive = true;
		scope.inSession(
				session -> {
					session.persist( fur );
					Transaction tx = session.beginTransaction();
					tx.commit();

					session.clear();

					tx = session.beginTransaction();
					Furniture retrievedFurniture = session.get( Furniture.class, fur.getId() );
					assertFalse( retrievedFurniture.isAlive );
					assertNotNull( retrievedFurniture.getColor() );
					session.remove( retrievedFurniture );
					tx.commit();
				}
		);
	}

	@Test
	public void testSuperclassNonOverriding(SessionFactoryScope scope) {
		Furniture fur = new Furniture();
		fur.setGod( "Buddha" );
		scope.inSession(
				session -> {
					session.persist( fur );
					Transaction tx = session.beginTransaction();
					tx.commit();

					session.clear();

					tx = session.beginTransaction();
					Furniture retrievedFurniture = session.get( Furniture.class, fur.getId() );
					assertNotNull( retrievedFurniture.getGod() );
					session.remove( retrievedFurniture );
					tx.commit();
				}
		);
	}

	@Test
	public void testPropertyOverriding(SessionFactoryScope scope) {
		Furniture fur = new Furniture();
		fur.weight = 3;
		scope.inSession(
				session -> {
					session.persist( fur );
					Transaction tx = session.beginTransaction();
					tx.commit();

					session.clear();

					tx = session.beginTransaction();
					Furniture retrievedFurniture = session.get( Furniture.class, fur.getId() );
					assertEquals( 5, retrievedFurniture.weight );
					session.remove( retrievedFurniture );
					tx.commit();
				}
		);
	}

	@Test
	public void testNonOverridenSubclass(SessionFactoryScope scope) {
		Chair chair = new Chair();
		chair.setPillow( "Blue" );

		scope.inSession(
				session -> {
					session.persist( chair );
					Transaction tx = session.beginTransaction();
					tx.commit();

					session.clear();

					tx = session.beginTransaction();
					Chair retrievedChair = session.get( Chair.class, chair.getId() );
					assertNull( retrievedChair.getPillow() );
					session.remove( retrievedChair );
					tx.commit();
				}
		);
	}

	@Test
	public void testOverridenSubclass(SessionFactoryScope scope) {
		BigBed bed = new BigBed();
		bed.size = 5;
		bed.setQuality( "good" );
		scope.inSession(
				session -> {
					session.persist( bed );
					Transaction tx = session.beginTransaction();
					tx.commit();
					session.clear();
					tx = session.beginTransaction();
					BigBed retievedBed = session.get( BigBed.class, bed.getId() );
					assertEquals( 5, retievedBed.size );
					assertNull( retievedBed.getQuality() );
					session.remove( retievedBed );
					tx.commit();
				}
		);
	}

	@Test
	public void testFieldsOverriding(SessionFactoryScope scope) {
		Gardenshed gs = new Gardenshed();
		gs.floors = 4;

		scope.inSession(
				session -> {
					session.persist( gs );
					Transaction tx = session.beginTransaction();
					tx.commit();
					session.clear();
					tx = session.beginTransaction();
					Gardenshed retrievedGardenshed = session.get( Gardenshed.class, gs.getId() );
					assertEquals( 4, retrievedGardenshed.floors );
					assertEquals( 6, retrievedGardenshed.getFloors() );
					session.remove( retrievedGardenshed );
					tx.commit();
				}
		);
	}

	@Test
	public void testEmbeddableUsesAccessStrategyOfContainingClass(SessionFactoryScope scope) {
		Circle circle = new Circle();
		Color color = new Color( 5, 10, 15 );
		circle.setColor( color );

		scope.inSession(
				session -> {
					session.persist( circle );
					Transaction tx = session.beginTransaction();
					tx.commit();

					session.clear();

					tx = session.beginTransaction();
					Circle retrievedCircle = session.get( Circle.class, circle.getId() );
					assertEquals( 5, retrievedCircle.getColor().r );
					try {
						retrievedCircle.getColor().getR();
						fail();
					}
					catch (RuntimeException e) {
						// success
					}
					session.remove( retrievedCircle );
					tx.commit();
				}
		);
	}

	@Test
	public void testEmbeddableExplicitAccessStrategy(SessionFactoryScope scope) {
		Square square = new Square();
		Position pos = new Position( 10, 15 );
		square.setPosition( pos );

		scope.inSession(
				session -> {
					session.persist( square );
					Transaction tx = session.beginTransaction();
					tx.commit();
					session.clear();
					tx = session.beginTransaction();
					Square retrievedsquare = session.get( Square.class, square.getId() );
					assertEquals( 10, retrievedsquare.getPosition().x );
					try {
						retrievedsquare.getPosition().getX();
						fail();
					}
					catch (RuntimeException e) {
						// success
					}
					session.remove( retrievedsquare );
					tx.commit();
				}
		);
	}
}
