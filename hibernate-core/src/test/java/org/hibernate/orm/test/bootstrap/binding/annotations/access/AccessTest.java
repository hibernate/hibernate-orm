/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.access;

import org.hibernate.Transaction;

import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Emmanuel Bernard
 */
@DomainModel(
		annotatedClasses = {
				Bed.class,
				Chair.class,
				Furniture.class,
				BigBed.class,
				Gardenshed.class,
		}
)
@ServiceRegistry
@SessionFactory
public class AccessTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testSuperclassOverriding(SessionFactoryScope scope) {
		Furniture fur = new Furniture();
		fur.setColor( "Black" );
		fur.setName( "Beech" );
		fur.isAlive = true;
		scope.inSession(
				session -> {
					Transaction tx = session.beginTransaction();
					session.persist( fur );
					tx.commit();

					session.clear();

					tx = session.beginTransaction();
					Furniture retrievedFur = session.get( Furniture.class, fur.getId() );
					assertFalse( retrievedFur.isAlive );
					assertNotNull( retrievedFur.getColor() );
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
					Transaction tx = session.beginTransaction();
					session.persist( fur );
					tx.commit();

					session.clear();

					tx = session.beginTransaction();
					Furniture retrievedFur = session.get( Furniture.class, fur.getId() );
					assertNotNull( retrievedFur.getGod() );
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
					Transaction tx = session.beginTransaction();
					session.persist( fur );
					tx.commit();

					session.clear();

					tx = session.beginTransaction();
					Furniture retrievedFur = session.get( Furniture.class, fur.getId() );
					assertEquals( 5, retrievedFur.weight );
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
					Transaction tx = session.beginTransaction();
					session.persist( chair );
					tx.commit();

					session.clear();

					tx = session.beginTransaction();
					Chair retrievedChair = ( session.get( Chair.class, chair.getId() ) );
					assertNull( retrievedChair.getPillow() );
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
					Transaction tx = session.beginTransaction();
					session.persist( bed );
					tx.commit();

					session.clear();

					tx = session.beginTransaction();
					BigBed retrievedBed = session.get( BigBed.class, bed.getId() );
					assertEquals( 5, retrievedBed.size );
					assertNull( retrievedBed.getQuality() );
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
					Transaction tx = session.beginTransaction();
					session.persist( gs );
					tx.commit();

					session.clear();

					tx = session.beginTransaction();
					Gardenshed retrievedGs = session.get( Gardenshed.class, gs.getId() );
					assertEquals( 4, retrievedGs.floors );
					assertEquals( 6, retrievedGs.getFloors() );
					tx.commit();
				}
		);
	}

}
