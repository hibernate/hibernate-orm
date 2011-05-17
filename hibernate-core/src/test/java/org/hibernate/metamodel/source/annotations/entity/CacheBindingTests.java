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
package org.hibernate.metamodel.source.annotations.entity;

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.SharedCacheMode;

import org.junit.Test;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.binding.Caching;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.source.internal.MetadataImpl;
import org.hibernate.service.ServiceRegistryBuilder;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

/**
 * Tests for {@code o.h.a.Cache} and {@code j.p.Cacheable}.
 *
 * @author Hardy Ferentschik
 */
public class CacheBindingTests extends BaseUnitTestCase {
	@Test
	public void testHibernateCaching() {
		EntityBinding binding = getEntityBinding( HibernateCacheEntity.class, SharedCacheMode.ALL );
		assertNotNull( "There should be a cache binding", binding.getCaching() );
		Caching caching = binding.getCaching();
		assertEquals( "Wrong region", "foo", caching.getRegion() );
		assertEquals( "Wrong strategy", AccessType.READ_WRITE, caching.getAccessType() );
		assertEquals( "Wrong lazy properties configuration", false, caching.isCacheLazyProperties() );
	}

	@Test
	public void testJpaCaching() {
		EntityBinding binding = getEntityBinding( JpaCacheEntity.class, SharedCacheMode.ALL );
		assertNotNull( "There should be a cache binding", binding.getCaching() );
		Caching caching = binding.getCaching();
		assertEquals( "Wrong region", "CacheBindingTests$JpaCacheEntity", caching.getRegion() );
		assertEquals( "Wrong lazy properties configuration", true, caching.isCacheLazyProperties() );
	}

	@Test
	public void testNoCaching() {
		EntityBinding binding = getEntityBinding( NoCacheEntity.class, SharedCacheMode.NONE );
		assertNull( "There should be no cache binding", binding.getCaching() );
	}

	private EntityBinding getEntityBinding(Class<?> clazz, SharedCacheMode cacheMode) {
		MetadataSources sources = new MetadataSources( new ServiceRegistryBuilder().buildServiceRegistry() );
		sources.addAnnotatedClass( clazz );
		sources.getMetadataBuilder().with( cacheMode );
		MetadataImpl metadata = (MetadataImpl) sources.buildMetadata();

		return metadata.getEntityBinding( this.getClass().getSimpleName() + "$" + clazz.getSimpleName() );
	}

	@Entity
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "foo", include = "non-lazy")
	class HibernateCacheEntity {
		@Id
		private int id;
	}

	@Entity
	@Cacheable
	class JpaCacheEntity {
		@Id
		private int id;
	}

	@Entity
	@Cacheable
	class NoCacheEntity {
		@Id
		private int id;
	}
}


