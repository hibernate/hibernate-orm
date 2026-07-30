/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.service.javaservice;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cache.cfg.spi.DomainDataRegionConfig;
import org.hibernate.cache.internal.DisabledCaching;
import org.hibernate.cache.spi.CacheConstructionContext;
import org.hibernate.cache.spi.CacheFactory;
import org.hibernate.cache.spi.CacheImplementor;
import org.hibernate.engine.query.internal.NativeQueryInterpreterStandardImpl;
import org.hibernate.engine.query.spi.NativeQueryInterpreter;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@BootstrapServiceRegistry(javaServices = {
		@BootstrapServiceRegistry.JavaService(
				role = CacheFactory.class,
				impl = SessionFactoryJavaServiceTests.CustomCacheFactory.class
		),
		@BootstrapServiceRegistry.JavaService(
				role = NativeQueryInterpreter.class,
				impl = SessionFactoryJavaServiceTests.CustomNativeQueryInterpreter.class
		)
})
@DomainModel(annotatedClasses = SessionFactoryJavaServiceTests.CachedEntity.class)
@SessionFactory
public class SessionFactoryJavaServiceTests {
	private static final AtomicReference<Set<DomainDataRegionConfig>> CACHE_REGION_CONFIGS = new AtomicReference<>();

	@Test
	void factoryOwnedServicesAreSelectedThroughJavaServices(SessionFactoryScope scope) {
		final var sessionFactory = scope.getSessionFactory();
		assertThat( sessionFactory.getCache() ).isInstanceOf( MarkerCache.class );
		assertThat( sessionFactory.getQueryEngine().getNativeQueryInterpreter() )
				.isInstanceOf( CustomNativeQueryInterpreter.class );
		assertThat( CACHE_REGION_CONFIGS.get() )
				.singleElement()
				.satisfies( regionConfig -> {
					assertThat( regionConfig.getRegionName() ).isEqualTo( "prepared-cache-region" );
					assertThat( regionConfig.getEntityCaching() ).hasSize( 1 );
				} );
	}

	public static class CustomCacheFactory implements CacheFactory {
		@Override
		public CacheImplementor buildCache(CacheConstructionContext constructionContext) {
			CACHE_REGION_CONFIGS.set( constructionContext.getCacheRegionConfigs() );
			return new MarkerCache( constructionContext.getSessionFactory() );
		}
	}

	public static class MarkerCache extends DisabledCaching {
		public MarkerCache(SessionFactoryImplementor sessionFactory) {
			super( sessionFactory );
		}

	}

	public static class CustomNativeQueryInterpreter extends NativeQueryInterpreterStandardImpl {
		public CustomNativeQueryInterpreter() {
			super( false );
		}
	}

	@Entity(name = "CachedEntity")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "prepared-cache-region")
	public static class CachedEntity {
		@Id
		private Integer id;
	}
}
