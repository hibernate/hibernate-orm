/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.internal;

import java.util.function.Supplier;

import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.EntityNameResolver;
import org.hibernate.Interceptor;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.StatementObserver;
import org.hibernate.annotations.CacheLayout;
import org.hibernate.audit.AuditStrategy;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.orchestration.ResolvedMetadata;
import org.hibernate.boot.settings.ResolvedBootstrapSettings;
import org.hibernate.boot.settings.SettingsResolver;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryBuilderImplementor;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.bytecode.internal.SessionFactoryObserverForBytecodeEnhancer;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.cache.spi.TimestampsCacheFactory;
import org.hibernate.temporal.TemporalTableStrategy;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.context.spi.TenantCredentialsMapper;
import org.hibernate.context.spi.TenantSchemaMapper;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.type.format.FormatMapper;

import jakarta.persistence.criteria.Nulls;

import static org.hibernate.internal.SessionFactoryRegistry.instantiateSessionFactory;

/**
 * @author Gail Badner
 * @author Steve Ebersole
 */
public class SessionFactoryBuilderImpl implements SessionFactoryBuilderImplementor {
	private final MetadataImplementor metadata;
	private final SessionFactoryOptionsCollector optionsCollector = new SessionFactoryOptionsCollector();
	private final Supplier<SessionFactoryOptionsBuilder> legacyOptionsBuilderSupplier;
	private final BootstrapContext bootstrapContext;
	private final ResolvedBootstrapSettings bootstrapSettings;
	private final ResolvedMetadata resolvedMetadata;

	public SessionFactoryBuilderImpl(MetadataImplementor metadata, BootstrapContext bootstrapContext) {
		this(
				metadata,
				() -> new SessionFactoryOptionsBuilder(
						metadata.getMetadataBuildingOptions().getServiceRegistry(),
						bootstrapContext
				),
				bootstrapContext,
				null,
				null,
				true
		);
	}

	public SessionFactoryBuilderImpl(MetadataImplementor metadata, SessionFactoryOptionsBuilder optionsBuilder, BootstrapContext context) {
		this( metadata, () -> optionsBuilder, context, null, null, true );
	}

	public SessionFactoryBuilderImpl(
			MetadataImplementor metadata,
			BootstrapContext bootstrapContext,
			ResolvedBootstrapSettings bootstrapSettings,
			ResolvedMetadata resolvedMetadata) {
		this(
				metadata,
				() -> new SessionFactoryOptionsBuilder(
						metadata.getMetadataBuildingOptions().getServiceRegistry(),
						bootstrapContext
				),
				bootstrapContext,
				bootstrapSettings,
				resolvedMetadata,
				false
		);
	}

	private SessionFactoryBuilderImpl(
			MetadataImplementor metadata,
			Supplier<SessionFactoryOptionsBuilder> legacyOptionsBuilderSupplier,
			BootstrapContext context,
			ResolvedBootstrapSettings bootstrapSettings,
			ResolvedMetadata resolvedMetadata,
			boolean applyLegacyBuiltInObservers) {
		this.metadata = metadata;
		this.legacyOptionsBuilderSupplier = legacyOptionsBuilderSupplier;
		this.bootstrapContext = context;
		this.bootstrapSettings = bootstrapSettings;
		this.resolvedMetadata = resolvedMetadata;

		if ( metadata.getSqlFunctionMap() != null ) {
			for ( var sqlFunctionEntry : metadata.getSqlFunctionMap().entrySet() ) {
				applySqlFunction( sqlFunctionEntry.getKey(), sqlFunctionEntry.getValue() );
			}
		}

		final var bytecodeProvider =
				metadata.getMetadataBuildingOptions().getServiceRegistry()
						.getService( BytecodeProvider.class );
		if ( applyLegacyBuiltInObservers ) {
			addSessionFactoryObservers( new SessionFactoryObserverForBytecodeEnhancer( bytecodeProvider ) );
			addSessionFactoryObservers( new SessionFactoryObserverForNamedQueryValidation( metadata ) );
			addSessionFactoryObservers( new SessionFactoryObserverForSchemaExport( metadata ) );
			addSessionFactoryObservers( new SessionFactoryObserverForRegistration() );
		}
	}

	@Override
	public SessionFactoryBuilder applyBeanManager(Object beanManager) {
		optionsCollector.applyBeanManager(  beanManager );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyValidatorFactory(Object validatorFactory) {
		optionsCollector.applyValidatorFactory( validatorFactory );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyName(String sessionFactoryName) {
		optionsCollector.applySessionFactoryName( sessionFactoryName );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyNameAsJndiName(boolean isJndiName) {
		optionsCollector.enableSessionFactoryNameAsJndiName( isJndiName );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyAutoClosing(boolean enabled) {
		optionsCollector.enableSessionAutoClosing( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyAutoFlushing(boolean enabled) {
		optionsCollector.enableSessionAutoFlushing( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyJtaTrackingByThread(boolean enabled) {
		optionsCollector.enableJtaTrackingByThread( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyPreferUserTransactions(boolean preferUserTransactions) {
		optionsCollector.enablePreferUserTransaction( preferUserTransactions );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyStatisticsSupport(boolean enabled) {
		optionsCollector.enableStatisticsSupport( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder addSessionFactoryObservers(SessionFactoryObserver... observers) {
		optionsCollector.addSessionFactoryObservers( observers );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyInterceptor(Interceptor interceptor) {
		optionsCollector.applyInterceptor( interceptor );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyStatelessInterceptor(Class<? extends Interceptor> statelessInterceptorClass) {
		optionsCollector.applyStatelessInterceptor( statelessInterceptorClass );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyStatelessInterceptor(Supplier<? extends Interceptor> statelessInterceptorSupplier) {
		optionsCollector.applyStatelessInterceptorSupplier( statelessInterceptorSupplier );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyStatementObserver(StatementObserver statementObserver) {
		optionsCollector.applyStatementObserver( statementObserver );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyStatementInspector(StatementInspector statementInspector) {
		optionsCollector.applyStatementInspector( statementInspector );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyCustomEntityDirtinessStrategy(CustomEntityDirtinessStrategy strategy) {
		optionsCollector.applyCustomEntityDirtinessStrategy( strategy );
		return this;
	}

	@Override
	public SessionFactoryBuilder addEntityNameResolver(EntityNameResolver... entityNameResolvers) {
		optionsCollector.addEntityNameResolvers( entityNameResolvers );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyEntityNotFoundDelegate(EntityNotFoundDelegate entityNotFoundDelegate) {
		optionsCollector.applyEntityNotFoundDelegate( entityNotFoundDelegate );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyIdentifierRollbackSupport(boolean enabled) {
		optionsCollector.enableIdentifierRollbackSupport( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyNullabilityChecking(boolean enabled) {
		optionsCollector.enableNullabilityChecking( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyLazyInitializationOutsideTransaction(boolean enabled) {
		optionsCollector.allowLazyInitializationOutsideTransaction( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyDefaultBatchFetchSize(int size) {
		optionsCollector.applyDefaultBatchFetchSize( size );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyMaximumFetchDepth(int depth) {
		optionsCollector.applyMaximumFetchDepth( depth );
		return this;
	}

	@Override
	public SessionFactoryBuilder applySubselectFetchEnabled(boolean enabled) {
		optionsCollector.applySubselectFetchEnabled( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyDefaultNullPrecedence(Nulls nullPrecedence) {
		optionsCollector.applyDefaultNullPrecedence( nullPrecedence );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyOrderingOfInserts(boolean enabled) {
		optionsCollector.enableOrderingOfInserts( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyOrderingOfUpdates(boolean enabled) {
		optionsCollector.enableOrderingOfUpdates( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyMultiTenancy(boolean enabled) {
		optionsCollector.applyMultiTenancy(enabled);
		return this;
	}

	@Override
	public SessionFactoryBuilder applyCurrentTenantIdentifierResolver(CurrentTenantIdentifierResolver<?> resolver) {
		optionsCollector.applyCurrentTenantIdentifierResolver( resolver );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyTenantSchemaMapper(TenantSchemaMapper<?> mapper) {
		optionsCollector.applyTenantSchemaMapper( mapper );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyTenantCredentialsMapper(TenantCredentialsMapper<?> mapper) {
		optionsCollector.applyTenantCredentialsMapper( mapper );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyNamedQueryCheckingOnStartup(boolean enabled) {
		optionsCollector.enableNamedQueryCheckingOnStartup( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applySecondLevelCacheSupport(boolean enabled) {
		optionsCollector.enableSecondLevelCacheSupport( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyQueryCacheSupport(boolean enabled) {
		optionsCollector.enableQueryCacheSupport( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyQueryCacheLayout(CacheLayout cacheLayout) {
		optionsCollector.applyQueryCacheLayout( cacheLayout );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyTimestampsCacheFactory(TimestampsCacheFactory factory) {
		optionsCollector.applyTimestampsCacheFactory( factory );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyCacheRegionPrefix(String prefix) {
		optionsCollector.applyCacheRegionPrefix( prefix );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyMinimalPutsForCaching(boolean enabled) {
		optionsCollector.enableMinimalPuts( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyStructuredCacheEntries(boolean enabled) {
		optionsCollector.enabledStructuredCacheEntries( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyDirectReferenceCaching(boolean enabled) {
		optionsCollector.allowDirectReferenceCacheEntries( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyAutomaticEvictionOfCollectionCaches(boolean enabled) {
		optionsCollector.enableAutoEvictCollectionCaches( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyJdbcBatchSize(int size) {
		optionsCollector.applyJdbcBatchSize( size );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyScrollableResultsSupport(boolean enabled) {
		optionsCollector.enableScrollableResultSupport( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyGetGeneratedKeysSupport(boolean enabled) {
		optionsCollector.enableGeneratedKeysSupport( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyJdbcFetchSize(int size) {
		optionsCollector.applyJdbcFetchSize( size );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyConnectionHandlingMode(PhysicalConnectionHandlingMode connectionHandlingMode) {
		optionsCollector.applyConnectionHandlingMode( connectionHandlingMode );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyConnectionProviderDisablesAutoCommit(boolean providerDisablesAutoCommit) {
		optionsCollector.applyConnectionProviderDisablesAutoCommit( providerDisablesAutoCommit );
		return this;
	}

	@Override
	public SessionFactoryBuilder applySqlComments(boolean enabled) {
		optionsCollector.enableCommentsSupport( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applySqlFunction(String registrationName, SqmFunctionDescriptor functionDescriptor) {
		optionsCollector.applySqlFunction( registrationName, functionDescriptor );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyCollectionsInDefaultFetchGroup(boolean enabled) {
		optionsCollector.enableCollectionInDefaultFetchGroup( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyTemporalTableStrategy(TemporalTableStrategy strategy) {
		optionsCollector.applyTemporalTableStrategy( strategy );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyAuditStrategy(AuditStrategy strategy) {
		optionsCollector.applyAuditStrategy( strategy );
		return this;
	}

	@Override
	public SessionFactoryBuilder allowOutOfTransactionUpdateOperations(boolean allow) {
		optionsCollector.allowOutOfTransactionUpdateOperations( allow );
		return this;
	}

	@Override
	public SessionFactoryBuilder enableJpaQueryCompliance(boolean enabled) {
		optionsCollector.enableJpaQueryCompliance( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder enableJpaOrderByMappingCompliance(boolean enabled) {
		optionsCollector.enableJpaOrderByMappingCompliance( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder enableJpaTransactionCompliance(boolean enabled) {
		optionsCollector.enableJpaTransactionCompliance( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder enableJpaClosedCompliance(boolean enabled) {
		optionsCollector.enableJpaClosedCompliance( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyJsonFormatMapper(FormatMapper jsonFormatMapper) {
		optionsCollector.applyJsonFormatMapper( jsonFormatMapper );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyXmlFormatMapper(FormatMapper xmlFormatMapper) {
		optionsCollector.applyXmlFormatMapper( xmlFormatMapper );
		return this;
	}

	@Override
	public void disableJtaTransactionAccess() {
		optionsCollector.disableJtaTransactionAccess();
	}

	@Override
	public SessionFactory build() {
		if ( resolvedMetadata != null ) {
			final var sessionFactorySettings = optionsCollector.applyTo(
					SettingsResolver.resolveSessionFactorySettings(
							bootstrapSettings,
							metadata.getMetadataBuildingOptions().getServiceRegistry()
					)
			);
			return org.hibernate.boot.orchestration.SessionFactoryBuilder.build(
					sessionFactorySettings,
					resolvedMetadata,
					metadata.getMetadataBuildingOptions().getServiceRegistry()
			);
		}
		return instantiateSessionFactory( metadata, buildSessionFactoryOptions(), bootstrapContext );
	}

	@Override
	public SessionFactoryOptions buildSessionFactoryOptions() {
		return optionsCollector.buildOptions( legacyOptionsBuilderSupplier.get() );
	}
}
