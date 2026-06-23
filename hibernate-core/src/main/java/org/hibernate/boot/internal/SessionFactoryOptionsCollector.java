/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.EntityNameResolver;
import org.hibernate.Interceptor;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.StatementObserver;
import org.hibernate.annotations.CacheLayout;
import org.hibernate.audit.AuditStrategy;
import org.hibernate.boot.pipeline.spi.ResolvedSessionFactorySettings;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.spi.TimestampsCacheFactory;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.context.spi.TenantCredentialsMapper;
import org.hibernate.context.spi.TenantSchemaMapper;
import org.hibernate.jpa.internal.JpaComplianceImpl;
import org.hibernate.jpa.spi.JpaCompliance;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.temporal.TemporalTableStrategy;
import org.hibernate.type.format.FormatMapper;

import jakarta.persistence.criteria.Nulls;

/**
 * Collects factory customizations as named bootstrap state.
 * <p>
 * During the transition to the new bootstrap pipeline this collector can still
 * apply the collected values to {@link SessionFactoryOptionsBuilder}.  The
 * longer-term target is for these values to feed a narrower factory build
 * request rather than materializing {@link SessionFactoryOptions}.
 *
 * @since 9.0
 */
public class SessionFactoryOptionsCollector {
	private Object beanManager;
	private Object validatorFactory;
	private String sessionFactoryName;
	private Boolean sessionFactoryNameAsJndiName;
	private Boolean autoClosing;
	private Boolean autoFlushing;
	private Boolean jtaTrackingByThread;
	private Boolean preferUserTransactions;
	private Boolean statisticsSupport;
	private final List<SessionFactoryObserver> sessionFactoryObservers = new ArrayList<>();
	private Interceptor interceptor;
	private Class<? extends Interceptor> statelessInterceptorClass;
	private Supplier<? extends Interceptor> statelessInterceptorSupplier;
	private StatementObserver statementObserver;
	private StatementInspector statementInspector;
	private CustomEntityDirtinessStrategy customEntityDirtinessStrategy;
	private final List<EntityNameResolver> entityNameResolvers = new ArrayList<>();
	private EntityNotFoundDelegate entityNotFoundDelegate;
	private Boolean identifierRollbackSupport;
	private Boolean nullabilityChecking;
	private Boolean lazyInitializationOutsideTransaction;
	private Integer defaultBatchFetchSize;
	private Integer maximumFetchDepth;
	private Boolean subselectFetchEnabled;
	private Nulls defaultNullPrecedence;
	private Boolean orderingOfInserts;
	private Boolean orderingOfUpdates;
	private Boolean multiTenancy;
	private CurrentTenantIdentifierResolver<?> currentTenantIdentifierResolver;
	private TenantSchemaMapper<?> tenantSchemaMapper;
	private TenantCredentialsMapper<?> tenantCredentialsMapper;
	private Boolean namedQueryCheckingOnStartup;
	private Boolean secondLevelCacheSupport;
	private Boolean queryCacheSupport;
	private CacheLayout queryCacheLayout;
	private TimestampsCacheFactory timestampsCacheFactory;
	private String cacheRegionPrefix;
	private Boolean minimalPutsForCaching;
	private Boolean structuredCacheEntries;
	private Boolean directReferenceCaching;
	private Boolean automaticEvictionOfCollectionCaches;
	private Integer jdbcBatchSize;
	private Boolean scrollableResultsSupport;
	private Boolean getGeneratedKeysSupport;
	private Integer jdbcFetchSize;
	private PhysicalConnectionHandlingMode connectionHandlingMode;
	private Boolean connectionProviderDisablesAutoCommit;
	private Boolean sqlComments;
	private final Map<String, SqmFunctionDescriptor> sqlFunctions = new LinkedHashMap<>();
	private Boolean collectionsInDefaultFetchGroup;
	private TemporalTableStrategy temporalTableStrategy;
	private AuditStrategy auditStrategy;
	private Boolean outOfTransactionUpdateOperations;
	private Boolean jpaQueryCompliance;
	private Boolean jpaOrderByMappingCompliance;
	private Boolean jpaTransactionCompliance;
	private Boolean jpaClosedCompliance;
	private FormatMapper jsonFormatMapper;
	private FormatMapper xmlFormatMapper;
	private boolean jtaTransactionAccessDisabled;

	public SessionFactoryOptions buildOptions(SessionFactoryOptionsBuilder optionsBuilder) {
		applyTo( optionsBuilder );
		return optionsBuilder.buildOptions();
	}

	public ResolvedSessionFactorySettings applyTo(ResolvedSessionFactorySettings settings) {
		return new ResolvedSessionFactorySettings(
				settings.configurationValues(),
				settings.jpaBootstrap(),
				settings.serviceRegistry(),
				sessionFactoryName != null ? sessionFactoryName : settings.sessionFactoryName(),
				settings.sessionFactoryJndiName(),
				sessionFactoryNameAsJndiName != null
						? sessionFactoryNameAsJndiName
						: settings.sessionFactoryNameAlsoJndiName(),
				statementObserver != null ? statementObserver : settings.statementObserver(),
				statementInspector != null ? statementInspector : settings.statementInspector(),
				settings.initialSessionCacheMode(),
				settings.defaultCacheRetrieveMode(),
				settings.defaultCacheStoreMode(),
				settings.graphParserMode(),
				connectionHandlingMode != null ? connectionHandlingMode : settings.physicalConnectionHandlingMode(),
				settings.jdbcTimeZone(),
				autoFlushing != null ? autoFlushing : settings.flushBeforeCompletionEnabled(),
				autoClosing != null ? autoClosing : settings.autoCloseSessionEnabled(),
				identifierRollbackSupport != null ? identifierRollbackSupport : settings.identifierRollbackEnabled(),
				lazyInitializationOutsideTransaction != null
						? lazyInitializationOutsideTransaction
						: settings.initializeLazyStateOutsideTransactionsEnabled(),
				settings.unownedAssociationTransientCheck(),
				settings.bidirectionalAssociationManagementEnabled(),
				interceptor != null ? interceptor : settings.interceptor(),
				collectSessionFactoryObservers( settings ),
				validatorFactory != null ? validatorFactory : settings.validatorFactoryReference(),
				secondLevelCacheSupport != null ? secondLevelCacheSupport : settings.secondLevelCacheEnabled(),
				queryCacheSupport != null ? queryCacheSupport : settings.queryCacheEnabled(),
				queryCacheLayout != null ? queryCacheLayout : settings.queryCacheLayout(),
				timestampsCacheFactory != null ? timestampsCacheFactory : settings.timestampsCacheFactory(),
				cacheRegionPrefix != null ? cacheRegionPrefix : settings.cacheRegionPrefix(),
				minimalPutsForCaching != null ? minimalPutsForCaching : settings.minimalPutsEnabled(),
				structuredCacheEntries != null ? structuredCacheEntries : settings.structuredCacheEntriesEnabled(),
				directReferenceCaching != null ? directReferenceCaching : settings.directReferenceCacheEntriesEnabled(),
				automaticEvictionOfCollectionCaches != null
						? automaticEvictionOfCollectionCaches
						: settings.autoEvictCollectionCache(),
				collectSqlFunctions( settings ),
				settings.customSqmFunctionRegistry(),
				settings.customHqlTranslator(),
				settings.customSqmTranslatorFactory(),
				settings.customSqmMultiTableMutationStrategy(),
				settings.customSqmMultiTableInsertStrategy(),
				collectJpaCompliance( settings ),
				settings.criteriaValueHandlingMode(),
				settings.immutableEntityUpdateQueryHandlingMode(),
				settings.jsonFunctionsEnabled(),
				settings.xmlFunctionsEnabled(),
				settings.portableIntegerDivisionEnabled(),
				settings.nativeJdbcParametersIgnored(),
				settings.safeModeEnabled(),
				namedQueryCheckingOnStartup != null
						? namedQueryCheckingOnStartup
						: settings.namedQueryStartupCheckingEnabled(),
				collectionsInDefaultFetchGroup != null
						? collectionsInDefaultFetchGroup
						: settings.collectionsInDefaultFetchGroupEnabled(),
				settings.jpaCallbacksEnabled(),
				defaultBatchFetchSize != null ? defaultBatchFetchSize : settings.defaultBatchFetchSize(),
				maximumFetchDepth != null ? maximumFetchDepth : settings.maximumFetchDepth(),
				subselectFetchEnabled != null ? subselectFetchEnabled : settings.subselectFetchEnabled(),
				sqlComments != null ? sqlComments : settings.commentsEnabled(),
				temporalTableStrategy != null ? temporalTableStrategy : settings.temporalTableStrategy(),
				auditStrategy != null ? auditStrategy : settings.auditStrategy(),
				multiTenancy != null ? multiTenancy : settings.multiTenancyEnabled(),
				collectCurrentTenantIdentifierResolver( settings ),
				settings.defaultTenantIdentifierJavaType(),
				settings.defaultCatalog(),
				settings.defaultSchema()
		);
	}

	public void applyTo(SessionFactoryOptionsBuilder optionsBuilder) {
		if ( beanManager != null ) {
			optionsBuilder.applyBeanManager( beanManager );
		}
		if ( validatorFactory != null ) {
			optionsBuilder.applyValidatorFactory( validatorFactory );
		}
		if ( sessionFactoryName != null ) {
			optionsBuilder.applySessionFactoryName( sessionFactoryName );
		}
		if ( sessionFactoryNameAsJndiName != null ) {
			optionsBuilder.enableSessionFactoryNameAsJndiName( sessionFactoryNameAsJndiName );
		}
		if ( autoClosing != null ) {
			optionsBuilder.enableSessionAutoClosing( autoClosing );
		}
		if ( autoFlushing != null ) {
			optionsBuilder.enableSessionAutoFlushing( autoFlushing );
		}
		if ( jtaTrackingByThread != null ) {
			optionsBuilder.enableJtaTrackingByThread( jtaTrackingByThread );
		}
		if ( preferUserTransactions != null ) {
			optionsBuilder.enablePreferUserTransaction( preferUserTransactions );
		}
		if ( statisticsSupport != null ) {
			optionsBuilder.enableStatisticsSupport( statisticsSupport );
		}
		if ( !sessionFactoryObservers.isEmpty() ) {
			optionsBuilder.addSessionFactoryObservers(
					sessionFactoryObservers.toArray( SessionFactoryObserver[]::new )
			);
		}
		if ( interceptor != null ) {
			optionsBuilder.applyInterceptor( interceptor );
		}
		if ( statelessInterceptorClass != null ) {
			optionsBuilder.applyStatelessInterceptor( statelessInterceptorClass );
		}
		if ( statelessInterceptorSupplier != null ) {
			optionsBuilder.applyStatelessInterceptorSupplier( statelessInterceptorSupplier );
		}
		if ( statementObserver != null ) {
			optionsBuilder.applyStatementObserver( statementObserver );
		}
		if ( statementInspector != null ) {
			optionsBuilder.applyStatementInspector( statementInspector );
		}
		if ( customEntityDirtinessStrategy != null ) {
			optionsBuilder.applyCustomEntityDirtinessStrategy( customEntityDirtinessStrategy );
		}
		if ( !entityNameResolvers.isEmpty() ) {
			optionsBuilder.addEntityNameResolvers( entityNameResolvers.toArray( EntityNameResolver[]::new ) );
		}
		if ( entityNotFoundDelegate != null ) {
			optionsBuilder.applyEntityNotFoundDelegate( entityNotFoundDelegate );
		}
		if ( identifierRollbackSupport != null ) {
			optionsBuilder.enableIdentifierRollbackSupport( identifierRollbackSupport );
		}
		if ( nullabilityChecking != null ) {
			optionsBuilder.enableNullabilityChecking( nullabilityChecking );
		}
		if ( lazyInitializationOutsideTransaction != null ) {
			optionsBuilder.allowLazyInitializationOutsideTransaction( lazyInitializationOutsideTransaction );
		}
		if ( defaultBatchFetchSize != null ) {
			optionsBuilder.applyDefaultBatchFetchSize( defaultBatchFetchSize );
		}
		if ( maximumFetchDepth != null ) {
			optionsBuilder.applyMaximumFetchDepth( maximumFetchDepth );
		}
		if ( subselectFetchEnabled != null ) {
			optionsBuilder.applySubselectFetchEnabled( subselectFetchEnabled );
		}
		if ( defaultNullPrecedence != null ) {
			optionsBuilder.applyDefaultNullPrecedence( defaultNullPrecedence );
		}
		if ( orderingOfInserts != null ) {
			optionsBuilder.enableOrderingOfInserts( orderingOfInserts );
		}
		if ( orderingOfUpdates != null ) {
			optionsBuilder.enableOrderingOfUpdates( orderingOfUpdates );
		}
		if ( multiTenancy != null ) {
			optionsBuilder.applyMultiTenancy( multiTenancy );
		}
		if ( currentTenantIdentifierResolver != null ) {
			optionsBuilder.applyCurrentTenantIdentifierResolver( currentTenantIdentifierResolver );
		}
		if ( tenantSchemaMapper != null ) {
			optionsBuilder.applyTenantSchemaMapper( tenantSchemaMapper );
		}
		if ( tenantCredentialsMapper != null ) {
			optionsBuilder.applyTenantCredentialsMapper( tenantCredentialsMapper );
		}
		if ( namedQueryCheckingOnStartup != null ) {
			optionsBuilder.enableNamedQueryCheckingOnStartup( namedQueryCheckingOnStartup );
		}
		if ( secondLevelCacheSupport != null ) {
			optionsBuilder.enableSecondLevelCacheSupport( secondLevelCacheSupport );
		}
		if ( queryCacheSupport != null ) {
			optionsBuilder.enableQueryCacheSupport( queryCacheSupport );
		}
		if ( queryCacheLayout != null ) {
			optionsBuilder.applyQueryCacheLayout( queryCacheLayout );
		}
		if ( timestampsCacheFactory != null ) {
			optionsBuilder.applyTimestampsCacheFactory( timestampsCacheFactory );
		}
		if ( cacheRegionPrefix != null ) {
			optionsBuilder.applyCacheRegionPrefix( cacheRegionPrefix );
		}
		if ( minimalPutsForCaching != null ) {
			optionsBuilder.enableMinimalPuts( minimalPutsForCaching );
		}
		if ( structuredCacheEntries != null ) {
			optionsBuilder.enabledStructuredCacheEntries( structuredCacheEntries );
		}
		if ( directReferenceCaching != null ) {
			optionsBuilder.allowDirectReferenceCacheEntries( directReferenceCaching );
		}
		if ( automaticEvictionOfCollectionCaches != null ) {
			optionsBuilder.enableAutoEvictCollectionCaches( automaticEvictionOfCollectionCaches );
		}
		if ( jdbcBatchSize != null ) {
			optionsBuilder.applyJdbcBatchSize( jdbcBatchSize );
		}
		if ( scrollableResultsSupport != null ) {
			optionsBuilder.enableScrollableResultSupport( scrollableResultsSupport );
		}
		if ( getGeneratedKeysSupport != null ) {
			optionsBuilder.enableGeneratedKeysSupport( getGeneratedKeysSupport );
		}
		if ( jdbcFetchSize != null ) {
			optionsBuilder.applyJdbcFetchSize( jdbcFetchSize );
		}
		if ( connectionHandlingMode != null ) {
			optionsBuilder.applyConnectionHandlingMode( connectionHandlingMode );
		}
		if ( connectionProviderDisablesAutoCommit != null ) {
			optionsBuilder.applyConnectionProviderDisablesAutoCommit( connectionProviderDisablesAutoCommit );
		}
		if ( sqlComments != null ) {
			optionsBuilder.enableCommentsSupport( sqlComments );
		}
		sqlFunctions.forEach( optionsBuilder::applySqlFunction );
		if ( collectionsInDefaultFetchGroup != null ) {
			optionsBuilder.enableCollectionInDefaultFetchGroup( collectionsInDefaultFetchGroup );
		}
		if ( temporalTableStrategy != null ) {
			optionsBuilder.applyTemporalTableStrategy( temporalTableStrategy );
		}
		if ( auditStrategy != null ) {
			optionsBuilder.applyAuditStrategy( auditStrategy );
		}
		if ( outOfTransactionUpdateOperations != null ) {
			optionsBuilder.allowOutOfTransactionUpdateOperations( outOfTransactionUpdateOperations );
		}
		if ( jpaQueryCompliance != null ) {
			optionsBuilder.enableJpaQueryCompliance( jpaQueryCompliance );
		}
		if ( jpaOrderByMappingCompliance != null ) {
			optionsBuilder.enableJpaOrderByMappingCompliance( jpaOrderByMappingCompliance );
		}
		if ( jpaTransactionCompliance != null ) {
			optionsBuilder.enableJpaTransactionCompliance( jpaTransactionCompliance );
		}
		if ( jpaClosedCompliance != null ) {
			optionsBuilder.enableJpaClosedCompliance( jpaClosedCompliance );
		}
		if ( jsonFormatMapper != null ) {
			optionsBuilder.applyJsonFormatMapper( jsonFormatMapper );
		}
		if ( xmlFormatMapper != null ) {
			optionsBuilder.applyXmlFormatMapper( xmlFormatMapper );
		}
		if ( jtaTransactionAccessDisabled ) {
			optionsBuilder.disableJtaTransactionAccess();
		}
	}

	private SessionFactoryObserver[] collectSessionFactoryObservers(ResolvedSessionFactorySettings settings) {
		if ( sessionFactoryObservers.isEmpty() ) {
			return settings.sessionFactoryObservers();
		}

		final var observers = settings.sessionFactoryObservers();
		final var combined = Arrays.copyOf( observers, observers.length + sessionFactoryObservers.size() );
		for ( int i = 0; i < sessionFactoryObservers.size(); i++ ) {
			combined[observers.length + i] = sessionFactoryObservers.get( i );
		}
		return combined;
	}

	private Map<String, SqmFunctionDescriptor> collectSqlFunctions(ResolvedSessionFactorySettings settings) {
		if ( sqlFunctions.isEmpty() ) {
			return settings.customSqlFunctionMap();
		}

		final var functions = new LinkedHashMap<>( settings.customSqlFunctionMap() );
		functions.putAll( sqlFunctions );
		return functions;
	}

	private JpaCompliance collectJpaCompliance(ResolvedSessionFactorySettings settings) {
		final var jpaCompliance = settings.jpaCompliance();
		if ( jpaQueryCompliance == null
				&& jpaOrderByMappingCompliance == null
				&& jpaTransactionCompliance == null
				&& jpaClosedCompliance == null ) {
			return jpaCompliance;
		}

		return new JpaComplianceImpl(
				jpaOrderByMappingCompliance != null
						? jpaOrderByMappingCompliance
						: jpaCompliance.isJpaOrderByMappingComplianceEnabled(),
				jpaCompliance.isJpaProxyComplianceEnabled(),
				jpaCompliance.isGlobalGeneratorScopeEnabled(),
				jpaQueryCompliance != null ? jpaQueryCompliance : jpaCompliance.isJpaQueryComplianceEnabled(),
				jpaTransactionCompliance != null
						? jpaTransactionCompliance
						: jpaCompliance.isJpaTransactionComplianceEnabled(),
				jpaClosedCompliance != null ? jpaClosedCompliance : jpaCompliance.isJpaClosedComplianceEnabled(),
				jpaCompliance.isJpaCacheComplianceEnabled(),
				jpaCompliance.isLoadByIdComplianceEnabled(),
				jpaCompliance instanceof JpaComplianceImpl jpaComplianceImpl
						&& jpaComplianceImpl.cascadeCompliance()
		);
	}

	@SuppressWarnings("unchecked")
	private CurrentTenantIdentifierResolver<Object> collectCurrentTenantIdentifierResolver(
			ResolvedSessionFactorySettings settings) {
		return currentTenantIdentifierResolver != null
				? (CurrentTenantIdentifierResolver<Object>) currentTenantIdentifierResolver
				: settings.currentTenantIdentifierResolver();
	}

	public void applyBeanManager(Object beanManager) {
		this.beanManager = beanManager;
	}

	public void applyValidatorFactory(Object validatorFactory) {
		this.validatorFactory = validatorFactory;
	}

	public void applySessionFactoryName(String sessionFactoryName) {
		this.sessionFactoryName = sessionFactoryName;
	}

	public void enableSessionFactoryNameAsJndiName(boolean isJndiName) {
		this.sessionFactoryNameAsJndiName = isJndiName;
	}

	public void enableSessionAutoClosing(boolean enabled) {
		this.autoClosing = enabled;
	}

	public void enableSessionAutoFlushing(boolean enabled) {
		this.autoFlushing = enabled;
	}

	public void enableJtaTrackingByThread(boolean enabled) {
		this.jtaTrackingByThread = enabled;
	}

	public void enablePreferUserTransaction(boolean preferUserTransactions) {
		this.preferUserTransactions = preferUserTransactions;
	}

	public void enableStatisticsSupport(boolean enabled) {
		this.statisticsSupport = enabled;
	}

	public void addSessionFactoryObservers(SessionFactoryObserver... observers) {
		if ( observers != null ) {
			sessionFactoryObservers.addAll( List.of( observers ) );
		}
	}

	public void applyInterceptor(Interceptor interceptor) {
		this.interceptor = interceptor;
	}

	public void applyStatelessInterceptor(Class<? extends Interceptor> statelessInterceptorClass) {
		this.statelessInterceptorClass = statelessInterceptorClass;
		this.statelessInterceptorSupplier = null;
	}

	public void applyStatelessInterceptorSupplier(Supplier<? extends Interceptor> statelessInterceptorSupplier) {
		this.statelessInterceptorSupplier = statelessInterceptorSupplier;
		this.statelessInterceptorClass = null;
	}

	public void applyStatementObserver(StatementObserver statementObserver) {
		this.statementObserver = statementObserver;
	}

	public void applyStatementInspector(StatementInspector statementInspector) {
		this.statementInspector = statementInspector;
	}

	public void applyCustomEntityDirtinessStrategy(CustomEntityDirtinessStrategy strategy) {
		this.customEntityDirtinessStrategy = strategy;
	}

	public void addEntityNameResolvers(EntityNameResolver... entityNameResolvers) {
		if ( entityNameResolvers != null ) {
			this.entityNameResolvers.addAll( List.of( entityNameResolvers ) );
		}
	}

	public void applyEntityNotFoundDelegate(EntityNotFoundDelegate entityNotFoundDelegate) {
		this.entityNotFoundDelegate = entityNotFoundDelegate;
	}

	public void enableIdentifierRollbackSupport(boolean enabled) {
		this.identifierRollbackSupport = enabled;
	}

	public void enableNullabilityChecking(boolean enabled) {
		this.nullabilityChecking = enabled;
	}

	public void allowLazyInitializationOutsideTransaction(boolean enabled) {
		this.lazyInitializationOutsideTransaction = enabled;
	}

	public void applyDefaultBatchFetchSize(int size) {
		this.defaultBatchFetchSize = size;
	}

	public void applyMaximumFetchDepth(int depth) {
		this.maximumFetchDepth = depth;
	}

	public void applySubselectFetchEnabled(boolean enabled) {
		this.subselectFetchEnabled = enabled;
	}

	public void applyDefaultNullPrecedence(Nulls nullPrecedence) {
		this.defaultNullPrecedence = nullPrecedence;
	}

	public void enableOrderingOfInserts(boolean enabled) {
		this.orderingOfInserts = enabled;
	}

	public void enableOrderingOfUpdates(boolean enabled) {
		this.orderingOfUpdates = enabled;
	}

	public void applyMultiTenancy(boolean enabled) {
		this.multiTenancy = enabled;
	}

	public void applyCurrentTenantIdentifierResolver(CurrentTenantIdentifierResolver<?> resolver) {
		this.currentTenantIdentifierResolver = resolver;
	}

	public void applyTenantSchemaMapper(TenantSchemaMapper<?> mapper) {
		this.tenantSchemaMapper = mapper;
	}

	public void applyTenantCredentialsMapper(TenantCredentialsMapper<?> mapper) {
		this.tenantCredentialsMapper = mapper;
	}

	public void enableNamedQueryCheckingOnStartup(boolean enabled) {
		this.namedQueryCheckingOnStartup = enabled;
	}

	public void enableSecondLevelCacheSupport(boolean enabled) {
		this.secondLevelCacheSupport = enabled;
	}

	public void enableQueryCacheSupport(boolean enabled) {
		this.queryCacheSupport = enabled;
	}

	public void applyQueryCacheLayout(CacheLayout cacheLayout) {
		this.queryCacheLayout = cacheLayout;
	}

	public void applyTimestampsCacheFactory(TimestampsCacheFactory factory) {
		this.timestampsCacheFactory = factory;
	}

	public void applyCacheRegionPrefix(String prefix) {
		this.cacheRegionPrefix = prefix;
	}

	public void enableMinimalPuts(boolean enabled) {
		this.minimalPutsForCaching = enabled;
	}

	public void enabledStructuredCacheEntries(boolean enabled) {
		this.structuredCacheEntries = enabled;
	}

	public void allowDirectReferenceCacheEntries(boolean enabled) {
		this.directReferenceCaching = enabled;
	}

	public void enableAutoEvictCollectionCaches(boolean enabled) {
		this.automaticEvictionOfCollectionCaches = enabled;
	}

	public void applyJdbcBatchSize(int size) {
		this.jdbcBatchSize = size;
	}

	public void enableScrollableResultSupport(boolean enabled) {
		this.scrollableResultsSupport = enabled;
	}

	public void enableGeneratedKeysSupport(boolean enabled) {
		this.getGeneratedKeysSupport = enabled;
	}

	public void applyJdbcFetchSize(int size) {
		this.jdbcFetchSize = size;
	}

	public void applyConnectionHandlingMode(PhysicalConnectionHandlingMode connectionHandlingMode) {
		this.connectionHandlingMode = connectionHandlingMode;
	}

	public void applyConnectionProviderDisablesAutoCommit(boolean providerDisablesAutoCommit) {
		this.connectionProviderDisablesAutoCommit = providerDisablesAutoCommit;
	}

	public void enableCommentsSupport(boolean enabled) {
		this.sqlComments = enabled;
	}

	public void applySqlFunction(String registrationName, SqmFunctionDescriptor functionDescriptor) {
		sqlFunctions.put( registrationName, functionDescriptor );
	}

	public void enableCollectionInDefaultFetchGroup(boolean enabled) {
		this.collectionsInDefaultFetchGroup = enabled;
	}

	public void applyTemporalTableStrategy(TemporalTableStrategy strategy) {
		this.temporalTableStrategy = strategy;
	}

	public void applyAuditStrategy(AuditStrategy strategy) {
		this.auditStrategy = strategy;
	}

	public void allowOutOfTransactionUpdateOperations(boolean allow) {
		this.outOfTransactionUpdateOperations = allow;
	}

	public void enableJpaQueryCompliance(boolean enabled) {
		this.jpaQueryCompliance = enabled;
	}

	public void enableJpaOrderByMappingCompliance(boolean enabled) {
		this.jpaOrderByMappingCompliance = enabled;
	}

	public void enableJpaTransactionCompliance(boolean enabled) {
		this.jpaTransactionCompliance = enabled;
	}

	public void enableJpaClosedCompliance(boolean enabled) {
		this.jpaClosedCompliance = enabled;
	}

	public void applyJsonFormatMapper(FormatMapper jsonFormatMapper) {
		this.jsonFormatMapper = jsonFormatMapper;
	}

	public void applyXmlFormatMapper(FormatMapper xmlFormatMapper) {
		this.xmlFormatMapper = xmlFormatMapper;
	}

	public void disableJtaTransactionAccess() {
		this.jtaTransactionAccessDisabled = true;
	}
}
