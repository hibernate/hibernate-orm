/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.spi;

import java.util.Map;
import java.util.TimeZone;
import java.util.function.Supplier;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import org.hibernate.CacheMode;
import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.EntityNameResolver;
import org.hibernate.FlushMode;
import org.hibernate.Incubating;
import org.hibernate.Interceptor;
import org.hibernate.Internal;
import org.hibernate.LockOptions;
import org.hibernate.SessionEventListener;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.type.TimeZoneStorageStrategy;
import org.hibernate.annotations.CacheLayout;
import org.hibernate.boot.SchemaAutoTooling;
import org.hibernate.boot.TempTableDdlTransactionHandling;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.cache.spi.TimestampsCache;
import org.hibernate.cache.spi.TimestampsCacheFactory;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.BaselineSessionEventsListenerBuilder;
import org.hibernate.jpa.spi.JpaCompliance;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.query.spi.QueryEngineOptions;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.ObjectJavaType;
import org.hibernate.type.format.FormatMapper;

import jakarta.persistence.criteria.Nulls;

import static org.hibernate.internal.log.DeprecationLogger.DEPRECATION_LOGGER;

/**
 * Aggregator of special options used to build the {@link org.hibernate.SessionFactory}.
 *
 * @apiNote This type belongs to an SPI package. Due to a historical mistake, it is exposed
 * via the deprecated method {@link org.hibernate.SessionFactory#getSessionFactoryOptions}.
 *
 * @since 5.0
 *
 * @see SessionFactoryImplementor#getSessionFactoryOptions
 */
public interface SessionFactoryOptions extends QueryEngineOptions {
	/**
	 * Get the UUID unique to this SessionFactoryOptions.  Will be the
	 * same value available as {@link SessionFactoryImplementor#getUuid()}.
	 *
	 * @apiNote The value is generated as a {@link java.util.UUID}, but kept
	 * as a String.
	 *
	 * @return The UUID for this SessionFactory.
	 *
	 * @see org.hibernate.internal.SessionFactoryRegistry#getSessionFactory
	 * @see SessionFactoryImplementor#getUuid
	 */
	@Override
	String getUuid();

	/**
	 * The service registry to use in building the factory.
	 *
	 * @return The service registry to use.
	 */
	StandardServiceRegistry getServiceRegistry();

	/**
	 * @see org.hibernate.cfg.ManagedBeanSettings#JAKARTA_CDI_BEAN_MANAGER
	 */
	Object getBeanManagerReference();

	/**
	 * @see org.hibernate.cfg.ValidationSettings#JAKARTA_VALIDATION_FACTORY
	 */
	Object getValidatorFactoryReference();

	/**
	 * @see org.hibernate.cfg.JpaComplianceSettings
	 */
	@Override
	JpaCompliance getJpaCompliance();

	/**
	 * Was building of the {@link org.hibernate.SessionFactory} initiated through JPA
	 * bootstrapping, or through Hibernate-native bootstrapping?
	 *
	 * @return {@code true} indicates the SessionFactory was built through JPA
	 * bootstrapping; {@code false} indicates it was built through native bootstrapping.
	 */
	boolean isJpaBootstrap();

	/**
	 * @deprecated with no replacement.
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	default boolean isAllowRefreshDetachedEntity() {
		DEPRECATION_LOGGER.deprecatedRefreshLockDetachedEntity();
		return false;
	}

	/**
	 * The name to be used for the {@code SessionFactory}. This is used during in-VM serialization;
	 * see {@link org.hibernate.internal.SessionFactoryRegistry}. May also be used as a JNDI name,
	 * depending on {@value org.hibernate.cfg.PersistenceSettings#SESSION_FACTORY_JNDI_NAME} and
	 * {@value org.hibernate.cfg.PersistenceSettings#SESSION_FACTORY_NAME_IS_JNDI}.
	 *
	 * @return The session factory name
	 *
	 * @see org.hibernate.cfg.PersistenceSettings#SESSION_FACTORY_NAME
	 */
	@Override
	String getSessionFactoryName();

	/**
	 * Is the {@linkplain #getSessionFactoryName session factory name} also a JNDI name, indicating
	 * we should bind it into JNDI?
	 *
	 * @return {@code true} if the SessionFactory name is also a JNDI name; {@code false} otherwise.
	 *
	 * @see org.hibernate.cfg.PersistenceSettings#SESSION_FACTORY_NAME_IS_JNDI
	 */
	Boolean isSessionFactoryNameAlsoJndiName();

	/**
	 * Is collection of {@linkplain org.hibernate.stat.Statistics statistics}
	 * enabled by default for this factory?
	 *
	 * @see org.hibernate.cfg.StatisticsSettings#GENERATE_STATISTICS
	 *
	 * @see org.hibernate.stat.Statistics#setStatisticsEnabled(boolean)
	 */
	boolean isStatisticsEnabled();

	/**
	 * An {@linkplain Interceptor interceptor} instance shared between all sessions
	 * created by this factory. Such an interceptor must be thread-safe and may not
	 * hold state associated with any given session.
	 *
	 * @return The interceptor instance to use factory-wide by default. May be {@code null}.
	 *
	 * @see org.hibernate.cfg.SessionEventSettings#INTERCEPTOR
	 *
	 * @see org.hibernate.SessionBuilder#interceptor(Interceptor)
	 */
	Interceptor getInterceptor();

	/**
	 * A stateless {@link Supplier} for {@linkplain Interceptor interceptor} instances
	 * which are not shared between sessions created by this factory. This allows each
	 * {@code Interceptor} instances itself to hold state associated with its session.
	 *
	 * @return The interceptor supplier to use by default. May be {@code null}.
	 *
	 * @see org.hibernate.cfg.SessionEventSettings#SESSION_SCOPED_INTERCEPTOR
	 *
	 * @see org.hibernate.SessionBuilder#interceptor(Interceptor)
	 */
	Supplier<? extends Interceptor> getStatelessInterceptorImplementorSupplier();

	/**
	 * The default {@link StatementInspector} for this factory.
	 *
	 * @see org.hibernate.cfg.JdbcSettings#STATEMENT_INSPECTOR
	 *
	 * @see org.hibernate.SessionBuilder#statementInspector(StatementInspector)
	 */
	StatementInspector getStatementInspector();

	/**
	 * {@linkplain SessionFactoryObserver Observers} for events raised by the factory.
	 *
	 * @see org.hibernate.cfg.AvailableSettings#SESSION_FACTORY_OBSERVER
	 */
	SessionFactoryObserver[] getSessionFactoryObservers();

	/**
	 * @deprecated This operation is a layer-breaker, exposing an
	 *             internal type. It will be removed. Use
	 *             {@link #buildSessionEventListeners()} instead.
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	BaselineSessionEventsListenerBuilder getBaselineSessionEventsListenerBuilder();

	/**
	 * Build an array of baseline {@link SessionEventListener}s.
	 *
	 * @since 7.0
	 *
	 * @see org.hibernate.SessionBuilder#eventListeners(SessionEventListener...)
	 */
	default SessionEventListener[] buildSessionEventListeners() {
		return getBaselineSessionEventsListenerBuilder().buildBaseline();
	}

	/**
	 * Should generated identifiers be reset after entity removal?
	 *
	 * @see org.hibernate.cfg.AvailableSettings#USE_IDENTIFIER_ROLLBACK
	 *
	 * @see org.hibernate.SessionBuilder#identifierRollback(boolean)
	 */
	boolean isIdentifierRollbackEnabled();

	/**
	 * Should Hibernate validate that non-optional attributes have non-null values?
	 *
	 * @see org.hibernate.cfg.ValidationSettings#CHECK_NULLABILITY
	 */
	boolean isCheckNullability();

	/**
	 * Allows Bean Validation to disable null checking.
	 *
	 * @apiNote It's quite ugly to have a setter method on this SPI.
	 *          This operation is for internal use by Bean Validation
	 *          integration logic.
	 */
	@Internal
	void setCheckNullability(boolean enabled);

	/**
	 * Are detached proxies and collections fetched in a temporary
	 * persistence context?
	 *
	 * @apiNote The naming here is very misleading. This behavior has
	 *          little to do with transactions.
	 *
	 * @see org.hibernate.cfg.AvailableSettings#ENABLE_LAZY_LOAD_NO_TRANS
	 */
	boolean isInitializeLazyStateOutsideTransactionsEnabled();

	/**
	 * This is unused and will be removed.
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	TempTableDdlTransactionHandling getTempTableDdlTransactionHandling();

	/**
	 * @see org.hibernate.cfg.AvailableSettings#DELAY_ENTITY_LOADER_CREATIONS
	 *
	 * @deprecated This is no longer used
	 */
	@Deprecated(since = "7", forRemoval = true)
	boolean isDelayBatchFetchLoaderCreationsEnabled();

	/**
	 * The default batch size for batch fetching in new sessions.
	 *
	 * @see org.hibernate.cfg.FetchSettings#DEFAULT_BATCH_FETCH_SIZE
	 *
	 * @see org.hibernate.Session#setFetchBatchSize(int)
	 */
	int getDefaultBatchFetchSize();

	/**
	 * @see org.hibernate.cfg.FetchSettings#MAX_FETCH_DEPTH
	 */
	Integer getMaximumFetchDepth();

	/**
	 * Is subselect fetching enabled by default in new sessions?
	 *
	 * @see org.hibernate.cfg.FetchSettings#USE_SUBSELECT_FETCH
	 *
	 * @see org.hibernate.Session#setSubselectFetchingEnabled(boolean)
	 */
	boolean isSubselectFetchEnabled();

	/**
	 * The default {@linkplain Nulls precedence of null values} in sorted query results.
	 *
	 * @see org.hibernate.cfg.QuerySettings#DEFAULT_NULL_ORDERING
	 */
	Nulls getDefaultNullPrecedence();

	/**
	 * @see org.hibernate.cfg.BatchSettings#ORDER_UPDATES
	 */
	boolean isOrderUpdatesEnabled();

	/**
	 * @see org.hibernate.cfg.BatchSettings#ORDER_INSERTS
	 */
	boolean isOrderInsertsEnabled();

	/**
	 * Is there a
	 * {@linkplain org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider
	 * multi-tenant connection provider} configured?
	 *
	 * @see org.hibernate.cfg.MultiTenancySettings#MULTI_TENANT_CONNECTION_PROVIDER
	 */
	boolean isMultiTenancyEnabled();

	/**
	 * Obtain a reference to the
	 * {@linkplain CurrentTenantIdentifierResolver current tenant identifier resolver},
	 * if any, or return {@code null} is there is no resolver configured.
	 *
	 * @see org.hibernate.cfg.MultiTenancySettings#MULTI_TENANT_IDENTIFIER_RESOLVER
	 */
	CurrentTenantIdentifierResolver<Object> getCurrentTenantIdentifierResolver();

	/**
	 * @see org.hibernate.cfg.TransactionSettings#JTA_TRACK_BY_THREAD
	 */
	boolean isJtaTrackByThread();

	/**
	 * Are named queries validated when the factory is created?
	 *
	 * @see org.hibernate.cfg.QuerySettings#QUERY_STARTUP_CHECKING
	 */
	boolean isNamedQueryStartupCheckingEnabled();

	/**
	 * Is the {@linkplain org.hibernate.Cache second-level cache} enabled?
	 *
	 * @see org.hibernate.cfg.CacheSettings#USE_SECOND_LEVEL_CACHE
	 */
	boolean isSecondLevelCacheEnabled();

	/**
	 * Is the {@linkplain org.hibernate.cache.spi.QueryResultsCache query cache} enabled?
	 *
	 * @see org.hibernate.cfg.CacheSettings#USE_QUERY_CACHE
	 */
	boolean isQueryCacheEnabled();

	/**
	 * The {@linkplain CacheLayout layout} of entries in the query cache.
	 *
	 * @see org.hibernate.cfg.CacheSettings#QUERY_CACHE_LAYOUT
	 */
	@Incubating
	CacheLayout getQueryCacheLayout();

	/**
	 * A factory for the {@link TimestampsCache} used to track invalidation
	 * of cached query result sets.
	 *
	 * @see org.hibernate.cfg.CacheSettings#QUERY_CACHE_FACTORY
	 */
	TimestampsCacheFactory getTimestampsCacheFactory();

	/**
	 * A factory-specific prefix to be applied to all cache region names.
	 *
	 * @see org.hibernate.cfg.CacheSettings#CACHE_REGION_PREFIX
	 */
	String getCacheRegionPrefix();

	/**
	 * Should we avoid overwriting cache entries with identical entries?
	 * <p>
	 * This is useful if cache writes are much more expensive than cache
	 * reads.
	 *
	 * @apiNote This is currently unused.
	 *
	 * @see org.hibernate.cfg.CacheSettings#USE_MINIMAL_PUTS
	 */
	boolean isMinimalPutsEnabled();

	/**
	 * Are second-level cache entries stored in a human-readable format.
	 *
	 * @see org.hibernate.cfg.CacheSettings#USE_STRUCTURED_CACHE
	 */
	boolean isStructuredCacheEntriesEnabled();

	/**
	 * @see org.hibernate.cfg.CacheSettings#USE_DIRECT_REFERENCE_CACHE_ENTRIES
	 */
	boolean isDirectReferenceCacheEntriesEnabled();

	/**
	 * @see org.hibernate.cfg.CacheSettings#AUTO_EVICT_COLLECTION_CACHE
	 */
	boolean isAutoEvictCollectionCache();

	/**
	 * @see org.hibernate.cfg.AvailableSettings#HBM2DDL_AUTO
	 *
	 * @deprecated This is unused and will be removed.
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	SchemaAutoTooling getSchemaAutoTooling();

	/**
	 * The default JDBC statement batch size for new sessions.
	 *
	 * @see org.hibernate.cfg.BatchSettings#STATEMENT_BATCH_SIZE
	 *
	 * @see org.hibernate.Session#setJdbcBatchSize(Integer)
	 */
	int getJdbcBatchSize();

	/**
	 * @see org.hibernate.cfg.JdbcSettings#USE_SCROLLABLE_RESULTSET
	 */
	boolean isScrollableResultSetsEnabled();

	/**
	 * @see org.hibernate.cfg.JdbcSettings#USE_GET_GENERATED_KEYS
	 */
	boolean isGetGeneratedKeysEnabled();

	/**
	 * @see org.hibernate.cfg.JdbcSettings#STATEMENT_FETCH_SIZE
	 */
	Integer getJdbcFetchSize();

	/**
	 * @see org.hibernate.cfg.JdbcSettings#CONNECTION_HANDLING
	 *
	 * @see org.hibernate.SessionBuilder#connectionHandlingMode(PhysicalConnectionHandlingMode)
	 */
	PhysicalConnectionHandlingMode getPhysicalConnectionHandlingMode();

	/**
	 * Do newly-obtained JDBC connections come with
	 * {@linkplain java.sql.Connection#getAutoCommit autocommit} already disabled?
	 *
	 * @see org.hibernate.cfg.JdbcSettings#CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT
	 */
	boolean doesConnectionProviderDisableAutoCommit();

	/**
	 * Should explanatory comment text be added to the generated SQL?
	 *
	 * @see org.hibernate.cfg.JdbcSettings#USE_SQL_COMMENTS
	 */
	boolean isCommentsEnabled();

	/**
	 * A {@linkplain CustomEntityDirtinessStrategy custom strategy} for determining
	 * if an entity instance is dirty.
	 *
	 * @see org.hibernate.cfg.AvailableSettings#CUSTOM_ENTITY_DIRTINESS_STRATEGY
	 */
	CustomEntityDirtinessStrategy getCustomEntityDirtinessStrategy();

	/**
	 * An array of custom {@linkplain EntityNameResolver entity name resolvers}.
	 *
	 * @see org.hibernate.cfg.Configuration#addEntityNameResolver(EntityNameResolver)
	 */
	EntityNameResolver[] getEntityNameResolvers();

	/**
	 * Get the delegate for handling entity-not-found exception conditions.
	 * <p>
	 * Returns {@link org.hibernate.boot.internal.StandardEntityNotFoundDelegate}
	 * by default.
	 *
	 * @return The specific {@link EntityNotFoundDelegate} to use, may be {@code null}
	 *
	 * @see org.hibernate.cfg.Configuration#setEntityNotFoundDelegate
	 */
	EntityNotFoundDelegate getEntityNotFoundDelegate();

	/**
	 * @see org.hibernate.cfg.TransactionSettings#FLUSH_BEFORE_COMPLETION
	 */
	boolean isFlushBeforeCompletionEnabled();

	/**
	 * @see org.hibernate.cfg.TransactionSettings#AUTO_CLOSE_SESSION
	 */
	boolean isAutoCloseSessionEnabled();

	/**
	 * @see org.hibernate.cfg.TransactionSettings#ALLOW_JTA_TRANSACTION_ACCESS
	 */
	boolean isJtaTransactionAccessEnabled();

	/**
	 * @see org.hibernate.cfg.TransactionSettings#PREFER_USER_TRANSACTION
	 */
	boolean isPreferUserTransaction();

	/**
	 * @see org.hibernate.cfg.TransactionSettings#ALLOW_UPDATE_OUTSIDE_TRANSACTION
	 */
	boolean isAllowOutOfTransactionUpdateOperations();

	/**
	 * @see org.hibernate.cfg.AvailableSettings#DISCARD_PC_ON_CLOSE
	 *
	 * @deprecated since {@value org.hibernate.cfg.AvailableSettings#DISCARD_PC_ON_CLOSE}
	 *             is deprecated
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	boolean isReleaseResourcesOnCloseEnabled();

	/**
	 * The timezone to use when interacting with JDBC.
	 *
	 * @see org.hibernate.cfg.JdbcSettings#JDBC_TIME_ZONE
	 *
	 * @see org.hibernate.SessionBuilder#jdbcTimeZone(TimeZone)
	 */
	TimeZone getJdbcTimeZone();

	/**
	 * @see org.hibernate.cfg.AvailableSettings#CRITERIA_COPY_TREE
	 */
	default boolean isCriteriaCopyTreeEnabled() {
		return false;
	}
	/**
	 * @see org.hibernate.cfg.AvailableSettings#CRITERIA_PLAN_CACHE_ENABLED
	 */
	default boolean isCriteriaPlanCacheEnabled() {
		return false;
	}

	/**
	 * @see org.hibernate.cfg.AvailableSettings#NATIVE_IGNORE_JDBC_PARAMETERS
	 */
	default boolean getNativeJdbcParametersIgnored() {
		return false;
	}

	/**
	 * @see org.hibernate.cfg.QuerySettings#FAIL_ON_PAGINATION_OVER_COLLECTION_FETCH
	 */
	boolean isFailOnPaginationOverCollectionFetchEnabled();

	/**
	 * The default catalog to use in generated SQL when a catalog wasn't specified in the mapping,
	 * neither explicitly nor implicitly (see the concept of implicit catalog in XML mapping).
	 *
	 * @return The default catalog to use.
	 *
	 * @see org.hibernate.cfg.MappingSettings#DEFAULT_CATALOG
	 */
	default String getDefaultCatalog() {
		return null;
	}

	/**
	 * The default schema to use in generated SQL when a catalog wasn't specified in the mapping,
	 * neither explicitly nor implicitly (see the concept of implicit schema in XML mapping).
	 *
	 * @return The default schema to use.
	 *
	 * @see org.hibernate.cfg.MappingSettings#DEFAULT_SCHEMA
	 */
	default String getDefaultSchema() {
		return null;
	}

	/**
	 * @see org.hibernate.cfg.AvailableSettings#IN_CLAUSE_PARAMETER_PADDING
	 */
	default boolean inClauseParameterPaddingEnabled() {
		return false;
	}

	/**
	 * The number of {@link org.hibernate.stat.QueryStatistics} entries
	 * that should be stored by {@link org.hibernate.stat.Statistics}.
	 *
	 * @see org.hibernate.cfg.StatisticsSettings#QUERY_STATISTICS_MAX_SIZE
	 */
	int getQueryStatisticsMaxSize();

	/**
	 * Should JPA entity lifecycle callbacks be processed by
	 * the {@link org.hibernate.event.spi.EventEngine} and
	 * {@link org.hibernate.jpa.event.spi.CallbackRegistry}?
	 *
	 * @see org.hibernate.cfg.PersistenceSettings#JPA_CALLBACKS_ENABLED
	 */
	boolean areJPACallbacksEnabled();

	/**
	 * Controls whether Hibernate should try to map named parameter names
	 * specified in a {@link org.hibernate.procedure.ProcedureCall} or
	 * {@link jakarta.persistence.StoredProcedureQuery} to named parameters in
	 * the JDBC {@link java.sql.CallableStatement}.
	 * <p>
	 * As JPA is defined, the use of named parameters is essentially of dubious
	 * value since by spec the parameters have to be defined in the order they are
	 * defined in the procedure/function declaration - we can always bind them
	 * positionally.  The whole idea of named parameters for CallableStatement
	 * is the ability to bind these in any order, but since we unequivocally
	 * know the order anyway binding them via name really gains nothing.
	 * <p>
	 * If this is {@code true}, we still need to make sure the Dialect supports
	 * named binding.  Setting this to {@code false} simply circumvents that
	 * check and always performs positional binding.
	 *
	 * @return {@code true} indicates we should try to use {@link java.sql.CallableStatement}
	 * named parameters, if the Dialect says it is supported; {@code false}
	 * indicates that we should never try to use {@link java.sql.CallableStatement}
	 * named parameters, regardless of what the Dialect says.
	 *
	 * @see org.hibernate.cfg.AvailableSettings#CALLABLE_NAMED_PARAMS_ENABLED
	 */
	boolean isUseOfJdbcNamedParametersEnabled();

	default boolean isCollectionsInDefaultFetchGroupEnabled() {
		return false;
	}

	/**
	 * @see org.hibernate.cfg.PersistenceSettings#UNOWNED_ASSOCIATION_TRANSIENT_CHECK
	 */
	boolean isUnownedAssociationTransientCheck();

	/**
	 * @see org.hibernate.cfg.MappingSettings#PREFERRED_BOOLEAN_JDBC_TYPE
	 */
	@Incubating
	int getPreferredSqlTypeCodeForBoolean();

	/**
	 * @see org.hibernate.cfg.MappingSettings#PREFERRED_DURATION_JDBC_TYPE
	 */
	@Incubating
	int getPreferredSqlTypeCodeForDuration();

	/**
	 * @see org.hibernate.cfg.MappingSettings#PREFERRED_UUID_JDBC_TYPE
	 */
	@Incubating
	int getPreferredSqlTypeCodeForUuid();

	/**
	 * @see org.hibernate.cfg.MappingSettings#PREFERRED_INSTANT_JDBC_TYPE
	 */
	@Incubating
	int getPreferredSqlTypeCodeForInstant();

	/**
	 * @see org.hibernate.cfg.MappingSettings#PREFERRED_ARRAY_JDBC_TYPE
	 */
	@Incubating
	int getPreferredSqlTypeCodeForArray();

	/**
	 * @see org.hibernate.cfg.MappingSettings#TIMEZONE_DEFAULT_STORAGE
	 */
	@Incubating
	TimeZoneStorageStrategy getDefaultTimeZoneStorageStrategy();

	/**
	 * @see org.hibernate.cfg.MappingSettings#JAVA_TIME_USE_DIRECT_JDBC
	 */
	boolean isPreferJavaTimeJdbcTypesEnabled();

	/**
	 * @see org.hibernate.cfg.MappingSettings#PREFER_NATIVE_ENUM_TYPES
	 */
	boolean isPreferNativeEnumTypesEnabled();

	/**
	 * The format mapper to use for serializing/deserializing JSON data.
	 *
	 * @see org.hibernate.cfg.MappingSettings#JSON_FORMAT_MAPPER
	 *
	 * @since 6.0
	 */
	@Incubating
	FormatMapper getJsonFormatMapper();

	/**
	 * The format mapper to use for serializing/deserializing XML data.
	 *
	 * @see org.hibernate.cfg.MappingSettings#XML_FORMAT_MAPPER
	 *
	 * @since 6.0.1
	 */
	@Incubating
	FormatMapper getXmlFormatMapper();

	/**
	 * Whether to use the legacy format for serializing/deserializing XML data.
	 *
	 * @since 7.0
	 * @see org.hibernate.cfg.MappingSettings#XML_FORMAT_MAPPER_LEGACY_FORMAT
	 */
	@Incubating
	boolean isXmlFormatMapperLegacyFormatEnabled();

	/**
	 * The default tenant identifier java type to use, in case no explicit tenant identifier property is defined.
	 *
	 * @since 6.4
	 */
	@Incubating
	default JavaType<Object> getDefaultTenantIdentifierJavaType() {
		return ObjectJavaType.INSTANCE;
	}

	/**
	 * @see org.hibernate.cfg.QuerySettings#QUERY_PASS_PROCEDURE_PARAMETER_NAMES
	 */
	boolean isPassProcedureParameterNames();

	/**
	 * Should native queries return JDBC datetime types
	 * instead of using {@code java.time} types.
	 *
	 * @since 7.0
	 *
	 * @see org.hibernate.cfg.QuerySettings#NATIVE_PREFER_JDBC_DATETIME_TYPES
	 */
	boolean isPreferJdbcDatetimeTypesInNativeQueriesEnabled();

	/**
	 * Determine the default {@link CacheStoreMode}, given the current
	 * {@linkplain org.hibernate.Session#getProperties session properties}.
	 *
	 * @param properties the Session properties
	 * @return either the {@link CacheStoreMode} as defined in the session-specific properties,
	 *         or as defined in the properties shared across all sessions (the defaults).
	 */
	CacheStoreMode getCacheStoreMode(Map<String, Object> properties);

	/**
	 * Determine the default {@link CacheRetrieveMode}, given the current
	 * {@linkplain org.hibernate.Session#getProperties session properties}.
	 *
	 * @param properties the Session properties
	 * @return either the {@link CacheRetrieveMode} as defined in the session-specific properties,
	 *         or as defined in the properties shared across all sessions (the defaults).
	 */
	CacheRetrieveMode getCacheRetrieveMode(Map<String, Object> properties);

	/**
	 * The default initial {@link CacheMode} for new sessions.
	 *
	 * @see org.hibernate.cfg.CacheSettings#JAKARTA_SHARED_CACHE_RETRIEVE_MODE
	 * @see org.hibernate.cfg.CacheSettings#JAKARTA_SHARED_CACHE_STORE_MODE
	 *
	 * @see org.hibernate.Session#setCacheMode(CacheMode)
	 */
	CacheMode getInitialSessionCacheMode();

	/**
	 * The default initial {@link FlushMode} for new sessions.
	 *
	 * @see org.hibernate.jpa.HibernateHints#HINT_FLUSH_MODE
	 *
	 * @see org.hibernate.SessionBuilder#flushMode(FlushMode)
	 * @see org.hibernate.Session#setHibernateFlushMode(FlushMode)
	 */
	FlushMode getInitialSessionFlushMode();

	/**
	 * The default lock scope and lock timeout.
	 *
	 * @see org.hibernate.cfg.AvailableSettings#JAKARTA_LOCK_TIMEOUT
	 * @see org.hibernate.cfg.AvailableSettings#JAKARTA_LOCK_SCOPE
	 */
	LockOptions getDefaultLockOptions();

	/**
	 * Default properties for brand-new sessions.
	 *
	 * @see org.hibernate.Session#setProperty(String, Object)
	 */
	Map<String, Object> getDefaultSessionProperties();
}
