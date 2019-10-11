/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.bytecode.enhancement.lazy.proxy;

import javax.persistence.CascadeType;
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
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */
@TestForIssue( jiraKey = "HHH-13640" )
@RunWith(BytecodeEnhancerRunner.class )
@EnhancementOptions(lazyLoading = true)
public class LazyToOnesProxyMergeWithSubclassesTest extends BaseNonConfigCoreFunctionalTestCase {
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
	public void mergeUpdatedHibernateProxy() {

		checkAgeInNewSession( 1 );

		final Statistics stats = sessionFactory().getStatistics();
		stats.clear();

		final OtherEntity withInitializedHibernateProxy = doInHibernate(
				this::sessionFactory,
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

		checkAgeInNewSession( 1 );
		stats.clear();

		// merge updated HibernateProxy to updated HibernateProxy

		withInitializedHibernateProxy.getHuman().setAge( 2 );

		doInHibernate(
				this::sessionFactory,
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

		checkAgeInNewSession( 2 );
		stats.clear();

		// merge updated HibernateProxy to updated enhanced entity

		withInitializedHibernateProxy.getHuman().setAge( 4 );

		doInHibernate(
				this::sessionFactory,
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

		checkAgeInNewSession( 4 );
		stats.clear();

		// merge updated HibernateProxy to uninitialized HibernateProxy

		withInitializedHibernateProxy.getHuman().setAge( 6 );

		doInHibernate(
				this::sessionFactory,
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

		checkAgeInNewSession( 6 );
		stats.clear();

		// merge updated HibernateProxy To uninitialized enhanced proxy

		withInitializedHibernateProxy.getHuman().setAge( 7 );

		doInHibernate(
				this::sessionFactory,
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

		checkAgeInNewSession( 7 );
	}

	@Test
	public void mergeUpdatedEnhancedProxy() {

		checkAgeInNewSession( 1 );

		final Statistics stats = sessionFactory().getStatistics();
		stats.clear();

		final OtherEntity withInitializedEnhancedProxy = doInHibernate(
				this::sessionFactory,
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

		checkAgeInNewSession( 1 );
		stats.clear();

		// merge updated enhanced proxy to updated HibernateProxy

		withInitializedEnhancedProxy.getHuman().setAge( 2 );

		doInHibernate(
				this::sessionFactory,
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

		checkAgeInNewSession( 2 );
		stats.clear();

		// merge updated enhanced proxy to updated enhanced proxy

		withInitializedEnhancedProxy.getHuman().setAge( 4 );

		doInHibernate(
				this::sessionFactory,
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

		checkAgeInNewSession( 4 );
		stats.clear();

		// merge updated enhanced proxy to uninitialized HibernateProxy

		withInitializedEnhancedProxy.getHuman().setAge( 6 );

		doInHibernate(
				this::sessionFactory,
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

		checkAgeInNewSession( 6 );
		stats.clear();

		// merge updated enhanced proxy to uninitialized enhanced proxy

		withInitializedEnhancedProxy.getHuman().setAge( 7 );

		doInHibernate(
				this::sessionFactory,
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

		checkAgeInNewSession( 7 );
	}

	@Test
	public void mergeUninitializedHibernateProxy() {

		checkAgeInNewSession( 1 );

		final Statistics stats = sessionFactory().getStatistics();
		stats.clear();

		final OtherEntity withUninitializedHibernateProxy = doInHibernate(
				this::sessionFactory,
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

		checkAgeInNewSession( 1 );
		stats.clear();

		// merge uninitialized HibernateProxy to updated HibernateProxy

		doInHibernate(
				this::sessionFactory,
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

		checkAgeInNewSession( 3 );
		stats.clear();

		// merge uninitialized HibernateProxy to updated enhanced proxy

		doInHibernate(
				this::sessionFactory,
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

		checkAgeInNewSession( 5 );
		stats.clear();

		// merge uninitialized HibernateProxy to uninitialized HibernateProxy

		doInHibernate(
				this::sessionFactory,
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

		checkAgeInNewSession( 5 );
		stats.clear();

		// merge uninitialized HibernateProxy to uninitialized enhanced proxy

		doInHibernate(
				this::sessionFactory,
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

		checkAgeInNewSession( 5 );
	}

	@Test
	public void testmergeUninitializedEnhancedProxy() {

		checkAgeInNewSession( 1 );

		final Statistics stats = sessionFactory().getStatistics();
		stats.clear();

		final OtherEntity withUninitializedEnhancedProxy = doInHibernate(
				this::sessionFactory,
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

		checkAgeInNewSession( 1 );
		stats.clear();

		// merge uninitialized enhanced proxy to updated HibernateProxy

		doInHibernate(
				this::sessionFactory,
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

		checkAgeInNewSession( 3 );
		stats.clear();

		// merge uninitialized enhanced proxy to updated enhanced entity

		doInHibernate(
				this::sessionFactory,
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

		checkAgeInNewSession( 5 );
		stats.clear();

		// merge uninitialized enhanced proxy to uninitialized HibernateProxy

		doInHibernate(
				this::sessionFactory,
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

		checkAgeInNewSession( 5 );
		stats.clear();

		// merge uninitialized enhanced proxy to uninitialized enhanced proxy

		doInHibernate(
				this::sessionFactory,
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

		checkAgeInNewSession( 5 );
	}

	private void checkAgeInNewSession(int expectedAge) {

		doInHibernate(
				this::sessionFactory,
				session -> {
					final Human human = session.get( Human.class, "A Human" );
					assertEquals( expectedAge, human.getAge() );
				}
		);

	}

	@Before
	public void setupData() {
		inTransaction(
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

		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
		@LazyToOne(LazyToOneOption.NO_PROXY)
		private Animal animal = null;

		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
		@LazyToOne(LazyToOneOption.NO_PROXY)
		private Primate primate = null;

		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
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