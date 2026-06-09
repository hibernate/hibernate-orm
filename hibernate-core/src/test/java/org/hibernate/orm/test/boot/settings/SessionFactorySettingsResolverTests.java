/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.settings;

import java.util.LinkedHashMap;

import org.hibernate.Interceptor;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.annotations.CacheLayout;
import org.hibernate.boot.orchestration.internal.SessionFactoryOptionsAdapter;
import org.hibernate.boot.settings.ResolvedBootstrapSettings;
import org.hibernate.boot.settings.ResolvedMappingSettings;
import org.hibernate.boot.settings.SessionFactorySettingsResolver;
import org.hibernate.cache.internal.StandardTimestampsCacheFactory;
import org.hibernate.cfg.CacheSettings;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.cfg.PersistenceSettings;
import org.hibernate.cfg.SessionEventSettings;
import org.hibernate.cfg.ValidationSettings;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;

import org.junit.jupiter.api.Test;

import jakarta.persistence.FetchType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry
public class SessionFactorySettingsResolverTests {
	@Test
	void projectsFromBootstrapSettings(ServiceRegistryScope registryScope) {
		final var configurationValues = new LinkedHashMap<String, Object>();
		configurationValues.put( "hibernate.example", "original" );
		configurationValues.put( PersistenceSettings.SESSION_FACTORY_NAME, "example-factory" );
		configurationValues.put( PersistenceSettings.SESSION_FACTORY_NAME_IS_JNDI, false );
		configurationValues.put( MappingSettings.DEFAULT_CATALOG, "test_catalog" );
		configurationValues.put( MappingSettings.DEFAULT_SCHEMA, "test_schema" );
		configurationValues.put( CacheSettings.USE_SECOND_LEVEL_CACHE, true );
		configurationValues.put( CacheSettings.USE_QUERY_CACHE, true );
		configurationValues.put( CacheSettings.QUERY_CACHE_LAYOUT, "shallow" );
		configurationValues.put( CacheSettings.CACHE_REGION_PREFIX, "prefix" );
		configurationValues.put( CacheSettings.USE_MINIMAL_PUTS, true );
		configurationValues.put( CacheSettings.USE_STRUCTURED_CACHE, true );
		configurationValues.put( CacheSettings.USE_DIRECT_REFERENCE_CACHE_ENTRIES, true );
		configurationValues.put( CacheSettings.AUTO_EVICT_COLLECTION_CACHE, true );
		final var interceptor = new TestInterceptor();
		configurationValues.put( SessionEventSettings.INTERCEPTOR, interceptor );
		final var validatorFactory = new Object();
		configurationValues.put( ValidationSettings.JAKARTA_VALIDATION_FACTORY, validatorFactory );
		final var observer = new TestSessionFactoryObserver();
		configurationValues.put( PersistenceSettings.SESSION_FACTORY_OBSERVER, observer );

		final var bootstrapSettings = new ResolvedBootstrapSettings(
				configurationValues,
				true,
				new ResolvedMappingSettings( true, false, FetchType.EAGER, null )
		);

		final var sessionFactorySettings = SessionFactorySettingsResolver.resolve(
				bootstrapSettings,
				registryScope.getRegistry()
		);

		configurationValues.put( "hibernate.example", "changed" );

		assertThat( sessionFactorySettings.jpaBootstrap() ).isTrue();
		assertThat( sessionFactorySettings.configurationValues() )
				.containsEntry( "hibernate.example", "original" );
		assertThat( sessionFactorySettings.serviceRegistry() ).isSameAs( registryScope.getRegistry() );
		assertThat( sessionFactorySettings.uuid() ).isNotBlank();
		assertThat( sessionFactorySettings.sessionFactoryName() ).isEqualTo( "example-factory" );
		assertThat( sessionFactorySettings.sessionFactoryNameAlsoJndiName() ).isFalse();
		assertThat( sessionFactorySettings.interceptor() ).isSameAs( interceptor );
		assertThat( sessionFactorySettings.sessionFactoryObservers() ).containsExactly( observer );
		assertThat( sessionFactorySettings.validatorFactoryReference() ).isSameAs( validatorFactory );
		assertThat( sessionFactorySettings.secondLevelCacheEnabled() ).isTrue();
		assertThat( sessionFactorySettings.queryCacheEnabled() ).isTrue();
		assertThat( sessionFactorySettings.queryCacheLayout() ).isEqualTo( CacheLayout.SHALLOW );
		assertThat( sessionFactorySettings.timestampsCacheFactory() )
				.isSameAs( StandardTimestampsCacheFactory.INSTANCE );
		assertThat( sessionFactorySettings.cacheRegionPrefix() ).isEqualTo( "prefix" );
		assertThat( sessionFactorySettings.minimalPutsEnabled() ).isTrue();
		assertThat( sessionFactorySettings.structuredCacheEntriesEnabled() ).isTrue();
		assertThat( sessionFactorySettings.directReferenceCacheEntriesEnabled() ).isTrue();
		assertThat( sessionFactorySettings.autoEvictCollectionCache() ).isTrue();
		assertThat( sessionFactorySettings.multiTenancyEnabled() ).isFalse();
		assertThat( sessionFactorySettings.currentTenantIdentifierResolver() ).isNull();
		assertThat( sessionFactorySettings.defaultTenantIdentifierJavaType() ).isNotNull();
		assertThat( sessionFactorySettings.defaultCatalog() ).isEqualTo( "test_catalog" );
		assertThat( sessionFactorySettings.defaultSchema() ).isEqualTo( "test_schema" );
		assertThatThrownBy( () -> sessionFactorySettings.configurationValues().put( "another", "value" ) )
				.isInstanceOf( UnsupportedOperationException.class );
	}

	@Test
	void rejectsNullBootstrapSettings() {
		assertThatThrownBy( () -> SessionFactorySettingsResolver.resolve( null, null ) )
				.isInstanceOf( NullPointerException.class );
	}

	@Test
	void minimalOptionsAdapterExposesAuditedConstructorValues(ServiceRegistryScope registryScope) {
		final var configurationValues = new LinkedHashMap<String, Object>();
		configurationValues.put( PersistenceSettings.SESSION_FACTORY_NAME, "adapter-test" );
		configurationValues.put( MappingSettings.DEFAULT_SCHEMA, "adapter_schema" );

		final var settings = SessionFactorySettingsResolver.resolve(
				new ResolvedBootstrapSettings(
						configurationValues,
						true,
						new ResolvedMappingSettings( true, false, FetchType.EAGER, null )
				),
				registryScope.getRegistry()
		);

		final var options = SessionFactoryOptionsAdapter.create( settings );

		assertThat( options.getUuid() ).isEqualTo( settings.uuid() );
		assertThat( options.getServiceRegistry() ).isSameAs( registryScope.getRegistry() );
		assertThat( options.isJpaBootstrap() ).isTrue();
		assertThat( options.getSessionFactoryName() ).isEqualTo( "adapter-test" );
		assertThat( options.getInterceptor() ).isNull();
		assertThat( options.getDefaultSchema() ).isEqualTo( "adapter_schema" );
		assertThat( options.getValidatorFactoryReference() ).isNull();
		assertThat( options.isSecondLevelCacheEnabled() ).isTrue();
		assertThat( options.isQueryCacheEnabled() ).isFalse();
		assertThat( options.getQueryCacheLayout() ).isEqualTo( CacheLayout.FULL );
		assertThat( options.getTimestampsCacheFactory() ).isSameAs( StandardTimestampsCacheFactory.INSTANCE );
		assertThat( options.getCacheRegionPrefix() ).isNull();
		assertThat( options.isMultiTenancyEnabled() ).isFalse();
		assertThat( options.getDefaultTenantIdentifierJavaType() )
				.isSameAs( settings.defaultTenantIdentifierJavaType() );
	}

	private static class TestSessionFactoryObserver implements SessionFactoryObserver {
	}

	private static class TestInterceptor implements Interceptor {
	}
}
