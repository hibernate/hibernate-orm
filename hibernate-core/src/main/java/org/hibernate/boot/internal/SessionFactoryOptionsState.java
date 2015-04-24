/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
