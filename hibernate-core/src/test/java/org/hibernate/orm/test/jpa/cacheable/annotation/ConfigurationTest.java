/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.cacheable.annotation;

import java.util.Arrays;
import java.util.Map;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.SharedCacheMode;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.testing.orm.jpa.PersistenceUnitInfoAdapter;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.testing.cache.CachingRegionFactory;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Steve Ebersole
 */
@BaseUnitTest
public class ConfigurationTest {

	private EntityManagerFactory emf;

	@AfterEach
	public void tearDown() {
		if ( emf != null ) {
			emf.close();
		}
	}

	@Test
	public void testSharedCacheModeNone() {
		MetadataImplementor metadata = buildMetadata( SharedCacheMode.NONE );

		PersistentClass pc = metadata.getEntityBinding( ExplicitlyCacheableEntity.class.getName() );
		assertFalse( pc.isCached() );

		pc = metadata.getEntityBinding( ExplicitlyNonCacheableEntity.class.getName() );
		assertFalse( pc.isCached() );

		pc = metadata.getEntityBinding( NoCacheableAnnotationEntity.class.getName() );
		assertFalse( pc.isCached() );
	}

	@Test
	public void testSharedCacheModeUnspecified() {
		MetadataImplementor metadata = buildMetadata( SharedCacheMode.UNSPECIFIED );

		PersistentClass pc = metadata.getEntityBinding( ExplicitlyCacheableEntity.class.getName() );
		assertTrue( pc.isCached() );

		pc = metadata.getEntityBinding( ExplicitlyNonCacheableEntity.class.getName() );
		assertFalse( pc.isCached() );

		pc = metadata.getEntityBinding( NoCacheableAnnotationEntity.class.getName() );
		assertFalse( pc.isCached() );
	}

	@Test
	public void testSharedCacheModeAll() {
		MetadataImplementor metadata = buildMetadata( SharedCacheMode.ALL );

		PersistentClass pc = metadata.getEntityBinding( ExplicitlyCacheableEntity.class.getName() );
		assertTrue( pc.isCached() );

		pc = metadata.getEntityBinding( ExplicitlyNonCacheableEntity.class.getName() );
		assertTrue( pc.isCached() );

		pc = metadata.getEntityBinding( NoCacheableAnnotationEntity.class.getName() );
		assertTrue( pc.isCached() );
	}

	@Test
	public void testSharedCacheModeEnable() {
		MetadataImplementor metadata = buildMetadata( SharedCacheMode.ENABLE_SELECTIVE );

		PersistentClass pc = metadata.getEntityBinding( ExplicitlyCacheableEntity.class.getName() );
		assertTrue( pc.isCached() );

		pc = metadata.getEntityBinding( ExplicitlyNonCacheableEntity.class.getName() );
		assertFalse( pc.isCached() );

		pc = metadata.getEntityBinding( NoCacheableAnnotationEntity.class.getName() );
		assertFalse( pc.isCached() );
	}

	@Test
	public void testSharedCacheModeDisable() {
		MetadataImplementor metadata = buildMetadata( SharedCacheMode.DISABLE_SELECTIVE );

		PersistentClass pc = metadata.getEntityBinding( ExplicitlyCacheableEntity.class.getName() );
		assertTrue( pc.isCached() );

		pc = metadata.getEntityBinding( ExplicitlyNonCacheableEntity.class.getName() );
		assertFalse( pc.isCached() );

		pc = metadata.getEntityBinding( NoCacheableAnnotationEntity.class.getName() );
		assertTrue( pc.isCached() );
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private MetadataImplementor buildMetadata(SharedCacheMode mode) {
		Map settings = ServiceRegistryUtil.createBaseSettings();
		settings.put( AvailableSettings.JPA_SHARED_CACHE_MODE, mode );
		settings.put( AvailableSettings.CACHE_REGION_FACTORY, CustomRegionFactory.class.getName() );
		settings.put(
				AvailableSettings.LOADED_CLASSES,
				Arrays.asList(
						ExplicitlyCacheableEntity.class,
						ExplicitlyNonCacheableEntity.class,
						NoCacheableAnnotationEntity.class
				)
		);

		PersistenceUnitInfoAdapter adapter = new PersistenceUnitInfoAdapter();

		final EntityManagerFactoryBuilderImpl emfb = (EntityManagerFactoryBuilderImpl) Bootstrap.getEntityManagerFactoryBuilder(
				adapter,
				settings
		);

		emf = emfb.build();
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
