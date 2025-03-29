/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gail Badner
 */
@JiraKey( "HHH-13640" )
@DomainModel(
		annotatedClasses = {
				LazyToOnesProxyWithSubclassesTest.Animal.class,
				LazyToOnesProxyWithSubclassesTest.Primate.class,
				LazyToOnesProxyWithSubclassesTest.Human.class,
				LazyToOnesProxyWithSubclassesTest.OtherEntity.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.GENERATE_STATISTICS, value = "true" ),
				@Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "false" ),
				@Setting( name = AvailableSettings.USE_QUERY_CACHE, value = "false" ),
		}
)
@SessionFactory
@BytecodeEnhanced
public class LazyToOnesProxyWithSubclassesTest {

	@Test
	public void testNewProxyAssociation(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Human human = new Human( "A Human" );
					OtherEntity otherEntity = new OtherEntity( "test1" );
					otherEntity.animal = human;
					session.persist( human );
					session.persist( otherEntity );
				}
		);

		scope.inSession(
				session -> {
					final Statistics stats = scope.getSessionFactory().getStatistics();
					stats.clear();
					final OtherEntity otherEntity = session.get( OtherEntity.class, "test1" );
					assertTrue( Hibernate.isPropertyInitialized( otherEntity, "animal" ) );
					assertFalse( Hibernate.isInitialized( otherEntity.animal ) );
					assertTrue( HibernateProxy.class.isInstance( otherEntity.animal ) );

					assertEquals( 1, stats.getPrepareStatementCount() );

					Animal animal = session.getReference( Animal.class, "A Human" );
					assertFalse( Hibernate.isInitialized( animal ) );
					assertEquals( 1, stats.getPrepareStatementCount() );
				}
		);
	}

	@Test
	public void testGetInitializeAssociations(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Human human = new Human( "A Human" );
					OtherEntity otherEntity = new OtherEntity( "test1" );
					otherEntity.animal = human;

					session.persist( human );
					session.persist( otherEntity );
				}
		);

		scope.inSession(
				session -> {
					final Statistics stats = scope.getSessionFactory().getStatistics();
					stats.clear();
					final OtherEntity otherEntity = session.get( OtherEntity.class, "test1" );
					assertTrue( Hibernate.isPropertyInitialized( otherEntity, "animal" ) );
					assertFalse( Hibernate.isInitialized( otherEntity.animal ) );
					assertEquals( 1, stats.getPrepareStatementCount() );

					Animal animal = session.get( Animal.class, "A Human" );
					assertTrue( Hibernate.isInitialized( animal ) );
					assertEquals( 2, stats.getPrepareStatementCount() );
				}
		);

	}

	@Test
	public void testExistingProxyAssociation(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Human human = new Human( "A Human" );
					OtherEntity otherEntity = new OtherEntity( "test1" );
					otherEntity.animal = human;
					otherEntity.primate = human;
					session.persist( human );
					session.persist( otherEntity );
				}
		);

		scope.inSession(
				session -> {
					final Statistics stats = scope.getSessionFactory().getStatistics();
					stats.clear();
					final OtherEntity otherEntity = session.get( OtherEntity.class, "test1" );
					assertTrue( Hibernate.isPropertyInitialized( otherEntity, "animal" ) );
					assertFalse( Hibernate.isInitialized( otherEntity.animal ) );
					assertTrue( HibernateProxy.class.isInstance( otherEntity.animal ) );
					assertTrue( Hibernate.isPropertyInitialized( otherEntity, "primate" ) );
					assertFalse( Hibernate.isInitialized( otherEntity.primate ) );
					assertTrue( HibernateProxy.class.isInstance( otherEntity.primate ) );
					assertEquals( 1, stats.getPrepareStatementCount() );
				}
		);
	}

	@Test
	public void testExistingHibernateProxyAssociationLeafSubclass(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Human human = new Human( "A Human" );
					OtherEntity otherEntity = new OtherEntity( "test1" );
					otherEntity.animal = human;
					otherEntity.primate = human;
					otherEntity.human = human;
					session.persist( human );
					session.persist( otherEntity );
				}
		);

		final Statistics stats = scope.getSessionFactory().getStatistics();
		stats.clear();

		scope.inSession(
				session -> {

					final OtherEntity otherEntity = session.get( OtherEntity.class, "test1" );
					assertTrue( Hibernate.isPropertyInitialized( otherEntity, "animal" ) );
					assertFalse( Hibernate.isInitialized( otherEntity.animal ) );
					assertTrue( HibernateProxy.class.isInstance( otherEntity.animal ) );
					assertTrue( Hibernate.isPropertyInitialized( otherEntity, "primate" ) );
					assertFalse( Hibernate.isInitialized( otherEntity.primate ) );
					assertTrue( HibernateProxy.class.isInstance( otherEntity.primate ) );
					assertTrue( Hibernate.isPropertyInitialized( otherEntity, "human" ) );
					assertFalse( Hibernate.isInitialized( otherEntity.human ) );

					assertEquals( 1, stats.getPrepareStatementCount() );

					// Make sure human can still get loaded and not initialized.
					final Human human = session.getReference( Human.class, "A Human" );
					assertTrue( HibernateProxy.class.isInstance( otherEntity.human ) );
					assertFalse( Hibernate.isInitialized( human ) );

					human.getName();
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertFalse( Hibernate.isInitialized( human ) );
					assertTrue( HibernateProxy.class.isInstance( otherEntity.human ) );

					human.getSex();

					assertTrue( Hibernate.isInitialized( human ) );
					assertTrue( HibernateProxy.class.isInstance( otherEntity.human ) );
					assertEquals( 2, stats.getPrepareStatementCount() );
				}
		);

		assertEquals( 2, stats.getPrepareStatementCount() );
		stats.clear();

		scope.inSession(
				session -> {

					final OtherEntity otherEntity = session.get( OtherEntity.class, "test1" );
					assertTrue( Hibernate.isPropertyInitialized( otherEntity, "animal" ) );
					assertFalse( Hibernate.isInitialized( otherEntity.animal ) );
					assertTrue( HibernateProxy.class.isInstance( otherEntity.animal ) );
					assertTrue( Hibernate.isPropertyInitialized( otherEntity, "primate" ) );
					assertFalse( Hibernate.isInitialized( otherEntity.primate ) );
					assertTrue( HibernateProxy.class.isInstance( otherEntity.primate ) );
					assertTrue( Hibernate.isPropertyInitialized( otherEntity, "human" ) );
					assertFalse( Hibernate.isInitialized( otherEntity.human ) );

					assertEquals( 1, stats.getPrepareStatementCount() );

					// Make sure human can still get loaded
					final Human human = session.get( Human.class, "A Human" );
					assertTrue( !HibernateProxy.class.isInstance( human ) );
					assertTrue( Hibernate.isInitialized( human ) );
					assertTrue( HibernateProxy.class.isInstance( otherEntity.getHuman() ) );

					assertEquals( 2, stats.getPrepareStatementCount() );
				}
		);

		assertEquals( 2, stats.getPrepareStatementCount() );

	}

	@Test
	public void testExistingEnhancedProxyAssociationLeafSubclassOnly(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Human human = new Human( "A Human" );
					OtherEntity otherEntity = new OtherEntity( "test1" );
					otherEntity.human = human;
					session.persist( human );
					session.persist( otherEntity );
				}
		);

		scope.fromTransaction( session -> {
					final Statistics stats = scope.getSessionFactory().getStatistics();
					stats.clear();

					final OtherEntity otherEntity = session.get( OtherEntity.class, "test1" );
					assertNull( otherEntity.animal );
					assertNull( otherEntity.primate );
					assertTrue( Hibernate.isPropertyInitialized( otherEntity, "human" ) );
					assertFalse( Hibernate.isInitialized( otherEntity.human ) );
					assertFalse( HibernateProxy.class.isInstance( otherEntity.human ) );
					assertEquals( 1, stats.getPrepareStatementCount() );

					// Make sure human can still get loaded and not initialized.
					final Human human = session.getReference( Human.class, "A Human" );
					assertFalse( Hibernate.isInitialized( human ) );
					assertFalse( HibernateProxy.class.isInstance( otherEntity.human ) );

					human.getName();
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertFalse( HibernateProxy.class.isInstance( otherEntity.human ) );

					human.getSex();
					assertTrue( Hibernate.isInitialized( otherEntity.human ) );
					assertFalse( HibernateProxy.class.isInstance( otherEntity.human ) );
					assertEquals( 2, stats.getPrepareStatementCount() );

					return otherEntity;
				}
		);
	}

	@AfterEach
	public void cleanUpTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "delete from OtherEntity" ).executeUpdate();
					session.createQuery( "delete from Human" ).executeUpdate();
					session.createQuery( "delete from Primate" ).executeUpdate();
					session.createQuery( "delete from Animal" ).executeUpdate();
				}
		);
	}

	@Entity(name = "Animal")
	@Table(name = "Animal")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	public static abstract class Animal {

		@Id
		private String name;

		private int age;

		public String getName() {
			return name;
		}

		protected void setName(String name) {
			this.name = name;
		}

		public int getAge() {
			return age;
		}

		public void setAge(int age) {
			this.age = age;
		}
	}

	@Entity(name = "Primate")
	@Table(name = "Primate")
	public static class Primate extends Animal {

		public Primate(String name) {
			this();
			setName( name );
		}

		protected Primate() {
			// this form used by Hibernate
		}
	}

	@Entity(name = "Human")
	@Table(name = "Human")
	public static class Human extends Primate {

		private String sex;

		public Human(String name) {
			this();
			setName( name );
		}

		protected Human() {
			// this form used by Hibernate
		}

		public String getSex() {
			return sex;
		}

		public void setSex(String sex) {
			this.sex = sex;
		}
	}

	@Entity(name = "OtherEntity")
	@Table(name = "OtherEntity")
	public static class OtherEntity {

		@Id
		private String id;

		@ManyToOne(fetch = FetchType.LAZY)
		private Animal animal = null;

		@ManyToOne(fetch = FetchType.LAZY)
		private Primate primate = null;

		@ManyToOne(fetch = FetchType.LAZY)
		private Human human = null;

		protected OtherEntity() {
			// this form used by Hibernate
		}

		public OtherEntity(String id) {
			this.id = id;
		}

		public String getId() {
			return id;
		}

		public Human getHuman() {
			return human;
		}

		public void setHuman(Human human) {
			this.human = human;
		}
	}
}
