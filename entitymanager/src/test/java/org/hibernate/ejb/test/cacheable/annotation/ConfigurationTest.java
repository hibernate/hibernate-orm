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
package org.hibernate.ejb.test.cacheable.annotation;

import java.util.Properties;
import javax.persistence.SharedCacheMode;

import org.hibernate.cache.access.AccessType;
import org.hibernate.cache.impl.NoCachingRegionFactory;
import org.hibernate.cfg.Environment;
import org.hibernate.ejb.AvailableSettings;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.junit.UnitTestCase;
import org.hibernate.mapping.PersistentClass;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class ConfigurationTest extends UnitTestCase {
	public ConfigurationTest(String string) {
		super( string );
	}

	public void testSharedCacheModeNone() {
		Ejb3Configuration config = buildConfiguration( SharedCacheMode.NONE );

		PersistentClass pc = config.getClassMapping( ExplicitlyCacheableEntity.class.getName() );
		assertNull( pc.getCacheConcurrencyStrategy() );

		pc = config.getClassMapping( ExplicitlyNonCacheableEntity.class.getName() );
		assertNull( pc.getCacheConcurrencyStrategy() );

		pc = config.getClassMapping( NoCacheableAnnotationEntity.class.getName() );
		assertNull( pc.getCacheConcurrencyStrategy() );
	}

	public void testSharedCacheModeUnspecified() {
		Ejb3Configuration config = buildConfiguration( SharedCacheMode.UNSPECIFIED );

		PersistentClass pc = config.getClassMapping( ExplicitlyCacheableEntity.class.getName() );
		assertNull( pc.getCacheConcurrencyStrategy() );

		pc = config.getClassMapping( ExplicitlyNonCacheableEntity.class.getName() );
		assertNull( pc.getCacheConcurrencyStrategy() );

		pc = config.getClassMapping( NoCacheableAnnotationEntity.class.getName() );
		assertNull( pc.getCacheConcurrencyStrategy() );
	}

	public void testSharedCacheModeAll() {
		Ejb3Configuration config = buildConfiguration( SharedCacheMode.ALL );

		PersistentClass pc = config.getClassMapping( ExplicitlyCacheableEntity.class.getName() );
		assertNotNull( pc.getCacheConcurrencyStrategy() );

		pc = config.getClassMapping( ExplicitlyNonCacheableEntity.class.getName() );
		assertNotNull( pc.getCacheConcurrencyStrategy() );

		pc = config.getClassMapping( NoCacheableAnnotationEntity.class.getName() );
		assertNotNull( pc.getCacheConcurrencyStrategy() );
	}

	public void testSharedCacheModeEnable() {
		Ejb3Configuration config = buildConfiguration( SharedCacheMode.ENABLE_SELECTIVE );

		PersistentClass pc = config.getClassMapping( ExplicitlyCacheableEntity.class.getName() );
		assertNotNull( pc.getCacheConcurrencyStrategy() );

		pc = config.getClassMapping( ExplicitlyNonCacheableEntity.class.getName() );
		assertNull( pc.getCacheConcurrencyStrategy() );

		pc = config.getClassMapping( NoCacheableAnnotationEntity.class.getName() );
		assertNull( pc.getCacheConcurrencyStrategy() );
	}

	public void testSharedCacheModeDisable() {
		Ejb3Configuration config = buildConfiguration( SharedCacheMode.DISABLE_SELECTIVE );

		PersistentClass pc = config.getClassMapping( ExplicitlyCacheableEntity.class.getName() );
		assertNotNull( pc.getCacheConcurrencyStrategy() );

		pc = config.getClassMapping( ExplicitlyNonCacheableEntity.class.getName() );
		assertNull( pc.getCacheConcurrencyStrategy() );

		pc = config.getClassMapping( NoCacheableAnnotationEntity.class.getName() );
		assertNotNull( pc.getCacheConcurrencyStrategy() );
	}

	private Ejb3Configuration buildConfiguration(SharedCacheMode mode) {
		Properties properties = new Properties();
		properties.put( AvailableSettings.SHARED_CACHE_MODE, mode );
		properties.put( Environment.CACHE_REGION_FACTORY, CustomRegionFactory.class.getName() );
		Ejb3Configuration config = new Ejb3Configuration();
		config.setProperties( properties );
		config.addAnnotatedClass( ExplicitlyCacheableEntity.class );
		config.addAnnotatedClass( ExplicitlyNonCacheableEntity.class );
		config.addAnnotatedClass( NoCacheableAnnotationEntity.class );
		config.buildMappings();
		return config;
	}

	public static class CustomRegionFactory extends NoCachingRegionFactory {
		public CustomRegionFactory(Properties properties) {
			super( properties );
		}

		@Override
		public AccessType getDefaultAccessType() {
			return AccessType.READ_WRITE;
		}
	}
}
