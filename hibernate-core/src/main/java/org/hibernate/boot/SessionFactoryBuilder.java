/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot;

import java.util.Map;
import java.util.function.Supplier;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.EntityMode;
import org.hibernate.EntityNameResolver;
import org.hibernate.Interceptor;
import org.hibernate.MultiTenancyStrategy;
import org.hibernate.NullPrecedence;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.cache.spi.TimestampsCacheFactory;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.jpa.spi.JpaCompliance;
import org.hibernate.loader.BatchFetchStyle;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.tuple.entity.EntityTuplizer;
import org.hibernate.tuple.entity.EntityTuplizerFactory;

/**
 * The contract for building a {@link org.hibernate.SessionFactory} given a number of options.
 *
 * @author Steve Ebersole
 * @author Gail Badner
 *
 * @since 5.0
 */
@SuppressWarnings("UnusedReturnValue")
public interface SessionFactoryBuilder {
	/**
	 * Apply a Bean Validation ValidatorFactory to the SessionFactory being built.
	 *
	 * NOTE : De-typed to avoid hard dependency on Bean Validation jar at runtime.
	 *
	 * @param validatorFactory The Bean Validation ValidatorFactory to use
	 *
	 * @return {@code this}, for method chaining
	 */
	SessionFactoryBuilder applyValidatorFactory(Object validatorFactory);

	/**
	 * Apply a CDI BeanManager to the SessionFactory being built.
	 *
	 * NOTE : De-typed to avoid hard dependency on CDI jar at runtime.
	 *
	 * @param beanManager The CDI BeanManager to use
	 *
	 * @return {@code this}, for method chaining
	 */
	SessionFactoryBuilder applyBeanManager(Object beanManager);

	/**
	 * Applies a SessionFactory name.
	 *
	 * @param sessionFactoryName The name to use for the SessionFactory being built
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#SESSION_FACTORY_NAME
	 */
	SessionFactoryBuilder applyName(String sessionFactoryName);

	/**
	 * Applies a SessionFactory name.
	 *
	 * @param isJndiName {@code true} indicates that the name specified in
	 * {@link #applyName} will be used for binding the SessionFactory into JNDI.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#SESSION_FACTORY_NAME_IS_JNDI
	 */
	SessionFactoryBuilder applyNameAsJndiName(boolean isJndiName);

	/**
	 * Applies whether Sessions should be automatically closed at the end of the transaction.
	 *
	 * @param enabled {@code true} indicates they should be auto-closed; {@code false} indicates not.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#AUTO_CLOSE_SESSION
	 */
	SessionFactoryBuilder applyAutoClosing(boolean enabled);

	/**
	 * Applies whether Sessions should be automatically flushed at the end of the transaction.
	 *
	 * @param enabled {@code true} indicates they should be auto-flushed; {@code false} indicates not.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#FLUSH_BEFORE_COMPLETION
	 */
	SessionFactoryBuilder applyAutoFlushing(boolean enabled);

	/**
	 * Applies whether statistics gathering is enabled.
	 *
	 * @param enabled {@code true} indicates that statistics gathering should be enabled; {@code false} indicates not.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#GENERATE_STATISTICS
	 */
	SessionFactoryBuilder applyStatisticsSupport(boolean enabled);

	/**
	 * Names an interceptor to be applied to the SessionFactory, which in turn means it will be used by all
	 * Sessions unless one is explicitly specified in {@link org.hibernate.SessionBuilder#interceptor}
	 *
	 * @param interceptor The interceptor
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#INTERCEPTOR
	 */
	SessionFactoryBuilder applyInterceptor(Interceptor interceptor);

	/**
	 * Names an interceptor Class to be applied to the SessionFactory, which in turn means it will be used by all
	 * Sessions unless one is explicitly specified in {@link org.hibernate.SessionBuilder#interceptor}
	 *
	 * @param statelessInterceptorClass The interceptor class
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#SESSION_SCOPED_INTERCEPTOR
	 */
	SessionFactoryBuilder applyStatelessInterceptor(Class<? extends Interceptor> statelessInterceptorClass);

	/**
	 * Names a {@link Supplier} instance which is used to retrieve the interceptor to be applied to the SessionFactory,
	 * which in turn means it will be used by all Sessions unless one is explicitly specified in
	 * {@link org.hibernate.SessionBuilder#interceptor}
	 *
	 * @param statelessInterceptorSupplier {@link Supplier} instance which is used to retrieve the interceptor
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#SESSION_SCOPED_INTERCEPTOR
	 */
	SessionFactoryBuilder applyStatelessInterceptor(Supplier<? extends Interceptor> statelessInterceptorSupplier);

	/**
	 * Names a StatementInspector to be applied to the SessionFactory, which in turn means it will be used by all
	 * Sessions unless one is explicitly specified in {@link org.hibernate.SessionBuilder#statementInspector}
	 *
	 * @param statementInspector The StatementInspector
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#STATEMENT_INSPECTOR
	 */
	SessionFactoryBuilder applyStatementInspector(StatementInspector statementInspector);

	/**
	 * Specifies one or more observers to be applied to the SessionFactory.  Can be called multiple times to add
	 * additional observers.
	 *
	 * @param observers The observers to add
	 *
	 * @return {@code this}, for method chaining
	 */
	SessionFactoryBuilder addSessionFactoryObservers(SessionFactoryObserver... observers);

	/**
	 * Specifies a custom entity dirtiness strategy to be applied to the SessionFactory.  See the contract
	 * of {@link org.hibernate.CustomEntityDirtinessStrategy} for details.
	 *
	 * @param strategy The custom strategy to be used.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#CUSTOM_ENTITY_DIRTINESS_STRATEGY
	 */
	SessionFactoryBuilder applyCustomEntityDirtinessStrategy(CustomEntityDirtinessStrategy strategy);

	/**
	 * Specifies one or more entity name resolvers to be applied to the SessionFactory (see the {@link org.hibernate.EntityNameResolver}
	 * contract for more information..  Can be called multiple times to add additional resolvers..
	 *
	 * @param entityNameResolvers The entityNameResolvers to add
	 *
	 * @return {@code this}, for method chaining
	 */
	SessionFactoryBuilder addEntityNameResolver(EntityNameResolver... entityNameResolvers);

	/**
	 * Names the {@link org.hibernate.proxy.EntityNotFoundDelegate} to be applied to the SessionFactory.  EntityNotFoundDelegate is a
	 * strategy that accounts for different exceptions thrown between Hibernate and JPA when an entity cannot be found.
	 *
	 * @param entityNotFoundDelegate The delegate/strategy to use.
	 *
	 * @return {@code this}, for method chaining
	 */
	SessionFactoryBuilder applyEntityNotFoundDelegate(EntityNotFoundDelegate entityNotFoundDelegate);

	/**
	 * Should generated identifiers be "unset" on entities during a rollback?
	 *
	 * @param enabled {@code true} indicates identifiers should be unset; {@code false} indicates not.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#USE_IDENTIFIER_ROLLBACK
	 */
	SessionFactoryBuilder applyIdentifierRollbackSupport(boolean enabled);

	/**
	 * Applies the given entity mode as the default for the SessionFactory.
	 *
	 * @param entityMode The default entity mode to use.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#DEFAULT_ENTITY_MODE
	 *
	 * @deprecated Different entity modes per entity is soon to be removed as a feature.
	 */
	@Deprecated
	SessionFactoryBuilder applyDefaultEntityMode(EntityMode entityMode);

	/**
	 * Should attributes using columns marked as not-null be checked (by Hibernate) for nullness?
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
	 * Should the application be allowed to initialize uninitialized lazy state outside the bounds of a transaction?
	 *
	 * @param enabled {@code true} indicates initialization outside the transaction should be allowed; {@code false}
	 * indicates it should not.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#ENABLE_LAZY_LOAD_NO_TRANS
	 */
	SessionFactoryBuilder applyLazyInitializationOutsideTransaction(boolean enabled);

	/**
	 * Specify the EntityTuplizerFactory to use.
	 *
	 * @param entityTuplizerFactory The EntityTuplizerFactory to use.
	 *
	 * @return {@code this}, for method chaining
	 */
	SessionFactoryBuilder applyEntityTuplizerFactory(EntityTuplizerFactory entityTuplizerFactory);

	/**
	 * Register the default {@link org.hibernate.tuple.entity.EntityTuplizer} to be applied to the SessionFactory.
	 *
	 * @param entityMode The entity mode that which this tuplizer will be applied.
	 * @param tuplizerClass The custom tuplizer class.
	 *
	 * @return {@code this}, for method chaining
	 */
	SessionFactoryBuilder applyEntityTuplizer(
			EntityMode entityMode,
			Class<? extends EntityTuplizer> tuplizerClass);

	/**
	 * How should updates and deletes that span multiple tables be handled?
	 *
	 * @param strategy The strategy for handling multi-table updates and deletes.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#HQL_BULK_ID_STRATEGY
	 */
	SessionFactoryBuilder applyMultiTableBulkIdStrategy(MultiTableBulkIdStrategy strategy);

	SessionFactoryBuilder applyTempTableDdlTransactionHandling(TempTableDdlTransactionHandling handling);

	/**
	 * What style of batching should be used?
	 *
	 * @param style The style to use
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#BATCH_FETCH_STYLE
	 */
	SessionFactoryBuilder applyBatchFetchStyle(BatchFetchStyle style);

	/**
	 * Should entity Loaders be generated immediately?  Or should the creation
	 * be delayed until first need?
	 *
	 * @see org.hibernate.cfg.AvailableSettings#DELAY_ENTITY_LOADER_CREATIONS
	 */
	SessionFactoryBuilder applyDelayedEntityLoaderCreations(boolean delay);

	/**
	 * Allows specifying a default batch-fetch size for all entities and collections
	 * which do not otherwise specify a batch-fetch size.
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
	 * Apply a limit to the depth Hibernate will use for outer joins.  Note that this is different than an
	 * overall limit on the number of joins...
	 *
	 * @param depth The depth for limiting joins.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#MAX_FETCH_DEPTH
	 */
	SessionFactoryBuilder applyMaximumFetchDepth(int depth);

	/**
	 * Apply a null precedence (NULLS FIRST, NULLS LAST) to be applied order-by clauses rendered into
	 * SQL queries.
	 *
	 * @param nullPrecedence The default null precedence to use.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#DEFAULT_NULL_ORDERING
	 */
	SessionFactoryBuilder applyDefaultNullPrecedence(NullPrecedence nullPrecedence);

	/**
	 * Apply whether ordering of inserts should be enabled.  This allows more efficient SQL
	 * generation via the use of batching for the inserts; the cost is that the determination of the
	 * ordering is far more inefficient than not ordering.
	 *
	 * @param enabled {@code true} indicates that ordering should be enabled; {@code false} indicates not
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#ORDER_INSERTS
	 */
	SessionFactoryBuilder applyOrderingOfInserts(boolean enabled);

	/**
	 * Apply whether ordering of updates should be enabled.  This allows more efficient SQL
	 * generation via the use of batching for the updates; the cost is that the determination of the
	 * ordering is far more inefficient than not ordering.
	 *
	 * @param enabled {@code true} indicates that ordering should be enabled; {@code false} indicates not
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#ORDER_UPDATES
	 */
	SessionFactoryBuilder applyOrderingOfUpdates(boolean enabled);

	/**
	 * Apply the form of multi-tenancy used by the application
	 *
	 * @param strategy The form of multi-tenancy in use.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#MULTI_TENANT
	 */
	SessionFactoryBuilder applyMultiTenancyStrategy(MultiTenancyStrategy strategy);

	/**
	 * Specifies a strategy for resolving the notion of a "current" tenant-identifier when using multi-tenancy
	 * together with current sessions
	 *
	 * @param resolver The resolution strategy to use.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#MULTI_TENANT_IDENTIFIER_RESOLVER
	 */
	SessionFactoryBuilder applyCurrentTenantIdentifierResolver(CurrentTenantIdentifierResolver resolver);

	/**
	 * If using the built-in Hibernate JTA-based TransactionCoordinator/Builder, should it track JTA
	 * transactions by thread in an attempt to detect timeouts?
	 *
	 * @param enabled {@code true} indicates we should track by thread; {@code false} indicates not
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#JTA_TRACK_BY_THREAD
	 */
	SessionFactoryBuilder applyJtaTrackingByThread(boolean enabled);

	/**
	 * If using the built-in Hibernate JTA-based TransactionCoordinator/Builder, should it prefer to use
	 * {@link javax.transaction.UserTransaction} over {@link javax.transaction.Transaction}?
	 *
	 * @param preferUserTransactions {@code true} indicates we should prefer {@link javax.transaction.UserTransaction};
	 * {@code false} indicates we should prefer {@link javax.transaction.Transaction}
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#PREFER_USER_TRANSACTION
	 */
	SessionFactoryBuilder applyPreferUserTransactions(boolean preferUserTransactions);

	/**
	 * Apply query substitutions to use in HQL queries.  Note, this is a legacy feature and almost always
	 * never needed anymore...
	 *
	 * @param substitutions The substitution map
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#QUERY_SUBSTITUTIONS
	 *
	 * @deprecated This is a legacy feature and should never be needed anymore...
	 */
	@Deprecated
	SessionFactoryBuilder applyQuerySubstitutions(Map substitutions);

	/**
	 * Should we strictly adhere to JPA Query Language (JPQL) syntax, or more broadly support
	 * all of Hibernate's superset (HQL)?
	 * <p/>
	 * Setting this to {@code true} may cause valid HQL to throw an exception because it violates
	 * the JPQL subset.
	 *
	 * @param enabled {@code true} indicates that we should strictly adhere to the JPQL subset; {@code false}
	 * indicates we should accept the broader HQL syntax.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#JPAQL_STRICT_COMPLIANCE
	 *
	 * @deprecated Use {@link #enableJpaQueryCompliance} instead
	 */
	@Deprecated
	SessionFactoryBuilder applyStrictJpaQueryLanguageCompliance(boolean enabled);

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
	 * @param enabled {@code true} indicates we should enable the use of second level caching; {@code false}
	 * indicates we should disable the use of second level caching.
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
	 * Specifies a QueryCacheFactory to use for building query cache handlers.
	 *
	 * @param factory The QueryCacheFactory to use
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#QUERY_CACHE_FACTORY
	 */
	SessionFactoryBuilder applyTimestampsCacheFactory(TimestampsCacheFactory factory);

	/**
	 * Apply a prefix to prepended to all cache region names for this SessionFactory.
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
	 * <p/>
	 * Cache integrations also report whether "minimal puts" should be enabled by default.  So its is
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
	 * @param enabled {@code true} indicates that structured cache entries (human readable) should be used;
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
	 * When using bi-directional many-to-one associations and caching the one-to-many side
	 * it is expected that both sides of the association are managed (actually that is true of
	 * all bi-directional associations).  However, in this case, if the user forgets to manage the
	 * one-to-many side stale data can be left in the second-level cache.
	 * <p/>
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
	 * This setting controls whether versioned entities will be included in JDBC batching.  The reason
	 * being that some JDBC drivers have a problems returning "accurate" update counts from batch statements.
	 * This is setting is {@code false} by default.
	 *
	 * @param enabled The batch size to use.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#BATCH_VERSIONED_DATA
	 */
	SessionFactoryBuilder applyJdbcBatchingForVersionedEntities(boolean enabled);

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
	 * Hibernate currently accesses results from the JDBC ResultSet by name.  This is known
	 * to be VERY slow on some drivers, especially older Oracle drivers.  This setting
	 * allows Hibernate to wrap the ResultSet of the JDBC driver to manage the name->position
	 * resolution itself.
	 *
	 * @param enabled {@code true} indicates Hibernate should wrap result sets; {@code false} indicates
	 * it should not.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#WRAP_RESULT_SETS
	 */
	SessionFactoryBuilder applyResultSetsWrapping(boolean enabled);

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
	 * @see org.hibernate.cfg.AvailableSettings#USE_GET_GENERATED_KEYS
	 * @see java.sql.Statement#setFetchSize(int)
	 */
	SessionFactoryBuilder applyJdbcFetchSize(int size);

	/**
	 * Apply the specified handling mode for JDBC connections
	 *
	 * @param connectionHandlingMode The handling mode to apply
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#ACQUIRE_CONNECTIONS
	 * @see org.hibernate.cfg.AvailableSettings#RELEASE_CONNECTIONS
	 * @see org.hibernate.ConnectionAcquisitionMode
	 * @see ConnectionReleaseMode
	 */
	SessionFactoryBuilder applyConnectionHandlingMode(PhysicalConnectionHandlingMode connectionHandlingMode);

	/**
	 * Apply a ConnectionReleaseMode.
	 *
	 * @param connectionReleaseMode The ConnectionReleaseMode to use.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#RELEASE_CONNECTIONS
	 *
	 * @deprecated Use {@link #applyConnectionHandlingMode} instead
	 */
	@Deprecated
	SessionFactoryBuilder applyConnectionReleaseMode(ConnectionReleaseMode connectionReleaseMode);

	/**
	 * @see org.hibernate.cfg.AvailableSettings#CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT
	 */
	default SessionFactoryBuilder applyConnectionProviderDisablesAutoCommit(boolean providerDisablesAutoCommit) {
		return this;
	}

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
	 * Apply a SQLFunction to the underlying {@link org.hibernate.dialect.function.SQLFunctionRegistry}.
	 * <p/>
	 * TODO : Ultimately I would like this to move to {@link org.hibernate.boot.MetadataBuilder} in conjunction with allowing mappings to reference SQLFunctions.
	 * today mappings can only name SQL functions directly, not through the SQLFunctionRegistry indirection
	 *
	 * @param registrationName The name to register it under.
	 * @param sqlFunction The SQLFunction impl
	 *
	 * @return {@code this}, for method chaining
	 */
	SessionFactoryBuilder applySqlFunction(String registrationName, SQLFunction sqlFunction);

	SessionFactoryBuilder allowOutOfTransactionUpdateOperations(boolean allow);

	/**
	 * Should resources held by {@link javax.persistence.EntityManager} instance be released immediately on close?
	 * <p/>
	 * The other option is to release them as part of an after-transaction callback.
	 *
	 */
	SessionFactoryBuilder enableReleaseResourcesOnCloseEnabled(boolean enable);


	/**
	 * @see JpaCompliance#isJpaQueryComplianceEnabled()
	 */
	SessionFactoryBuilder enableJpaQueryCompliance(boolean enabled);

	/**
	 * @see JpaCompliance#isJpaTransactionComplianceEnabled()
	 */
	SessionFactoryBuilder enableJpaTransactionCompliance(boolean enabled);

	/**
	 * @see JpaCompliance#isJpaListComplianceEnabled()
	 */
	SessionFactoryBuilder enableJpaListCompliance(boolean enabled);

	/**
	 * @see JpaCompliance#isJpaClosedComplianceEnabled()
	 */
	SessionFactoryBuilder enableJpaClosedCompliance(boolean enabled);


	/**
	 * Allows unwrapping this builder as another, more specific type.
	 *
	 * @param type
	 * @param <T>
	 *
	 * @return The unwrapped builder.
	 */
	<T extends SessionFactoryBuilder> T unwrap(Class<T> type);

	/**
	 * After all options have been set, build the SessionFactory.
	 *
	 * @return The built SessionFactory.
	 */
	SessionFactory build();
}
