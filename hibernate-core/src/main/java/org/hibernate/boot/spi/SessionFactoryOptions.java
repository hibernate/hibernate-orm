/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.spi;

import java.util.TimeZone;
import java.util.function.Supplier;

import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.EntityNameResolver;
import org.hibernate.Incubating;
import org.hibernate.Interceptor;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.TimeZoneStorageStrategy;
import org.hibernate.boot.SchemaAutoTooling;
import org.hibernate.boot.TempTableDdlTransactionHandling;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.cache.spi.TimestampsCacheFactory;
import org.hibernate.cfg.BaselineSessionEventsListenerBuilder;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.spi.JpaCompliance;
import org.hibernate.loader.BatchFetchStyle;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.query.ImmutableEntityUpdateQueryHandlingMode;
import org.hibernate.query.sqm.NullPrecedence;
import org.hibernate.query.criteria.ValueHandlingMode;
import org.hibernate.query.spi.QueryEngineOptions;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.stat.Statistics;
import org.hibernate.type.FormatMapper;

/**
 * Aggregator of special options used to build the SessionFactory.
 *
 * @since 5.0
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
	String getUuid();

	/**
	 * The service registry to use in building the factory.
	 *
	 * @return The service registry to use.
	 */
	StandardServiceRegistry getServiceRegistry();

	Object getBeanManagerReference();

	Object getValidatorFactoryReference();

	/**
	 * Was building of the SessionFactory initiated through JPA bootstrapping, or
	 * through Hibernate's native bootstrapping?
	 *
	 * @return {@code true} indicates the SessionFactory was built through JPA
	 * bootstrapping; {@code false} indicates it was built through native bootstrapping.
	 */
	boolean isJpaBootstrap();

	boolean isJtaTransactionAccessEnabled();

	default boolean isAllowRefreshDetachedEntity() {
		return false;
	}

	/**
	 * The name to be used for the SessionFactory.  This is used both in:<ul>
	 *     <li>in-VM serialization</li>
	 *     <li>JNDI binding, depending on {@link #isSessionFactoryNameAlsoJndiName}</li>
	 * </ul>
	 *
	 * @return The SessionFactory name
	 */
	String getSessionFactoryName();

	/**
	 * Is the {@link #getSessionFactoryName SesssionFactory name} also a JNDI name, indicating we
	 * should bind it into JNDI?
	 *
	 * @return {@code true} if the SessionFactory name is also a JNDI name; {@code false} otherwise.
	 */
	boolean isSessionFactoryNameAlsoJndiName();

	boolean isFlushBeforeCompletionEnabled();

	boolean isAutoCloseSessionEnabled();

	boolean isStatisticsEnabled();

	/**
	 * Get the interceptor to use by default for all sessions opened from this factory.
	 *
	 * @return The interceptor to use factory wide.  May be {@code null}
	 */
	Interceptor getInterceptor();

	/**
	 * Get the interceptor to use by default for all sessions opened from this factory.
	 *
	 * @return The interceptor to use factory wide.  May be {@code null}
	 */
	Supplier<? extends Interceptor> getStatelessInterceptorImplementorSupplier();

	StatementInspector getStatementInspector();

	SessionFactoryObserver[] getSessionFactoryObservers();

	BaselineSessionEventsListenerBuilder getBaselineSessionEventsListenerBuilder();

	boolean isIdentifierRollbackEnabled();

	boolean isCheckNullability();

	boolean isInitializeLazyStateOutsideTransactionsEnabled();

	TempTableDdlTransactionHandling getTempTableDdlTransactionHandling();

	/**
	 * @deprecated : No longer used internally
	 */
	@Deprecated(since = "6.0")
	BatchFetchStyle getBatchFetchStyle();

	boolean isDelayBatchFetchLoaderCreationsEnabled();

	int getDefaultBatchFetchSize();

	Integer getMaximumFetchDepth();

	NullPrecedence getDefaultNullPrecedence();

	boolean isOrderUpdatesEnabled();

	boolean isOrderInsertsEnabled();

	boolean isMultiTenancyEnabled();

	CurrentTenantIdentifierResolver getCurrentTenantIdentifierResolver();

	boolean isJtaTrackByThread();

	boolean isNamedQueryStartupCheckingEnabled();

	boolean isSecondLevelCacheEnabled();

	boolean isQueryCacheEnabled();

	TimestampsCacheFactory getTimestampsCacheFactory();

	String getCacheRegionPrefix();

	boolean isMinimalPutsEnabled();

	boolean isStructuredCacheEntriesEnabled();

	boolean isDirectReferenceCacheEntriesEnabled();

	boolean isAutoEvictCollectionCache();

	SchemaAutoTooling getSchemaAutoTooling();

	int getJdbcBatchSize();

	boolean isJdbcBatchVersionedData();

	boolean isScrollableResultSetsEnabled();

	boolean isGetGeneratedKeysEnabled();

	Integer getJdbcFetchSize();

	PhysicalConnectionHandlingMode getPhysicalConnectionHandlingMode();

	default boolean doesConnectionProviderDisableAutoCommit() {
		return false;
	}

	boolean isCommentsEnabled();


	CustomEntityDirtinessStrategy getCustomEntityDirtinessStrategy();

	/**
	 * @deprecated as this is not the most efficient way to get the EntityNameResolver(s) list.
	 */
	@Deprecated
	EntityNameResolver[] getEntityNameResolvers();

	/**
	 * Get the delegate for handling entity-not-found exception conditions.
	 *
	 * @return The specific EntityNotFoundDelegate to use,  May be {@code null}
	 */
	EntityNotFoundDelegate getEntityNotFoundDelegate();

	void setCheckNullability(boolean enabled);

	boolean isPreferUserTransaction();

	boolean isAllowOutOfTransactionUpdateOperations();

	boolean isReleaseResourcesOnCloseEnabled();

	TimeZone getJdbcTimeZone();

	/**
	 * @see org.hibernate.cfg.AvailableSettings#CRITERIA_VALUE_HANDLING_MODE
	 */
	default ValueHandlingMode getCriteriaValueHandlingMode() {
		return ValueHandlingMode.BIND;
	}

	/**
	 * @see org.hibernate.cfg.AvailableSettings#CRITERIA_COPY_TREE
	 */
	default boolean isCriteriaCopyTreeEnabled() {
		return false;
	}

	JpaCompliance getJpaCompliance();

	boolean isFailOnPaginationOverCollectionFetchEnabled();

	default ImmutableEntityUpdateQueryHandlingMode getImmutableEntityUpdateQueryHandlingMode() {
		return ImmutableEntityUpdateQueryHandlingMode.WARNING;
	}

	/**
	 * The default catalog to use in generated SQL when a catalog wasn't specified in the mapping,
	 * neither explicitly nor implicitly (see the concept of implicit catalog in XML mapping).
	 *
	 * @return The default catalog to use.
	 */
	default String getDefaultCatalog() {
		return null;
	}

	/**
	 * The default schema to use in generated SQL when a catalog wasn't specified in the mapping,
	 * neither explicitly nor implicitly (see the concept of implicit schema in XML mapping).
	 *
	 * @return The default schema to use.
	 */
	default String getDefaultSchema() {
		return null;
	}

	default boolean inClauseParameterPaddingEnabled() {
		return false;
	}

	default int getQueryStatisticsMaxSize() {
		return Statistics.DEFAULT_QUERY_STATISTICS_MAX_SIZE;
	}

	default boolean areJPACallbacksEnabled() {
		return true;
	}

	/**
	 * Controls whether Hibernate should try to map named parameter names
	 * specified in a {@link org.hibernate.procedure.ProcedureCall} or
	 * {@link jakarta.persistence.StoredProcedureQuery} to named parameters in
	 * the JDBC {@link java.sql.CallableStatement}.
	 * <p/>
	 * As JPA is defined, the use of named parameters is essentially of dubious
	 * value since by spec the parameters have to be defined in the order they are
	 * defined in the procedure/function declaration - we can always bind them
	 * positionally.  The whole idea of named parameters for CallableStatement
	 * is the ability to bind these in any order, but since we unequivocally
	 * know the order anyway binding them via name really gains nothing.
	 * <p/>
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

	@Incubating
	int getPreferredSqlTypeCodeForBoolean();

	@Incubating
	int getPreferredSqlTypeCodeForDuration();

	@Incubating
	int getPreferredSqlTypeCodeForUuid();

	@Incubating
	int getPreferredSqlTypeCodeForInstant();

	@Incubating
	int getPreferredSqlTypeCodeForArray();

	TimeZoneStorageStrategy getDefaultTimeZoneStorageStrategy();

	FormatMapper getJsonFormatMapper();

	/**
	 * The format mapper to use for serializing/deserializing XML data.
	 *
	 * @since 6.0.1
	 */
	FormatMapper getXmlFormatMapper();
}
