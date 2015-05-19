/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache;

import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Proxy;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.ManagedEntity;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author John O'Hara
 */
public class ByteCodeEnhancedImmutableReferenceCacheTest extends BaseCoreFunctionalTestCase {
	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		configuration.setProperty( AvailableSettings.USE_DIRECT_REFERENCE_CACHE_ENTRIES, "true" );
		configuration.setProperty( AvailableSettings.USE_QUERY_CACHE, "true" );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{MyEnhancedReferenceData.class};
	}

	@Test
	public void testUseOfDirectReferencesInCache() throws Exception {
		EntityPersister persister = (EntityPersister) sessionFactory().getClassMetadata( MyEnhancedReferenceData.class );
		assertFalse( persister.isMutable() );
		assertTrue( persister.buildCacheEntry( null, null, null, null ).isReferenceEntry() );
		assertFalse( persister.hasProxy() );

		final MyEnhancedReferenceData myReferenceData = new MyEnhancedReferenceData( 1, "first item", "abc" );

		// save a reference in one session
		Session s = openSession();
		s.beginTransaction();
		s.save( myReferenceData );
		s.getTransaction().commit();
		s.close();

		assertNotNull( myReferenceData.$$_hibernate_getEntityEntry() );

		// now load it in another
		s = openSession();
		s.beginTransaction();
		//		MyEnhancedReferenceData loaded = (MyEnhancedReferenceData) s.get( MyEnhancedReferenceData.class, 1 );
		MyEnhancedReferenceData loaded = (MyEnhancedReferenceData) s.load( MyEnhancedReferenceData.class, 1 );
		s.getTransaction().commit();
		s.close();

		// the 2 instances should be the same (==)
		assertTrue( "The two instances were different references", myReferenceData == loaded );

		// now try query caching
		s = openSession();
		s.beginTransaction();
		MyEnhancedReferenceData queried = (MyEnhancedReferenceData) s.createQuery( "from MyEnhancedReferenceData" ).setCacheable( true ).list().get( 0 );
		s.getTransaction().commit();
		s.close();

		// the 2 instances should be the same (==)
		assertTrue( "The two instances were different references", myReferenceData == queried );

		// cleanup
		s = openSession();
		s.beginTransaction();
		s.delete( myReferenceData );
		s.getTransaction().commit();
		s.close();
	}

	@Entity(name = "MyEnhancedReferenceData")
	@Immutable
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
	@Proxy(lazy = false)
	@SuppressWarnings("UnusedDeclaration")
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

	}
}
