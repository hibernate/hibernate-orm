/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.id;

import org.hibernate.mapping.Column;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.orm.test.annotations.id.entities.Ball;
import org.hibernate.orm.test.annotations.id.entities.BreakDance;
import org.hibernate.orm.test.annotations.id.entities.Computer;
import org.hibernate.orm.test.annotations.id.entities.Department;
import org.hibernate.orm.test.annotations.id.entities.Dog;
import org.hibernate.orm.test.annotations.id.entities.FirTree;
import org.hibernate.orm.test.annotations.id.entities.Footballer;
import org.hibernate.orm.test.annotations.id.entities.FootballerPk;
import org.hibernate.orm.test.annotations.id.entities.Furniture;
import org.hibernate.orm.test.annotations.id.entities.GoalKeeper;
import org.hibernate.orm.test.annotations.id.entities.Home;
import org.hibernate.orm.test.annotations.id.entities.Hotel;
import org.hibernate.orm.test.annotations.id.entities.Monkey;
import org.hibernate.orm.test.annotations.id.entities.Phone;
import org.hibernate.orm.test.annotations.id.entities.Shoe;
import org.hibernate.orm.test.annotations.id.entities.SoundSystem;
import org.hibernate.orm.test.annotations.id.entities.Store;
import org.hibernate.orm.test.annotations.id.entities.Tree;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Emmanuel Bernard
 */
@DomainModel(
		annotatedClasses = {
				Ball.class, Shoe.class, Store.class,
				Department.class, Dog.class, Computer.class, Home.class,
				Phone.class, Tree.class, FirTree.class, Footballer.class,
				SoundSystem.class, Furniture.class, GoalKeeper.class,
				BreakDance.class, Monkey.class, Hotel.class
		},
		annotatedPackageNames = {
				"org.hibernate.orm.test.annotations",
				"org.hibernate.orm.test.annotations.id"
		},
		xmlMappings = "org/hibernate/orm/test/annotations/orm.xml"
)
@SessionFactory
public class IdTest {

	@Test
	public void testNoGenerator(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Hotel hotel = new Hotel();
					hotel.setId( 12L );
					hotel.setName( "California" );
					session.merge( hotel );
				}
		);

		Hotel savedHotel = scope.fromTransaction(
				session -> {
					Hotel hotel = session.get( Hotel.class, 12L );
					assertNotNull( hotel );
					assertEquals( "California", hotel.getName() );
					assertNull( session.get( Hotel.class, 13L ) );
					return hotel;
				}
		);

		//savedHotel is now detached

		scope.inTransaction(
				session -> {
					savedHotel.setName( "Hotel du nord" );
					session.merge( savedHotel );
				}
		);

		scope.inTransaction(
				session -> {
					Hotel hotel = session.get( Hotel.class, 12L );
					assertNotNull( hotel );
					assertEquals( "Hotel du nord", hotel.getName() );
					session.remove( hotel );
				}
		);
	}

	@Test
	public void testGenericGenerator(SessionFactoryScope scope) {
		SoundSystem system = new SoundSystem();
		Furniture fur = new Furniture();
		scope.inTransaction(
				session -> {
					system.setBrand( "Genelec" );
					system.setModel( "T234" );
					session.persist( system );
					session.persist( fur );
				}
		);

		scope.inTransaction(
				session -> {
					SoundSystem systemFromDb = session.get( SoundSystem.class, system.getId() );
					Furniture furFromDb = session.get( Furniture.class, fur.getId() );
					assertNotNull( systemFromDb );
					assertNotNull( furFromDb );
					session.remove( systemFromDb );
					session.remove( furFromDb );
				}
		);
	}

	/*
	 * Ensures that GenericGenerator annotations wrapped inside a
	 * GenericGenerators holder are bound correctly
	 */
	@Test
	public void testGenericGenerators(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Monkey monkey = new Monkey();
					session.persist( monkey );
					session.flush();
					assertNotNull( monkey.getId() );
					session.remove( monkey );
				}
		);
	}

	@Test
	public void testTableGenerator(SessionFactoryScope scope) {

		Ball b = new Ball();
		Dog d = new Dog();
		Computer c = new Computer();
		scope.inTransaction(
				session -> {
					session.persist( b );
					session.persist( d );
					session.persist( c );
				}
		);

		assertEquals( new Integer( 1 ), b.getId(), "table id not generated" );
		assertEquals( new Integer( 1 ), d.getId(), "generator should not be shared" );
		assertEquals( new Long( 1 ), c.getId(), "default value should work" );

		scope.inTransaction(
				session -> {
					session.remove( session.get( Ball.class, 1 ) );
					session.remove( session.get( Dog.class, 1 ) );
					session.remove( session.get( Computer.class, 1L ) );
				}
		);
	}

	@Test
	public void testSequenceGenerator(SessionFactoryScope scope) {
		Shoe b = new Shoe();
		scope.inTransaction(
				session ->
						session.persist( b )
		);

		assertNotNull( b.getId() );

		scope.inTransaction(
				session ->
						session.remove( session.get( Shoe.class, b.getId() ) )
		);
	}

	@Test
	public void testClassLevelGenerator(SessionFactoryScope scope) {
		Store b = new Store();
		scope.inTransaction(
				session ->
						session.persist( b )
		);

		assertNotNull( b.getId() );

		scope.inTransaction(
				session ->
						session.remove( session.get( Store.class, b.getId() ) )

		);
	}

	@Test
	public void testMethodLevelGenerator(SessionFactoryScope scope) {
		Department b = new Department();
		scope.inTransaction(
				session ->
						session.persist( b )
		);

		assertNotNull( b.getId() );

		scope.inTransaction(
				session ->
						session.remove( session.get( Department.class, b.getId() ) )
		);
	}

	@Test
	public void testDefaultSequence(SessionFactoryScope scope) {
		Home h = new Home();
		scope.inTransaction(
				session ->
						session.persist( h )
		);

		assertNotNull( h.getId() );

		scope.inTransaction(
				session -> {
					Home reloadedHome = session.get( Home.class, h.getId() );
					assertEquals( h.getId(), reloadedHome.getId() );
					session.remove( reloadedHome );
				}
		);
	}

	@Test
	public void testParameterizedAuto(SessionFactoryScope scope) {
		Home h = new Home();
		scope.inTransaction(
				session ->
						session.persist( h )
		);

		assertNotNull( h.getId() );

		scope.inTransaction(
				session -> {
					Home reloadedHome = session.get( Home.class, h.getId() );
					assertEquals( h.getId(), reloadedHome.getId() );
					session.remove( reloadedHome );
				}
		);
	}

	@Test
	public void testIdInEmbeddableSuperclass(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					FirTree christmasTree = new FirTree();
					session.persist( christmasTree );
					session.getTransaction().commit();
					session.clear();
					session.beginTransaction();
					christmasTree = session.get( FirTree.class, christmasTree.getId() );
					assertNotNull( christmasTree );
					session.remove( christmasTree );
				}
		);
	}

	@Test
	public void testIdClass(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Footballer fb = new Footballer( "David", "Beckam", "Arsenal" );
					GoalKeeper keeper = new GoalKeeper( "Fabien", "Bartez", "OM" );
					session.persist( fb );
					session.persist( keeper );
					session.getTransaction().commit();
					session.clear();

					// lookup by id
					session.beginTransaction();
					FootballerPk fpk = new FootballerPk( "David", "Beckam" );
					fb = session.get( Footballer.class, fpk );
					FootballerPk fpk2 = new FootballerPk( "Fabien", "Bartez" );
					keeper = session.get( GoalKeeper.class, fpk2 );
					assertNotNull( fb );
					assertNotNull( keeper );
					assertEquals( "Beckam", fb.getLastname() );
					assertEquals( "Arsenal", fb.getClub() );
					assertEquals( 1, session.createQuery(
							"from Footballer f where f.firstname = 'David'" ).list().size() );
					session.getTransaction().commit();

					// reattach by merge
					session.beginTransaction();
					fb.setClub( "Bimbo FC" );
					session.merge( fb );
					session.getTransaction().commit();

					// reattach by saveOrUpdate
					session.beginTransaction();
					fb.setClub( "Bimbo FC SA" );
					session.merge( fb );
					session.getTransaction().commit();

					// clean up
					session.clear();
					session.beginTransaction();
					fpk = new FootballerPk( "David", "Beckam" );
					fb = session.get( Footballer.class, fpk );
					assertEquals( "Bimbo FC SA", fb.getClub() );
					session.remove( fb );
					session.remove( keeper );
				}
		);
	}

	@Test
	public void testColumnDefinition(SessionFactoryScope scope) {
		Column idCol = (Column) scope.getMetadataImplementor().getEntityBinding( Ball.class.getName() )
				.getIdentifierProperty()
				.getValue()
				.getSelectables()
				.get( 0 );
		assertEquals( "ball_id", idCol.getName() );
	}

	@Test
	public void testLowAllocationSize(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					int size = 4;
					BreakDance[] bds = new BreakDance[size];
					for ( int i = 0; i < size; i++ ) {
						bds[i] = new BreakDance();
						session.persist( bds[i] );
					}
					session.flush();
					for ( int i = 0; i < size; i++ ) {
						assertEquals( i + 1, bds[i].id.intValue() );
					}
				}
		);
		scope.inTransaction(
				session ->
						session.createQuery( "delete from BreakDance" ).executeUpdate()
		);

	}


}
