/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.internal;

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
import org.hibernate.boot.spi.SessionFactoryOptions;
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
 * Sort of a mutable SessionFactoryOptions used during SessionFactoryBuilder calls.
 *
 * @author Steve Ebersole
 */
public interface SessionFactoryOptionsState {
	StandardServiceRegistry getServiceRegistry();

	/**
	 * @deprecated (since 5.2) see {@link SessionFactoryOptions#isJpaBootstrap} for details
	 * on deprecation and intention/use.
	 */
	@Deprecated
	boolean isJpaBootstrap();

	boolean isJtaTransactionAccessEnabled();

	boolean isAllowRefreshDetachedEntity();

	boolean isAllowOutOfTransactionUpdateOperations();

	boolean isReleaseResourcesOnCloseEnabled();

	Object getBeanManagerReference();

	Object getValidatorFactoryReference();

	String getSessionFactoryName();

	boolean isSessionFactoryNameAlsoJndiName();

	boolean isFlushBeforeCompletionEnabled();

	boolean isAutoCloseSessionEnabled();

	boolean isStatisticsEnabled();

	Interceptor getInterceptor();

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

	boolean isProcedureParameterNullPassingEnabled();

	boolean isCollectionJoinSubqueryRewriteEnabled();

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

	EntityNotFoundDelegate getEntityNotFoundDelegate();

	Map<String, SQLFunction> getCustomSqlFunctionMap();

	boolean isPreferUserTransaction();

	TimeZone getJdbcTimeZone();
}
