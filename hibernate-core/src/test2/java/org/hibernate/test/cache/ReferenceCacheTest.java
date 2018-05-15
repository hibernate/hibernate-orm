/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache;

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Proxy;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.persister.entity.EntityPersister;

import org.junit.Test;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class ReferenceCacheTest extends BaseCoreFunctionalTestCase {
	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		configuration.setProperty( AvailableSettings.USE_DIRECT_REFERENCE_CACHE_ENTRIES, "true" );
		configuration.setProperty( AvailableSettings.USE_QUERY_CACHE, "true" );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { MyReferenceData.class };
	}

	@Test
	public void testUseOfDirectReferencesInCache() throws Exception {
		EntityPersister persister = (EntityPersister) sessionFactory().getClassMetadata( MyReferenceData.class );
		assertFalse( persister.isMutable() );
		assertTrue( persister.buildCacheEntry( null, null, null, null ).isReferenceEntry() );
		assertFalse( persister.hasProxy() );

		final MyReferenceData myReferenceData = new MyReferenceData( 1, "first item", "abc" );

		// save a reference in one session
		Session s = openSession();
		s.beginTransaction();
		s.save( myReferenceData );
		s.getTransaction().commit();
		s.close();

		// now load it in another
		s = openSession();
		s.beginTransaction();
//		MyReferenceData loaded = (MyReferenceData) s.get( MyReferenceData.class, 1 );
		MyReferenceData loaded = (MyReferenceData) s.load( MyReferenceData.class, 1 );
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
		s.delete( myReferenceData );
		s.getTransaction().commit();
		s.close();
	}

	@Entity( name="MyReferenceData" )
	@Immutable
	@Cacheable
	@Cache( usage = CacheConcurrencyStrategy.READ_ONLY )
	@Proxy( lazy = false )
	@SuppressWarnings("UnusedDeclaration")
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
