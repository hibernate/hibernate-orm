/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.bytecode.enhancement.lazy.proxy;

import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.EntityMode;
import org.hibernate.EntityNameResolver;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.stat.Statistics;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.tuple.entity.EntityTuplizer;
import org.hibernate.tuple.entity.PojoEntityTuplizer;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */
@TestForIssue( jiraKey = "HHH-13640" )
@RunWith(BytecodeEnhancerRunner.class)
public class LazyToOnesNoProxyFactoryWithSubclassesStatelessTest extends BaseNonConfigCoreFunctionalTestCase {

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
	public void testNewEnhancedProxyAssociation() {
		inStatelessTransaction(
				session -> {
					Human human = new Human( "A Human" );
					OtherEntity otherEntity = new OtherEntity( "test1" );
					otherEntity.human = human;

					session.insert( human );
					session.insert( otherEntity );
				}
		);

		inStatelessSession(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
					stats.clear();
					final OtherEntity otherEntity = (OtherEntity) session.get( OtherEntity.class, "test1" );
					assertTrue( Hibernate.isPropertyInitialized( otherEntity, "human" ) );
					assertFalse( Hibernate.isInitialized( otherEntity.human ) );
					assertFalse( HibernateProxy.class.isInstance( otherEntity.animal ) );
					assertEquals( 1, stats.getPrepareStatementCount() );
				}
		);
	}

	@Test
	public void testExistingInitializedAssociationLeafSubclass() {
		inStatelessSession(
				session -> {
					Human human = new Human( "A Human" );
					OtherEntity otherEntity = new OtherEntity( "test1" );
					otherEntity.animal = human;
					otherEntity.primate = human;
					otherEntity.human = human;
					session.insert( human );
					session.insert( otherEntity );
				}
		);

		final Statistics stats = sessionFactory().getStatistics();
		stats.clear();

		inStatelessSession(
				session -> {

					final OtherEntity otherEntity = (OtherEntity) session.get( OtherEntity.class, "test1" );
					assertTrue( Hibernate.isPropertyInitialized( otherEntity, "animal" ) );
					assertTrue( Hibernate.isInitialized( otherEntity.animal ) );
					assertFalse( HibernateProxy.class.isInstance( otherEntity.animal ) );
					assertTrue( Hibernate.isPropertyInitialized( otherEntity, "primate" ) );
					assertTrue( Hibernate.isInitialized( otherEntity.primate ) );
					assertFalse( HibernateProxy.class.isInstance( otherEntity.primate ) );
					assertTrue( Hibernate.isPropertyInitialized( otherEntity, "human" ) );
					assertTrue( Hibernate.isInitialized( otherEntity.human ) );
					assertFalse( HibernateProxy.class.isInstance( otherEntity.human ) );
					assertSame( otherEntity.human, otherEntity.animal );
					assertSame( otherEntity.human, otherEntity.primate );
					assertEquals( 2, stats.getPrepareStatementCount() );
				}
		);

		assertEquals( 2, stats.getPrepareStatementCount() );
	}

	@Test
	public void testExistingEnhancedProxyAssociationLeafSubclassOnly() {
		inStatelessSession(
				session -> {
					Human human = new Human( "A Human" );
					OtherEntity otherEntity = new OtherEntity( "test1" );
					otherEntity.human = human;
					otherEntity.otherHuman = human;
					session.insert( human );
					session.insert( otherEntity );
				}
		);

		inStatelessSession(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
					stats.clear();

					final OtherEntity otherEntity = (OtherEntity) session.get( OtherEntity.class, "test1" );
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