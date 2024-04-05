/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy;

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

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gail Badner
 */
@JiraKey( "HHH-13640" )
@DomainModel(
		annotatedClasses = {
				LazyToOnesNoProxyFactoryWithSubclassesStatefulTest.Animal.class,
				LazyToOnesNoProxyFactoryWithSubclassesStatefulTest.Primate.class,
				LazyToOnesNoProxyFactoryWithSubclassesStatefulTest.Human.class,
				LazyToOnesNoProxyFactoryWithSubclassesStatefulTest.OtherEntity.class
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
public class LazyToOnesNoProxyFactoryWithSubclassesStatefulTest {

	@Test
	public void testNewEnhancedProxyAssociation(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Human human = new Human( "A Human" );
					OtherEntity otherEntity = new OtherEntity( "test1" );
					otherEntity.human = human;

					session.persist( human );
					session.persist( otherEntity );
				}
		);

		scope.inSession(
				session -> {
					final Statistics stats = scope.getSessionFactory().getStatistics();
					stats.clear();
					final OtherEntity otherEntity = session.get( OtherEntity.class, "test1" );
					assertTrue( Hibernate.isPropertyInitialized( otherEntity, "human" ) );
					assertFalse( Hibernate.isInitialized( otherEntity.human ) );
					assertFalse( HibernateProxy.class.isInstance( otherEntity.animal ) );
					assertEquals( 1, stats.getPrepareStatementCount() );
				}
		);
	}

	@Test
	public void testExistingInitializedAssociationLeafSubclass(SessionFactoryScope scope) {
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

					/*
						The original test used
						@Tuplizer(impl=NoProxyFactoryPojoEntityTuplizer.class)
						public static abstract class Animal {
						}
						this is not supported anymore, so the test expectation changed
					 */

//					assertTrue( Hibernate.isInitialized( otherEntity.animal ) );
//					assertFalse( HibernateProxy.class.isInstance( otherEntity.animal ) );
					assertFalse( Hibernate.isInitialized( otherEntity.animal ) );
					assertTrue( HibernateProxy.class.isInstance( otherEntity.animal ) );

					assertTrue( Hibernate.isPropertyInitialized( otherEntity, "primate" ) );

//					assertTrue( Hibernate.isInitialized( otherEntity.primate ) );
//					assertFalse( HibernateProxy.class.isInstance( otherEntity.primate ) );
					assertFalse( Hibernate.isInitialized( otherEntity.primate ) );
					assertTrue( HibernateProxy.class.isInstance( otherEntity.primate ) );

					assertTrue( Hibernate.isPropertyInitialized( otherEntity, "human" ) );

//					assertTrue( Hibernate.isInitialized( otherEntity.human ) );
//					assertFalse( HibernateProxy.class.isInstance( otherEntity.human ) );
					assertFalse( Hibernate.isInitialized( otherEntity.human ) );
					assertTrue( HibernateProxy.class.isInstance( otherEntity.human ) );

//					assertSame( otherEntity.human, otherEntity.animal );
//					assertSame( otherEntity.human, otherEntity.primate );

//					assertEquals( 2, stats.getPrepareStatementCount() );
					assertEquals( 1, stats.getPrepareStatementCount() );
				}
		);

//		assertEquals( 2, stats.getPrepareStatementCount() );
		assertEquals( 1, stats.getPrepareStatementCount() );
	}

	@Test
	public void testExistingEnhancedProxyAssociationLeafSubclassOnly(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Human human = new Human( "A Human" );
					OtherEntity otherEntity = new OtherEntity( "test1" );
					otherEntity.human = human;
					otherEntity.otherHuman = human;
					session.persist( human );
					session.persist( otherEntity );
				}
		);

		scope.inSession(
				session -> {
					final Statistics stats = scope.getSessionFactory().getStatistics();
					stats.clear();
					final OtherEntity otherEntity = session.get( OtherEntity.class, "test1" );
					assertNull( otherEntity.animal );
					assertNull( otherEntity.primate );
					assertTrue( Hibernate.isPropertyInitialized( otherEntity, "human" ) );
					assertFalse( Hibernate.isInitialized( otherEntity.human ) );
					assertFalse( HibernateProxy.class.isInstance( otherEntity.human ) );
					assertTrue( Hibernate.isPropertyInitialized( otherEntity, "otherHuman" ) );
					assertFalse( Hibernate.isInitialized( otherEntity.otherHuman ) );
					assertFalse( HibernateProxy.class.isInstance( otherEntity.otherHuman ) );
					assertSame( otherEntity.human, otherEntity.otherHuman );
					assertEquals( 1, stats.getPrepareStatementCount() );
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

		@ManyToOne(fetch = FetchType.LAZY)
		private Human otherHuman = null;

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
