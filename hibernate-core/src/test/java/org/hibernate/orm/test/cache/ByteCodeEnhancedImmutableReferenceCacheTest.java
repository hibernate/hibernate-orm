/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Immutable;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.ManagedEntity;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author John O'Hara
 */
@DomainModel(
		annotatedClasses = {
				ByteCodeEnhancedImmutableReferenceCacheTest.MyEnhancedReferenceData.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting(name = Environment.USE_DIRECT_REFERENCE_CACHE_ENTRIES, value = "true"),
				@Setting(name = Environment.USE_QUERY_CACHE, value = "true"),
		}
)
@SessionFactory
public class ByteCodeEnhancedImmutableReferenceCacheTest {

	@Test
	public void testUseOfDirectReferencesInCache(SessionFactoryScope scope) {
		EntityPersister persister = scope.getSessionFactory().getMappingMetamodel()
				.getEntityDescriptor( MyEnhancedReferenceData.class );
		assertFalse( persister.isMutable() );
		assertTrue( persister.buildCacheEntry( null, null, null, null ).isReferenceEntry() );
		assertFalse( persister.hasProxy() );

		final MyEnhancedReferenceData myReferenceData = new MyEnhancedReferenceData( 1, "first item", "abc" );

		// save a reference in one session
		Session s = scope.getSessionFactory().openSession();
		s.beginTransaction();
		s.persist( myReferenceData );
		s.getTransaction().commit();
		s.close();

		assertNotNull( myReferenceData.$$_hibernate_getEntityEntry() );

		// now load it in another
		s = scope.getSessionFactory().openSession();
		s.beginTransaction();
		//		MyEnhancedReferenceData loaded = (MyEnhancedReferenceData) s.get( MyEnhancedReferenceData.class, 1 );
		MyEnhancedReferenceData loaded = s.getReference( MyEnhancedReferenceData.class, 1 );
		s.getTransaction().commit();
		s.close();

		// the 2 instances should be the same (==)
		assertSame( myReferenceData, loaded, "The two instances were different references" );

		// now try query caching
		s = scope.getSessionFactory().openSession();
		s.beginTransaction();
		MyEnhancedReferenceData queried = (MyEnhancedReferenceData)
				s.createQuery( "from MyEnhancedReferenceData" )
						.setCacheable( true )
						.list().get( 0 );
		s.getTransaction().commit();
		s.close();

		// the 2 instances should be the same (==)
		assertSame( myReferenceData, queried, "The two instances were different references" );

		// cleanup
		s = scope.getSessionFactory().openSession();
		s.beginTransaction();
		s.remove( myReferenceData );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@JiraKey(value = "HHH-10795")
	public void testAssociatedWithMultiplePersistenceContexts(SessionFactoryScope scope) {
		MyEnhancedReferenceData myReferenceData = new MyEnhancedReferenceData( 1, "first item", "abc" );
		MyEnhancedReferenceData myOtherReferenceData = new MyEnhancedReferenceData( 2, "second item", "def" );

		// save a reference in one session
		Session s1 = scope.getSessionFactory().openSession();
		s1.beginTransaction();
		s1.persist( myReferenceData );
		s1.persist( myOtherReferenceData );
		s1.getTransaction().commit();
		s1.close();

		assertNotNull( myReferenceData.$$_hibernate_getEntityEntry() );
		assertNotNull( myOtherReferenceData.$$_hibernate_getEntityEntry() );

		// now associate myReferenceData with 2 sessions
		s1 = scope.getSessionFactory().openSession();
		s1.beginTransaction();
		myReferenceData = s1.get( MyEnhancedReferenceData.class, myReferenceData.getId() );
		myOtherReferenceData = s1.get( MyEnhancedReferenceData.class, myOtherReferenceData.getId() );
		assertTrue( s1.contains( myReferenceData ) );
		assertTrue( s1.contains( myOtherReferenceData ) );
		// prev/next references should be null; entityEntry should be non-null;
		assertNull( myReferenceData.$$_hibernate_getPreviousManagedEntity() );
		assertNull( myReferenceData.$$_hibernate_getNextManagedEntity() );
		assertNull( myOtherReferenceData.$$_hibernate_getPreviousManagedEntity() );
		assertNull( myOtherReferenceData.$$_hibernate_getNextManagedEntity() );

		assertSame(
				myReferenceData.$$_hibernate_getEntityEntry(),
				((SharedSessionContractImplementor) s1).getPersistenceContext().getEntry( myReferenceData )
		);
		assertSame(
				myOtherReferenceData.$$_hibernate_getEntityEntry(),
				((SharedSessionContractImplementor) s1).getPersistenceContext().getEntry( myOtherReferenceData )
		);

		Session s2 = scope.getSessionFactory().openSession();
		s2.beginTransaction();

		// s2 should contains no entities
		assertFalse( s2.contains( myReferenceData ) );
		assertFalse( s2.contains( myOtherReferenceData ) );

		assertNull( ((SharedSessionContractImplementor) s2).getPersistenceContext().getEntry( myReferenceData ) );
		assertNull( ((SharedSessionContractImplementor) s2).getPersistenceContext().getEntry( myOtherReferenceData ) );

		// evict should do nothing, since p is not associated with s2
		s2.evict( myReferenceData );
		s2.evict( myOtherReferenceData );

		assertSame( myReferenceData, s2.get( MyEnhancedReferenceData.class, myReferenceData.getId() ) );
		assertSame( myOtherReferenceData, s2.get( MyEnhancedReferenceData.class, myOtherReferenceData.getId() ) );

		assertTrue( s2.contains( myReferenceData ) );
		assertTrue( s2.contains( myOtherReferenceData ) );

		// still associated with s1
		assertTrue( s1.contains( myReferenceData ) );
		assertTrue( s1.contains( myOtherReferenceData ) );

		s2.evict( myReferenceData );
		s2.evict( myOtherReferenceData );

		assertFalse( s2.contains( myReferenceData ) );
		assertFalse( s2.contains( myOtherReferenceData ) );

		s2.getTransaction().commit();
		s2.close();

		// still associated with s1
		assertTrue( s1.contains( myReferenceData ) );
		assertTrue( s1.contains( myOtherReferenceData ) );

		s1.clear();

		assertFalse( s1.contains( myReferenceData ) );
		assertFalse( s1.contains( myOtherReferenceData ) );

		s1.close();

		// EntityEntry should still be set
		assertNotNull( myReferenceData.$$_hibernate_getEntityEntry() );
		assertNotNull( myOtherReferenceData.$$_hibernate_getEntityEntry() );

		// load them into 2 sessions
		s1 = scope.getSessionFactory().openSession();
		s1.getTransaction().begin();
		assertSame( myReferenceData, s1.get( MyEnhancedReferenceData.class, myReferenceData.getId() ) );
		assertSame( myOtherReferenceData, s1.get( MyEnhancedReferenceData.class, myOtherReferenceData.getId() ) );

		s2 = scope.getSessionFactory().openSession();
		s2.getTransaction().begin();
		assertSame( myReferenceData, s2.get( MyEnhancedReferenceData.class, myReferenceData.getId() ) );
		assertSame( myOtherReferenceData, s2.get( MyEnhancedReferenceData.class, myOtherReferenceData.getId() ) );

		assertEquals( Status.READ_ONLY, myReferenceData.$$_hibernate_getEntityEntry().getStatus() );
		assertEquals( Status.READ_ONLY, myOtherReferenceData.$$_hibernate_getEntityEntry().getStatus() );

		// delete myReferenceData from s1
		s1.remove( myReferenceData );

		assertEquals( Status.DELETED, myReferenceData.$$_hibernate_getEntityEntry().getStatus() );
		assertEquals( Status.READ_ONLY, myOtherReferenceData.$$_hibernate_getEntityEntry().getStatus() );

		// delete myOtherReferenceData from s2
		s2.remove( myOtherReferenceData );

		assertEquals( Status.DELETED, myReferenceData.$$_hibernate_getEntityEntry().getStatus() );
		assertEquals( Status.DELETED, myOtherReferenceData.$$_hibernate_getEntityEntry().getStatus() );

		s1.getTransaction().commit();
		s1.close();

		assertEquals( Status.GONE, myReferenceData.$$_hibernate_getEntityEntry().getStatus() );
		assertEquals( Status.DELETED, myOtherReferenceData.$$_hibernate_getEntityEntry().getStatus() );

		s2.getTransaction().commit();
		s2.close();

		assertEquals( Status.GONE, myReferenceData.$$_hibernate_getEntityEntry().getStatus() );
		assertEquals( Status.GONE, myOtherReferenceData.$$_hibernate_getEntityEntry().getStatus() );
	}

	@Entity(name = "MyEnhancedReferenceData")
	@Immutable
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
	@SuppressWarnings("unused")
	public static class MyEnhancedReferenceData implements ManagedEntity {
		@Id
		private Integer id;
		private String name;
		private String theValue;

		@Transient
		private transient EntityEntry entityEntry;
		@Transient
		private transient ManagedEntity previous;
		@Transient
		private transient ManagedEntity next;
		@Transient
		private transient int instanceId;

		public MyEnhancedReferenceData(Integer id, String name, String theValue) {
			this.id = id;
			this.name = name;
			this.theValue = theValue;
		}

		protected MyEnhancedReferenceData() {
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getTheValue() {
			return theValue;
		}

		public void setTheValue(String theValue) {
			this.theValue = theValue;
		}

		@Override
		public Object $$_hibernate_getEntityInstance() {
			return this;
		}

		@Override
		public EntityEntry $$_hibernate_getEntityEntry() {
			return entityEntry;
		}

		@Override
		public void $$_hibernate_setEntityEntry(EntityEntry entityEntry) {
			this.entityEntry = entityEntry;
		}

		@Override
		public ManagedEntity $$_hibernate_getNextManagedEntity() {
			return next;
		}

		@Override
		public void $$_hibernate_setNextManagedEntity(ManagedEntity next) {
			this.next = next;
		}

		@Override
		public ManagedEntity $$_hibernate_getPreviousManagedEntity() {
			return previous;
		}

		@Override
		public void $$_hibernate_setPreviousManagedEntity(ManagedEntity previous) {
			this.previous = previous;
		}

		@Override
		public void $$_hibernate_setUseTracker(boolean useTracker) {

		}

		@Override
		public boolean $$_hibernate_useTracker() {
			return false;
		}

		@Override
		public int $$_hibernate_getInstanceId() {
			return instanceId;
		}

		@Override
		public void $$_hibernate_setInstanceId(int id) {
			this.instanceId = id;
		}
	}
}
