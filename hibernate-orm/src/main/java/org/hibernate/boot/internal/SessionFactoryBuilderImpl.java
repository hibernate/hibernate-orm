/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.internal;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.hibernate.ConnectionAcquisitionMode;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.EmptyInterceptor;
import org.hibernate.EntityMode;
import org.hibernate.EntityNameResolver;
import org.hibernate.Interceptor;
import org.hibernate.MultiTenancyStrategy;
import org.hibernate.NullPrecedence;
import org.hibernate.SessionEventListener;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.SchemaAutoTooling;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.TempTableDdlTransactionHandling;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryBuilderImplementor;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.internal.StandardQueryCacheFactory;
import org.hibernate.cache.spi.QueryCacheFactory;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.BaselineSessionEventsListenerBuilder;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.engine.config.internal.ConfigurationServiceImpl;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.env.spi.ExtractedDatabaseMetaData;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.internal.log.DeprecationLogger;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.loader.BatchFetchStyle;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tuple.entity.EntityTuplizer;
import org.hibernate.tuple.entity.EntityTuplizerFactory;

import org.jboss.logging.Logger;

import static org.hibernate.cfg.AvailableSettings.*;
import static org.hibernate.engine.config.spi.StandardConverters.BOOLEAN;
import static org.hibernate.jpa.AvailableSettings.DISCARD_PC_ON_CLOSE;

/**
 * @author Gail Badner
 * @author Steve Ebersole
 */
public class SessionFactoryBuilderImpl implements SessionFactoryBuilderImplementor, SessionFactoryOptionsState {
	private static final Logger log = Logger.getLogger( SessionFactoryBuilderImpl.class );

	private final MetadataImplementor metadata;
	private final SessionFactoryOptionsStateStandardImpl options;

	public SessionFactoryBuilderImpl(MetadataImplementor metadata) {
		this.metadata = metadata;
		this.options = new SessionFactoryOptionsStateStandardImpl( metadata.getMetadataBuildingOptions().getServiceRegistry() );

		if ( metadata.getSqlFunctionMap() != null ) {
			for ( Map.Entry<String, SQLFunction> sqlFunctionEntry : metadata.getSqlFunctionMap().entrySet() ) {
				applySqlFunction( sqlFunctionEntry.getKey(), sqlFunctionEntry.getValue() );
			}
		}
	}

	@Override
	public SessionFactoryBuilder applyValidatorFactory(Object validatorFactory) {
		this.options.validatorFactoryReference = validatorFactory;
		return this;
	}

	@Override
	public SessionFactoryBuilder applyBeanManager(Object beanManager) {
		this.options.beanManagerReference = beanManager;
		return this;
	}

	@Override
	public SessionFactoryBuilder applyName(String sessionFactoryName) {
		this.options.sessionFactoryName = sessionFactoryName;
		return this;
	}

	@Override
	public SessionFactoryBuilder applyNameAsJndiName(boolean isJndiName) {
		this.options.sessionFactoryNameAlsoJndiName = isJndiName;
		return this;
	}

	@Override
	public SessionFactoryBuilder applyAutoClosing(boolean enabled) {
		this.options.autoCloseSessionEnabled = enabled;
		return this;
	}

	@Override
	public SessionFactoryBuilder applyAutoFlushing(boolean enabled) {
		this.options.flushBeforeCompletionEnabled = enabled;
		return this;
	}

	@Override
	public SessionFactoryBuilder applyJtaTrackingByThread(boolean enabled) {
		this.options.jtaTrackByThread = enabled;
		return this;
	}

	@Override
	public SessionFactoryBuilder applyPreferUserTransactions(boolean preferUserTransactions) {
		this.options.preferUserTransaction = preferUserTransactions;
		return this;
	}

	@Override
	public SessionFactoryBuilder applyStatisticsSupport(boolean enabled) {
		this.options.statisticsEnabled = enabled;
		return this;
	}

	@Override
	public SessionFactoryBuilder addSessionFactoryObservers(SessionFactoryObserver... observers) {
		this.options.sessionFactoryObserverList.addAll( Arrays.asList( observers ) );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyInterceptor(Interceptor interceptor) {
		this.options.interceptor = interceptor;
		return this;
	}

	@Override
	public SessionFactoryBuilder applyStatelessInterceptor(Class<? extends Interceptor> statelessInterceptorClass) {
		this.options.statelessInterceptorClass = statelessInterceptorClass;
		return this;
	}

	@Override
	public SessionFactoryBuilder applyStatementInspector(StatementInspector statementInspector) {
		this.options.statementInspector = statementInspector;
		return this;
	}

	@Override
	public SessionFactoryBuilder applyCustomEntityDirtinessStrategy(CustomEntityDirtinessStrategy strategy) {
		this.options.customEntityDirtinessStrategy = strategy;
		return this;
	}

	@Override
	public SessionFactoryBuilder addEntityNameResolver(EntityNameResolver... entityNameResolvers) {
		this.options.entityNameResolvers.addAll( Arrays.asList( entityNameResolvers ) );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyEntityNotFoundDelegate(EntityNotFoundDelegate entityNotFoundDelegate) {
		this.options.entityNotFoundDelegate = entityNotFoundDelegate;
		return this;
	}

	@Override
	public SessionFactoryBuilder applyIdentifierRollbackSupport(boolean enabled) {
		this.options.identifierRollbackEnabled = enabled;
		return this;
	}

	@Override
	public SessionFactoryBuilder applyDefaultEntityMode(EntityMode entityMode) {
		this.options.defaultEntityMode = entityMode;
		return this;
	}

	@Override
	public SessionFactoryBuilder applyNullabilityChecking(boolean enabled) {
		this.options.checkNullability = enabled;
		return this;
	}

	@Override
	public SessionFactoryBuilder applyLazyInitializationOutsideTransaction(boolean enabled) {
		this.options.initializeLazyStateOutsideTransactions = enabled;
		return this;
	}

	@Override
	public SessionFactoryBuilder applyEntityTuplizerFactory(EntityTuplizerFactory entityTuplizerFactory) {
		this.options.entityTuplizerFactory = entityTuplizerFactory;
		return this;
	}

	@Override
	public SessionFactoryBuilder applyEntityTuplizer(
			EntityMode entityMode,
			Class<? extends EntityTuplizer> tuplizerClass) {
		this.options.entityTuplizerFactory.registerDefaultTuplizerClass( entityMode, tuplizerClass );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyMultiTableBulkIdStrategy(MultiTableBulkIdStrategy strategy) {
		this.options.multiTableBulkIdStrategy = strategy;
		return this;
	}

	@Override
	public SessionFactoryBuilder applyTempTableDdlTransactionHandling(TempTableDdlTransactionHandling handling) {
		this.options.tempTableDdlTransactionHandling = handling;
		return this;
	}

	@Override
	public SessionFactoryBuilder applyBatchFetchStyle(BatchFetchStyle style) {
		this.options.batchFetchStyle = style;
		return this;
	}

	@Override
	public SessionFactoryBuilder applyDefaultBatchFetchSize(int size) {
		this.options.defaultBatchFetchSize = size;
		return this;
	}

	@Override
	public SessionFactoryBuilder applyMaximumFetchDepth(int depth) {
		this.options.maximumFetchDepth = depth;
		return this;
	}

	@Override
	public SessionFactoryBuilder applyDefaultNullPrecedence(NullPrecedence nullPrecedence) {
		this.options.defaultNullPrecedence = nullPrecedence;
		return this;
	}

	@Override
	public SessionFactoryBuilder applyOrderingOfInserts(boolean enabled) {
		this.options.orderInsertsEnabled = enabled;
		return this;
	}

	@Override
	public SessionFactoryBuilder applyOrderingOfUpdates(boolean enabled) {
		this.options.orderUpdatesEnabled = enabled;
		return this;
	}

	@Override
	public SessionFactoryBuilder applyMultiTenancyStrategy(MultiTenancyStrategy strategy) {
		this.options.multiTenancyStrategy = strategy;
		return this;
	}

	@Override
	public SessionFactoryBuilder applyCurrentTenantIdentifierResolver(CurrentTenantIdentifierResolver resolver) {
		this.options.currentTenantIdentifierResolver = resolver;
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public SessionFactoryBuilder applyQuerySubstitutions(Map substitutions) {
		this.options.querySubstitutions.putAll( substitutions );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyStrictJpaQueryLanguageCompliance(boolean enabled) {
		this.options.strictJpaQueryLanguageCompliance = enabled;
		return this;
	}

	@Override
	public SessionFactoryBuilder applyNamedQueryCheckingOnStartup(boolean enabled) {
		this.options.namedQueryStartupCheckingEnabled = enabled;
		return this;
	}

	@Override
	public SessionFactoryBuilder applySecondLevelCacheSupport(boolean enabled) {
		this.options.secondLevelCacheEnabled = enabled;
		return this;
	}

	@Override
	public SessionFactoryBuilder applyQueryCacheSupport(boolean enabled) {
		this.options.queryCacheEnabled = enabled;
		return this;
	}

	@Override
	public SessionFactoryBuilder applyQueryCacheFactory(QueryCacheFactory factory) {
		this.options.queryCacheFactory = factory;
		return this;
	}

	@Override
	public SessionFactoryBuilder applyCacheRegionPrefix(String prefix) {
		this.options.cacheRegionPrefix = prefix;
		return this;
	}

	@Override
	public SessionFactoryBuilder applyMinimalPutsForCaching(boolean enabled) {
		this.options.minimalPutsEnabled = enabled;
		return this;
	}

	@Override
	public SessionFactoryBuilder applyStructuredCacheEntries(boolean enabled) {
		this.options.structuredCacheEntriesEnabled = enabled;
		return this;
	}

	@Override
	public SessionFactoryBuilder applyDirectReferenceCaching(boolean enabled) {
		this.options.directReferenceCacheEntriesEnabled = enabled;
		return this;
	}

	@Override
	public SessionFactoryBuilder applyAutomaticEvictionOfCollectionCaches(boolean enabled) {
		this.options.autoEvictCollectionCache = enabled;
		return this;
	}

	@Override
	public SessionFactoryBuilder applyJdbcBatchSize(int size) {
		this.options.jdbcBatchSize = size;
		return this;
	}

	@Override
	public SessionFactoryBuilder applyJdbcBatchingForVersionedEntities(boolean enabled) {
		this.options.jdbcBatchVersionedData = enabled;
		return this;
	}

	@Override
	public SessionFactoryBuilder applyScrollableResultsSupport(boolean enabled) {
		this.options.scrollableResultSetsEnabled = enabled;
		return this;
	}

	@Override
	public SessionFactoryBuilder applyResultSetsWrapping(boolean enabled) {
		this.options.wrapResultSetsEnabled = enabled;
		return this;
	}

	@Override
	public SessionFactoryBuilder applyGetGeneratedKeysSupport(boolean enabled) {
		this.options.getGeneratedKeysEnabled = enabled;
		return this;
	}

	@Override
	public SessionFactoryBuilder applyJdbcFetchSize(int size) {
		this.options.jdbcFetchSize = size;
		return this;
	}

	@Override
	public SessionFactoryBuilder applyConnectionHandlingMode(PhysicalConnectionHandlingMode connectionHandlingMode) {
		this.options.connectionHandlingMode = connectionHandlingMode;
		return this;
	}

	@Override
	public SessionFactoryBuilder applyConnectionReleaseMode(ConnectionReleaseMode connectionReleaseMode) {
		if ( this.options.connectionHandlingMode == null ) {
			this.options.connectionHandlingMode = PhysicalConnectionHandlingMode.interpret(
					ConnectionAcquisitionMode.AS_NEEDED,
					connectionReleaseMode
			);
		}
		else {
			this.options.connectionHandlingMode = PhysicalConnectionHandlingMode.interpret(
					this.options.connectionHandlingMode.getAcquisitionMode(),
					connectionReleaseMode
			);
		}
		return this;
	}

	@Override
	public SessionFactoryBuilder applySqlComments(boolean enabled) {
		this.options.commentsEnabled = enabled;
		return this;
	}

	@Override
	public SessionFactoryBuilder applySqlFunction(String registrationName, SQLFunction sqlFunction) {
		if ( this.options.sqlFunctions == null ) {
			this.options.sqlFunctions = new HashMap<String, SQLFunction>();
		}
		this.options.sqlFunctions.put( registrationName, sqlFunction );
		return this;
	}

	@Override
	public SessionFactoryBuilder allowOutOfTransactionUpdateOperations(boolean allow) {
		this.options.allowOutOfTransactionUpdateOperations = allow;
		return this;
	}

	@Override
	public SessionFactoryBuilder enableReleaseResourcesOnCloseEnabled(boolean enable) {
		this.options.releaseResourcesOnCloseEnabled = enable;
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends SessionFactoryBuilder> T unwrap(Class<T> type) {
		return (T) this;
	}

	@Override
	public SessionFactory build() {
		metadata.validate();
		return new SessionFactoryImpl( metadata, buildSessionFactoryOptions() );
	}

	@Override
	public void markAsJpaBootstrap() {
		this.options.jpaBootstrap = true;
	}

	@Override
	public void disableRefreshDetachedEntity() {
		this.options.allowRefreshDetachedEntity = false;
	}

	@Override
	public void disableJtaTransactionAccess() {
		this.options.jtaTransactionAccessEnabled = false;
	}

	@Override
	public SessionFactoryOptions buildSessionFactoryOptions() {
		return new SessionFactoryOptionsImpl( this );
	}



	// SessionFactoryOptionsState impl ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public static class SessionFactoryOptionsStateStandardImpl implements SessionFactoryOptionsState {
		private final StandardServiceRegistry serviceRegistry;

		// integration
		private Object beanManagerReference;
		private Object validatorFactoryReference;

		// SessionFactory behavior
		private boolean jpaBootstrap;
		private String sessionFactoryName;
		private boolean sessionFactoryNameAlsoJndiName;

		// Session behavior
		private boolean flushBeforeCompletionEnabled;
		private boolean autoCloseSessionEnabled;
		private boolean jtaTransactionAccessEnabled;
		private boolean allowOutOfTransactionUpdateOperations;
		private boolean releaseResourcesOnCloseEnabled;
		private boolean allowRefreshDetachedEntity;

		// (JTA) transaction handling
		private boolean jtaTrackByThread;
		private boolean preferUserTransaction;

		// Statistics/Interceptor/observers
		private boolean statisticsEnabled;
		private Interceptor interceptor;
		private Class<? extends Interceptor> statelessInterceptorClass;
		private StatementInspector statementInspector;
		private List<SessionFactoryObserver> sessionFactoryObserverList = new ArrayList<>();
		private BaselineSessionEventsListenerBuilder baselineSessionEventsListenerBuilder;	// not exposed on builder atm

		// persistence behavior
		private CustomEntityDirtinessStrategy customEntityDirtinessStrategy;
		private List<EntityNameResolver> entityNameResolvers = new ArrayList<>();
		private EntityNotFoundDelegate entityNotFoundDelegate;
		private boolean identifierRollbackEnabled;
		private EntityMode defaultEntityMode;
		private EntityTuplizerFactory entityTuplizerFactory = new EntityTuplizerFactory();
		private boolean checkNullability;
		private boolean initializeLazyStateOutsideTransactions;
		private MultiTableBulkIdStrategy multiTableBulkIdStrategy;
		private TempTableDdlTransactionHandling tempTableDdlTransactionHandling;
		private BatchFetchStyle batchFetchStyle;
		private int defaultBatchFetchSize;
		private Integer maximumFetchDepth;
		private NullPrecedence defaultNullPrecedence;
		private boolean orderUpdatesEnabled;
		private boolean orderInsertsEnabled;


		// multi-tenancy
		private MultiTenancyStrategy multiTenancyStrategy;
		private CurrentTenantIdentifierResolver currentTenantIdentifierResolver;

		// Queries
		private Map querySubstitutions;
		private boolean strictJpaQueryLanguageCompliance;
		private boolean namedQueryStartupCheckingEnabled;
		private boolean conventionalJavaConstants;
		private final boolean procedureParameterNullPassingEnabled;
		private final boolean collectionJoinSubqueryRewriteEnabled;

		// Caching
		private boolean secondLevelCacheEnabled;
		private boolean queryCacheEnabled;
		private QueryCacheFactory queryCacheFactory;
		private String cacheRegionPrefix;
		private boolean minimalPutsEnabled;
		private boolean structuredCacheEntriesEnabled;
		private boolean directReferenceCacheEntriesEnabled;
		private boolean autoEvictCollectionCache;

		// Schema tooling
		private SchemaAutoTooling schemaAutoTooling;

		// JDBC Handling
		private boolean getGeneratedKeysEnabled;
		private int jdbcBatchSize;
		private boolean jdbcBatchVersionedData;
		private Integer jdbcFetchSize;
		private boolean scrollableResultSetsEnabled;
		private boolean commentsEnabled;
		private PhysicalConnectionHandlingMode connectionHandlingMode;
		private boolean wrapResultSetsEnabled;
		private TimeZone jdbcTimeZone;

		private Map<String, SQLFunction> sqlFunctions;

		public SessionFactoryOptionsStateStandardImpl(StandardServiceRegistry serviceRegistry) {
			this.serviceRegistry = serviceRegistry;

			final StrategySelector strategySelector = serviceRegistry.getService( StrategySelector.class );
			ConfigurationService cfgService = serviceRegistry.getService( ConfigurationService.class );
			final JdbcServices jdbcServices = serviceRegistry.getService( JdbcServices.class );

			final Map configurationSettings = new HashMap();
			//noinspection unchecked
			configurationSettings.putAll( jdbcServices.getJdbcEnvironment().getDialect().getDefaultProperties() );
			//noinspection unchecked
			configurationSettings.putAll( cfgService.getSettings() );
			cfgService = new ConfigurationServiceImpl( configurationSettings );
			( (ConfigurationServiceImpl) cfgService ).injectServices( (ServiceRegistryImplementor) serviceRegistry );

			this.beanManagerReference = configurationSettings.get( "javax.persistence.bean.manager" );
			this.validatorFactoryReference = configurationSettings.get( "javax.persistence.validation.factory" );

			this.sessionFactoryName = (String) configurationSettings.get( SESSION_FACTORY_NAME );
			this.sessionFactoryNameAlsoJndiName = cfgService.getSetting(
					SESSION_FACTORY_NAME_IS_JNDI,
					BOOLEAN,
					true
			);
			this.jtaTransactionAccessEnabled = cfgService.getSetting(
					ALLOW_JTA_TRANSACTION_ACCESS,
					BOOLEAN,
					true
			);

			this.allowRefreshDetachedEntity = cfgService.getSetting(
					ALLOW_REFRESH_DETACHED_ENTITY,
					BOOLEAN,
					true
			);

			this.flushBeforeCompletionEnabled = cfgService.getSetting( FLUSH_BEFORE_COMPLETION, BOOLEAN, true );
			this.autoCloseSessionEnabled = cfgService.getSetting( AUTO_CLOSE_SESSION, BOOLEAN, false );

			this.statisticsEnabled = cfgService.getSetting( GENERATE_STATISTICS, BOOLEAN, false );
			this.interceptor = determineInterceptor( configurationSettings, strategySelector );
			this.statelessInterceptorClass = determineStatelessInterceptorClass( configurationSettings, strategySelector );
			this.statementInspector = strategySelector.resolveStrategy(
					StatementInspector.class,
					configurationSettings.get( STATEMENT_INSPECTOR )
			);

			// todo : expose this from builder?
			final String autoSessionEventsListenerName = (String) configurationSettings.get(
					AUTO_SESSION_EVENTS_LISTENER
			);
			final Class<? extends SessionEventListener> autoSessionEventsListener = autoSessionEventsListenerName == null
					? null
					: strategySelector.selectStrategyImplementor( SessionEventListener.class, autoSessionEventsListenerName );

			final boolean logSessionMetrics = cfgService.getSetting( LOG_SESSION_METRICS, BOOLEAN, statisticsEnabled );
			this.baselineSessionEventsListenerBuilder = new BaselineSessionEventsListenerBuilder( logSessionMetrics, autoSessionEventsListener );

			this.customEntityDirtinessStrategy = strategySelector.resolveDefaultableStrategy(
					CustomEntityDirtinessStrategy.class,
					configurationSettings.get( CUSTOM_ENTITY_DIRTINESS_STRATEGY ),
					DefaultCustomEntityDirtinessStrategy.INSTANCE
			);

			this.entityNotFoundDelegate = StandardEntityNotFoundDelegate.INSTANCE;
			this.identifierRollbackEnabled = cfgService.getSetting( USE_IDENTIFIER_ROLLBACK, BOOLEAN, false );
			this.defaultEntityMode = EntityMode.parse( (String) configurationSettings.get( DEFAULT_ENTITY_MODE ) );
			this.checkNullability = cfgService.getSetting( CHECK_NULLABILITY, BOOLEAN, true );
			this.initializeLazyStateOutsideTransactions = cfgService.getSetting( ENABLE_LAZY_LOAD_NO_TRANS, BOOLEAN, false );

			this.multiTenancyStrategy = MultiTenancyStrategy.determineMultiTenancyStrategy( configurationSettings );
			this.currentTenantIdentifierResolver = strategySelector.resolveStrategy(
					CurrentTenantIdentifierResolver.class,
					configurationSettings.get( MULTI_TENANT_IDENTIFIER_RESOLVER )
			);

			this.multiTableBulkIdStrategy = strategySelector.resolveDefaultableStrategy(
					MultiTableBulkIdStrategy.class,
					configurationSettings.get( HQL_BULK_ID_STRATEGY ),
					jdbcServices.getJdbcEnvironment().getDialect().getDefaultMultiTableBulkIdStrategy()
			);

			this.batchFetchStyle = BatchFetchStyle.interpret( configurationSettings.get( BATCH_FETCH_STYLE ) );
			this.defaultBatchFetchSize = ConfigurationHelper.getInt( DEFAULT_BATCH_FETCH_SIZE, configurationSettings, -1 );
			this.maximumFetchDepth = ConfigurationHelper.getInteger( MAX_FETCH_DEPTH, configurationSettings );
			final String defaultNullPrecedence = ConfigurationHelper.getString(
					AvailableSettings.DEFAULT_NULL_ORDERING, configurationSettings, "none", "first", "last"
			);
			this.defaultNullPrecedence = NullPrecedence.parse( defaultNullPrecedence );
			this.orderUpdatesEnabled = ConfigurationHelper.getBoolean( ORDER_UPDATES, configurationSettings );
			this.orderInsertsEnabled = ConfigurationHelper.getBoolean( ORDER_INSERTS, configurationSettings );

			this.jtaTrackByThread = cfgService.getSetting( JTA_TRACK_BY_THREAD, BOOLEAN, true );

			this.querySubstitutions = ConfigurationHelper.toMap( QUERY_SUBSTITUTIONS, " ,=;:\n\t\r\f", configurationSettings );
			this.strictJpaQueryLanguageCompliance = cfgService.getSetting( JPAQL_STRICT_COMPLIANCE, BOOLEAN, false );
			this.namedQueryStartupCheckingEnabled = cfgService.getSetting( QUERY_STARTUP_CHECKING, BOOLEAN, true );
			this.conventionalJavaConstants = cfgService.getSetting(
					CONVENTIONAL_JAVA_CONSTANTS, BOOLEAN, true );
			this.procedureParameterNullPassingEnabled = cfgService.getSetting( PROCEDURE_NULL_PARAM_PASSING, BOOLEAN, false );
			this.collectionJoinSubqueryRewriteEnabled = cfgService.getSetting( COLLECTION_JOIN_SUBQUERY, BOOLEAN, true );

			this.secondLevelCacheEnabled = cfgService.getSetting( USE_SECOND_LEVEL_CACHE, BOOLEAN, true );
			this.queryCacheEnabled = cfgService.getSetting( USE_QUERY_CACHE, BOOLEAN, false );
			this.queryCacheFactory = strategySelector.resolveDefaultableStrategy(
					QueryCacheFactory.class,
					configurationSettings.get( QUERY_CACHE_FACTORY ),
					StandardQueryCacheFactory.INSTANCE
			);
			this.cacheRegionPrefix = ConfigurationHelper.extractPropertyValue(
					CACHE_REGION_PREFIX,
					configurationSettings
			);
			this.minimalPutsEnabled = cfgService.getSetting(
					USE_MINIMAL_PUTS,
					BOOLEAN,
					serviceRegistry.getService( RegionFactory.class ).isMinimalPutsEnabledByDefault()
			);
			this.structuredCacheEntriesEnabled = cfgService.getSetting( USE_STRUCTURED_CACHE, BOOLEAN, false );
			this.directReferenceCacheEntriesEnabled = cfgService.getSetting( USE_DIRECT_REFERENCE_CACHE_ENTRIES,BOOLEAN, false );
			this.autoEvictCollectionCache = cfgService.getSetting( AUTO_EVICT_COLLECTION_CACHE, BOOLEAN, false );

			try {
				this.schemaAutoTooling = SchemaAutoTooling.interpret( (String) configurationSettings.get( AvailableSettings.HBM2DDL_AUTO ) );
			}
			catch (Exception e) {
				log.warn( e.getMessage() + "  Ignoring" );
			}


			final ExtractedDatabaseMetaData meta = jdbcServices.getExtractedMetaDataSupport();

			this.tempTableDdlTransactionHandling = TempTableDdlTransactionHandling.NONE;
			if ( meta.doesDataDefinitionCauseTransactionCommit() ) {
				if ( meta.supportsDataDefinitionInTransaction() ) {
					this.tempTableDdlTransactionHandling = TempTableDdlTransactionHandling.ISOLATE_AND_TRANSACT;
				}
				else {
					this.tempTableDdlTransactionHandling = TempTableDdlTransactionHandling.ISOLATE;
				}
			}

			this.jdbcBatchSize = ConfigurationHelper.getInt( STATEMENT_BATCH_SIZE, configurationSettings, 0 );
			if ( !meta.supportsBatchUpdates() ) {
				this.jdbcBatchSize = 0;
			}

			this.jdbcBatchVersionedData = ConfigurationHelper.getBoolean( BATCH_VERSIONED_DATA, configurationSettings, true );
			this.scrollableResultSetsEnabled = ConfigurationHelper.getBoolean(
					USE_SCROLLABLE_RESULTSET,
					configurationSettings,
					meta.supportsScrollableResults()
			);
			this.wrapResultSetsEnabled = ConfigurationHelper.getBoolean(
					WRAP_RESULT_SETS,
					configurationSettings,
					false
			);
			this.getGeneratedKeysEnabled = ConfigurationHelper.getBoolean(
					USE_GET_GENERATED_KEYS,
					configurationSettings,
					meta.supportsGetGeneratedKeys()
			);
			this.jdbcFetchSize = ConfigurationHelper.getInteger( STATEMENT_FETCH_SIZE, configurationSettings );

			this.connectionHandlingMode = interpretConnectionHandlingMode( configurationSettings, serviceRegistry );

			this.commentsEnabled = ConfigurationHelper.getBoolean( USE_SQL_COMMENTS, configurationSettings );

			this.preferUserTransaction = ConfigurationHelper.getBoolean( PREFER_USER_TRANSACTION, configurationSettings, false  );

			this.allowOutOfTransactionUpdateOperations = ConfigurationHelper.getBoolean(
					ALLOW_UPDATE_OUTSIDE_TRANSACTION,
					configurationSettings,
					false
			);

			this.releaseResourcesOnCloseEnabled = ConfigurationHelper.getBoolean(
					DISCARD_PC_ON_CLOSE,
					configurationSettings,
					false
			);
			Object jdbcTimeZoneValue = configurationSettings.get(
					JDBC_TIME_ZONE
			);

			if ( jdbcTimeZoneValue instanceof TimeZone ) {
				this.jdbcTimeZone = (TimeZone) jdbcTimeZoneValue;
			}
			else if ( jdbcTimeZoneValue instanceof ZoneId ) {
				this.jdbcTimeZone = TimeZone.getTimeZone( (ZoneId) jdbcTimeZoneValue );
			}
			else if ( jdbcTimeZoneValue instanceof String ) {
				this.jdbcTimeZone = TimeZone.getTimeZone( ZoneId.of((String) jdbcTimeZoneValue) );
			}
			else if ( jdbcTimeZoneValue != null ) {
				throw new IllegalArgumentException( "Configuration property " + JDBC_TIME_ZONE + " value [" + jdbcTimeZoneValue + "] is not supported!" );
			}
		}

		private static Interceptor determineInterceptor(Map configurationSettings, StrategySelector strategySelector) {
			Object setting = configurationSettings.get( INTERCEPTOR );
			if ( setting == null ) {
				// try the legacy (deprecated) JPA name
				setting = configurationSettings.get( org.hibernate.jpa.AvailableSettings.INTERCEPTOR );
				if ( setting != null ) {
					DeprecationLogger.DEPRECATION_LOGGER.deprecatedSetting(
							org.hibernate.jpa.AvailableSettings.INTERCEPTOR,
							INTERCEPTOR
					);
				}
			}

			return strategySelector.resolveStrategy(
					Interceptor.class,
					setting
			);
		}

		@SuppressWarnings("unchecked")
		private static Class<? extends Interceptor> determineStatelessInterceptorClass(
				Map configurationSettings,
				StrategySelector strategySelector) {
			Object setting = configurationSettings.get( SESSION_SCOPED_INTERCEPTOR );
			if ( setting == null ) {
				// try the legacy (deprecated) JPA name
				setting = configurationSettings.get( org.hibernate.jpa.AvailableSettings.SESSION_INTERCEPTOR );
				if ( setting != null ) {
					DeprecationLogger.DEPRECATION_LOGGER.deprecatedSetting(
							org.hibernate.jpa.AvailableSettings.SESSION_INTERCEPTOR,
							SESSION_SCOPED_INTERCEPTOR
					);
				}
			}

			if ( setting == null ) {
				return null;
			}
			else if ( setting instanceof Class ) {
				return (Class<? extends Interceptor>) setting;
			}
			else {
				return strategySelector.selectStrategyImplementor(
						Interceptor.class,
						setting.toString()
				);
			}

		}

		private PhysicalConnectionHandlingMode interpretConnectionHandlingMode(
				Map configurationSettings,
				StandardServiceRegistry serviceRegistry) {
			final PhysicalConnectionHandlingMode specifiedHandlingMode = PhysicalConnectionHandlingMode.interpret(
					configurationSettings.get( CONNECTION_HANDLING )
			);

			if ( specifiedHandlingMode != null ) {
				return specifiedHandlingMode;
			}


			final TransactionCoordinatorBuilder transactionCoordinatorBuilder = serviceRegistry.getService( TransactionCoordinatorBuilder.class );

			// see if the deprecated ConnectionAcquisitionMode/ConnectionReleaseMode were used..
			final ConnectionAcquisitionMode specifiedAcquisitionMode = ConnectionAcquisitionMode.interpret(
					configurationSettings.get( ACQUIRE_CONNECTIONS )
			);
			final ConnectionReleaseMode specifiedReleaseMode = ConnectionReleaseMode.interpret(
					configurationSettings.get( RELEASE_CONNECTIONS )
			);
			if ( specifiedAcquisitionMode != null || specifiedReleaseMode != null ) {
				return interpretConnectionHandlingMode( specifiedAcquisitionMode, specifiedReleaseMode, configurationSettings, transactionCoordinatorBuilder );
			}

			return transactionCoordinatorBuilder.getDefaultConnectionHandlingMode();
		}

		/**
		 * @deprecated since 5.2
		 */
		@Deprecated
		private PhysicalConnectionHandlingMode interpretConnectionHandlingMode(
				ConnectionAcquisitionMode specifiedAcquisitionMode,
				ConnectionReleaseMode specifiedReleaseMode,
				Map configurationSettings,
				TransactionCoordinatorBuilder transactionCoordinatorBuilder) {
			DeprecationLogger.DEPRECATION_LOGGER.logUseOfDeprecatedConnectionHandlingSettings();

			final ConnectionAcquisitionMode effectiveAcquisitionMode = specifiedAcquisitionMode == null
					? ConnectionAcquisitionMode.AS_NEEDED
					: specifiedAcquisitionMode;

			final ConnectionReleaseMode effectiveReleaseMode;
			if ( specifiedReleaseMode == null ) {
				// check the actual setting.  If we get in here it *should* be "auto" or null
				final String releaseModeName = ConfigurationHelper.getString( RELEASE_CONNECTIONS, configurationSettings, "auto" );
				assert "auto".equalsIgnoreCase( releaseModeName );
				// nothing was specified (or someone happened to configure the "magic" value)
				if ( effectiveAcquisitionMode == ConnectionAcquisitionMode.IMMEDIATELY ) {
					effectiveReleaseMode = ConnectionReleaseMode.ON_CLOSE;
				}
				else {
					effectiveReleaseMode = transactionCoordinatorBuilder.getDefaultConnectionReleaseMode();
				}
			}
			else {
				effectiveReleaseMode = specifiedReleaseMode;
			}

			return PhysicalConnectionHandlingMode.interpret( effectiveAcquisitionMode, effectiveReleaseMode );
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

		public boolean isAllowRefreshDetachedEntity() {
			return allowRefreshDetachedEntity;
		}

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
		public boolean isSessionFactoryNameAlsoJndiName() {
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
		public Class<? extends Interceptor> getStatelessInterceptorImplementor() {
			return statelessInterceptorClass;
		}

		@Override
		public StatementInspector getStatementInspector() {
			return statementInspector;
		}

		@Override
		public SessionFactoryObserver[] getSessionFactoryObservers() {
			return sessionFactoryObserverList.toArray( new SessionFactoryObserver[ sessionFactoryObserverList.size() ] );
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
		public EntityMode getDefaultEntityMode() {
			return defaultEntityMode;
		}

		@Override
		public EntityTuplizerFactory getEntityTuplizerFactory() {
			return entityTuplizerFactory;
		}

		@Override
		public boolean isCheckNullability() {
			return checkNullability;
		}

		@Override
		public boolean isInitializeLazyStateOutsideTransactionsEnabled() {
			return initializeLazyStateOutsideTransactions;
		}

		@Override
		public MultiTableBulkIdStrategy getMultiTableBulkIdStrategy() {
			return multiTableBulkIdStrategy;
		}

		@Override
		public TempTableDdlTransactionHandling getTempTableDdlTransactionHandling() {
			return tempTableDdlTransactionHandling;
		}

		@Override
		public BatchFetchStyle getBatchFetchStyle() {
			return batchFetchStyle;
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
		public NullPrecedence getDefaultNullPrecedence() {
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
		public MultiTenancyStrategy getMultiTenancyStrategy() {
			return multiTenancyStrategy;
		}

		@Override
		public CurrentTenantIdentifierResolver getCurrentTenantIdentifierResolver() {
			return currentTenantIdentifierResolver;
		}

		@Override
		public boolean isJtaTrackByThread() {
			return jtaTrackByThread;
		}

		@Override
		public Map getQuerySubstitutions() {
			return querySubstitutions;
		}

		@Override
		public boolean isStrictJpaQueryLanguageCompliance() {
			return strictJpaQueryLanguageCompliance;
		}

		@Override
		public boolean isNamedQueryStartupCheckingEnabled() {
			return namedQueryStartupCheckingEnabled;
		}

		public boolean isConventionalJavaConstants() {
			return conventionalJavaConstants;
		}

		@Override
		public boolean isProcedureParameterNullPassingEnabled() {
			return procedureParameterNullPassingEnabled;
		}

		@Override
		public boolean isCollectionJoinSubqueryRewriteEnabled() {
			return collectionJoinSubqueryRewriteEnabled;
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
		public QueryCacheFactory getQueryCacheFactory() {
			return queryCacheFactory;
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

		@Override
		public SchemaAutoTooling getSchemaAutoTooling() {
			return schemaAutoTooling;
		}

		@Override
		public int getJdbcBatchSize() {
			return jdbcBatchSize;
		}

		@Override
		public boolean isJdbcBatchVersionedData() {
			return jdbcBatchVersionedData;
		}

		@Override
		public boolean isScrollableResultSetsEnabled() {
			return scrollableResultSetsEnabled;
		}

		@Override
		public boolean isWrapResultSetsEnabled() {
			return wrapResultSetsEnabled;
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
		public ConnectionReleaseMode getConnectionReleaseMode() {
			return getPhysicalConnectionHandlingMode().getReleaseMode();
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
			return entityNameResolvers.toArray( new EntityNameResolver[ entityNameResolvers.size() ] );
		}

		@Override
		public EntityNotFoundDelegate getEntityNotFoundDelegate() {
			return entityNotFoundDelegate;
		}

		@Override
		public Map<String, SQLFunction> getCustomSqlFunctionMap() {
			return sqlFunctions == null ? Collections.<String, SQLFunction>emptyMap() : sqlFunctions;
		}

		@Override
		public boolean isPreferUserTransaction() {
			return this.preferUserTransaction;
		}

		@Override
		public TimeZone getJdbcTimeZone() {
			return this.jdbcTimeZone;
		}
	}

	@Override
	public StandardServiceRegistry getServiceRegistry() {
		return options.getServiceRegistry();
	}

	@Override
	public boolean isJpaBootstrap() {
		return options.isJpaBootstrap();
	}

	@Override
	public boolean isJtaTransactionAccessEnabled() {
		return options.isJtaTransactionAccessEnabled();
	}

	public boolean isAllowRefreshDetachedEntity() {
		return options.isAllowRefreshDetachedEntity();
	}

	public boolean isAllowOutOfTransactionUpdateOperations() {
		return options.isAllowOutOfTransactionUpdateOperations();
	}

	@Override
	public boolean isReleaseResourcesOnCloseEnabled(){
		return options.releaseResourcesOnCloseEnabled;
	}

	@Override
	public Object getBeanManagerReference() {
		return options.getBeanManagerReference();
	}

	@Override
	public Object getValidatorFactoryReference() {
		return options.getValidatorFactoryReference();
	}

	@Override
	public String getSessionFactoryName() {
		return options.getSessionFactoryName();
	}

	@Override
	public boolean isSessionFactoryNameAlsoJndiName() {
		return options.isSessionFactoryNameAlsoJndiName();
	}

	@Override
	public boolean isFlushBeforeCompletionEnabled() {
		return options.isFlushBeforeCompletionEnabled();
	}

	@Override
	public boolean isAutoCloseSessionEnabled() {
		return options.isAutoCloseSessionEnabled();
	}

	@Override
	public boolean isStatisticsEnabled() {
		return options.isStatisticsEnabled();
	}

	@Override
	public Interceptor getInterceptor() {
		return options.getInterceptor();
	}

	@Override
	public Class<? extends Interceptor> getStatelessInterceptorImplementor() {
		return options.getStatelessInterceptorImplementor();
	}

	@Override
	public StatementInspector getStatementInspector() {
		return options.getStatementInspector();
	}

	@Override
	public SessionFactoryObserver[] getSessionFactoryObservers() {
		return options.getSessionFactoryObservers();
	}

	@Override
	public BaselineSessionEventsListenerBuilder getBaselineSessionEventsListenerBuilder() {
		return options.getBaselineSessionEventsListenerBuilder();
	}

	@Override
	public boolean isIdentifierRollbackEnabled() {
		return options.isIdentifierRollbackEnabled();
	}

	@Override
	public EntityMode getDefaultEntityMode() {
		return options.getDefaultEntityMode();
	}

	@Override
	public EntityTuplizerFactory getEntityTuplizerFactory() {
		return options.getEntityTuplizerFactory();
	}

	@Override
	public boolean isCheckNullability() {
		return options.isCheckNullability();
	}

	@Override
	public boolean isInitializeLazyStateOutsideTransactionsEnabled() {
		return options.isInitializeLazyStateOutsideTransactionsEnabled();
	}

	@Override
	public MultiTableBulkIdStrategy getMultiTableBulkIdStrategy() {
		return options.getMultiTableBulkIdStrategy();
	}

	@Override
	public TempTableDdlTransactionHandling getTempTableDdlTransactionHandling() {
		return options.getTempTableDdlTransactionHandling();
	}

	@Override
	public BatchFetchStyle getBatchFetchStyle() {
		return options.getBatchFetchStyle();
	}

	@Override
	public int getDefaultBatchFetchSize() {
		return options.getDefaultBatchFetchSize();
	}

	@Override
	public Integer getMaximumFetchDepth() {
		return options.getMaximumFetchDepth();
	}

	@Override
	public NullPrecedence getDefaultNullPrecedence() {
		return options.getDefaultNullPrecedence();
	}

	@Override
	public boolean isOrderUpdatesEnabled() {
		return options.isOrderUpdatesEnabled();
	}

	@Override
	public boolean isOrderInsertsEnabled() {
		return options.isOrderInsertsEnabled();
	}

	@Override
	public MultiTenancyStrategy getMultiTenancyStrategy() {
		return options.getMultiTenancyStrategy();
	}

	@Override
	public CurrentTenantIdentifierResolver getCurrentTenantIdentifierResolver() {
		return options.getCurrentTenantIdentifierResolver();
	}

	@Override
	public boolean isJtaTrackByThread() {
		return options.isJtaTrackByThread();
	}

	@Override
	public Map getQuerySubstitutions() {
		return options.getQuerySubstitutions();
	}

	@Override
	public boolean isStrictJpaQueryLanguageCompliance() {
		return options.isStrictJpaQueryLanguageCompliance();
	}

	@Override
	public boolean isNamedQueryStartupCheckingEnabled() {
		return options.isNamedQueryStartupCheckingEnabled();
	}

	public boolean isConventionalJavaConstants() {
		return options.isConventionalJavaConstants();
	}

	@Override
	public boolean isProcedureParameterNullPassingEnabled() {
		return options.isProcedureParameterNullPassingEnabled();
	}

	@Override
	public boolean isCollectionJoinSubqueryRewriteEnabled() {
		return options.isCollectionJoinSubqueryRewriteEnabled();
	}

	@Override
	public boolean isSecondLevelCacheEnabled() {
		return options.isSecondLevelCacheEnabled();
	}

	@Override
	public boolean isQueryCacheEnabled() {
		return options.isQueryCacheEnabled();
	}

	@Override
	public QueryCacheFactory getQueryCacheFactory() {
		return options.getQueryCacheFactory();
	}

	@Override
	public String getCacheRegionPrefix() {
		return options.getCacheRegionPrefix();
	}

	@Override
	public boolean isMinimalPutsEnabled() {
		return options.isMinimalPutsEnabled();
	}

	@Override
	public boolean isStructuredCacheEntriesEnabled() {
		return options.isStructuredCacheEntriesEnabled();
	}

	@Override
	public boolean isDirectReferenceCacheEntriesEnabled() {
		return options.isDirectReferenceCacheEntriesEnabled();
	}

	@Override
	public boolean isAutoEvictCollectionCache() {
		return options.isAutoEvictCollectionCache();
	}

	@Override
	public SchemaAutoTooling getSchemaAutoTooling() {
		return options.getSchemaAutoTooling();
	}

	@Override
	public int getJdbcBatchSize() {
		return options.getJdbcBatchSize();
	}

	@Override
	public boolean isJdbcBatchVersionedData() {
		return options.isJdbcBatchVersionedData();
	}

	@Override
	public boolean isScrollableResultSetsEnabled() {
		return options.isScrollableResultSetsEnabled();
	}

	@Override
	public boolean isWrapResultSetsEnabled() {
		return options.isWrapResultSetsEnabled();
	}

	@Override
	public boolean isGetGeneratedKeysEnabled() {
		return options.isGetGeneratedKeysEnabled();
	}

	@Override
	public Integer getJdbcFetchSize() {
		return options.getJdbcFetchSize();
	}

	@Override
	public PhysicalConnectionHandlingMode getPhysicalConnectionHandlingMode() {
		return options.getPhysicalConnectionHandlingMode();
	}

	@Override
	public ConnectionReleaseMode getConnectionReleaseMode() {
		return getPhysicalConnectionHandlingMode().getReleaseMode();
	}

	@Override
	public boolean isCommentsEnabled() {
		return options.isCommentsEnabled();
	}

	@Override
	public CustomEntityDirtinessStrategy getCustomEntityDirtinessStrategy() {
		return options.getCustomEntityDirtinessStrategy();
	}

	@Override
	public EntityNameResolver[] getEntityNameResolvers() {
		return options.getEntityNameResolvers();
	}

	@Override
	public EntityNotFoundDelegate getEntityNotFoundDelegate() {
		return options.getEntityNotFoundDelegate();
	}

	@Override
	public Map<String, SQLFunction> getCustomSqlFunctionMap() {
		return options.getCustomSqlFunctionMap();
	}

	@Override
	public boolean isPreferUserTransaction() {
		return options.isPreferUserTransaction();
	}

	@Override
	public TimeZone getJdbcTimeZone() {
		return options.getJdbcTimeZone();
	}
}
