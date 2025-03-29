/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache;

import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Immutable;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class ReferenceCacheTest extends BaseCoreFunctionalTestCase {
	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		configuration.setProperty( AvailableSettings.USE_DIRECT_REFERENCE_CACHE_ENTRIES, true );
		configuration.setProperty( AvailableSettings.USE_QUERY_CACHE, true );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { MyReferenceData.class };
	}

	@Test
	public void testUseOfDirectReferencesInCache() throws Exception {
		EntityPersister persister = sessionFactory().getMappingMetamodel().getEntityDescriptor( MyReferenceData.class );
		assertFalse( persister.isMutable() );
		assertTrue( persister.buildCacheEntry( null, null, null, null ).isReferenceEntry() );
		assertFalse( persister.hasProxy() );

		final MyReferenceData myReferenceData = new MyReferenceData( 1, "first item", "abc" );

		// save a reference in one session
		Session s = openSession();
		s.beginTransaction();
		s.persist( myReferenceData );
		s.getTransaction().commit();
		s.close();

		// now load it in another
		s = openSession();
		s.beginTransaction();
//		MyReferenceData loaded = (MyReferenceData) s.get( MyReferenceData.class, 1 );
		MyReferenceData loaded = (MyReferenceData) s.getReference( MyReferenceData.class, 1 );
		s.getTransaction().commit();
		s.close();

		// the 2 instances should be the same (==)
		assertTrue( "The two instances were different references", myReferenceData == loaded );

		// now try query caching
		s = openSession();
		s.beginTransaction();
		MyReferenceData queried = (MyReferenceData) s.createQuery( "from MyReferenceData" ).setCacheable( true ).list().get( 0 );
		s.getTransaction().commit();
		s.close();

		// the 2 instances should be the same (==)
		assertTrue( "The two instances were different references", myReferenceData == queried );

		// cleanup
		s = openSession();
		s.beginTransaction();
		s.remove( myReferenceData );
		s.getTransaction().commit();
		s.close();
	}

	@Entity( name="MyReferenceData" )
	@Immutable
	@Cacheable
	@Cache( usage = CacheConcurrencyStrategy.READ_ONLY )
	@SuppressWarnings("unused")
	public static class MyReferenceData {
		@Id
		private Integer id;
		private String name;
		private String theValue;

		public MyReferenceData(Integer id, String name, String theValue) {
			this.id = id;
			this.name = name;
			this.theValue = theValue;
		}

		protected MyReferenceData() {
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
	}
}
