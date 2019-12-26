/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.spi;

import java.util.Map;
import java.util.TimeZone;
import java.util.function.Supplier;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.EntityMode;
import org.hibernate.EntityNameResolver;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.MultiTenancyStrategy;
import org.hibernate.NullPrecedence;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.SchemaAutoTooling;
import org.hibernate.boot.TempTableDdlTransactionHandling;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.cache.spi.TimestampsCacheFactory;
import org.hibernate.cfg.BaselineSessionEventsListenerBuilder;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.jpa.spi.JpaCompliance;
import org.hibernate.loader.BatchFetchStyle;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.query.ImmutableEntityUpdateQueryHandlingMode;
import org.hibernate.query.criteria.LiteralHandlingMode;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.stat.Statistics;
import org.hibernate.tuple.entity.EntityTuplizerFactory;

/**
 * Aggregator of special options used to build the SessionFactory.
 *
 * @since 5.0
 */
public interface SessionFactoryOptions {
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
	 * @deprecated use {@link #getStatelessInterceptorImplementorSupplier()} instead.
	 */
	@Deprecated
	Class<? extends Interceptor> getStatelessInterceptorImplementor();

	/**
	 * Get the interceptor to use by default for all sessions opened from this factory.
	 *
	 * @return The interceptor to use factory wide.  May be {@code null}
	 */
	default Supplier<? extends Interceptor> getStatelessInterceptorImplementorSupplier() {
		return () -> {
			try {
				return getStatelessInterceptorImplementor().newInstance();
			}
			catch (InstantiationException | IllegalAccessException e) {
				throw new HibernateException( "Could not supply session-scoped SessionFactory Interceptor", e );
			}
		};
	}

	StatementInspector getStatementInspector();

	SessionFactoryObserver[] getSessionFactoryObservers();

	BaselineSessionEventsListenerBuilder getBaselineSessionEventsListenerBuilder();

	boolean isIdentifierRollbackEnabled();

	EntityMode getDefaultEntityMode();

	EntityTuplizerFactory getEntityTuplizerFactory();

	boolean isCheckNullability();

	boolean isInitializeLazyStateOutsideTransactionsEnabled();

	MultiTableBulkIdStrategy getMultiTableBulkIdStrategy();

	TempTableDdlTransactionHandling getTempTableDdlTransactionHandling();

	BatchFetchStyle getBatchFetchStyle();

	boolean isDelayBatchFetchLoaderCreationsEnabled();

	int getDefaultBatchFetchSize();

	Integer getMaximumFetchDepth();

	NullPrecedence getDefaultNullPrecedence();

	boolean isOrderUpdatesEnabled();

	boolean isOrderInsertsEnabled();

	MultiTenancyStrategy getMultiTenancyStrategy();

	CurrentTenantIdentifierResolver getCurrentTenantIdentifierResolver();

	boolean isJtaTrackByThread();

	Map getQuerySubstitutions();

	/**
	 * @deprecated Use {@link JpaCompliance#isJpaQueryComplianceEnabled()} instead
	 * via {@link #getJpaCompliance()}
	 */
	@Deprecated
	default boolean isStrictJpaQueryLanguageCompliance() {
		return getJpaCompliance().isJpaQueryComplianceEnabled();
	}

	boolean isNamedQueryStartupCheckingEnabled();

	boolean isConventionalJavaConstants();

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

	boolean isWrapResultSetsEnabled();

	boolean isGetGeneratedKeysEnabled();

	Integer getJdbcFetchSize();

	PhysicalConnectionHandlingMode getPhysicalConnectionHandlingMode();

	default boolean doesConnectionProviderDisableAutoCommit() {
		return false;
	}

	/**
	 * @deprecated Use {@link #getPhysicalConnectionHandlingMode()} instead
	 */
	@Deprecated
	ConnectionReleaseMode getConnectionReleaseMode();

	boolean isCommentsEnabled();


	CustomEntityDirtinessStrategy getCustomEntityDirtinessStrategy();
	EntityNameResolver[] getEntityNameResolvers();

	/**
	 * Get the delegate for handling entity-not-found exception conditions.
	 *
	 * @return The specific EntityNotFoundDelegate to use,  May be {@code null}
	 */
	EntityNotFoundDelegate getEntityNotFoundDelegate();

	Map<String, SQLFunction> getCustomSqlFunctionMap();

	void setCheckNullability(boolean enabled);

	boolean isPreferUserTransaction();

	boolean isProcedureParameterNullPassingEnabled();

	boolean isCollectionJoinSubqueryRewriteEnabled();

	boolean isAllowOutOfTransactionUpdateOperations();

	boolean isReleaseResourcesOnCloseEnabled();

	TimeZone getJdbcTimeZone();

	default boolean isQueryParametersValidationEnabled(){
		return isJpaBootstrap();
	}

	default LiteralHandlingMode getCriteriaLiteralHandlingMode() {
		return LiteralHandlingMode.AUTO;
	}

	boolean jdbcStyleParamsZeroBased();

	JpaCompliance getJpaCompliance();

	boolean isFailOnPaginationOverCollectionFetchEnabled();

	default ImmutableEntityUpdateQueryHandlingMode getImmutableEntityUpdateQueryHandlingMode() {
		return ImmutableEntityUpdateQueryHandlingMode.WARNING;
	}

	default boolean inClauseParameterPaddingEnabled() {
		return false;
	}

	default boolean nativeExceptionHandling51Compliance() {
		return false;
	}

	default int getQueryStatisticsMaxSize() {
		return Statistics.DEFAULT_QUERY_STATISTICS_MAX_SIZE;
	}

	/**
	 * @deprecated Since 5.4.1, this is no longer used.
	 */
	@Deprecated
	default boolean isPostInsertIdentifierDelayableEnabled() {
		return true;
	}

	default boolean areJPACallbacksEnabled() {
		return true;
	}

	/**
	 * Can bytecode-enhanced entity classes be used as a "proxy"?
	 */
	default boolean isEnhancementAsProxyEnabled() {
		return false;
	}

	boolean isOmitJoinOfSuperclassTablesEnabled();
}
