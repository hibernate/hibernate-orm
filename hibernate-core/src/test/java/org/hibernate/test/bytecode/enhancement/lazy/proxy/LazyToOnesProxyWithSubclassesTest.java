/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.bytecode.enhancement.lazy.proxy;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */
@TestForIssue( jiraKey = "HHH-13640" )
@RunWith(BytecodeEnhancerRunner.class)
public class LazyToOnesProxyWithSubclassesTest extends BaseNonConfigCoreFunctionalTestCase {
	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		super.configureStandardServiceRegistryBuilder( ssrb );
		ssrb.applySetting( AvailableSettings.ALLOW_ENHANCEMENT_AS_PROXY, "true" );
	}

	@Override
	protected void configureSessionFactoryBuilder(SessionFactoryBuilder sfb) {
		super.configureSessionFactoryBuilder( sfb );
		sfb.applyStatisticsSupport( true );
		sfb.applySecondLevelCacheSupport( false );
		sfb.applyQueryCacheSupport( false );
	}

	@Override
	protected void applyMetadataSources(MetadataSources sources) {
		super.applyMetadataSources( sources );
		sources.addAnnotatedClass( Animal.class );
		sources.addAnnotatedClass( Primate.class );
		sources.addAnnotatedClass( Human.class );
		sources.addAnnotatedClass( OtherEntity.class );
	}

	@Test
	public void testNewProxyAssociation() {
		inTransaction(
				session -> {
					Human human = new Human( "A Human" );
					OtherEntity otherEntity = new OtherEntity( "test1" );
					otherEntity.animal = human;
					session.persist( human );
					session.persist( otherEntity );
				}
		);

		inSession(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
					stats.clear();
					final OtherEntity otherEntity = session.get( OtherEntity.class, "test1" );
					assertTrue( Hibernate.isPropertyInitialized( otherEntity, "animal" ) );
					assertFalse( Hibernate.isInitialized( otherEntity.animal ) );
					assertTrue( HibernateProxy.class.isInstance( otherEntity.animal ) );

					assertEquals( 1, stats.getPrepareStatementCount() );

					Animal animal = session.load( Animal.class, "A Human" );
					assertFalse( Hibernate.isInitialized( animal ) );
					assertEquals( 1, stats.getPrepareStatementCount() );
				}
		);
	}

	@Test
	public void testGetInitializeAssociations() {
		inTransaction(
				session -> {
					Human human = new Human( "A Human" );
					OtherEntity otherEntity = new OtherEntity( "test1" );
					otherEntity.animal = human;

					session.persist( human );
					session.persist( otherEntity );
				}
		);

		inSession(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
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
	public void testExistingProxyAssociation() {
		inTransaction(
				session -> {
					Human human = new Human( "A Human" );
					OtherEntity otherEntity = new OtherEntity( "test1" );
					otherEntity.animal = human;
					otherEntity.primate = human;
					session.persist( human );
					session.persist( otherEntity );
				}
		);

		inSession(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
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
	public void testExistingHibernateProxyAssociationLeafSubclass() {
		inTransaction(
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

		final Statistics stats = sessionFactory().getStatistics();
		stats.clear();

		inSession(
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

		inSession(
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
	public void testExistingEnhancedProxyAssociationLeafSubclassOnly() {
		inTransaction(
				session -> {
					Human human = new Human( "A Human" );
					OtherEntity otherEntity = new OtherEntity( "test1" );
					otherEntity.human = human;
					session.persist( human );
					session.persist( otherEntity );
				}
		);

		doInHibernate(
				this::sessionFactory, session -> {
					final Statistics stats = sessionFactory().getStatistics();
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

	@After
	public void cleanUpTestData() {
		inTransaction(
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
		@LazyToOne(LazyToOneOption.NO_PROXY)
		private Animal animal = null;

		@ManyToOne(fetch = FetchType.LAZY)
		@LazyToOne(LazyToOneOption.NO_PROXY)
		private Primate primate = null;

		@ManyToOne(fetch = FetchType.LAZY)
		@LazyToOne(LazyToOneOption.NO_PROXY)
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