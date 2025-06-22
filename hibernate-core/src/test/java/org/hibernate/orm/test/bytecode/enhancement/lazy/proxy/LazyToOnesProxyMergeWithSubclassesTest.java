/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy;

import jakarta.persistence.CascadeType;
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

import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gail Badner
 */
@JiraKey( "HHH-13640" )
@DomainModel(
		annotatedClasses = {
				LazyToOnesProxyMergeWithSubclassesTest.Animal.class,
				LazyToOnesProxyMergeWithSubclassesTest.Primate.class,
				LazyToOnesProxyMergeWithSubclassesTest.Human.class,
				LazyToOnesProxyMergeWithSubclassesTest.OtherEntity.class
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
@EnhancementOptions(lazyLoading = true)
public class LazyToOnesProxyMergeWithSubclassesTest {

	@Test
	public void mergeUpdatedHibernateProxy(SessionFactoryScope scope) {

		checkAgeInNewSession( scope, 1 );

		final Statistics stats = scope.getSessionFactory().getStatistics();
		stats.clear();

		final OtherEntity withInitializedHibernateProxy = scope.fromTransaction(
				session -> {
					final OtherEntity otherEntity = session.get( OtherEntity.class, "test1" );
					assertEquals( 1, stats.getPrepareStatementCount() );

					assertTrue( Hibernate.isPropertyInitialized( otherEntity, "human" ) );
					assertFalse( Hibernate.isInitialized( otherEntity.getHuman() ) );
					assertTrue( HibernateProxy.class.isInstance( otherEntity.getHuman() ) );

					otherEntity.getHuman().getSex();
					assertTrue( Hibernate.isInitialized( otherEntity.getHuman() ) );
					assertEquals( 2, stats.getPrepareStatementCount() );

					return otherEntity;
				}
		);

		assertEquals( 2, stats.getPrepareStatementCount() );
		stats.clear();

		checkAgeInNewSession( scope, 1 );
		stats.clear();

		// merge updated HibernateProxy to updated HibernateProxy

		withInitializedHibernateProxy.getHuman().setAge( 2 );

		scope.inTransaction(
				session -> {
					final OtherEntity otherEntity = session.get( OtherEntity.class, "test1" );
					assertEquals( 1, stats.getPrepareStatementCount() );

					assertTrue( Hibernate.isPropertyInitialized( otherEntity, "human" ) );
					assertFalse( Hibernate.isInitialized( otherEntity.getHuman() ) );
					assertTrue( HibernateProxy.class.isInstance( otherEntity.getHuman() ) );

					otherEntity.getHuman().setAge( 3 );
					assertTrue( Hibernate.isInitialized( otherEntity.getHuman() ) );
					assertEquals( 2, stats.getPrepareStatementCount() );

					final Human humanImpl = (Human) ( (HibernateProxy) otherEntity.getHuman() )
							.getHibernateLazyInitializer()
							.getImplementation();

					session.merge( withInitializedHibernateProxy );
					// TODO: Reference to associated HibernateProxy is changed
					//       to the HibernateProxy's implementation.
					assertSame( humanImpl, otherEntity.getHuman() );
					assertEquals( 2, otherEntity.getHuman().getAge() );
				}
		);

		assertEquals( 3, stats.getPrepareStatementCount() );
		stats.clear();

		checkAgeInNewSession( scope, 2 );
		stats.clear();

		// merge updated HibernateProxy to updated enhanced entity

		withInitializedHibernateProxy.getHuman().setAge( 4 );

		scope.inTransaction(
				session -> {
					final Human human = session.getReference( Human.class, "A Human" );
					assertFalse( Hibernate.isInitialized( human ) );
					assertFalse( HibernateProxy.class.isInstance( human ) );

					assertEquals( 0, stats.getPrepareStatementCount() );

					final OtherEntity otherEntity = session.get( OtherEntity.class, "test1" );
					assertTrue( Hibernate.isPropertyInitialized( otherEntity, "human" ) );
					assertFalse( Hibernate.isInitialized( otherEntity.getHuman() ) );
					assertSame( human, otherEntity.getHuman() );

					assertEquals( 1, stats.getPrepareStatementCount() );

					human.setAge( 5 );
					assertTrue( Hibernate.isInitialized( otherEntity.getHuman() ) );
					assertEquals( 2, stats.getPrepareStatementCount() );

					session.merge( withInitializedHibernateProxy );
					assertTrue( Hibernate.isInitialized( otherEntity.getHuman() ) );
					assertSame( human, otherEntity.getHuman() );
					assertEquals( 4, human.getAge() );
				}
		);

		assertEquals( 3, stats.getPrepareStatementCount() );
		stats.clear();

		checkAgeInNewSession( scope, 4 );
		stats.clear();

		// merge updated HibernateProxy to uninitialized HibernateProxy

		withInitializedHibernateProxy.getHuman().setAge( 6 );

		scope.inTransaction(
				session -> {
					final OtherEntity otherEntity = session.get( OtherEntity.class, "test1" );
					assertEquals( 1, stats.getPrepareStatementCount() );

					assertTrue( Hibernate.isPropertyInitialized( otherEntity, "human" ) );
					assertFalse( Hibernate.isInitialized( otherEntity.getHuman() ) );
					assertTrue( HibernateProxy.class.isInstance( otherEntity.getHuman() ) );

					final Human humanHibernateProxy = otherEntity.getHuman();

					session.merge( withInitializedHibernateProxy );

					assertTrue( Hibernate.isInitialized( otherEntity.getHuman() ) );
					// TODO: Reference to associated HibernateProxy is changed
					//       reference to the HibernateProxy's implementation.
					assertFalse( HibernateProxy.class.isInstance( otherEntity.getHuman() ) );
					assertSame(
							otherEntity.getHuman(),
							( (HibernateProxy) humanHibernateProxy ).getHibernateLazyInitializer().getImplementation()
					);

					assertEquals( 6, otherEntity.getHuman().getAge() );
				}
		);

		assertEquals( 3, stats.getPrepareStatementCount() );
		stats.clear();

		checkAgeInNewSession( scope, 6 );
		stats.clear();

		// merge updated HibernateProxy To uninitialized enhanced proxy

		withInitializedHibernateProxy.getHuman().setAge( 7 );

		scope.inTransaction(
				session -> {
					final Human human = session.getReference( Human.class, "A Human" );
					assertFalse( Hibernate.isInitialized( human ) );
					assertFalse( HibernateProxy.class.isInstance( human ) );
					assertEquals( 0, stats.getPrepareStatementCount() );

					final OtherEntity otherEntity = session.get( OtherEntity.class, "test1" );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertSame( human, otherEntity.getHuman() );
					assertFalse( Hibernate.isInitialized( human ) );

					session.merge( withInitializedHibernateProxy );
					assertSame( human, otherEntity.getHuman() );
					assertTrue( Hibernate.isInitialized( human ) );

					assertEquals( 7, otherEntity.getHuman().getAge() );
				}
		);

		assertEquals( 3, stats.getPrepareStatementCount() );
		stats.clear();

		checkAgeInNewSession( scope, 7 );
	}

	@Test
	public void mergeUpdatedEnhancedProxy(SessionFactoryScope scope) {

		checkAgeInNewSession( scope, 1 );

		final Statistics stats = scope.getSessionFactory().getStatistics();
		stats.clear();

		final OtherEntity withInitializedEnhancedProxy = scope.fromTransaction(
				session -> {

					final Human human = session.getReference( Human.class, "A Human" );
					assertFalse( Hibernate.isInitialized( human ) );
					assertFalse( HibernateProxy.class.isInstance( human ) );

					final OtherEntity otherEntity = session.get( OtherEntity.class, "test1" );
					assertEquals( 1, stats.getPrepareStatementCount() );

					assertTrue( Hibernate.isPropertyInitialized( otherEntity, "human" ) );
					assertSame( human, otherEntity.getHuman() );
					assertFalse( Hibernate.isInitialized( otherEntity.getHuman() ) );
					assertFalse( HibernateProxy.class.isInstance( otherEntity.getHuman() ) );

					otherEntity.getHuman().getSex();
					assertTrue( Hibernate.isInitialized( otherEntity.getHuman() ) );
					assertEquals( 2, stats.getPrepareStatementCount() );

					return otherEntity;
				}
		);

		assertEquals( 2, stats.getPrepareStatementCount() );
		stats.clear();

		checkAgeInNewSession( scope, 1 );
		stats.clear();

		// merge updated enhanced proxy to updated HibernateProxy

		withInitializedEnhancedProxy.getHuman().setAge( 2 );

		scope.inTransaction(
				session -> {
					final OtherEntity otherEntity = session.get( OtherEntity.class, "test1" );
					assertEquals( 1, stats.getPrepareStatementCount() );

					assertTrue( Hibernate.isPropertyInitialized( otherEntity, "human" ) );
					assertFalse( Hibernate.isInitialized( otherEntity.getHuman() ) );
					assertTrue( HibernateProxy.class.isInstance( otherEntity.getHuman() ) );

					otherEntity.getHuman().setAge( 3 );
					assertTrue( Hibernate.isInitialized( otherEntity.getHuman() ) );
					assertEquals( 2, stats.getPrepareStatementCount() );

					final Human humanImpl = (Human) ( (HibernateProxy) otherEntity.getHuman() )
							.getHibernateLazyInitializer()
							.getImplementation();

					session.merge( withInitializedEnhancedProxy );
					// TODO: Reference to HibernateProxy is changed
					//       to the HibernateProxy's implementation.
					assertSame( humanImpl, otherEntity.getHuman() );

					assertEquals( 2, otherEntity.getHuman().getAge() );
				}
		);

		assertEquals( 3, stats.getPrepareStatementCount() );
		stats.clear();

		checkAgeInNewSession( scope, 2 );
		stats.clear();

		// merge updated enhanced proxy to updated enhanced proxy

		withInitializedEnhancedProxy.getHuman().setAge( 4 );

		scope.inTransaction(
				session -> {
					final Human human = session.getReference( Human.class, "A Human" );
					assertFalse( Hibernate.isInitialized( human ) );
					assertFalse( HibernateProxy.class.isInstance( human ) );

					assertEquals( 0, stats.getPrepareStatementCount() );

					final OtherEntity otherEntity = session.get( OtherEntity.class, "test1" );
					assertTrue( Hibernate.isPropertyInitialized( otherEntity, "human" ) );
					assertFalse( Hibernate.isInitialized( otherEntity.getHuman() ) );
					assertSame( human, otherEntity.getHuman() );

					assertEquals( 1, stats.getPrepareStatementCount() );

					human.setAge( 5 );
					assertTrue( Hibernate.isInitialized( otherEntity.getHuman() ) );
					assertEquals( 2, stats.getPrepareStatementCount() );

					session.merge( withInitializedEnhancedProxy );
					assertEquals( 4, human.getAge() );
				}
		);

		assertEquals( 3, stats.getPrepareStatementCount() );
		stats.clear();

		checkAgeInNewSession( scope, 4 );
		stats.clear();

		// merge updated enhanced proxy to uninitialized HibernateProxy

		withInitializedEnhancedProxy.getHuman().setAge( 6 );

		scope.inTransaction(
				session -> {
					final OtherEntity otherEntity = session.get( OtherEntity.class, "test1" );
					assertEquals( 1, stats.getPrepareStatementCount() );

					assertTrue( Hibernate.isPropertyInitialized( otherEntity, "human" ) );
					assertFalse( Hibernate.isInitialized( otherEntity.getHuman() ) );
					assertTrue( HibernateProxy.class.isInstance( otherEntity.getHuman() ) );

					session.merge( withInitializedEnhancedProxy );

					assertTrue( Hibernate.isInitialized( otherEntity.getHuman() ) );
					// TODO: Reference in managed entity gets changed from a HibernateProxy
					// to an initialized entity. This happens without enhancement as well.
					//assertTrue( HibernateProxy.class.isInstance( otherEntity.getHuman() ) );
					assertEquals( 6, otherEntity.getHuman().getAge() );
				}
		);

		assertEquals( 3, stats.getPrepareStatementCount() );
		stats.clear();

		checkAgeInNewSession( scope, 6 );
		stats.clear();

		// merge updated enhanced proxy to uninitialized enhanced proxy

		withInitializedEnhancedProxy.getHuman().setAge( 7 );

		scope.inTransaction(
				session -> {
					final Human human = session.getReference( Human.class, "A Human" );
					assertFalse( Hibernate.isInitialized( human ) );
					assertFalse( HibernateProxy.class.isInstance( human ) );
					assertEquals( 0, stats.getPrepareStatementCount() );

					final OtherEntity otherEntity = session.get( OtherEntity.class, "test1" );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertSame( human, otherEntity.getHuman() );
					assertFalse( Hibernate.isInitialized( human ) );

					session.merge( withInitializedEnhancedProxy );
					assertSame( human, otherEntity.getHuman() );
					assertTrue( Hibernate.isInitialized( human ) );

					assertEquals( 7, otherEntity.getHuman().getAge() );
				}
		);

		assertEquals( 3, stats.getPrepareStatementCount() );
		stats.clear();

		checkAgeInNewSession( scope, 7 );
	}

	@Test
	public void mergeUninitializedHibernateProxy(SessionFactoryScope scope) {

		checkAgeInNewSession( scope, 1 );

		final Statistics stats = scope.getSessionFactory().getStatistics();
		stats.clear();

		final OtherEntity withUninitializedHibernateProxy = scope.fromTransaction(
				session -> {

					final OtherEntity otherEntity = session.get( OtherEntity.class, "test1" );
					assertEquals( 1, stats.getPrepareStatementCount() );

					assertTrue( Hibernate.isPropertyInitialized( otherEntity, "human" ) );
					assertFalse( Hibernate.isInitialized( otherEntity.getHuman() ) );
					assertTrue( HibernateProxy.class.isInstance( otherEntity.getHuman() ) );

					return otherEntity;
				}
		);

		assertEquals( 1, stats.getPrepareStatementCount() );
		stats.clear();

		checkAgeInNewSession( scope, 1 );
		stats.clear();

		// merge uninitialized HibernateProxy to updated HibernateProxy

		scope.inTransaction(
				session -> {
					final OtherEntity otherEntity = session.get( OtherEntity.class, "test1" );
					assertEquals( 1, stats.getPrepareStatementCount() );

					assertTrue( Hibernate.isPropertyInitialized( otherEntity, "human" ) );
					assertFalse( Hibernate.isInitialized( otherEntity.getHuman() ) );
					assertTrue( HibernateProxy.class.isInstance( otherEntity.getHuman() ) );

					otherEntity.getHuman().setAge( 3 );
					assertTrue( Hibernate.isInitialized( otherEntity.getHuman() ) );
					assertEquals( 2, stats.getPrepareStatementCount() );

					final Human humanImpl = (Human) ( (HibernateProxy) otherEntity.getHuman() )
							.getHibernateLazyInitializer()
							.getImplementation();

					session.merge( withUninitializedHibernateProxy );
					//assertTrue( Hibernate.isInitialized( otherEntity.getHuman() ) );
					// TODO: Reference to HibernateProxy is changed
					//       to the HibernateProxy's implementation.
					assertSame( humanImpl, otherEntity.getHuman() );

					assertEquals( 3, otherEntity.getHuman().getAge() );
				}
		);

		assertEquals( 3, stats.getPrepareStatementCount() );
		stats.clear();

		checkAgeInNewSession( scope, 3 );
		stats.clear();

		// merge uninitialized HibernateProxy to updated enhanced proxy

		scope.inTransaction(
				session -> {
					final Human human = session.getReference( Human.class, "A Human" );
					assertFalse( Hibernate.isInitialized( human ) );
					assertFalse( HibernateProxy.class.isInstance( human ) );

					assertEquals( 0, stats.getPrepareStatementCount() );

					final OtherEntity otherEntity = session.get( OtherEntity.class, "test1" );
					assertTrue( Hibernate.isPropertyInitialized( otherEntity, "human" ) );
					assertFalse( Hibernate.isInitialized( otherEntity.getHuman() ) );
					assertSame( human, otherEntity.getHuman() );

					assertEquals( 1, stats.getPrepareStatementCount() );

					human.setAge( 5 );
					assertTrue( Hibernate.isInitialized( otherEntity.getHuman() ) );
					assertEquals( 2, stats.getPrepareStatementCount() );

					session.merge( withUninitializedHibernateProxy );
					assertEquals( 5, human.getAge() );
				}
		);

		assertEquals( 3, stats.getPrepareStatementCount() );
		stats.clear();

		checkAgeInNewSession( scope, 5 );
		stats.clear();

		// merge uninitialized HibernateProxy to uninitialized HibernateProxy

		scope.inTransaction(
				session -> {
					final OtherEntity otherEntity = session.get( OtherEntity.class, "test1" );
					assertEquals( 1, stats.getPrepareStatementCount() );

					assertTrue( Hibernate.isPropertyInitialized( otherEntity, "human" ) );
					assertFalse( Hibernate.isInitialized( otherEntity.getHuman() ) );
					assertTrue( HibernateProxy.class.isInstance( otherEntity.getHuman() ) );

					session.merge( withUninitializedHibernateProxy );

					assertFalse( Hibernate.isInitialized( otherEntity.getHuman() ) );
					assertTrue( HibernateProxy.class.isInstance( otherEntity.getHuman() ) );
				}
		);

		assertEquals( 1, stats.getPrepareStatementCount() );
		stats.clear();

		checkAgeInNewSession( scope, 5 );
		stats.clear();

		// merge uninitialized HibernateProxy to uninitialized enhanced proxy

		scope.inTransaction(
				session -> {
					final Human human = session.getReference( Human.class, "A Human" );
					assertFalse( Hibernate.isInitialized( human ) );
					assertFalse( HibernateProxy.class.isInstance( human ) );
					assertEquals( 0, stats.getPrepareStatementCount() );

					final OtherEntity otherEntity = session.get( OtherEntity.class, "test1" );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertSame( human, otherEntity.getHuman() );
					assertFalse( Hibernate.isInitialized( human ) );

					session.merge( withUninitializedHibernateProxy );
					assertSame( human, otherEntity.getHuman() );
					assertFalse( Hibernate.isInitialized( human ) );
				}
		);

		assertEquals( 1, stats.getPrepareStatementCount() );
		stats.clear();

		checkAgeInNewSession( scope, 5 );
	}

	@Test
	public void testmergeUninitializedEnhancedProxy(SessionFactoryScope scope) {

		checkAgeInNewSession( scope, 1 );

		final Statistics stats = scope.getSessionFactory().getStatistics();
		stats.clear();

		final OtherEntity withUninitializedEnhancedProxy = scope.fromTransaction(
				session -> {

					final Human human = session.getReference( Human.class, "A Human" );
					assertFalse( Hibernate.isInitialized( human ) );
					assertFalse( HibernateProxy.class.isInstance( human ) );

					final OtherEntity otherEntity = session.get( OtherEntity.class, "test1" );
					assertEquals( 1, stats.getPrepareStatementCount() );

					assertTrue( Hibernate.isPropertyInitialized( otherEntity, "human" ) );
					assertSame( human, otherEntity.getHuman() );
					assertFalse( Hibernate.isInitialized( otherEntity.getHuman() ) );
					assertFalse( HibernateProxy.class.isInstance( otherEntity.getHuman() ) );
					return otherEntity;
				}
		);

		assertEquals( 1, stats.getPrepareStatementCount() );
		stats.clear();

		checkAgeInNewSession( scope, 1 );
		stats.clear();

		// merge uninitialized enhanced proxy to updated HibernateProxy

		scope.inTransaction(
				session -> {
					final OtherEntity otherEntity = session.get( OtherEntity.class, "test1" );
					assertEquals( 1, stats.getPrepareStatementCount() );

					assertTrue( Hibernate.isPropertyInitialized( otherEntity, "human" ) );
					assertFalse( Hibernate.isInitialized( otherEntity.getHuman() ) );
					assertTrue( HibernateProxy.class.isInstance( otherEntity.getHuman() ) );

					otherEntity.getHuman().setAge( 3 );
					assertTrue( Hibernate.isInitialized( otherEntity.getHuman() ) );
					assertEquals( 2, stats.getPrepareStatementCount() );

					final Human humanImpl = (Human) ( (HibernateProxy) otherEntity.getHuman() )
							.getHibernateLazyInitializer()
							.getImplementation();

					session.merge( withUninitializedEnhancedProxy );
					// TODO: Reference to HibernateProxy is changed
					//       to the HibernateProxy's implementation.
					assertSame( humanImpl, otherEntity.getHuman() );

					assertEquals( 3, otherEntity.getHuman().getAge() );
				}
		);

		assertEquals( 3, stats.getPrepareStatementCount() );
		stats.clear();

		checkAgeInNewSession( scope, 3 );
		stats.clear();

		// merge uninitialized enhanced proxy to updated enhanced entity

		scope.inTransaction(
				session -> {
					final Human human = session.getReference( Human.class, "A Human" );
					assertFalse( Hibernate.isInitialized( human ) );
					assertFalse( HibernateProxy.class.isInstance( human ) );

					assertEquals( 0, stats.getPrepareStatementCount() );

					final OtherEntity otherEntity = session.get( OtherEntity.class, "test1" );
					assertTrue( Hibernate.isPropertyInitialized( otherEntity, "human" ) );
					assertFalse( Hibernate.isInitialized( otherEntity.getHuman() ) );
					assertSame( human, otherEntity.getHuman() );

					assertEquals( 1, stats.getPrepareStatementCount() );

					human.setAge( 5 );
					assertTrue( Hibernate.isInitialized( otherEntity.getHuman() ) );
					assertEquals( 2, stats.getPrepareStatementCount() );

					session.merge( withUninitializedEnhancedProxy );
					assertEquals( 5, human.getAge() );
				}
		);

		assertEquals( 3, stats.getPrepareStatementCount() );
		stats.clear();

		checkAgeInNewSession( scope, 5 );
		stats.clear();

		// merge uninitialized enhanced proxy to uninitialized HibernateProxy

		scope.inTransaction(
				session -> {
					final OtherEntity otherEntity = session.get( OtherEntity.class, "test1" );
					assertEquals( 1, stats.getPrepareStatementCount() );

					assertTrue( Hibernate.isPropertyInitialized( otherEntity, "human" ) );
					assertFalse( Hibernate.isInitialized( otherEntity.getHuman() ) );
					assertTrue( HibernateProxy.class.isInstance( otherEntity.getHuman() ) );

					session.merge( withUninitializedEnhancedProxy );

					assertFalse( Hibernate.isInitialized( otherEntity.getHuman() ) );
					assertTrue( HibernateProxy.class.isInstance( otherEntity.getHuman() ) );
				}
		);

		assertEquals( 1, stats.getPrepareStatementCount() );
		stats.clear();

		checkAgeInNewSession( scope, 5 );
		stats.clear();

		// merge uninitialized enhanced proxy to uninitialized enhanced proxy

		scope.inTransaction(
				session -> {
					final Human human = session.getReference( Human.class, "A Human" );
					assertFalse( Hibernate.isInitialized( human ) );
					assertFalse( HibernateProxy.class.isInstance( human ) );
					assertEquals( 0, stats.getPrepareStatementCount() );

					final OtherEntity otherEntity = session.get( OtherEntity.class, "test1" );
					assertEquals( 1, stats.getPrepareStatementCount() );
					assertSame( human, otherEntity.getHuman() );
					assertFalse( Hibernate.isInitialized( human ) );

					session.merge( withUninitializedEnhancedProxy );
					assertSame( human, otherEntity.getHuman() );
					assertFalse( Hibernate.isInitialized( human ) );
				}
		);

		assertEquals( 1, stats.getPrepareStatementCount() );
		stats.clear();

		checkAgeInNewSession( scope, 5 );
	}

	private void checkAgeInNewSession(SessionFactoryScope scope, int expectedAge) {
		scope.inTransaction(
				session -> {
					final Human human = session.get( Human.class, "A Human" );
					assertEquals( expectedAge, human.getAge() );
				}
		);
	}

	@BeforeEach
	public void setupData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Human human = new Human( "A Human" );
					OtherEntity otherEntity = new OtherEntity( "test1" );
					otherEntity.animal = human;
					otherEntity.primate = human;
					otherEntity.human = human;
					otherEntity.human.setAge( 1 );

					session.persist( human );
					session.persist( otherEntity );
				}
		);
	}

	@AfterEach
	public void cleanUpTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
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

		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
		private Animal animal = null;

		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
		private Primate primate = null;

		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
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
