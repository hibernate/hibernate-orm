/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.internal;

import java.util.Map;

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
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.tuple.entity.EntityTuplizerFactory;

/**
 * Sort of a mutable SessionFactoryOptions used during SessionFactoryBuilder calls.
 *
 * @author Steve Ebersole
 */
public interface SessionFactoryOptionsState {
	public StandardServiceRegistry getServiceRegistry();

	public Object getBeanManagerReference();

	public Object getValidatorFactoryReference();

	public String getSessionFactoryName();

	public boolean isSessionFactoryNameAlsoJndiName();

	public boolean isFlushBeforeCompletionEnabled();

	public boolean isAutoCloseSessionEnabled();

	public boolean isStatisticsEnabled();

	public Interceptor getInterceptor();

	public StatementInspector getStatementInspector();

	public SessionFactoryObserver[] getSessionFactoryObservers();

	public BaselineSessionEventsListenerBuilder getBaselineSessionEventsListenerBuilder();

	public boolean isIdentifierRollbackEnabled();

	public EntityMode getDefaultEntityMode();

	public EntityTuplizerFactory getEntityTuplizerFactory();

	public boolean isCheckNullability();

	public boolean isInitializeLazyStateOutsideTransactionsEnabled();

	public MultiTableBulkIdStrategy getMultiTableBulkIdStrategy();

	public TempTableDdlTransactionHandling getTempTableDdlTransactionHandling();

	public BatchFetchStyle getBatchFetchStyle();

	public int getDefaultBatchFetchSize();

	public Integer getMaximumFetchDepth();

	public NullPrecedence getDefaultNullPrecedence();

	public boolean isOrderUpdatesEnabled();

	public boolean isOrderInsertsEnabled();

	public MultiTenancyStrategy getMultiTenancyStrategy();

	public CurrentTenantIdentifierResolver getCurrentTenantIdentifierResolver();

	public boolean isJtaTrackByThread();

	public Map getQuerySubstitutions();

	public boolean isStrictJpaQueryLanguageCompliance();

	public boolean isNamedQueryStartupCheckingEnabled();

	public boolean isSecondLevelCacheEnabled();

	public boolean isQueryCacheEnabled();

	public QueryCacheFactory getQueryCacheFactory();

	public String getCacheRegionPrefix();

	public boolean isMinimalPutsEnabled();

	public boolean isStructuredCacheEntriesEnabled();

	public boolean isDirectReferenceCacheEntriesEnabled();

	public boolean isAutoEvictCollectionCache();

	public SchemaAutoTooling getSchemaAutoTooling();

	public int getJdbcBatchSize();

	public boolean isJdbcBatchVersionedData();

	public boolean isScrollableResultSetsEnabled();

	public boolean isWrapResultSetsEnabled();

	public boolean isGetGeneratedKeysEnabled();

	public Integer getJdbcFetchSize();

	public ConnectionReleaseMode getConnectionReleaseMode();

	public boolean isCommentsEnabled();

	public CustomEntityDirtinessStrategy getCustomEntityDirtinessStrategy();

	public EntityNameResolver[] getEntityNameResolvers();

	public EntityNotFoundDelegate getEntityNotFoundDelegate();

	public Map<String, SQLFunction> getCustomSqlFunctionMap();

	public boolean isPreferUserTransaction();
}
