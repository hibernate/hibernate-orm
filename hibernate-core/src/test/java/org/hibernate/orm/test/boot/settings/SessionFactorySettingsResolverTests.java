/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.settings;

import java.util.LinkedHashMap;

import org.hibernate.Interceptor;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.annotations.CacheLayout;
import org.hibernate.boot.pipeline.internal.SessionFactoryOptionsAdapter;
import org.hibernate.boot.pipeline.internal.settings.ResolvedBootstrapSettings;
import org.hibernate.boot.pipeline.internal.settings.SessionFactorySettingsResolver;
import org.hibernate.cache.internal.StandardTimestampsCacheFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.BatchSettings;
import org.hibernate.cfg.CacheSettings;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.cfg.PersistenceSettings;
import org.hibernate.cfg.QuerySettings;
import org.hibernate.cfg.SessionEventSettings;
import org.hibernate.cfg.TransactionSettings;
import org.hibernate.cfg.ValidationSettings;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;

import org.junit.jupiter.api.Test;

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
		configurationValues.put( PersistenceSettings.SESSION_FACTORY_JNDI_NAME, "java:hibernate/example-factory" );
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
		configurationValues.put( QuerySettings.SAFE_MODE_ENABLED, true );
		final var interceptor = new TestInterceptor();
		configurationValues.put( SessionEventSettings.INTERCEPTOR, interceptor );
		final var validatorFactory = new Object();
		configurationValues.put( ValidationSettings.JAKARTA_VALIDATION_FACTORY, validatorFactory );
		final var observer = new TestSessionFactoryObserver();
		configurationValues.put( PersistenceSettings.SESSION_FACTORY_OBSERVER, observer );

		final var bootstrapSettings = new ResolvedBootstrapSettings(
				configurationValues,
				true
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
		assertThat( sessionFactorySettings.sessionFactoryName() ).isEqualTo( "example-factory" );
		assertThat( sessionFactorySettings.sessionFactoryJndiName() ).isEqualTo( "java:hibernate/example-factory" );
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
		assertThat( sessionFactorySettings.safeModeEnabled() ).isTrue();
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
	void aggregateJpaComplianceSettingOverridesJpaBootstrapDefault(ServiceRegistryScope registryScope) {
		final var configurationValues = new LinkedHashMap<String, Object>();
		configurationValues.put( AvailableSettings.JPA_COMPLIANCE, "false" );

		final var settings = SessionFactorySettingsResolver.resolve(
				new ResolvedBootstrapSettings(
						configurationValues,
						true
				),
				registryScope.getRegistry()
		);

		assertThat( settings.jpaCompliance().isJpaQueryComplianceEnabled() ).isFalse();
		assertThat( settings.jpaCompliance().isJpaTransactionComplianceEnabled() ).isFalse();
	}

	@Test
	void jpaBootstrapSuppliesJpaComplianceDefaultWhenAggregateIsUnset(ServiceRegistryScope registryScope) {
		final var settings = SessionFactorySettingsResolver.resolve(
				new ResolvedBootstrapSettings(
						new LinkedHashMap<>(),
						true
				),
				registryScope.getRegistry()
		);

		assertThat( settings.jpaCompliance().isJpaQueryComplianceEnabled() ).isTrue();
		assertThat( settings.jpaCompliance().isJpaTransactionComplianceEnabled() ).isTrue();
	}

	@Test
	void nativeBootstrapSuppliesNativeComplianceDefaultWhenAggregateIsUnset(ServiceRegistryScope registryScope) {
		final var settings = SessionFactorySettingsResolver.resolve(
				new ResolvedBootstrapSettings(
						new LinkedHashMap<>(),
						false
				),
				registryScope.getRegistry()
		);

		assertThat( settings.jpaCompliance().isJpaQueryComplianceEnabled() ).isFalse();
		assertThat( settings.jpaCompliance().isJpaTransactionComplianceEnabled() ).isFalse();
	}

	@Test
	void specificJpaQueryComplianceOverridesAggregateCompliance(ServiceRegistryScope registryScope) {
		final var configurationValues = new LinkedHashMap<String, Object>();
		configurationValues.put( AvailableSettings.JPA_COMPLIANCE, "false" );
		configurationValues.put( AvailableSettings.JPA_QUERY_COMPLIANCE, "true" );

		final var settings = SessionFactorySettingsResolver.resolve(
				new ResolvedBootstrapSettings(
						configurationValues,
						true
				),
				registryScope.getRegistry()
		);

		assertThat( settings.jpaCompliance().isJpaQueryComplianceEnabled() ).isTrue();
		assertThat( settings.jpaCompliance().isJpaTransactionComplianceEnabled() ).isFalse();
	}

	@Test
	void legacyJpaqlStrictComplianceContributesQueryComplianceDefault(ServiceRegistryScope registryScope) {
		final var configurationValues = new LinkedHashMap<String, Object>();
		configurationValues.put( AvailableSettings.JPA_COMPLIANCE, "false" );
		configurationValues.put( AvailableSettings.JPAQL_STRICT_COMPLIANCE, "true" );

		final var settings = SessionFactorySettingsResolver.resolve(
				new ResolvedBootstrapSettings(
						configurationValues,
						true
				),
				registryScope.getRegistry()
		);

		assertThat( settings.jpaCompliance().isJpaQueryComplianceEnabled() ).isTrue();
		assertThat( settings.jpaCompliance().isJpaTransactionComplianceEnabled() ).isFalse();
	}

	@Test
	void projectsHighImpactRuntimeSettings(ServiceRegistryScope registryScope) {
		final var configurationValues = new LinkedHashMap<String, Object>();
		configurationValues.put( TransactionSettings.FLUSH_BEFORE_COMPLETION, "false" );
		configurationValues.put( TransactionSettings.AUTO_CLOSE_SESSION, "true" );
		configurationValues.put( TransactionSettings.ALLOW_JTA_TRANSACTION_ACCESS, "true" );
		configurationValues.put( BatchSettings.STATEMENT_BATCH_SIZE, "37" );
		configurationValues.put( PersistenceSettings.JPA_CALLBACKS_ENABLED, "false" );
		configurationValues.put( QuerySettings.QUERY_STARTUP_CHECKING, "false" );
		configurationValues.put( QuerySettings.SAFE_MODE_ENABLED, "true" );

		final var settings = SessionFactorySettingsResolver.resolve(
				new ResolvedBootstrapSettings(
						configurationValues,
						true
				),
				registryScope.getRegistry()
		);

		assertThat( settings.flushBeforeCompletionEnabled() ).isFalse();
		assertThat( settings.autoCloseSessionEnabled() ).isTrue();
		assertThat( settings.jtaTransactionAccessEnabled() ).isTrue();
		assertThat( settings.jdbcBatchSize() ).isEqualTo( 37 );
		assertThat( settings.jpaCallbacksEnabled() ).isFalse();
		assertThat( settings.namedQueryStartupCheckingEnabled() ).isFalse();
		assertThat( settings.safeModeEnabled() ).isTrue();
	}

	@Test
	void jpaBootstrapDisablesJtaTransactionAccessByDefault(ServiceRegistryScope registryScope) {
		final var settings = SessionFactorySettingsResolver.resolve(
				new ResolvedBootstrapSettings(
						new LinkedHashMap<>(),
						true
				),
				registryScope.getRegistry()
		);

		assertThat( settings.jtaTransactionAccessEnabled() ).isFalse();
		assertThat( SessionFactoryOptionsAdapter.create( settings ).isJtaTransactionAccessEnabled() ).isFalse();
	}

	@Test
	void minimalOptionsAdapterExposesAuditedConstructorValues(ServiceRegistryScope registryScope) {
		final var configurationValues = new LinkedHashMap<String, Object>();
		configurationValues.put( PersistenceSettings.SESSION_FACTORY_NAME, "adapter-test" );
		configurationValues.put( MappingSettings.DEFAULT_SCHEMA, "adapter_schema" );

		final var settings = SessionFactorySettingsResolver.resolve(
				new ResolvedBootstrapSettings(
						configurationValues,
						true
				),
				registryScope.getRegistry()
		);

		final var options = SessionFactoryOptionsAdapter.create( settings );

		assertThat( options.getUuid() ).isNotBlank();
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
		assertThat( options.getJdbcBatchSize() ).isEqualTo( settings.jdbcBatchSize() );
		assertThat( options.isSafeModeEnabled() ).isFalse();
		assertThat( options.isMultiTenancyEnabled() ).isFalse();
		assertThat( options.getDefaultTenantIdentifierJavaType() )
				.isSameAs( settings.defaultTenantIdentifierJavaType() );
	}

	private static class TestSessionFactoryObserver implements SessionFactoryObserver {
	}

	private static class TestInterceptor implements Interceptor {
	}
}
