/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.id.sequences;

import org.hibernate.mapping.Column;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.orm.test.annotations.id.generationmappings.sub.DedicatedSequenceEntity1;
import org.hibernate.orm.test.annotations.id.generationmappings.sub.DedicatedSequenceEntity2;
import org.hibernate.orm.test.annotations.id.sequences.entities.Ball;
import org.hibernate.orm.test.annotations.id.sequences.entities.BreakDance;
import org.hibernate.orm.test.annotations.id.sequences.entities.Computer;
import org.hibernate.orm.test.annotations.id.sequences.entities.Department;
import org.hibernate.orm.test.annotations.id.sequences.entities.Dog;
import org.hibernate.orm.test.annotations.id.sequences.entities.FirTree;
import org.hibernate.orm.test.annotations.id.sequences.entities.Footballer;
import org.hibernate.orm.test.annotations.id.sequences.entities.FootballerPk;
import org.hibernate.orm.test.annotations.id.sequences.entities.Furniture;
import org.hibernate.orm.test.annotations.id.sequences.entities.GoalKeeper;
import org.hibernate.orm.test.annotations.id.sequences.entities.Home;
import org.hibernate.orm.test.annotations.id.sequences.entities.Monkey;
import org.hibernate.orm.test.annotations.id.sequences.entities.Phone;
import org.hibernate.orm.test.annotations.id.sequences.entities.Shoe;
import org.hibernate.orm.test.annotations.id.sequences.entities.SoundSystem;
import org.hibernate.orm.test.annotations.id.sequences.entities.Store;
import org.hibernate.orm.test.annotations.id.sequences.entities.Tree;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Emmanuel Bernard
 * @author Lukasz Antoniak
 */
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSequences.class)
@DomainModel(
		annotatedClasses = {
				Ball.class, Shoe.class, Store.class,
				Department.class, Dog.class, Computer.class, Home.class,
				Phone.class, Tree.class, FirTree.class, Footballer.class,
				SoundSystem.class, Furniture.class, GoalKeeper.class,
				BreakDance.class, Monkey.class, DedicatedSequenceEntity1.class,
				DedicatedSequenceEntity2.class
		},
		annotatedPackageNames = {
				"org.hibernate.orm.test.annotations",
				"org.hibernate.orm.test.annotations.id",
				"org.hibernate.orm.test.annotations.id.generationmappings"
		},
		xmlMappings = "org/hibernate/orm/test/annotations/orm.xml"

)
@SessionFactory
public class IdTest {

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
					SoundSystem systemfromDb = session.get( SoundSystem.class, system.getId() );
					Furniture furFromDb = session.get( Furniture.class, fur.getId() );
					assertNotNull( systemfromDb );
					assertNotNull( furFromDb );
					session.remove( systemfromDb );
					session.remove( furFromDb );
				}
		);
	}

	@Test
	public void testGenericGenerators(SessionFactoryScope scope) {
		// Ensures that GenericGenerator annotations wrapped inside a GenericGenerators holder are bound correctly
		scope.inTransaction(
				session -> {
					Monkey monkey = new Monkey();
					session.persist( monkey );
					session.flush();
					assertNotNull( monkey.getId() );
				}
		);

		scope.inTransaction(
				session ->
						session.createQuery( "delete from Monkey" ).executeUpdate()
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
					assertEquals(
							1,
							session.createQuery( "from Footballer f where f.firstname = 'David'" )
									.list().size()
					);
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
	@JiraKey(value = "HHH-6790")
	public void testSequencePerEntity(SessionFactoryScope scope) {
		DedicatedSequenceEntity1 entity1 = new DedicatedSequenceEntity1();
		DedicatedSequenceEntity2 entity2 = new DedicatedSequenceEntity2();
		scope.inTransaction(
				session -> {
					session.persist( entity1 );
					session.persist( entity2 );
				}
		);

		assertEquals( 1, entity1.getId().intValue() );
		assertEquals( 1, entity2.getId().intValue() );

		scope.inTransaction(
				session -> {
					session.createQuery( "delete from DedicatedSequenceEntity1" ).executeUpdate();
					session.createQuery( "delete from " + DedicatedSequenceEntity2.ENTITY_NAME ).executeUpdate();
				}
		);
	}

	@Test
	public void testColumnDefinition(SessionFactoryScope scope) {
		Column idCol = (Column) scope.getMetadataImplementor().getEntityBinding( Ball.class.getName() )
				.getIdentifierProperty().getValue().getSelectables().get( 0 );
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
