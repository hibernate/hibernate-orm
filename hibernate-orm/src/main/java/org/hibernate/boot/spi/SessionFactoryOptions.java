/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.spi;

import java.util.Map;
import java.util.TimeZone;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.EntityMode;
import org.hibernate.EntityNameResolver;
import org.hibernate.Interceptor;
import org.hibernate.MultiTenancyStrategy;
import org.hibernate.NullPrecedence;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.SchemaAutoTooling;
import org.hibernate.boot.TempTableDdlTransactionHandling;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.cache.spi.QueryCacheFactory;
import org.hibernate.cfg.BaselineSessionEventsListenerBuilder;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.loader.BatchFetchStyle;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.tuple.entity.EntityTuplizerFactory;

/**
 * Aggregator of special options used to build the SessionFactory.
 *
 * @since 5.0
 */
public interface SessionFactoryOptions {
	/**
	 * The service registry to use in building the factory.
	 *
	 * @return The service registry to use.
	 */
	StandardServiceRegistry getServiceRegistry();

	Object getBeanManagerReference();

	Object getValidatorFactoryReference();

	/**
	 * @deprecated (since 5.2) In fact added in 5.2 as part of consolidating JPA support
	 * directly into Hibernate contracts (SessionFactory, Session); intended to provide
	 * transition help in cases where we need to know the difference in JPA/native use for
	 * various reasons.
	 *
	 * @see SessionFactoryBuilderImplementor#markAsJpaBootstrap
	 */
	@Deprecated
	boolean isJpaBootstrap();

	boolean isJtaTransactionAccessEnabled();

	default boolean isAllowRefreshDetachedEntity() {
		return false;
	}

	/**
	 * The name to be used for the SessionFactory.  This is use both in:<ul>
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
	Class<? extends Interceptor> getStatelessInterceptorImplementor();

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

	int getDefaultBatchFetchSize();

	Integer getMaximumFetchDepth();

	NullPrecedence getDefaultNullPrecedence();

	boolean isOrderUpdatesEnabled();

	boolean isOrderInsertsEnabled();

	MultiTenancyStrategy getMultiTenancyStrategy();

	CurrentTenantIdentifierResolver getCurrentTenantIdentifierResolver();

	boolean isJtaTrackByThread();

	Map getQuerySubstitutions();

	boolean isStrictJpaQueryLanguageCompliance();

	boolean isNamedQueryStartupCheckingEnabled();

	boolean isConventionalJavaConstants();

	boolean isSecondLevelCacheEnabled();

	boolean isQueryCacheEnabled();

	QueryCacheFactory getQueryCacheFactory();

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
}
