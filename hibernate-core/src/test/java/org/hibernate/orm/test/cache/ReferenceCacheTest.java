/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Immutable;
import org.hibernate.cfg.Environment;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = {
				ReferenceCacheTest.MyReferenceData.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting(name = Environment.USE_DIRECT_REFERENCE_CACHE_ENTRIES, value = "true"),
				@Setting(name = Environment.USE_QUERY_CACHE, value = "true"),
		}
)
@SessionFactory
public class ReferenceCacheTest {

	@Test
	public void testUseOfDirectReferencesInCache(SessionFactoryScope scope) throws Exception {
		EntityPersister persister = scope.getSessionFactory().getMappingMetamodel()
				.getEntityDescriptor( MyReferenceData.class );
		assertFalse( persister.isMutable() );
		assertTrue( persister.buildCacheEntry( null, null, null, null ).isReferenceEntry() );
		assertFalse( persister.hasProxy() );

		final MyReferenceData myReferenceData = new MyReferenceData( 1, "first item", "abc" );

		// save a reference in one session
		scope.inTransaction( s -> {
			s.persist( myReferenceData );
		} );

		// now load it in another
		MyReferenceData loaded = scope.fromTransaction( s -> {
			return  (MyReferenceData) s.getReference( MyReferenceData.class, 1 );
		} );

		// the 2 instances should be the same (==)
		assertSame( myReferenceData, loaded, "The two instances were different references" );

		// now try query caching
		MyReferenceData queried = scope.fromTransaction( s -> {
			return (MyReferenceData) s.createQuery( "from MyReferenceData" ).setCacheable( true )
					.list().get( 0 );
		} );

		// the 2 instances should be the same (==)
		assertSame( myReferenceData, queried, "The two instances were different references" );

		// cleanup
		scope.inTransaction( s -> {
			s.remove( myReferenceData );
		} );
	}

	@Entity(name = "MyReferenceData")
	@Immutable
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
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
