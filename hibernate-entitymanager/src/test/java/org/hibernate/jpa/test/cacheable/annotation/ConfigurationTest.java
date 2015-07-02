/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.cacheable.annotation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.SharedCacheMode;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cache.internal.NoCachingRegionFactory;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.Environment;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.test.PersistenceUnitInfoAdapter;
import org.hibernate.mapping.PersistentClass;

import org.hibernate.testing.cache.CachingRegionFactory;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * this is hacky transient step until EMF building is integrated with metamodel
 *
 * @author Steve Ebersole
 */
public class ConfigurationTest extends BaseUnitTestCase {
	@Test
	public void testSharedCacheModeNone() {
		MetadataImplementor metadata = buildMetadata( SharedCacheMode.NONE );

		PersistentClass pc = metadata.getEntityBinding( ExplicitlyCacheableEntity.class.getName() );
		assertNull( pc.getCacheConcurrencyStrategy() );

		pc = metadata.getEntityBinding( ExplicitlyNonCacheableEntity.class.getName() );
		assertNull( pc.getCacheConcurrencyStrategy() );

		pc = metadata.getEntityBinding( NoCacheableAnnotationEntity.class.getName() );
		assertNull( pc.getCacheConcurrencyStrategy() );
	}

	@Test
	public void testSharedCacheModeUnspecified() {
		MetadataImplementor metadata = buildMetadata( SharedCacheMode.UNSPECIFIED );

		PersistentClass pc = metadata.getEntityBinding( ExplicitlyCacheableEntity.class.getName() );
		assertNull( pc.getCacheConcurrencyStrategy() );

		pc = metadata.getEntityBinding( ExplicitlyNonCacheableEntity.class.getName() );
		assertNull( pc.getCacheConcurrencyStrategy() );

		pc = metadata.getEntityBinding( NoCacheableAnnotationEntity.class.getName() );
		assertNull( pc.getCacheConcurrencyStrategy() );
	}

	@Test
	public void testSharedCacheModeAll() {
		MetadataImplementor metadata = buildMetadata( SharedCacheMode.ALL );

		PersistentClass pc = metadata.getEntityBinding( ExplicitlyCacheableEntity.class.getName() );
		assertNotNull( pc.getCacheConcurrencyStrategy() );

		pc = metadata.getEntityBinding( ExplicitlyNonCacheableEntity.class.getName() );
		assertNotNull( pc.getCacheConcurrencyStrategy() );

		pc = metadata.getEntityBinding( NoCacheableAnnotationEntity.class.getName() );
		assertNotNull( pc.getCacheConcurrencyStrategy() );
	}

	@Test
	public void testSharedCacheModeEnable() {
		MetadataImplementor metadata = buildMetadata( SharedCacheMode.ENABLE_SELECTIVE );

		PersistentClass pc = metadata.getEntityBinding( ExplicitlyCacheableEntity.class.getName() );
		assertNotNull( pc.getCacheConcurrencyStrategy() );

		pc = metadata.getEntityBinding( ExplicitlyNonCacheableEntity.class.getName() );
		assertNull( pc.getCacheConcurrencyStrategy() );

		pc = metadata.getEntityBinding( NoCacheableAnnotationEntity.class.getName() );
		assertNull( pc.getCacheConcurrencyStrategy() );
	}

	@Test
	public void testSharedCacheModeDisable() {
		MetadataImplementor metadata = buildMetadata( SharedCacheMode.DISABLE_SELECTIVE );

		PersistentClass pc = metadata.getEntityBinding( ExplicitlyCacheableEntity.class.getName() );
		assertNotNull( pc.getCacheConcurrencyStrategy() );

		pc = metadata.getEntityBinding( ExplicitlyNonCacheableEntity.class.getName() );
		assertNull( pc.getCacheConcurrencyStrategy() );

		pc = metadata.getEntityBinding( NoCacheableAnnotationEntity.class.getName() );
		assertNotNull( pc.getCacheConcurrencyStrategy() );
	}

	@SuppressWarnings("unchecked")
	private MetadataImplementor buildMetadata(SharedCacheMode mode) {
		Map settings = new HashMap();
		settings.put( AvailableSettings.SHARED_CACHE_MODE, mode );
		settings.put( Environment.CACHE_REGION_FACTORY, CustomRegionFactory.class.getName() );
		settings.put(
				AvailableSettings.LOADED_CLASSES,
				Arrays.asList(
						ExplicitlyCacheableEntity.class,
						ExplicitlyNonCacheableEntity.class,
						NoCacheableAnnotationEntity.class
				)
		);

		PersistenceUnitInfoAdapter adapter = new PersistenceUnitInfoAdapter();

		EntityManagerFactoryBuilderImpl emfb = (EntityManagerFactoryBuilderImpl) Bootstrap.getEntityManagerFactoryBuilder(
				adapter,
				settings
		);
		emfb.build();
		return emfb.getMetadata();
	}

	public static class CustomRegionFactory extends CachingRegionFactory {
		public CustomRegionFactory() {
		}

		@Override
		public AccessType getDefaultAccessType() {
			return AccessType.READ_WRITE;
		}
	}
}
