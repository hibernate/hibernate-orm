/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.internal;

import java.lang.reflect.Constructor;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.PessimisticLockScope;
import org.hibernate.CacheMode;
import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.EntityNameResolver;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.LockOptions;
import org.hibernate.SessionEventListener;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.context.spi.TenantSchemaMapper;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.type.TimeZoneStorageStrategy;
import org.hibernate.annotations.CacheLayout;
import org.hibernate.boot.SchemaAutoTooling;
import org.hibernate.boot.TempTableDdlTransactionHandling;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.selector.spi.StrategySelectionException;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.boot.xsd.XmlValidationMode;
import org.hibernate.cache.internal.NoCachingRegionFactory;
import org.hibernate.cache.internal.StandardTimestampsCacheFactory;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.TimestampsCacheFactory;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentImpl;
import org.hibernate.engine.jdbc.env.spi.ExtractedDatabaseMetaData;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.id.uuid.LocalObjectUuidHelper;
import org.hibernate.internal.BaselineSessionEventsListenerBuilder;
import org.hibernate.internal.EmptyInterceptor;
import org.hibernate.internal.util.NullnessHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.jpa.HibernateHints;
import org.hibernate.jpa.LegacySpecHints;
import org.hibernate.jpa.SpecHints;
import org.hibernate.jpa.internal.util.CacheModeHelper;
import org.hibernate.jpa.spi.JpaCompliance;
import org.hibernate.jpa.spi.MutableJpaCompliance;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.query.spi.ImmutableEntityUpdateQueryHandlingMode;
import org.hibernate.query.NullPrecedence;
import org.hibernate.query.criteria.ValueHandlingMode;
import org.hibernate.query.hql.HqlTranslator;
import org.hibernate.query.internal.NullPrecedenceHelper;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.query.sqm.sql.SqmTranslatorFactory;
import org.hibernate.resource.beans.internal.Helper;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.stat.Statistics;
import org.hibernate.type.format.FormatMapper;
import org.hibernate.type.format.jaxb.JaxbXmlFormatMapper;

import jakarta.persistence.criteria.Nulls;

import static java.util.Collections.unmodifiableMap;
import static org.hibernate.Timeouts.WAIT_FOREVER_MILLI;
import static org.hibernate.cfg.AvailableSettings.*;
import static org.hibernate.cfg.DialectSpecificSettings.ORACLE_OSON_DISABLED;
import static org.hibernate.engine.config.spi.StandardConverters.BOOLEAN;
import static org.hibernate.internal.LockOptionsHelper.applyPropertiesToLockOptions;
import static org.hibernate.internal.log.DeprecationLogger.DEPRECATION_LOGGER;
import static org.hibernate.internal.util.PropertiesHelper.map;
import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.internal.util.config.ConfigurationHelper.extractPropertyValue;
import static org.hibernate.internal.util.config.ConfigurationHelper.getBoolean;
import static org.hibernate.internal.util.config.ConfigurationHelper.getInt;
import static org.hibernate.internal.util.config.ConfigurationHelper.getInteger;
import static org.hibernate.internal.util.config.ConfigurationHelper.getString;
import static org.hibernate.jpa.internal.util.CacheModeHelper.interpretCacheMode;
import static org.hibernate.jpa.internal.util.ConfigurationHelper.getFlushMode;
import static org.hibernate.type.format.jackson.JacksonIntegration.getJsonJacksonFormatMapperOrNull;
import static org.hibernate.type.format.jackson.JacksonIntegration.getOsonJacksonFormatMapperOrNull;
import static org.hibernate.type.format.jackson.JacksonIntegration.getXMLJacksonFormatMapperOrNull;
import static org.hibernate.type.format.jackson.JacksonIntegration.isJacksonOsonExtensionAvailable;
import static org.hibernate.type.format.jakartajson.JakartaJsonIntegration.getJakartaJsonBFormatMapperOrNull;

/**
 * In-flight state of {@link SessionFactoryOptions} during {@link org.hibernate.boot.SessionFactoryBuilder}
 * processing.
 * <p>
 * The intention is that {@code SessionFactoryBuilder} internally creates and populates this builder, which
 * is then used to construct the {@code SessionFactoryOptions} as part of building the {@code SessionFactory}
 * from {@link org.hibernate.boot.SessionFactoryBuilder#build}.
 *
 * @author Steve Ebersole
 */
public class SessionFactoryOptionsBuilder implements SessionFactoryOptions {

	private final String uuid = LocalObjectUuidHelper.generateLocalObjectUuid();
	private final StandardServiceRegistry serviceRegistry;

	// integration
	private Object beanManagerReference;
	private Object validatorFactoryReference;
	private FormatMapper jsonFormatMapper;
	private FormatMapper xmlFormatMapper;
	private final boolean xmlFormatMapperLegacyFormatEnabled;

	// SessionFactory behavior
	private final boolean jpaBootstrap;
	private String sessionFactoryName;
	private Boolean sessionFactoryNameAlsoJndiName;

	// Session behavior
	private boolean flushBeforeCompletionEnabled;
	private boolean autoCloseSessionEnabled;
	private boolean jtaTransactionAccessEnabled;
	private boolean allowOutOfTransactionUpdateOperations;

	// (JTA) transaction handling
	private boolean jtaTrackByThread;
	private boolean preferUserTransaction;

	// Statistics/Interceptor/observers
	private boolean statisticsEnabled;
	private Interceptor interceptor;
	private Supplier<? extends Interceptor> statelessInterceptorSupplier;
	private StatementInspector statementInspector;
	private final List<SessionFactoryObserver> sessionFactoryObserverList = new ArrayList<>();

	// persistence behavior
	private CustomEntityDirtinessStrategy customEntityDirtinessStrategy;
	private final List<EntityNameResolver> entityNameResolvers = new ArrayList<>();
	private EntityNotFoundDelegate entityNotFoundDelegate;
	private boolean identifierRollbackEnabled;
	private boolean checkNullability;
	private boolean initializeLazyStateOutsideTransactions;
	private int defaultBatchFetchSize;
	private Integer maximumFetchDepth;
	private boolean subselectFetchEnabled;
	private Nulls defaultNullPrecedence;
	private boolean orderUpdatesEnabled;
	private boolean orderInsertsEnabled;
	private boolean collectionsInDefaultFetchGroupEnabled = true;
	private final boolean unownedAssociationTransientCheck;
	private final boolean passProcedureParameterNames;
	private final boolean preferJdbcDatetimeTypes;

	// JPA callbacks
	private final boolean callbacksEnabled;

	// multi-tenancy
	private boolean multiTenancyEnabled;
	private CurrentTenantIdentifierResolver<Object> currentTenantIdentifierResolver;
	private TenantSchemaMapper<Object> tenantSchemaMapper;

	// Queries
	private SqmFunctionRegistry sqmFunctionRegistry;
	private final HqlTranslator hqlTranslator;
	private final SqmMultiTableMutationStrategy sqmMultiTableMutationStrategy;
	private final SqmMultiTableInsertStrategy sqmMultiTableInsertStrategy;
	private final Constructor<SqmMultiTableMutationStrategy> sqmMultiTableMutationStrategyConstructor;
	private final Constructor<SqmMultiTableInsertStrategy> sqmMultiTableInsertStrategyConstructor;
	private final SqmTranslatorFactory sqmTranslatorFactory;
	private final Boolean useOfJdbcNamedParametersEnabled;
	private boolean namedQueryStartupCheckingEnabled;
	private final boolean preferJavaTimeJdbcTypes;
	private final boolean preferNativeEnumTypes;
	private final int preferredSqlTypeCodeForBoolean;
	private final int preferredSqlTypeCodeForDuration;
	private final int preferredSqlTypeCodeForUuid;
	private final int preferredSqlTypeCodeForInstant;
	private final int preferredSqlTypeCodeForArray;
	private final TimeZoneStorageStrategy defaultTimeZoneStorageStrategy;

	// Caching
	private boolean secondLevelCacheEnabled;
	private boolean queryCacheEnabled;
	private CacheLayout queryCacheLayout;
	private TimestampsCacheFactory timestampsCacheFactory;
	private String cacheRegionPrefix;
	private boolean minimalPutsEnabled;
	private boolean structuredCacheEntriesEnabled;
	private boolean directReferenceCacheEntriesEnabled;
	private boolean autoEvictCollectionCache;

	// JDBC Handling
	private boolean getGeneratedKeysEnabled;
	private int jdbcBatchSize;
	private Integer jdbcFetchSize;
	private boolean scrollableResultSetsEnabled;
	private boolean commentsEnabled;
	private PhysicalConnectionHandlingMode connectionHandlingMode;
	private boolean connectionProviderDisablesAutoCommit;
	private final TimeZone jdbcTimeZone;
	private final ValueHandlingMode criteriaValueHandlingMode;
	private final boolean criteriaCopyTreeEnabled;
	private final boolean criteriaPlanCacheEnabled;
	private final boolean nativeJdbcParametersIgnored;
	private final ImmutableEntityUpdateQueryHandlingMode immutableEntityUpdateQueryHandlingMode;
	// These two settings cannot be modified from the builder,
	// in order to maintain consistency.
	// Indeed, other components (the schema tools) also make use of these settings,
	// and THOSE do not have access to session factory options.
	private final String defaultCatalog;
	private final String defaultSchema;

	private Map<String, SqmFunctionDescriptor> sqlFunctions;

	private JpaCompliance jpaCompliance;

	private final boolean failOnPaginationOverCollectionFetchEnabled;
	private final boolean inClauseParameterPaddingEnabled;

	private final boolean portableIntegerDivisionEnabled;
	private final boolean jsonFunctionsEnabled;
	private final boolean xmlFunctionsEnabled;

	private final int queryStatisticsMaxSize;

	private XmlValidationMode xmlValidationMode;
	private final Map<String, Object> defaultSessionProperties;
	private final CacheStoreMode defaultCacheStoreMode;
	private final CacheRetrieveMode defaultCacheRetrieveMode;
	private final CacheMode initialSessionCacheMode;
	private final FlushMode initialSessionFlushMode;
	private final LockOptions defaultLockOptions;

	// deprecated stuff
	@Deprecated
	private TempTableDdlTransactionHandling tempTableDdlTransactionHandling;
	@Deprecated(forRemoval = true)
	private final BaselineSessionEventsListenerBuilder baselineSessionEventsListenerBuilder;
	@Deprecated(forRemoval = true)
	private SchemaAutoTooling schemaAutoTooling;
	@Deprecated(forRemoval = true)
	private boolean delayBatchFetchLoaderCreations;
	@Deprecated(forRemoval = true)
	private boolean releaseResourcesOnCloseEnabled;

	@SuppressWarnings( "unchecked" )
	public SessionFactoryOptionsBuilder(StandardServiceRegistry serviceRegistry, BootstrapContext context) {
		this.serviceRegistry = serviceRegistry;
		jpaBootstrap = context.isJpaBootstrap();

		// we cannot use context.getConfigurationService() here because it might be missing some settings
		// (the StandardServiceRegistry passed in here does not need to be the bootstrap service registry)
		final ConfigurationService configurationService = serviceRegistry.requireService( ConfigurationService.class );

		final StrategySelector strategySelector = serviceRegistry.requireService( StrategySelector.class );
		final JdbcServices jdbcServices = serviceRegistry.requireService( JdbcServices.class );

		final Dialect dialect = jdbcServices.getJdbcEnvironment().getDialect();

		final Map<String,Object> settings = new HashMap<>();
		settings.putAll( map( dialect.getDefaultProperties() ) );
		settings.putAll( configurationService.getSettings() );

		beanManagerReference = NullnessHelper.coalesceSuppliedValues(
				() -> settings.get( JAKARTA_CDI_BEAN_MANAGER ),
				() -> {
					final Object value = settings.get( CDI_BEAN_MANAGER );
					if ( value != null ) {
						DEPRECATION_LOGGER.deprecatedSetting( CDI_BEAN_MANAGER,
								JAKARTA_CDI_BEAN_MANAGER );
					}
					return value;
				}
		);

		validatorFactoryReference = settings.getOrDefault(
				JPA_VALIDATION_FACTORY,
				settings.get( JAKARTA_VALIDATION_FACTORY )
		);

		jsonFormatMapper = jsonFormatMapper(
				settings.get( JSON_FORMAT_MAPPER ),
				!getBoolean( ORACLE_OSON_DISABLED, settings),
				strategySelector
		);

		xmlFormatMapper = xmlFormatMapper(
				settings.get( XML_FORMAT_MAPPER ),
				strategySelector,
				xmlFormatMapperLegacyFormatEnabled =
						context.getMetadataBuildingOptions()
								.isXmlFormatMapperLegacyFormatEnabled()
		);

		sessionFactoryName = (String) settings.get( SESSION_FACTORY_NAME );
		sessionFactoryNameAlsoJndiName =
				configurationService.getSetting( SESSION_FACTORY_NAME_IS_JNDI, BOOLEAN, true );
		jtaTransactionAccessEnabled =
				configurationService.getSetting( ALLOW_JTA_TRANSACTION_ACCESS, BOOLEAN, true );

		flushBeforeCompletionEnabled =
				configurationService.getSetting( FLUSH_BEFORE_COMPLETION, BOOLEAN, true );
		autoCloseSessionEnabled =
				configurationService.getSetting( AUTO_CLOSE_SESSION, BOOLEAN, false );

		statisticsEnabled =
				configurationService.getSetting( GENERATE_STATISTICS, BOOLEAN, false );

		interceptor = determineInterceptor( settings, strategySelector );
		statelessInterceptorSupplier = determineStatelessInterceptor( settings, strategySelector );

		statementInspector =
				strategySelector.resolveStrategy( StatementInspector.class,
						settings.get( STATEMENT_INSPECTOR ) );


		baselineSessionEventsListenerBuilder =
				new BaselineSessionEventsListenerBuilder( getAutoSessionEventsListener( settings, strategySelector ) );

		customEntityDirtinessStrategy =
				strategySelector.resolveDefaultableStrategy( CustomEntityDirtinessStrategy.class,
						settings.get( CUSTOM_ENTITY_DIRTINESS_STRATEGY ),
						DefaultCustomEntityDirtinessStrategy.INSTANCE );

		entityNotFoundDelegate = StandardEntityNotFoundDelegate.INSTANCE;

		identifierRollbackEnabled =
				configurationService.getSetting( USE_IDENTIFIER_ROLLBACK, BOOLEAN, false );
		checkNullability =
				configurationService.getSetting( CHECK_NULLABILITY, BOOLEAN, true );
		initializeLazyStateOutsideTransactions =
				configurationService.getSetting( ENABLE_LAZY_LOAD_NO_TRANS, BOOLEAN, false );

		multiTenancyEnabled = JdbcEnvironmentImpl.isMultiTenancyEnabled( serviceRegistry );
		currentTenantIdentifierResolver =
				strategySelector.resolveStrategy( CurrentTenantIdentifierResolver.class,
						settings.get( MULTI_TENANT_IDENTIFIER_RESOLVER ) );
		if ( currentTenantIdentifierResolver == null ) {
			currentTenantIdentifierResolver = Helper.getBean(
				Helper.getBeanContainer( serviceRegistry ),
				CurrentTenantIdentifierResolver.class,
				true,
				false,
				null
			);
		}
		tenantSchemaMapper =
				strategySelector.resolveStrategy( TenantSchemaMapper.class,
						settings.get( MULTI_TENANT_SCHEMA_MAPPER ) );

		delayBatchFetchLoaderCreations =
				configurationService.getSetting( DELAY_ENTITY_LOADER_CREATIONS, BOOLEAN, true );

		defaultBatchFetchSize = getInt( DEFAULT_BATCH_FETCH_SIZE, settings, -1 );
		subselectFetchEnabled = getBoolean( USE_SUBSELECT_FETCH, settings );
		maximumFetchDepth = getInteger( MAX_FETCH_DEPTH, settings );

		defaultNullPrecedence = getDefaultNullPrecedence( settings.get( DEFAULT_NULL_ORDERING ) );

		orderUpdatesEnabled = getBoolean( ORDER_UPDATES, settings );
		orderInsertsEnabled = getBoolean( ORDER_INSERTS, settings );

		callbacksEnabled = getBoolean( JPA_CALLBACKS_ENABLED, settings, true );

		jtaTrackByThread = configurationService.getSetting( JTA_TRACK_BY_THREAD, BOOLEAN, true );

		final String hqlTranslatorImplFqn =
				extractPropertyValue( SEMANTIC_QUERY_PRODUCER, settings );
		hqlTranslator = resolveHqlTranslator( hqlTranslatorImplFqn, serviceRegistry, strategySelector );

		final String sqmTranslatorFactoryImplFqn =
				extractPropertyValue( SEMANTIC_QUERY_TRANSLATOR, settings );
		sqmTranslatorFactory = resolveSqmTranslator( sqmTranslatorFactoryImplFqn, strategySelector );

		final String sqmMutationStrategyImplName =
				extractPropertyValue( QUERY_MULTI_TABLE_MUTATION_STRATEGY, settings );
		sqmMultiTableMutationStrategy =
				resolveSqmMutationStrategy( sqmMutationStrategyImplName, serviceRegistry, strategySelector );
		sqmMultiTableMutationStrategyConstructor =
				resolveSqmMutationStrategyConstructor( sqmMutationStrategyImplName, strategySelector );
		final String sqmInsertStrategyImplName =
				extractPropertyValue( QUERY_MULTI_TABLE_INSERT_STRATEGY, settings );
		sqmMultiTableInsertStrategy =
				resolveSqmInsertStrategy( sqmInsertStrategyImplName, serviceRegistry, strategySelector );
		sqmMultiTableInsertStrategyConstructor =
				resolveSqmInsertStrategyConstructor( sqmInsertStrategyImplName, strategySelector );

		useOfJdbcNamedParametersEnabled =
				configurationService.getSetting( CALLABLE_NAMED_PARAMS_ENABLED, BOOLEAN, true );

		namedQueryStartupCheckingEnabled =
				configurationService.getSetting( QUERY_STARTUP_CHECKING, BOOLEAN, true );

		preferJavaTimeJdbcTypes = MetadataBuildingContext.isPreferJavaTimeJdbcTypesEnabled( configurationService );
		preferNativeEnumTypes = MetadataBuildingContext.isPreferNativeEnumTypesEnabled( configurationService );
		preferredSqlTypeCodeForBoolean = ConfigurationHelper.getPreferredSqlTypeCodeForBoolean( serviceRegistry );
		preferredSqlTypeCodeForDuration = ConfigurationHelper.getPreferredSqlTypeCodeForDuration( serviceRegistry );
		preferredSqlTypeCodeForUuid = ConfigurationHelper.getPreferredSqlTypeCodeForUuid( serviceRegistry );
		preferredSqlTypeCodeForInstant = ConfigurationHelper.getPreferredSqlTypeCodeForInstant( serviceRegistry );
		preferredSqlTypeCodeForArray = ConfigurationHelper.getPreferredSqlTypeCodeForArray( serviceRegistry );
		defaultTimeZoneStorageStrategy = context.getMetadataBuildingOptions().getDefaultTimeZoneStorage();

		final RegionFactory regionFactory = serviceRegistry.getService( RegionFactory.class );
		if ( !(regionFactory instanceof NoCachingRegionFactory) ) {
			secondLevelCacheEnabled =
					configurationService.getSetting( USE_SECOND_LEVEL_CACHE, BOOLEAN, true );
			queryCacheEnabled =
					configurationService.getSetting( USE_QUERY_CACHE, BOOLEAN, false );
			cacheRegionPrefix = extractPropertyValue( CACHE_REGION_PREFIX, settings );
			queryCacheLayout =
					configurationService.getSetting( QUERY_CACHE_LAYOUT,
							value -> CacheLayout.valueOf( value.toString().toUpperCase( Locale.ROOT ) ),
							CacheLayout.FULL );
			timestampsCacheFactory =
					strategySelector.resolveDefaultableStrategy( TimestampsCacheFactory.class,
							settings.get( QUERY_CACHE_FACTORY ), StandardTimestampsCacheFactory.INSTANCE );
			minimalPutsEnabled =
					configurationService.getSetting( USE_MINIMAL_PUTS, BOOLEAN, regionFactory.isMinimalPutsEnabledByDefault() );
			structuredCacheEntriesEnabled =
					configurationService.getSetting( USE_STRUCTURED_CACHE, BOOLEAN, false );
			directReferenceCacheEntriesEnabled =
					configurationService.getSetting( USE_DIRECT_REFERENCE_CACHE_ENTRIES, BOOLEAN, false );
			autoEvictCollectionCache =
					configurationService.getSetting( AUTO_EVICT_COLLECTION_CACHE, BOOLEAN, false );
		}
		else {
			secondLevelCacheEnabled = false;
			queryCacheEnabled = false;
			queryCacheLayout = CacheLayout.AUTO;
			timestampsCacheFactory = null;
			cacheRegionPrefix = null;
			minimalPutsEnabled = false;
			structuredCacheEntriesEnabled = false;
			directReferenceCacheEntriesEnabled = false;
			autoEvictCollectionCache = false;
		}

		// deprecated, delete this:
		try {
			schemaAutoTooling = SchemaAutoTooling.interpret( (String) settings.get( HBM2DDL_AUTO ) );
		}
		catch (Exception e) {
			// ignore, since this member is deprecated and ignored
		}

		final var meta = jdbcServices.getExtractedMetaDataSupport();

		// deprecated
		tempTableDdlTransactionHandling = getTempTableDdlTransactionHandling( meta );

		jdbcBatchSize = disallowBatchUpdates( dialect, meta ) ? 0
				: getInt( STATEMENT_BATCH_SIZE, settings, 1 );

		scrollableResultSetsEnabled =
				getBoolean( USE_SCROLLABLE_RESULTSET, settings, meta.supportsScrollableResults() );
		getGeneratedKeysEnabled =
				getBoolean( USE_GET_GENERATED_KEYS, settings, meta.supportsGetGeneratedKeys() );

		jdbcFetchSize = getInteger( STATEMENT_FETCH_SIZE, settings );

		connectionHandlingMode = interpretConnectionHandlingMode( settings, serviceRegistry );

		connectionProviderDisablesAutoCommit =
				getBoolean( CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT, settings, false );

		commentsEnabled = getBoolean( USE_SQL_COMMENTS, settings );

		preferUserTransaction = getBoolean( PREFER_USER_TRANSACTION, settings  );

		allowOutOfTransactionUpdateOperations = getBoolean( ALLOW_UPDATE_OUTSIDE_TRANSACTION, settings );

		releaseResourcesOnCloseEnabled = getBoolean( DISCARD_PC_ON_CLOSE, settings );
		if ( releaseResourcesOnCloseEnabled) {
			DEPRECATION_LOGGER.deprecatedSetting( DISCARD_PC_ON_CLOSE );
		}

		jdbcTimeZone = getJdbcTimeZone( settings.get( JDBC_TIME_ZONE ) );

		criteriaValueHandlingMode = ValueHandlingMode.interpret( settings.get( CRITERIA_VALUE_HANDLING_MODE ) );
		criteriaCopyTreeEnabled = getBoolean( CRITERIA_COPY_TREE, settings, jpaBootstrap );
		criteriaPlanCacheEnabled = getBoolean( CRITERIA_PLAN_CACHE_ENABLED, settings, false );

		nativeJdbcParametersIgnored =
				getBoolean( NATIVE_IGNORE_JDBC_PARAMETERS, settings, false );

		// added the boolean parameter in case we want to define some form of "all" as discussed
		jpaCompliance = context.getJpaCompliance();

		failOnPaginationOverCollectionFetchEnabled = getBoolean( FAIL_ON_PAGINATION_OVER_COLLECTION_FETCH, settings );

		immutableEntityUpdateQueryHandlingMode =
				ImmutableEntityUpdateQueryHandlingMode.interpret(
						settings.get( IMMUTABLE_ENTITY_UPDATE_QUERY_HANDLING_MODE ) );

		defaultCatalog = getString( DEFAULT_CATALOG, settings );
		defaultSchema = getString( DEFAULT_SCHEMA, settings );

		inClauseParameterPaddingEnabled = getBoolean( IN_CLAUSE_PARAMETER_PADDING, settings );

		portableIntegerDivisionEnabled = getBoolean( PORTABLE_INTEGER_DIVISION, settings );

		jsonFunctionsEnabled = getBoolean( JSON_FUNCTIONS_ENABLED, settings );
		xmlFunctionsEnabled = getBoolean( XML_FUNCTIONS_ENABLED, settings );

		queryStatisticsMaxSize =
				getInt( QUERY_STATISTICS_MAX_SIZE, settings, Statistics.DEFAULT_QUERY_STATISTICS_MAX_SIZE );

		unownedAssociationTransientCheck =
				getBoolean( UNOWNED_ASSOCIATION_TRANSIENT_CHECK, settings, isJpaBootstrap() );

		passProcedureParameterNames =
				getBoolean( QUERY_PASS_PROCEDURE_PARAMETER_NAMES, settings, false );

		preferJdbcDatetimeTypes =
				getBoolean( NATIVE_PREFER_JDBC_DATETIME_TYPES, settings, false );

		defaultSessionProperties = initializeDefaultSessionProperties( configurationService );

		defaultCacheStoreMode = defaultCacheStoreMode( defaultSessionProperties );
		defaultCacheRetrieveMode = defaultCacheRetrieveMode( defaultSessionProperties );
		initialSessionCacheMode = interpretCacheMode( defaultCacheStoreMode, defaultCacheRetrieveMode );

		defaultLockOptions = defaultLockOptions( defaultSessionProperties );
		initialSessionFlushMode = defaultFlushMode( defaultSessionProperties );

		xmlValidationMode = ConfigurationHelper.resolveXmlValidationMode( settings );
	}

	@Deprecated(forRemoval = true)
	private static TempTableDdlTransactionHandling getTempTableDdlTransactionHandling(ExtractedDatabaseMetaData meta) {
		if ( meta.doesDataDefinitionCauseTransactionCommit() ) {
			return meta.supportsDataDefinitionInTransaction()
					? TempTableDdlTransactionHandling.ISOLATE_AND_TRANSACT
					: TempTableDdlTransactionHandling.ISOLATE;
		}
		else {
			return TempTableDdlTransactionHandling.NONE;
		}
	}

	private TimeZone getJdbcTimeZone(Object jdbcTimeZoneValue) {
		if ( jdbcTimeZoneValue instanceof TimeZone timeZone ) {
			return timeZone;
		}
		else if ( jdbcTimeZoneValue instanceof ZoneId zoneId ) {
			return TimeZone.getTimeZone( zoneId );
		}
		else if ( jdbcTimeZoneValue instanceof String string ) {
			return TimeZone.getTimeZone( ZoneId.of( string ) );
		}
		else if ( jdbcTimeZoneValue != null ) {
			throw new IllegalArgumentException( "Configuration property " + JDBC_TIME_ZONE
												+ " value [" + jdbcTimeZoneValue + "] is not supported" );
		}
		else {
			return null;
		}
	}

	private Nulls getDefaultNullPrecedence(Object defaultNullPrecedence) {
		if ( defaultNullPrecedence instanceof Nulls jpaValue ) {
			return jpaValue;
		}
		else if ( defaultNullPrecedence instanceof NullPrecedence hibernateValue ) {
			return hibernateValue.getJpaValue();
		}
		else if ( defaultNullPrecedence instanceof String string ) {
			return NullPrecedenceHelper.parse( string );
		}
		else if ( defaultNullPrecedence != null ) {
			throw new IllegalArgumentException( "Configuration property " + DEFAULT_NULL_ORDERING
					+ " value [" + defaultNullPrecedence + "] is not supported" );
		}
		else {
			return null;
		}
	}

	private static Class<? extends SessionEventListener> getAutoSessionEventsListener(Map<String, Object> configurationSettings, StrategySelector strategySelector) {
		// todo : expose this from builder?
		final String name = (String) configurationSettings.get( AUTO_SESSION_EVENTS_LISTENER );
		return name == null ? null : strategySelector.selectStrategyImplementor( SessionEventListener.class, name );
	}

	private static boolean disallowBatchUpdates(Dialect dialect, ExtractedDatabaseMetaData meta) {
		final Boolean dialectAnswer = dialect.supportsBatchUpdates();
		return dialectAnswer != null ? !dialectAnswer : !meta.supportsBatchUpdates();
	}

	@SuppressWarnings("unchecked")
	private SqmMultiTableMutationStrategy resolveSqmMutationStrategy(
			String strategyName,
			StandardServiceRegistry serviceRegistry,
			StrategySelector strategySelector) {
		if ( strategyName == null ) {
			return null;
		}

		return strategySelector.resolveStrategy(
				SqmMultiTableMutationStrategy.class,
				strategyName,
				(SqmMultiTableMutationStrategy) null,
				strategyClass -> {
					Constructor<? extends SqmMultiTableMutationStrategy> dialectConstructor = null;
					Constructor<? extends SqmMultiTableMutationStrategy> emptyConstructor = null;
					Constructor<SqmMultiTableMutationStrategy> entityBasedConstructor = null;
					// todo (6.0) : formalize the allowed constructor parameterizations
					for ( var declaredConstructor : strategyClass.getDeclaredConstructors() ) {
						final var parameterTypes = declaredConstructor.getParameterTypes();
						final var constructor =
								(Constructor<? extends SqmMultiTableMutationStrategy>)
										declaredConstructor;
						if ( parameterTypes.length == 1 && parameterTypes[0] == Dialect.class ) {
							dialectConstructor = constructor;
							break;
						}
						else if ( parameterTypes.length == 0 ) {
							emptyConstructor = constructor;
						}
						else if ( parameterTypes.length == 2 && parameterTypes[0] == EntityMappingType.class && parameterTypes[1] == RuntimeModelCreationContext.class ) {
							entityBasedConstructor = (Constructor<SqmMultiTableMutationStrategy>) declaredConstructor;
						}
					}

					if ( entityBasedConstructor == null ) {
						try {
							if ( dialectConstructor != null ) {
								return dialectConstructor.newInstance(
										serviceRegistry.requireService( JdbcServices.class ).getDialect()
								);
							}
							else if ( emptyConstructor != null ) {
								return emptyConstructor.newInstance();
							}
						}
						catch (Exception e) {
							throw new StrategySelectionException(
									"Could not instantiate named strategy class [" +
											strategyClass.getName() + "]",
									e
							);
						}
						throw new IllegalArgumentException(
								"Cannot instantiate the class [" + strategyClass.getName() + "] because it does not have a constructor that accepts a dialect or an empty constructor" );
					}
					else {
						return null;
					}
				}
		);
	}

	@SuppressWarnings("unchecked")
	private Constructor<SqmMultiTableMutationStrategy> resolveSqmMutationStrategyConstructor(
			String strategyName,
			StrategySelector strategySelector) {
		if ( strategyName == null ) {
			return null;
		}

		Class<? extends SqmMultiTableMutationStrategy> strategyClass =
				strategySelector.selectStrategyImplementor( SqmMultiTableMutationStrategy.class, strategyName );
		for ( Constructor<?> declaredConstructor : strategyClass.getDeclaredConstructors() ) {
			final Class<?>[] parameterTypes = declaredConstructor.getParameterTypes();
			if ( parameterTypes.length == 2 && parameterTypes[0] == EntityMappingType.class && parameterTypes[1] == RuntimeModelCreationContext.class ) {
				return (Constructor<SqmMultiTableMutationStrategy>) declaredConstructor;
			}
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	private SqmMultiTableInsertStrategy resolveSqmInsertStrategy(
			String strategyName,
			StandardServiceRegistry serviceRegistry,
			StrategySelector strategySelector) {
		if ( strategyName == null ) {
			return null;
		}

		return strategySelector.resolveStrategy(
				SqmMultiTableInsertStrategy.class,
				strategyName,
				(SqmMultiTableInsertStrategy) null,
				strategyClass -> {
					Constructor<? extends SqmMultiTableInsertStrategy> dialectConstructor = null;
					Constructor<? extends SqmMultiTableInsertStrategy> emptyConstructor = null;
					Constructor<SqmMultiTableInsertStrategy> entityBasedConstructor = null;
					// todo (6.0) : formalize the allowed constructor parameterizations
					for ( var declaredConstructor : strategyClass.getDeclaredConstructors() ) {
						final var parameterTypes = declaredConstructor.getParameterTypes();
						final var constructor =
								(Constructor<? extends SqmMultiTableInsertStrategy>)
										declaredConstructor;
						if ( parameterTypes.length == 1 && parameterTypes[0] == Dialect.class ) {
							dialectConstructor = constructor;
							break;
						}
						else if ( parameterTypes.length == 0 ) {
							emptyConstructor = constructor;
						}
						else if ( parameterTypes.length == 2 && parameterTypes[0] == EntityMappingType.class && parameterTypes[1] == RuntimeModelCreationContext.class ) {
							entityBasedConstructor = (Constructor<SqmMultiTableInsertStrategy>) declaredConstructor;
						}
					}

					if ( entityBasedConstructor == null ) {
						try {
							if ( dialectConstructor != null ) {
								return dialectConstructor.newInstance(
										serviceRegistry.requireService( JdbcServices.class ).getDialect()
								);
							}
							else if ( emptyConstructor != null ) {
								return emptyConstructor.newInstance();
							}
						}
						catch (Exception e) {
							throw new StrategySelectionException(
									"Could not instantiate named strategy class [" +
											strategyClass.getName() + "]",
									e
							);
						}
						throw new IllegalArgumentException(
								"Cannot instantiate the class [" + strategyClass.getName() + "] because it does not have a constructor that accepts a dialect or an empty constructor" );
					}
					else {
						return null;
					}
				}
		);
	}

	@SuppressWarnings("unchecked")
	private Constructor<SqmMultiTableInsertStrategy> resolveSqmInsertStrategyConstructor(
			String strategyName,
			StrategySelector strategySelector) {
		if ( strategyName == null ) {
			return null;
		}

		Class<? extends SqmMultiTableInsertStrategy> strategyClass =
				strategySelector.selectStrategyImplementor( SqmMultiTableInsertStrategy.class, strategyName );
		for ( Constructor<?> declaredConstructor : strategyClass.getDeclaredConstructors() ) {
			final Class<?>[] parameterTypes = declaredConstructor.getParameterTypes();
			if ( parameterTypes.length == 2 && parameterTypes[0] == EntityMappingType.class && parameterTypes[1] == RuntimeModelCreationContext.class ) {
				return (Constructor<SqmMultiTableInsertStrategy>) declaredConstructor;
			}
		}

		return null;
	}

	private HqlTranslator resolveHqlTranslator(
			String producerName,
			StandardServiceRegistry serviceRegistry,
			StrategySelector strategySelector) {
		return isEmpty( producerName )
				? null
				: strategySelector.<HqlTranslator>resolveDefaultableStrategy(
						HqlTranslator.class,
						producerName,
						() -> (HqlTranslator)
								serviceRegistry.requireService( ClassLoaderService.class )
										.classForName( producerName ).newInstance()
				);
	}

	private SqmTranslatorFactory resolveSqmTranslator(
			String translatorImplFqn,
			StrategySelector strategySelector) {
		if ( isEmpty( translatorImplFqn ) ) {
			return null;
		}
		else {
			return strategySelector.resolveStrategy(
					SqmTranslatorFactory.class,
					translatorImplFqn
			);
		}
	}

	private static Interceptor determineInterceptor(
			Map<String,Object> configurationSettings,
			StrategySelector strategySelector) {
		return strategySelector.resolveStrategy(
				Interceptor.class,
				configurationSettings.get( INTERCEPTOR )
		);
	}

	@SuppressWarnings("unchecked")
	private static Supplier<? extends Interceptor> determineStatelessInterceptor(
			Map<String,Object> configurationSettings,
			StrategySelector strategySelector) {
		final Object setting = configurationSettings.get( SESSION_SCOPED_INTERCEPTOR );
		if ( setting == null ) {
			return null;
		}
		else if ( setting instanceof Supplier ) {
			return (Supplier<? extends Interceptor>) setting;
		}
		else if ( setting instanceof Class ) {
			return interceptorSupplier( (Class<? extends Interceptor>) setting );
		}
		else {
			return interceptorSupplier(
					strategySelector.selectStrategyImplementor(
							Interceptor.class,
							setting.toString()
					)
			);
		}
	}


	private static Supplier<? extends Interceptor> interceptorSupplier(Class<? extends Interceptor> clazz) {
		return () -> {
			try {
				return clazz.newInstance();
			}
			catch (InstantiationException | IllegalAccessException e) {
				throw new org.hibernate.InstantiationException( "Could not instantiate session-scoped Interceptor", clazz, e );
			}
		};
	}

	private PhysicalConnectionHandlingMode interpretConnectionHandlingMode(
			Map<String,Object> configurationSettings,
			StandardServiceRegistry serviceRegistry) {
		final PhysicalConnectionHandlingMode specifiedHandlingMode =
				PhysicalConnectionHandlingMode.interpret( configurationSettings.get( CONNECTION_HANDLING ) );
		return specifiedHandlingMode != null
				? specifiedHandlingMode
				: serviceRegistry.requireService( TransactionCoordinatorBuilder.class )
						.getDefaultConnectionHandlingMode();
	}

	private static FormatMapper jsonFormatMapper(Object setting, boolean osonExtensionEnabled, StrategySelector selector) {
		return formatMapper(
				setting,
				selector,
				() -> {
					// Prefer the OSON Jackson FormatMapper by default if available
					final FormatMapper jsonJacksonFormatMapper =
							osonExtensionEnabled && isJacksonOsonExtensionAvailable()
									? getOsonJacksonFormatMapperOrNull()
									: getJsonJacksonFormatMapperOrNull();
					return jsonJacksonFormatMapper != null
							? jsonJacksonFormatMapper
							: getJakartaJsonBFormatMapperOrNull();
				}
		);
	}

	private static FormatMapper xmlFormatMapper(Object setting, StrategySelector selector, boolean legacyFormat) {
		return formatMapper(
				setting,
				selector,
				() -> {
					final FormatMapper jacksonFormatMapper =
							getXMLJacksonFormatMapperOrNull( legacyFormat );
					return jacksonFormatMapper != null
							? jacksonFormatMapper
							: new JaxbXmlFormatMapper( legacyFormat );
				}
		);
	}

	private static FormatMapper formatMapper(Object setting, StrategySelector selector, Callable<FormatMapper> defaultResolver) {
		return selector.resolveDefaultableStrategy( FormatMapper.class, setting, defaultResolver );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SessionFactoryOptionsState

	@Override
	public String getUuid() {
		return uuid;
	}

	@Override
	public StandardServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	@Override
	public boolean isJpaBootstrap() {
		return jpaBootstrap;
	}

	@Override
	public boolean isJtaTransactionAccessEnabled() {
		return jtaTransactionAccessEnabled;
	}

	@Override
	public boolean isAllowOutOfTransactionUpdateOperations() {
		return allowOutOfTransactionUpdateOperations;
	}


	@Override
	public boolean isReleaseResourcesOnCloseEnabled() {
		return releaseResourcesOnCloseEnabled;
	}

	@Override
	public Object getBeanManagerReference() {
		return beanManagerReference;
	}

	@Override
	public Object getValidatorFactoryReference() {
		return validatorFactoryReference;
	}

	@Override
	public String getSessionFactoryName() {
		return sessionFactoryName;
	}

	@Override
	public Boolean isSessionFactoryNameAlsoJndiName() {
		return sessionFactoryNameAlsoJndiName;
	}

	@Override
	public boolean isFlushBeforeCompletionEnabled() {
		return flushBeforeCompletionEnabled;
	}

	@Override
	public boolean isAutoCloseSessionEnabled() {
		return autoCloseSessionEnabled;
	}

	@Override
	public boolean isStatisticsEnabled() {
		return statisticsEnabled;
	}

	@Override
	public Interceptor getInterceptor() {
		return interceptor == null ? EmptyInterceptor.INSTANCE : interceptor;
	}

	@Override
	public Supplier<? extends Interceptor> getStatelessInterceptorImplementorSupplier() {
		return statelessInterceptorSupplier;
	}

	@Override
	public HqlTranslator getCustomHqlTranslator() {
		return hqlTranslator;
	}

	@Override
	public SqmTranslatorFactory getCustomSqmTranslatorFactory() {
		return sqmTranslatorFactory;
	}

	@Override
	public SqmMultiTableMutationStrategy getCustomSqmMultiTableMutationStrategy() {
		return sqmMultiTableMutationStrategy;
	}

	@Override
	public SqmMultiTableInsertStrategy getCustomSqmMultiTableInsertStrategy() {
		return sqmMultiTableInsertStrategy;
	}

	@Override
	public SqmMultiTableMutationStrategy resolveCustomSqmMultiTableMutationStrategy(EntityMappingType rootEntityDescriptor, RuntimeModelCreationContext creationContext) {
		if ( sqmMultiTableMutationStrategyConstructor != null ) {
			try {
				return sqmMultiTableMutationStrategyConstructor.newInstance( rootEntityDescriptor, creationContext );
			}
			catch (Exception e) {
				throw new StrategySelectionException(
						String.format( "Could not instantiate named strategy class [%s]", sqmMultiTableMutationStrategyConstructor.getDeclaringClass().getName() ),
						e
				);
			}
		}
		return null;
	}

	@Override
	public SqmMultiTableInsertStrategy resolveCustomSqmMultiTableInsertStrategy(EntityMappingType rootEntityDescriptor, RuntimeModelCreationContext creationContext) {
		if ( sqmMultiTableInsertStrategyConstructor != null ) {
			try {
				return sqmMultiTableInsertStrategyConstructor.newInstance( rootEntityDescriptor, creationContext );
			}
			catch (Exception e) {
				throw new StrategySelectionException(
						String.format( "Could not instantiate named strategy class [%s]", sqmMultiTableInsertStrategyConstructor.getDeclaringClass().getName() ),
						e
				);
			}
		}
		return null;
	}

	@Override
	public boolean isUseOfJdbcNamedParametersEnabled() {
		return useOfJdbcNamedParametersEnabled;
	}

	@Override
	public SqmFunctionRegistry getCustomSqmFunctionRegistry() {
		return sqmFunctionRegistry;
	}

	@Override
	public StatementInspector getStatementInspector() {
		return statementInspector;
	}

	@Override
	public SessionFactoryObserver[] getSessionFactoryObservers() {
		return sessionFactoryObserverList.toArray(new SessionFactoryObserver[0]);
	}

	@Override
	public BaselineSessionEventsListenerBuilder getBaselineSessionEventsListenerBuilder() {
		return baselineSessionEventsListenerBuilder;
	}

	@Override
	public boolean isIdentifierRollbackEnabled() {
		return identifierRollbackEnabled;
	}

	@Override
	public boolean isCheckNullability() {
		return checkNullability;
	}

	@Override
	public boolean isInitializeLazyStateOutsideTransactionsEnabled() {
		return initializeLazyStateOutsideTransactions;
	}

	@Override @Deprecated
	public TempTableDdlTransactionHandling getTempTableDdlTransactionHandling() {
		return tempTableDdlTransactionHandling;
	}

	@Override @Deprecated(forRemoval = true)
	public boolean isDelayBatchFetchLoaderCreationsEnabled() {
		return delayBatchFetchLoaderCreations;
	}

	@Override
	public int getDefaultBatchFetchSize() {
		return defaultBatchFetchSize;
	}

	@Override
	public Integer getMaximumFetchDepth() {
		return maximumFetchDepth;
	}

	@Override
	public boolean isSubselectFetchEnabled() {
		return subselectFetchEnabled;
	}

	@Override
	public Nulls getDefaultNullPrecedence() {
		return defaultNullPrecedence;
	}

	@Override
	public boolean isOrderUpdatesEnabled() {
		return orderUpdatesEnabled;
	}

	@Override
	public boolean isOrderInsertsEnabled() {
		return orderInsertsEnabled;
	}

	@Override
	public boolean isMultiTenancyEnabled() {
		return multiTenancyEnabled;
	}

	@Override
	public TenantSchemaMapper<Object> getTenantSchemaMapper() {
		return tenantSchemaMapper;
	}

	@Override
	public CurrentTenantIdentifierResolver<Object> getCurrentTenantIdentifierResolver() {
		return currentTenantIdentifierResolver;
	}

	@Override
	public boolean isJtaTrackByThread() {
		return jtaTrackByThread;
	}

	@Override
	public boolean isNamedQueryStartupCheckingEnabled() {
		return namedQueryStartupCheckingEnabled;
	}

	@Override
	public boolean isSecondLevelCacheEnabled() {
		return secondLevelCacheEnabled;
	}

	@Override
	public boolean isQueryCacheEnabled() {
		return queryCacheEnabled;
	}

	@Override
	public CacheLayout getQueryCacheLayout() {
		return queryCacheLayout;
	}

	@Override
	public TimestampsCacheFactory getTimestampsCacheFactory() {
		return timestampsCacheFactory;
	}

	@Override
	public String getCacheRegionPrefix() {
		return cacheRegionPrefix;
	}

	@Override
	public boolean isMinimalPutsEnabled() {
		return minimalPutsEnabled;
	}

	@Override
	public boolean isStructuredCacheEntriesEnabled() {
		return structuredCacheEntriesEnabled;
	}

	@Override
	public boolean isDirectReferenceCacheEntriesEnabled() {
		return directReferenceCacheEntriesEnabled;
	}

	@Override
	public boolean isAutoEvictCollectionCache() {
		return autoEvictCollectionCache;
	}

	@Override @Deprecated
	public SchemaAutoTooling getSchemaAutoTooling() {
		return schemaAutoTooling;
	}

	@Override
	public int getJdbcBatchSize() {
		return jdbcBatchSize;
	}

	@Override
	public boolean isScrollableResultSetsEnabled() {
		return scrollableResultSetsEnabled;
	}

	@Override
	public boolean isGetGeneratedKeysEnabled() {
		return getGeneratedKeysEnabled;
	}

	@Override
	public Integer getJdbcFetchSize() {
		return jdbcFetchSize;
	}

	@Override
	public PhysicalConnectionHandlingMode getPhysicalConnectionHandlingMode() {
		return connectionHandlingMode;
	}

	@Override
	public void setCheckNullability(boolean enabled) {
		checkNullability = enabled;
	}

	@Override
	public boolean doesConnectionProviderDisableAutoCommit() {
		return connectionProviderDisablesAutoCommit;
	}

	@Override
	public boolean isCommentsEnabled() {
		return commentsEnabled;
	}

	@Override
	public CustomEntityDirtinessStrategy getCustomEntityDirtinessStrategy() {
		return customEntityDirtinessStrategy;
	}

	@Override
	public EntityNameResolver[] getEntityNameResolvers() {
		return entityNameResolvers.toArray(new EntityNameResolver[0]);
	}

	@Override
	public EntityNotFoundDelegate getEntityNotFoundDelegate() {
		return entityNotFoundDelegate;
	}

	@Override
	public Map<String, SqmFunctionDescriptor> getCustomSqlFunctionMap() {
		return sqlFunctions == null ? Collections.emptyMap() : sqlFunctions;
	}

	@Override
	public boolean isPreferUserTransaction() {
		return preferUserTransaction;
	}

	@Override
	public TimeZone getJdbcTimeZone() {
		return jdbcTimeZone;
	}

	@Override
	public ValueHandlingMode getCriteriaValueHandlingMode() {
		return criteriaValueHandlingMode;
	}

	@Override
	public boolean isCriteriaCopyTreeEnabled() {
		return criteriaCopyTreeEnabled;
	}

	@Override
	public boolean isCriteriaPlanCacheEnabled() {
		return criteriaPlanCacheEnabled;
	}

	@Override
	public boolean getNativeJdbcParametersIgnored() {
		return nativeJdbcParametersIgnored;
	}

	@Override
	public ImmutableEntityUpdateQueryHandlingMode getImmutableEntityUpdateQueryHandlingMode() {
		return immutableEntityUpdateQueryHandlingMode;
	}

	@Override
	public boolean allowImmutableEntityUpdate() {
		return immutableEntityUpdateQueryHandlingMode != ImmutableEntityUpdateQueryHandlingMode.EXCEPTION;
	}

	@Override
	public String getDefaultCatalog() {
		return defaultCatalog;
	}

	@Override
	public String getDefaultSchema() {
		return defaultSchema;
	}

	@Override
	public boolean isFailOnPaginationOverCollectionFetchEnabled() {
		return failOnPaginationOverCollectionFetchEnabled;
	}

	@Override
	public boolean inClauseParameterPaddingEnabled() {
		return inClauseParameterPaddingEnabled;
	}

	@Override
	public boolean isJsonFunctionsEnabled() {
		return jsonFunctionsEnabled;
	}

	@Override
	public boolean isXmlFunctionsEnabled() {
		return xmlFunctionsEnabled;
	}

	@Override
	public boolean isPortableIntegerDivisionEnabled() {
		return portableIntegerDivisionEnabled;
	}

	@Override
	public JpaCompliance getJpaCompliance() {
		return jpaCompliance;
	}

	@Override
	public int getQueryStatisticsMaxSize() {
		return queryStatisticsMaxSize;
	}

	@Override
	public boolean areJPACallbacksEnabled() {
		return callbacksEnabled;
	}

	@Override
	public boolean isCollectionsInDefaultFetchGroupEnabled() {
		return collectionsInDefaultFetchGroupEnabled;
	}

	@Override
	public boolean isUnownedAssociationTransientCheck() {
		return unownedAssociationTransientCheck;
	}

	@Override
	public int getPreferredSqlTypeCodeForBoolean() {
		return preferredSqlTypeCodeForBoolean;
	}

	@Override
	public int getPreferredSqlTypeCodeForDuration() {
		return preferredSqlTypeCodeForDuration;
	}

	@Override
	public int getPreferredSqlTypeCodeForUuid() {
		return preferredSqlTypeCodeForUuid;
	}

	@Override
	public int getPreferredSqlTypeCodeForInstant() {
		return preferredSqlTypeCodeForInstant;
	}

	@Override
	public int getPreferredSqlTypeCodeForArray() {
		return preferredSqlTypeCodeForArray;
	}

	@Override
	public TimeZoneStorageStrategy getDefaultTimeZoneStorageStrategy() {
		return defaultTimeZoneStorageStrategy;
	}

	@Override
	public boolean isPreferJavaTimeJdbcTypesEnabled() {
		return preferJavaTimeJdbcTypes;
	}

	@Override
	public boolean isPreferNativeEnumTypesEnabled() {
		return preferNativeEnumTypes;
	}

	@Override
	public FormatMapper getJsonFormatMapper() {
		if ( jsonFormatMapper == null ) {
			throw new HibernateException(
					"Could not find a FormatMapper for the JSON format, which is required for mapping JSON types. JSON FormatMapper configuration is automatic, but requires that you have either Jackson or a JSONB implementation like Yasson on the class path."
			);
		}
		return jsonFormatMapper;
	}

	@Override
	public FormatMapper getXmlFormatMapper() {
		if ( xmlFormatMapper == null ) {
			throw new HibernateException(
					"Could not find a FormatMapper for the XML format, which is required for mapping XML types. XML FormatMapper configuration is automatic, but requires that you have either Jackson XML or a JAXB implementation like Glassfish JAXB on the class path."
			);
		}
		return xmlFormatMapper;
	}

	@Override
	public boolean isXmlFormatMapperLegacyFormatEnabled() {
		return xmlFormatMapperLegacyFormatEnabled;
	}

	@Override
	public boolean isPassProcedureParameterNames() {
		return passProcedureParameterNames;
	}

	@Override
	public boolean isPreferJdbcDatetimeTypesInNativeQueriesEnabled() {
		return preferJdbcDatetimeTypes;
	}

	@Override
	public XmlValidationMode getXmlValidationMode() { return xmlValidationMode; }

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// In-flight mutation access

	public void applyBeanManager(Object beanManager) {
		this.beanManagerReference = beanManager;
	}

	public void applyValidatorFactory(Object validatorFactory) {
		this.validatorFactoryReference = validatorFactory;
	}

	public void applyJsonFormatMapper(FormatMapper jsonFormatMapper) {
		this.jsonFormatMapper = jsonFormatMapper;
	}

	public void applyXmlFormatMapper(FormatMapper xmlFormatMapper) {
		this.xmlFormatMapper = xmlFormatMapper;
	}

	public void applySessionFactoryName(String sessionFactoryName) {
		this.sessionFactoryName = sessionFactoryName;
	}

	public void enableSessionFactoryNameAsJndiName(boolean isJndiName) {
		this.sessionFactoryNameAlsoJndiName = isJndiName;
	}

	public void enableSessionAutoClosing(boolean autoClosingEnabled) {
		this.autoCloseSessionEnabled = autoClosingEnabled;
	}

	public void enableSessionAutoFlushing(boolean flushBeforeCompletionEnabled) {
		this.flushBeforeCompletionEnabled = flushBeforeCompletionEnabled;
	}

	public void enableJtaTrackingByThread(boolean enabled) {
		this.jtaTrackByThread = enabled;
	}

	public void enablePreferUserTransaction(boolean preferUserTransaction) {
		this.preferUserTransaction = preferUserTransaction;
	}

	public void enableStatisticsSupport(boolean enabled) {
		this.statisticsEnabled = enabled;
	}

	public void addSessionFactoryObservers(SessionFactoryObserver... observers) {
		Collections.addAll( sessionFactoryObserverList, observers );
	}

	public void applyInterceptor(Interceptor interceptor) {
		this.interceptor = interceptor;
	}

	public void applyStatelessInterceptor(Class<? extends Interceptor> statelessInterceptorClass) {
		applyStatelessInterceptorSupplier(
				() -> {
					try {
						return statelessInterceptorClass.newInstance();
					}
					catch (InstantiationException | IllegalAccessException e) {
						throw new HibernateException( "Could not supply stateless Interceptor of class '"
								+ statelessInterceptorClass.getName() + "'", e );
					}
				}
		);
	}

	public void applyStatelessInterceptorSupplier(Supplier<? extends Interceptor> statelessInterceptorSupplier) {
		this.statelessInterceptorSupplier = statelessInterceptorSupplier;
	}

	public void applySqmFunctionRegistry(SqmFunctionRegistry sqmFunctionRegistry) {
		this.sqmFunctionRegistry = sqmFunctionRegistry;
	}

	public void applyStatementInspector(StatementInspector statementInspector) {
		this.statementInspector = statementInspector;
	}

	public void applyCustomEntityDirtinessStrategy(CustomEntityDirtinessStrategy strategy) {
		this.customEntityDirtinessStrategy = strategy;
	}

	public void addEntityNameResolvers(EntityNameResolver... entityNameResolvers) {
		Collections.addAll( this.entityNameResolvers, entityNameResolvers );
	}

	public void applyEntityNotFoundDelegate(EntityNotFoundDelegate entityNotFoundDelegate) {
		this.entityNotFoundDelegate = entityNotFoundDelegate;
	}

	public void enableIdentifierRollbackSupport(boolean enabled) {
		this.identifierRollbackEnabled = enabled;
	}

	public void enableNullabilityChecking(boolean enabled) {
		this.checkNullability = enabled;
	}

	public void allowLazyInitializationOutsideTransaction(boolean enabled) {
		this.initializeLazyStateOutsideTransactions = enabled;
	}

	@Deprecated(forRemoval = true)
	public void applyTempTableDdlTransactionHandling(TempTableDdlTransactionHandling handling) {
		this.tempTableDdlTransactionHandling = handling;
	}

	public void applyDelayedEntityLoaderCreations(boolean delay) {
		this.delayBatchFetchLoaderCreations = delay;
	}

	public void applyDefaultBatchFetchSize(int size) {
		this.defaultBatchFetchSize = size;
	}

	public void applyMaximumFetchDepth(int depth) {
		this.maximumFetchDepth = depth;
	}

	public void applySubselectFetchEnabled(boolean subselectFetchEnabled) {
		this.subselectFetchEnabled = subselectFetchEnabled;
	}

	public void applyDefaultNullPrecedence(Nulls nullPrecedence) {
		this.defaultNullPrecedence = nullPrecedence;
	}

	public void enableOrderingOfInserts(boolean enabled) {
		this.orderInsertsEnabled = enabled;
	}

	public void enableOrderingOfUpdates(boolean enabled) {
		this.orderUpdatesEnabled = enabled;
	}

	public void applyMultiTenancy(boolean enabled) {
		this.multiTenancyEnabled = enabled;
	}

	public void applyCurrentTenantIdentifierResolver(CurrentTenantIdentifierResolver<?> resolver) {
		//noinspection unchecked
		this.currentTenantIdentifierResolver = (CurrentTenantIdentifierResolver<Object>) resolver;
	}

	public void applyTenantSchemaMapper(TenantSchemaMapper<?> mapper) {
		//noinspection unchecked
		this.tenantSchemaMapper = (TenantSchemaMapper<Object>) mapper;
	}

	public void enableNamedQueryCheckingOnStartup(boolean enabled) {
		this.namedQueryStartupCheckingEnabled = enabled;
	}

	public void enableSecondLevelCacheSupport(boolean enabled) {
		this.secondLevelCacheEnabled =  enabled;
	}

	public void enableQueryCacheSupport(boolean enabled) {
		this.queryCacheEnabled = enabled;
	}

	public void applyQueryCacheLayout(CacheLayout queryCacheLayout) {
		this.queryCacheLayout = queryCacheLayout;
	}

	public void applyTimestampsCacheFactory(TimestampsCacheFactory factory) {
		this.timestampsCacheFactory = factory;
	}

	public void applyCacheRegionPrefix(String prefix) {
		this.cacheRegionPrefix = prefix;
	}

	public void enableMinimalPuts(boolean enabled) {
		this.minimalPutsEnabled = enabled;
	}

	public void enabledStructuredCacheEntries(boolean enabled) {
		this.structuredCacheEntriesEnabled = enabled;
	}

	public void allowDirectReferenceCacheEntries(boolean enabled) {
		this.directReferenceCacheEntriesEnabled = enabled;
	}

	public void enableAutoEvictCollectionCaches(boolean enabled) {
		this.autoEvictCollectionCache = enabled;
	}

	public void applyJdbcBatchSize(int size) {
		this.jdbcBatchSize = size;
	}

	public void enableScrollableResultSupport(boolean enabled) {
		this.scrollableResultSetsEnabled = enabled;
	}

	public void enableGeneratedKeysSupport(boolean enabled) {
		this.getGeneratedKeysEnabled = enabled;
	}

	public void applyJdbcFetchSize(int size) {
		this.jdbcFetchSize = size;
	}

	public void applyConnectionHandlingMode(PhysicalConnectionHandlingMode mode) {
		this.connectionHandlingMode = mode;
	}

	public void applyConnectionProviderDisablesAutoCommit(boolean providerDisablesAutoCommit) {
		this.connectionProviderDisablesAutoCommit = providerDisablesAutoCommit;
	}

	public void enableCommentsSupport(boolean enabled) {
		this.commentsEnabled = enabled;
	}

	public void applySqlFunction(String registrationName, SqmFunctionDescriptor sqlFunction) {
		if ( sqlFunctions == null ) {
			sqlFunctions = new HashMap<>();
		}
		sqlFunctions.put( registrationName, sqlFunction );
	}

	public void allowOutOfTransactionUpdateOperations(boolean allow) {
		this.allowOutOfTransactionUpdateOperations = allow;
	}

	@Deprecated(since = "7.0", forRemoval = true)
	public void enableReleaseResourcesOnClose(boolean enable) {
		this.releaseResourcesOnCloseEnabled = enable;
	}

	public void enableJpaQueryCompliance(boolean enabled) {
		mutableJpaCompliance().setQueryCompliance( enabled );
	}

	private MutableJpaCompliance mutableJpaCompliance() {
		if ( jpaCompliance instanceof MutableJpaCompliance mutableJpaCompliance ) {
			return mutableJpaCompliance;
		}
		else {
			throw new IllegalStateException( "JpaCompliance is no longer mutable" );
		}
	}

	public void enableJpaTransactionCompliance(boolean enabled) {
		mutableJpaCompliance().setTransactionCompliance( enabled );
	}

	public void enableJpaCascadeCompliance(boolean enabled) {
		mutableJpaCompliance().setCascadeCompliance( enabled );
	}

	public void enableJpaClosedCompliance(boolean enabled) {
		mutableJpaCompliance().setClosedCompliance( enabled );
	}

	public void enableJpaProxyCompliance(boolean enabled) {
		mutableJpaCompliance().setProxyCompliance( enabled );
	}

	public void enableJpaCachingCompliance(boolean enabled) {
		mutableJpaCompliance().setCachingCompliance( enabled );
	}

	public void enableJpaOrderByMappingCompliance(boolean enabled) {
		mutableJpaCompliance().setOrderByMappingCompliance( enabled );
	}

	public void enableGeneratorNameScopeCompliance(boolean enabled) {
		mutableJpaCompliance().setGeneratorNameScopeCompliance( enabled );
	}

	public void enableCollectionInDefaultFetchGroup(boolean enabled) {
		this.collectionsInDefaultFetchGroupEnabled = enabled;
	}

	public void disableJtaTransactionAccess() {
		this.jtaTransactionAccessEnabled = false;
	}

	public void applyXmlValidationMode(XmlValidationMode xmlValidationMode) {
		this.xmlValidationMode = xmlValidationMode;
	}

	public SessionFactoryOptions buildOptions() {
		if ( jpaCompliance instanceof MutableJpaCompliance mutableJpaCompliance ) {
			jpaCompliance = mutableJpaCompliance.immutableCopy();
		}
		return this;
	}

	@Override
	public CacheStoreMode getCacheStoreMode(final Map<String, Object> properties) {
		return properties == null ? defaultCacheStoreMode : defaultCacheStoreMode( properties );
	}

	@Override
	public CacheRetrieveMode getCacheRetrieveMode(Map<String, Object> properties) {
		return properties == null ? defaultCacheRetrieveMode : defaultCacheRetrieveMode( properties );
	}

	private static CacheRetrieveMode defaultCacheRetrieveMode(Map<String, Object> settings) {
		final CacheRetrieveMode cacheRetrieveMode = (CacheRetrieveMode) settings.get( JPA_SHARED_CACHE_RETRIEVE_MODE );
		return cacheRetrieveMode == null
				? (CacheRetrieveMode) settings.get( JAKARTA_SHARED_CACHE_RETRIEVE_MODE )
				: cacheRetrieveMode;
	}

	private static CacheStoreMode defaultCacheStoreMode(Map<String, Object> settings) {
		final CacheStoreMode cacheStoreMode = (CacheStoreMode) settings.get( JPA_SHARED_CACHE_STORE_MODE );
		return cacheStoreMode == null
				? (CacheStoreMode) settings.get( JAKARTA_SHARED_CACHE_STORE_MODE )
				: cacheStoreMode;
	}

	@Override
	public CacheMode getInitialSessionCacheMode() {
		return initialSessionCacheMode;
	}

	@Override
	public FlushMode getInitialSessionFlushMode() {
		return initialSessionFlushMode;
	}

	@Override
	public LockOptions getDefaultLockOptions() {
		return defaultLockOptions;
	}

	private static FlushMode defaultFlushMode(Map<String, Object> properties) {
		return getFlushMode( properties.get( HibernateHints.HINT_FLUSH_MODE ), FlushMode.AUTO );
	}

	private static LockOptions defaultLockOptions(Map<String, Object> defaultSessionProperties) {
		final LockOptions lockOptions = new LockOptions();
		applyPropertiesToLockOptions( defaultSessionProperties, () -> lockOptions );
		return lockOptions;
	}

	@Override
	public Map<String, Object> getDefaultSessionProperties() {
		return defaultSessionProperties;
	}

	private Map<String, Object> initializeDefaultSessionProperties(ConfigurationService configurationService) {
		final HashMap<String,Object> settings = new HashMap<>();

		//Static defaults:
		settings.putIfAbsent( HibernateHints.HINT_FLUSH_MODE, FlushMode.AUTO );
		settings.putIfAbsent( JPA_LOCK_SCOPE, PessimisticLockScope.EXTENDED );
		settings.putIfAbsent( JAKARTA_LOCK_SCOPE, PessimisticLockScope.EXTENDED );
		settings.putIfAbsent( JPA_LOCK_TIMEOUT, WAIT_FOREVER_MILLI );
		settings.putIfAbsent( JAKARTA_LOCK_TIMEOUT, WAIT_FOREVER_MILLI );
		settings.putIfAbsent( JPA_SHARED_CACHE_RETRIEVE_MODE, CacheModeHelper.DEFAULT_RETRIEVE_MODE );
		settings.putIfAbsent( JAKARTA_SHARED_CACHE_RETRIEVE_MODE, CacheModeHelper.DEFAULT_RETRIEVE_MODE );
		settings.putIfAbsent( JPA_SHARED_CACHE_STORE_MODE, CacheModeHelper.DEFAULT_STORE_MODE );
		settings.putIfAbsent( JAKARTA_SHARED_CACHE_STORE_MODE, CacheModeHelper.DEFAULT_STORE_MODE );

		//Defaults defined by SessionFactory configuration:
		final String[] ENTITY_MANAGER_SPECIFIC_PROPERTIES = {
				SpecHints.HINT_SPEC_LOCK_SCOPE,
				SpecHints.HINT_SPEC_LOCK_TIMEOUT,
				SpecHints.HINT_SPEC_QUERY_TIMEOUT,
				SpecHints.HINT_SPEC_CACHE_RETRIEVE_MODE,
				SpecHints.HINT_SPEC_CACHE_STORE_MODE,

				HibernateHints.HINT_FLUSH_MODE,

				LegacySpecHints.HINT_JAVAEE_LOCK_SCOPE,
				LegacySpecHints.HINT_JAVAEE_LOCK_TIMEOUT,
				LegacySpecHints.HINT_JAVAEE_CACHE_RETRIEVE_MODE,
				LegacySpecHints.HINT_JAVAEE_CACHE_STORE_MODE,
				LegacySpecHints.HINT_JAVAEE_QUERY_TIMEOUT
		};

		final var configurationServiceSettings = configurationService.getSettings();
		for ( String key : ENTITY_MANAGER_SPECIFIC_PROPERTIES ) {
			if ( configurationServiceSettings.containsKey( key ) ) {
				settings.put( key, configurationServiceSettings.get( key ) );
			}
		}
		return unmodifiableMap( settings );
	}
}
