/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot;

import java.util.function.Supplier;

import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.EntityNameResolver;
import org.hibernate.Incubating;
import org.hibernate.Interceptor;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.annotations.CacheLayout;
import org.hibernate.cache.spi.TimestampsCacheFactory;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.jpa.spi.JpaCompliance;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.type.format.FormatMapper;

import jakarta.persistence.criteria.Nulls;

/**
 * The contract for building a {@link SessionFactory} given a specified set of options.
 *
 * @author Steve Ebersole
 * @author Gail Badner
 *
 * @since 5.0
 *
 * @see Metadata#getSessionFactoryBuilder()
 */
public interface SessionFactoryBuilder {
	/**
	 * Specifies a Bean Validation {@link jakarta.validation.ValidatorFactory}.
	 *
	 * @apiNote De-typed to avoid a hard dependency on the Bean Validation jar
	 *
	 * @param validatorFactory The Bean Validation {@code ValidatorFactory} to use
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#JAKARTA_VALIDATION_FACTORY
	 */
	SessionFactoryBuilder applyValidatorFactory(Object validatorFactory);

	/**
	 * Specifies a CDI {@link jakarta.enterprise.inject.spi.BeanManager}.
	 *
	 * @apiNote De-typed to avoid a hard dependency on the CDI jar
	 *
	 * @param beanManager The CDI {@code BeanManager} to use
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#JAKARTA_CDI_BEAN_MANAGER
	 */
	SessionFactoryBuilder applyBeanManager(Object beanManager);

	/**
	 * Specifies a name for the {@link SessionFactory}.
	 *
	 * @param sessionFactoryName The name to use
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#SESSION_FACTORY_NAME
	 */
	SessionFactoryBuilder applyName(String sessionFactoryName);

	/**
	 * Specifies that the {@link SessionFactory} should be registered in JNDI,
	 * under the name specified using {@link #applyName(String)}.
	 *
	 * @param isJndiName {@code true} indicates that the name specified in
	 * {@link #applyName} will be used for binding the SessionFactory into JNDI.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#SESSION_FACTORY_NAME
	 * @see org.hibernate.cfg.AvailableSettings#SESSION_FACTORY_NAME_IS_JNDI
	 * @see org.hibernate.cfg.AvailableSettings#SESSION_FACTORY_JNDI_NAME
	 */
	SessionFactoryBuilder applyNameAsJndiName(boolean isJndiName);

	/**
	 * Specifies whether {@link org.hibernate.Session}s should be automatically
	 * closed at the end of the transaction.
	 *
	 * @param enabled {@code true} indicates they should be auto-closed; {@code false} indicates not.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#AUTO_CLOSE_SESSION
	 */
	SessionFactoryBuilder applyAutoClosing(boolean enabled);

	/**
	 * Applies whether {@link org.hibernate.Session}s should be automatically
	 * flushed at the end of the transaction.
	 *
	 * @param enabled {@code true} indicates they should be auto-flushed; {@code false} indicates not.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#FLUSH_BEFORE_COMPLETION
	 */
	SessionFactoryBuilder applyAutoFlushing(boolean enabled);

	/**
	 * Specifies whether statistics gathering is enabled.
	 *
	 * @param enabled {@code true} indicates that statistics gathering should be enabled; {@code false} indicates not.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#GENERATE_STATISTICS
	 */
	SessionFactoryBuilder applyStatisticsSupport(boolean enabled);

	/**
	 * Specifies an {@link Interceptor} associated with the {@link SessionFactory},
	 * which will be used by all sessions unless an interceptor is explicitly
	 * specified using {@link org.hibernate.SessionBuilder#interceptor}.
	 *
	 * @param interceptor The interceptor
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#INTERCEPTOR
	 */
	SessionFactoryBuilder applyInterceptor(Interceptor interceptor);

	/**
	 * Specifies an interceptor {@link Class} associated with the
	 * {@link SessionFactory}, which is used to instantiate a new interceptor
	 * for each session, unless an interceptor is explicitly specified using
	 * {@link org.hibernate.SessionBuilder#interceptor}.
	 *
	 * @param statelessInterceptorClass The interceptor class
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#SESSION_SCOPED_INTERCEPTOR
	 */
	SessionFactoryBuilder applyStatelessInterceptor(Class<? extends Interceptor> statelessInterceptorClass);

	/**
	 * Specifies an interceptor {@link Supplier} associated with the
	 * {@link SessionFactory}, which is used to obtain an interceptor for
	 * each session, unless an interceptor is explicitly specified using
	 * {@link org.hibernate.SessionBuilder#interceptor}.
	 *
	 * @param statelessInterceptorSupplier {@link Supplier} instance which is used to retrieve the interceptor
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#SESSION_SCOPED_INTERCEPTOR
	 */
	SessionFactoryBuilder applyStatelessInterceptor(Supplier<? extends Interceptor> statelessInterceptorSupplier);

	/**
	 * Specifies a {@link StatementInspector} associated with the
	 * {@link SessionFactory}, which will be used by all sessions unless a
	 * statement inspector is explicitly specified using
	 * {@link org.hibernate.SessionBuilder#statementInspector}
	 *
	 * @param statementInspector The StatementInspector
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#STATEMENT_INSPECTOR
	 */
	SessionFactoryBuilder applyStatementInspector(StatementInspector statementInspector);

	/**
	 * Specifies one or more {@linkplain SessionFactoryObserver observers}.
	 * May be called multiple times to add additional observers.
	 *
	 * @param observers The observers to add
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#SESSION_FACTORY_OBSERVER
	 */
	SessionFactoryBuilder addSessionFactoryObservers(SessionFactoryObserver... observers);

	/**
	 * Specifies a {@link CustomEntityDirtinessStrategy}.
	 *
	 * @param strategy The custom strategy to be used.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#CUSTOM_ENTITY_DIRTINESS_STRATEGY
	 */
	SessionFactoryBuilder applyCustomEntityDirtinessStrategy(CustomEntityDirtinessStrategy strategy);

	/**
	 * Specifies one or more {@linkplain EntityNameResolver entity name resolvers}.
	 * May be called multiple times to add additional resolvers.
	 *
	 * @param entityNameResolvers The entityNameResolvers to add
	 *
	 * @return {@code this}, for method chaining
	 */
	SessionFactoryBuilder addEntityNameResolver(EntityNameResolver... entityNameResolvers);

	/**
	 * Specifies an {@link EntityNotFoundDelegate}. An {@code EntityNotFoundDelegate}
	 * is a strategy that accounts for different exceptions thrown between Hibernate
	 * and JPA when an entity cannot be found.
	 *
	 * @param entityNotFoundDelegate The delegate/strategy to use.
	 *
	 * @return {@code this}, for method chaining
	 */
	SessionFactoryBuilder applyEntityNotFoundDelegate(EntityNotFoundDelegate entityNotFoundDelegate);

	/**
	 * Should the generated identifier be "unset" when an entity is deleted?
	 *
	 * @param enabled {@code true} indicates identifiers should be unset; {@code false} indicates not.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#USE_IDENTIFIER_ROLLBACK
	 */
	SessionFactoryBuilder applyIdentifierRollbackSupport(boolean enabled);

	/**
	 * Should attributes using columns marked as not-null be checked (by Hibernate)
	 * for nullness, or should this be left as a job for the database?
	 *
	 * @param enabled {@code true} indicates that Hibernate should perform nullness checking; {@code false} indicates
	 * it should not.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#CHECK_NULLABILITY
	 */
	SessionFactoryBuilder applyNullabilityChecking(boolean enabled);

	/**
	 * Should the application be allowed to initialize uninitialized lazy state
	 * outside the bounds of a transaction?
	 *
	 * @param enabled {@code true} indicates initialization outside the transaction
	 *                should be allowed; {@code false} indicates it should not.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#ENABLE_LAZY_LOAD_NO_TRANS
	 */
	SessionFactoryBuilder applyLazyInitializationOutsideTransaction(boolean enabled);

	/**
	 * Specifies how temporary tables should be created or dropped with respect
	 * to transaction handling.
	 *
	 * @see TempTableDdlTransactionHandling
	 *
	 * @deprecated This has no effect and will be removed.
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	SessionFactoryBuilder applyTempTableDdlTransactionHandling(TempTableDdlTransactionHandling handling);

	/**
	 * Should entity {@linkplain org.hibernate.loader.ast.spi.Loader loaders} be
	 * generated immediately? Or should the creation be delayed until first need?
	 *
	 * @see org.hibernate.cfg.AvailableSettings#DELAY_ENTITY_LOADER_CREATIONS
	 */
	SessionFactoryBuilder applyDelayedEntityLoaderCreations(boolean delay);

	/**
	 * Specifies a default batch fetch size for all entities and collections which
	 * do not otherwise specify a batch fetch size.
	 *
	 * @param size The size to use for batch fetching for entities/collections which
	 * do not specify an explicit batch fetch size.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#DEFAULT_BATCH_FETCH_SIZE
	 */
	SessionFactoryBuilder applyDefaultBatchFetchSize(int size);

	/**
	 * Apply a limit to the depth Hibernate will use for outer joins.
	 * <p>
	 * Note that this is different to an overall limit on the number of joins.
	 *
	 * @param depth The depth for limiting joins.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#MAX_FETCH_DEPTH
	 */
	SessionFactoryBuilder applyMaximumFetchDepth(int depth);

	/**
	 * Enable the use of subselect fetching.
	 *
	 * @param enabled {@code true} indicates that subselect fetching is enabled
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#USE_SUBSELECT_FETCH
	 */
	SessionFactoryBuilder applySubselectFetchEnabled(boolean enabled);

	/**
	 * Apply a null precedence, {@code NULLS FIRST} or {@code NULLS LAST},
	 * to {@code order by} clauses rendered in SQL queries.
	 *
	 * @param nullPrecedence The default null precedence to use.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#DEFAULT_NULL_ORDERING
	 */
	SessionFactoryBuilder applyDefaultNullPrecedence(Nulls nullPrecedence);

	/**
	 * Specify whether ordering of inserts should be enabled.
	 * <p>
	 * This allows more efficient SQL execution via the use of batching
	 * for the inserts; the cost is that the determination of the ordering
	 * is far less efficient than not ordering.
	 *
	 * @param enabled {@code true} indicates that ordering should be enabled; {@code false} indicates not
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#ORDER_INSERTS
	 */
	SessionFactoryBuilder applyOrderingOfInserts(boolean enabled);

	/**
	 * Specify whether ordering of updates should be enabled.
	 * <p>
	 * This allows more efficient SQL execution via the use of batching for
	 * the updates; the cost is that the determination of the ordering is far
	 * less efficient than not ordering.
	 *
	 * @param enabled {@code true} indicates that ordering should be enabled; {@code false} indicates not
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#ORDER_UPDATES
	 */
	SessionFactoryBuilder applyOrderingOfUpdates(boolean enabled);

	/**
	 * Specifies whether multitenancy is enabled via use of a
	 * {@link org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider}.
	 * <p>
	 * Note that this setting does not affect
	 * {@linkplain org.hibernate.annotations.TenantId discriminator-based}
	 * multitenancy.
	 *
	 * @param enabled True if multi-tenancy in use via a {@code MultiTenantConnectionProvider}.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#MULTI_TENANT_CONNECTION_PROVIDER
	 */
	SessionFactoryBuilder applyMultiTenancy(boolean enabled);

	/**
	 * Specifies a {@link CurrentTenantIdentifierResolver} that is responsible for
	 * resolving the current tenant identifier.
	 *
	 * @param resolver The resolution strategy to use.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#MULTI_TENANT_IDENTIFIER_RESOLVER
	 */
	SessionFactoryBuilder applyCurrentTenantIdentifierResolver(CurrentTenantIdentifierResolver<?> resolver);

	/**
	 * If using the built-in JTA-based
	 * {@link org.hibernate.resource.transaction.spi.TransactionCoordinator} or
	 * {@link org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder},
	 * should it track JTA transactions by thread in an attempt to detect timeouts?
	 *
	 * @param enabled {@code true} indicates we should track by thread; {@code false} indicates not
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#JTA_TRACK_BY_THREAD
	 */
	SessionFactoryBuilder applyJtaTrackingByThread(boolean enabled);

	/**
	 * If using the built-in JTA-based
	 * {@link org.hibernate.resource.transaction.spi.TransactionCoordinator} or
	 * {@link org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder},
	 * should it prefer to use {@link jakarta.transaction.UserTransaction} in
	 * preference to {@link jakarta.transaction.Transaction}?
	 *
	 * @param preferUserTransactions {@code true} indicates we should prefer
	 * {@link jakarta.transaction.UserTransaction}; {@code false} indicates we
	 * should prefer {@link jakarta.transaction.Transaction}
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#PREFER_USER_TRANSACTION
	 */
	SessionFactoryBuilder applyPreferUserTransactions(boolean preferUserTransactions);

	/**
	 * Should named queries be checked on startup?
	 *
	 * @param enabled {@code true} indicates that they should; {@code false} indicates they should not.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#QUERY_STARTUP_CHECKING
	 */
	SessionFactoryBuilder applyNamedQueryCheckingOnStartup(boolean enabled);

	/**
	 * Should second level caching support be enabled?
	 *
	 * @param enabled {@code true} indicates we should enable the use of second level caching;
	 * {@code false} indicates we should disable the use of second level caching.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#USE_SECOND_LEVEL_CACHE
	 */
	SessionFactoryBuilder applySecondLevelCacheSupport(boolean enabled);

	/**
	 * Should second level query caching support be enabled?
	 *
	 * @param enabled {@code true} indicates we should enable the use of second level query
	 * caching; {@code false} indicates we should disable the use of second level query caching.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#USE_QUERY_CACHE
	 */
	SessionFactoryBuilder applyQueryCacheSupport(boolean enabled);

	/**
	 * Specifies the default {@link CacheLayout} to use for query cache entries.
	 *
	 * @param cacheLayout The cache layout to use.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#QUERY_CACHE_LAYOUT
	 * @since 6.5
	 */
	@Incubating
	SessionFactoryBuilder applyQueryCacheLayout(CacheLayout cacheLayout);

	/**
	 * Specifies a {@link org.hibernate.cache.spi.TimestampsCacheFactory}.
	 *
	 * @param factory The {@link org.hibernate.cache.spi.TimestampsCacheFactory} to use
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#QUERY_CACHE_FACTORY
	 */
	SessionFactoryBuilder applyTimestampsCacheFactory(TimestampsCacheFactory factory);

	/**
	 * Specify a prefix to prepended to all cache region names.
	 *
	 * @param prefix The prefix.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#CACHE_REGION_PREFIX
	 */
	SessionFactoryBuilder applyCacheRegionPrefix(String prefix);

	/**
	 * By default, Hibernate will always just push data into the cache without first checking
	 * if that data already exists.  For some caches (mainly distributed caches) this can have a
	 * major adverse performance impact.  For these caches, it is best to enable this "minimal puts"
	 * feature.
	 * <p>
	 * Cache integrations also report whether "minimal puts" should be enabled by default.  So it's
	 * very rare that users need to set this, generally speaking.
	 *
	 * @param enabled {@code true} indicates Hibernate should first check whether data exists and only
	 * push to the cache if it does not already exist. {@code false} indicates to perform the default
	 * behavior.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#USE_MINIMAL_PUTS
	 * @see org.hibernate.cache.spi.RegionFactory#isMinimalPutsEnabledByDefault()
	 */
	SessionFactoryBuilder applyMinimalPutsForCaching(boolean enabled);

	/**
	 * By default, Hibernate stores data in the cache in its own optimized format.  However,
	 * that format is impossible to "read" if browsing the cache.  The use of "structured" cache
	 * entries allows the cached data to be read.
	 *
	 * @param enabled {@code true} indicates that structured (human-readable) cache entries should be used;
	 * {@code false} indicates that the native entry structure should be used.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#USE_STRUCTURED_CACHE
	 */
	SessionFactoryBuilder applyStructuredCacheEntries(boolean enabled);

	/**
	 * Generally, Hibernate will extract the information from an entity and put that
	 * extracted information into the second-level cache.  This is by far the safest way to
	 * second-level cache persistent data.  However, there are some cases where it is safe
	 * to cache the entity instance directly.  This setting controls whether that is used
	 * in those cases.
	 *
	 * @param enabled {@code true} indicates that applicable entities will be stored into the
	 * second-level cache directly by reference; false indicates that all entities will be stored
	 * via the extraction approach.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#USE_DIRECT_REFERENCE_CACHE_ENTRIES
	 */
	SessionFactoryBuilder applyDirectReferenceCaching(boolean enabled);

	/**
	 * When using bidirectional many-to-one associations and caching the one-to-many side
	 * it is expected that both sides of the association are managed (actually that is true of
	 * all bidirectional associations).  However, in this case, if the user forgets to manage the
	 * one-to-many side stale data can be left in the second-level cache.
	 * <p>
	 * Warning: enabling this will have a performance impact.  Hence why it is disabled by default
	 * (for good citizens) and is an opt-in setting.
	 *
	 * @param enabled {@code true} indicates that these collection caches should be evicted automatically.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#AUTO_EVICT_COLLECTION_CACHE
	 */
	SessionFactoryBuilder applyAutomaticEvictionOfCollectionCaches(boolean enabled);

	/**
	 * Specifies the maximum number of statements to batch together in a JDBC batch for
	 * insert, update and delete operations.  A non-zero number enables batching, but really
	 * only a number greater than zero will have any effect.  If used, a number great than 5
	 * is suggested.
	 *
	 * @param size The batch size to use.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#STATEMENT_BATCH_SIZE
	 */
	SessionFactoryBuilder applyJdbcBatchSize(int size);

	/**
	 * Should scrollable results be supported in queries?  We ask the JDBC driver whether it
	 * supports scrollable result sets as the default for this setting, but some drivers do not
	 * accurately report this via DatabaseMetaData.  Also, needed if user is supplying connections
	 * (and so no Connection is available when we bootstrap).
	 *
	 * @param enabled {@code true} to enable this support, {@code false} to disable it
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#USE_SCROLLABLE_RESULTSET
	 */
	SessionFactoryBuilder applyScrollableResultsSupport(boolean enabled);

	/**
	 * Should JDBC {@link java.sql.PreparedStatement#getGeneratedKeys()} feature be used for
	 * retrieval of *insert-generated* ids?
	 *
	 * @param enabled {@code true} indicates we should use JDBC getGeneratedKeys support; {@code false}
	 * indicates we should not.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#USE_GET_GENERATED_KEYS
	 */
	SessionFactoryBuilder applyGetGeneratedKeysSupport(boolean enabled);

	/**
	 * Apply a fetch size to the JDBC driver for fetching results.
	 *
	 * @param size The fetch size to be passed to the driver.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#STATEMENT_FETCH_SIZE
	 * @see java.sql.Statement#setFetchSize(int)
	 */
	SessionFactoryBuilder applyJdbcFetchSize(int size);

	/**
	 * Specifies the {@linkplain PhysicalConnectionHandlingMode connection handling mode}
	 * for JDBC connections.
	 *
	 * @param connectionHandlingMode The handling mode to apply
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#CONNECTION_HANDLING
	 * @see PhysicalConnectionHandlingMode
	 *
	 * @see org.hibernate.ConnectionAcquisitionMode
	 * @see org.hibernate.ConnectionReleaseMode
	 */
	SessionFactoryBuilder applyConnectionHandlingMode(PhysicalConnectionHandlingMode connectionHandlingMode);

	/**
	 * @see org.hibernate.cfg.AvailableSettings#CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT
	 */
	SessionFactoryBuilder applyConnectionProviderDisablesAutoCommit(boolean providerDisablesAutoCommit);

	/**
	 * Should Hibernate apply comments to SQL it generates?
	 *
	 * @param enabled {@code true} indicates comments should be applied; {@code false} indicates not.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#USE_SQL_COMMENTS
	 */
	SessionFactoryBuilder applySqlComments(boolean enabled);

	/**
	 * Register a {@link SqmFunctionDescriptor SQL function} with the underlying
	 * {@link org.hibernate.query.sqm.function.SqmFunctionRegistry}.
	 *
	 * @param registrationName The function name to register it under
	 * @param functionDescriptor The {@link SqmFunctionDescriptor}
	 *
	 * @return {@code this}, for method chaining
	 */
	// Ultimately I would like this to move to {@link MetadataBuilder} in conjunction with allowing mappings to reference SQLFunctions.
	// today mappings can only name SQL functions directly, not through the SQLFunctionRegistry indirection
	SessionFactoryBuilder applySqlFunction(String registrationName, SqmFunctionDescriptor functionDescriptor);

	/**
	 * Should collections be included in the default fetch group when bytecode
	 * enhancement is used?
	 *
	 * @param enabled {@code true} collections should be included, {@code false} they should not.
	 * Default is {@code true}.
	 *
	 * @return {@code this}, for method chaining
	 */
	SessionFactoryBuilder applyCollectionsInDefaultFetchGroup(boolean enabled);

	/**
	 * @see org.hibernate.cfg.AvailableSettings#ALLOW_UPDATE_OUTSIDE_TRANSACTION
	 */
	SessionFactoryBuilder allowOutOfTransactionUpdateOperations(boolean allow);

	/**
	 * Should resources held by an {@link jakarta.persistence.EntityManager} be
	 * released immediately on close?
	 * <p>
	 * The other option is to release them as part of an after transaction callback.
	 *
	 * @deprecated since {@value org.hibernate.cfg.AvailableSettings#DISCARD_PC_ON_CLOSE}
	 *             is deprecated
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	SessionFactoryBuilder enableReleaseResourcesOnCloseEnabled(boolean enable);

	/**
	 * @see JpaCompliance#isJpaQueryComplianceEnabled()
	 *
	 * @see org.hibernate.cfg.AvailableSettings#JPA_QUERY_COMPLIANCE
	 */
	SessionFactoryBuilder enableJpaQueryCompliance(boolean enabled);

	/**
	 * @see JpaCompliance#isJpaQueryComplianceEnabled()
	 *
	 * @see org.hibernate.cfg.AvailableSettings#JPA_ORDER_BY_MAPPING_COMPLIANCE
	 */
	SessionFactoryBuilder enableJpaOrderByMappingCompliance(boolean enabled);

	/**
	 * @see JpaCompliance#isJpaTransactionComplianceEnabled()
	 *
	 * @see org.hibernate.cfg.AvailableSettings#JPA_TRANSACTION_COMPLIANCE
	 */
	SessionFactoryBuilder enableJpaTransactionCompliance(boolean enabled);

	/**
	 * @deprecated No longer has any effect.
	 *
	 * @see JpaCompliance#isJpaCascadeComplianceEnabled()
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	SessionFactoryBuilder enableJpaCascadeCompliance(boolean enabled);

	/**
	 * @see JpaCompliance#isJpaClosedComplianceEnabled()
	 *
	 * @see org.hibernate.cfg.AvailableSettings#JPA_CLOSED_COMPLIANCE
	 */
	SessionFactoryBuilder enableJpaClosedCompliance(boolean enabled);

	/**
	 * Specifies a {@link FormatMapper format mapper} to use for serialization/deserialization of JSON properties.
	 *
	 * @param jsonFormatMapper The {@link FormatMapper} to use.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#JSON_FORMAT_MAPPER
	 */
	@Incubating
	SessionFactoryBuilder applyJsonFormatMapper(FormatMapper jsonFormatMapper);

	/**
	 * Specifies a {@link FormatMapper format mapper} to use for serialization/deserialization of XML properties.
	 *
	 * @param xmlFormatMapper The {@link FormatMapper} to use.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#XML_FORMAT_MAPPER
	 */
	@Incubating
	SessionFactoryBuilder applyXmlFormatMapper(FormatMapper xmlFormatMapper);

	/**
	 * After all options have been set, build the SessionFactory.
	 *
	 * @return The built SessionFactory.
	 */
	SessionFactory build();
}
