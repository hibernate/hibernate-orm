/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.bytecode.enhancement.lazy.proxy;

import java.io.Serializable;
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
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.annotations.Tuplizer;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
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
public class LazyToOnesNoProxyFactoryWithSubclassesStatefulTest extends BaseNonConfigCoreFunctionalTestCase {
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
	public void testNewEnhancedProxyAssociation() {
		inTransaction(
				session -> {
					Human human = new Human( "A Human" );
					OtherEntity otherEntity = new OtherEntity( "test1" );
					otherEntity.human = human;

					session.persist( human );
					session.persist( otherEntity );
				}
		);

		inSession(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
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
	public void testExistingInitializedAssociationLeafSubclass() {
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
		inTransaction(
				session -> {
					Human human = new Human( "A Human" );
					OtherEntity otherEntity = new OtherEntity( "test1" );
					otherEntity.human = human;
					otherEntity.otherHuman = human;
					session.persist( human );
					session.persist( otherEntity );
				}
		);

		inSession(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
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
	@Tuplizer(impl=NoProxyFactoryPojoEntityTuplizer.class)
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

		@ManyToOne(fetch = FetchType.LAZY)
		@LazyToOne(LazyToOneOption.NO_PROXY)
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

	public static class NoProxyFactoryPojoEntityTuplizer implements EntityTuplizer {

		private final PojoEntityTuplizer pojoEntityTuplizer;

		public NoProxyFactoryPojoEntityTuplizer(EntityMetamodel entityMetamodel, PersistentClass mappedEntity) {
			pojoEntityTuplizer = new PojoEntityTuplizer( entityMetamodel, mappedEntity );
		}

		@Override
		public EntityMode getEntityMode() {
			return pojoEntityTuplizer.getEntityMode();
		}

		@Override
		public Object instantiate(Serializable id) throws HibernateException {
			return pojoEntityTuplizer.instantiate( id );
		}

		@Override
		public Object instantiate(Serializable id, SharedSessionContractImplementor session) {
			return pojoEntityTuplizer.instantiate( id, session );

		}

		@Override
		public Serializable getIdentifier(Object entity) throws HibernateException {
			return pojoEntityTuplizer.getIdentifier( entity );
		}

		@Override
		public Serializable getIdentifier(Object entity, SharedSessionContractImplementor session) {
			return pojoEntityTuplizer.getIdentifier( entity, session );
		}

		@Override
		public void setIdentifier(Object entity, Serializable id) throws HibernateException {
			pojoEntityTuplizer.setIdentifier( entity, id );
		}

		@Override
		public void setIdentifier(Object entity, Serializable id, SharedSessionContractImplementor session) {
			pojoEntityTuplizer.setIdentifier( entity, id, session );
		}

		@Override
		public void resetIdentifier(Object entity, Serializable currentId, Object currentVersion) {
			pojoEntityTuplizer.resetIdentifier( entity, currentId, currentVersion );
		}

		@Override
		public void resetIdentifier(
				Object entity,
				Serializable currentId,
				Object currentVersion,
				SharedSessionContractImplementor session) {
			pojoEntityTuplizer.resetIdentifier( entity, currentId, currentVersion, session );
		}

		@Override
		public Object getVersion(Object entity) throws HibernateException {
			return pojoEntityTuplizer.getVersion( entity );
		}

		@Override
		public void setPropertyValue(Object entity, int i, Object value) throws HibernateException {
			pojoEntityTuplizer. setPropertyValue( entity, i, value );
		}

		@Override
		public void setPropertyValue(Object entity, String propertyName, Object value) throws HibernateException {
			pojoEntityTuplizer.setPropertyValue( entity, propertyName, value );
		}

		@Override
		public Object[] getPropertyValuesToInsert(
				Object entity,
				Map mergeMap,
				SharedSessionContractImplementor session) throws HibernateException {
			return pojoEntityTuplizer.getPropertyValuesToInsert( entity, mergeMap, session );
		}

		@Override
		public Object getPropertyValue(Object entity, String propertyName) throws HibernateException {
			return pojoEntityTuplizer.getPropertyValue( entity, propertyName );
		}

		@Override
		public void afterInitialize(Object entity, SharedSessionContractImplementor session) {
			pojoEntityTuplizer.afterInitialize( entity, session );

		}

		@Override
		public boolean hasProxy() {
			return pojoEntityTuplizer.hasProxy();
		}

		@Override
		public Object createProxy(Serializable id, SharedSessionContractImplementor session) throws HibernateException {
			return pojoEntityTuplizer.createProxy( id, session );
		}

		@Override
		public boolean isLifecycleImplementor() {
			return pojoEntityTuplizer.isLifecycleImplementor();
		}

		@Override
		public Class getConcreteProxyClass() {
			return pojoEntityTuplizer.getConcreteProxyClass();
		}

		@Override
		public EntityNameResolver[] getEntityNameResolvers() {
			return pojoEntityTuplizer.getEntityNameResolvers();
		}

		@Override
		public String determineConcreteSubclassEntityName(
				Object entityInstance, SessionFactoryImplementor factory) {
			return pojoEntityTuplizer.determineConcreteSubclassEntityName( entityInstance, factory );
		}

		@Override
		public Getter getIdentifierGetter() {
			return pojoEntityTuplizer.getIdentifierGetter();
		}

		@Override
		public Getter getVersionGetter() {
			return pojoEntityTuplizer.getVersionGetter();
		}

		@Override
		public ProxyFactory getProxyFactory() {
			return null;
		}

		@Override
		public Object[] getPropertyValues(Object entity) {
			return pojoEntityTuplizer.getPropertyValues( entity );
		}

		@Override
		public void setPropertyValues(Object entity, Object[] values) {
			pojoEntityTuplizer.setPropertyValues( entity, values );
		}

		@Override
		public Object getPropertyValue(Object entity, int i) {
			return pojoEntityTuplizer.getPropertyValue( entity, i );
		}

		@Override
		public Object instantiate() {
			return pojoEntityTuplizer.instantiate();
		}

		@Override
		public boolean isInstance(Object object) {
			return pojoEntityTuplizer.isInstance( object );
		}

		@Override
		public Class getMappedClass() {
			return pojoEntityTuplizer.getMappedClass();
		}

		@Override
		public Getter getGetter(int i) {
			return pojoEntityTuplizer.getGetter( i );
		}
	}
}