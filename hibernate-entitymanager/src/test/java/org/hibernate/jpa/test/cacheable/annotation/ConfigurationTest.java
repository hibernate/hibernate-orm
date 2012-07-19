/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpa.test.cacheable.annotation;

import javax.persistence.SharedCacheMode;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.cache.internal.NoCachingRegionFactory;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.jpa.test.PersistenceUnitInfoAdapter;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.mapping.PersistentClass;

import org.junit.Test;

import org.hibernate.testing.junit4.BaseUnitTestCase;

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
		Configuration config = buildConfiguration( SharedCacheMode.NONE );

		PersistentClass pc = config.getClassMapping( ExplicitlyCacheableEntity.class.getName() );
		assertNull( pc.getCacheConcurrencyStrategy() );

		pc = config.getClassMapping( ExplicitlyNonCacheableEntity.class.getName() );
		assertNull( pc.getCacheConcurrencyStrategy() );

		pc = config.getClassMapping( NoCacheableAnnotationEntity.class.getName() );
		assertNull( pc.getCacheConcurrencyStrategy() );
	}

	@Test
	public void testSharedCacheModeUnspecified() {
		Configuration config = buildConfiguration( SharedCacheMode.UNSPECIFIED );

		PersistentClass pc = config.getClassMapping( ExplicitlyCacheableEntity.class.getName() );
		assertNull( pc.getCacheConcurrencyStrategy() );

		pc = config.getClassMapping( ExplicitlyNonCacheableEntity.class.getName() );
		assertNull( pc.getCacheConcurrencyStrategy() );

		pc = config.getClassMapping( NoCacheableAnnotationEntity.class.getName() );
		assertNull( pc.getCacheConcurrencyStrategy() );
	}

	@Test
	public void testSharedCacheModeAll() {
		Configuration config = buildConfiguration( SharedCacheMode.ALL );

		PersistentClass pc = config.getClassMapping( ExplicitlyCacheableEntity.class.getName() );
		assertNotNull( pc.getCacheConcurrencyStrategy() );

		pc = config.getClassMapping( ExplicitlyNonCacheableEntity.class.getName() );
		assertNotNull( pc.getCacheConcurrencyStrategy() );

		pc = config.getClassMapping( NoCacheableAnnotationEntity.class.getName() );
		assertNotNull( pc.getCacheConcurrencyStrategy() );
	}

	@Test
	public void testSharedCacheModeEnable() {
		Configuration config = buildConfiguration( SharedCacheMode.ENABLE_SELECTIVE );

		PersistentClass pc = config.getClassMapping( ExplicitlyCacheableEntity.class.getName() );
		assertNotNull( pc.getCacheConcurrencyStrategy() );

		pc = config.getClassMapping( ExplicitlyNonCacheableEntity.class.getName() );
		assertNull( pc.getCacheConcurrencyStrategy() );

		pc = config.getClassMapping( NoCacheableAnnotationEntity.class.getName() );
		assertNull( pc.getCacheConcurrencyStrategy() );
	}

	@Test
	public void testSharedCacheModeDisable() {
		Configuration config = buildConfiguration( SharedCacheMode.DISABLE_SELECTIVE );

		PersistentClass pc = config.getClassMapping( ExplicitlyCacheableEntity.class.getName() );
		assertNotNull( pc.getCacheConcurrencyStrategy() );

		pc = config.getClassMapping( ExplicitlyNonCacheableEntity.class.getName() );
		assertNull( pc.getCacheConcurrencyStrategy() );

		pc = config.getClassMapping( NoCacheableAnnotationEntity.class.getName() );
		assertNotNull( pc.getCacheConcurrencyStrategy() );
	}

	@SuppressWarnings("unchecked")
	private Configuration buildConfiguration(SharedCacheMode mode) {
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

		Configuration hibernateConfiguration = emfb.buildHibernateConfiguration( emfb.buildServiceRegistry() );
		hibernateConfiguration.buildMappings();
		return hibernateConfiguration;
	}

	public static class CustomRegionFactory extends NoCachingRegionFactory {
		public CustomRegionFactory() {
		}

		@Override
		public AccessType getDefaultAccessType() {
			return AccessType.READ_WRITE;
		}
	}
}
