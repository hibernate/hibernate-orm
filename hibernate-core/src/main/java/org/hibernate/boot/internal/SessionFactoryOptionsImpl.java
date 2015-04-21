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
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.spi.QueryCacheFactory;
import org.hibernate.cfg.BaselineSessionEventsListenerBuilder;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.hql.spi.MultiTableBulkIdStrategy;
import org.hibernate.loader.BatchFetchStyle;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.tuple.entity.EntityTuplizerFactory;

/**
 * Standard implementation of SessionFactoryOptions
 *
 * @author Steve Ebersole
 */
public class SessionFactoryOptionsImpl implements SessionFactoryOptions {
	private final StandardServiceRegistry serviceRegistry;

	// integration
	private final Object beanManagerReference;
	private final Object validatorFactoryReference;

	// SessionFactory behavior
	private final String sessionFactoryName;
	private final boolean sessionFactoryNameAlsoJndiName;

	// Session behavior
	private final boolean flushBeforeCompletionEnabled;
	private final boolean autoCloseSessionEnabled;

	// Statistics/Interceptor/observers
	private final boolean statisticsEnabled;
	private final Interceptor interceptor;
	private final SessionFactoryObserver[] sessionFactoryObserverList;
	private final BaselineSessionEventsListenerBuilder baselineSessionEventsListenerBuilder;	// not exposed on builder atm

	// persistence behavior
	private final CustomEntityDirtinessStrategy customEntityDirtinessStrategy;
	private final EntityNameResolver[] entityNameResolvers;
	private final EntityNotFoundDelegate entityNotFoundDelegate;
	private final boolean identifierRollbackEnabled;
	private final EntityMode defaultEntityMode;
	private final EntityTuplizerFactory entityTuplizerFactory;
	private boolean checkNullability;
	private final boolean initializeLazyStateOutsideTransactions;
	private final MultiTableBulkIdStrategy multiTableBulkIdStrategy;
	private final BatchFetchStyle batchFetchStyle;
	private final int defaultBatchFetchSize;
	private final Integer maximumFetchDepth;
	private final NullPrecedence defaultNullPrecedence;
	private final boolean orderUpdatesEnabled;
	private final boolean orderInsertsEnabled;

	// multi-tenancy
	private final MultiTenancyStrategy multiTenancyStrategy;
	private final CurrentTenantIdentifierResolver currentTenantIdentifierResolver;

	// JTA timeout detection
	private final boolean jtaTrackByThread;

	// Queries
	private final Map querySubstitutions;
	private final boolean strictJpaQueryLanguageCompliance;
	private final boolean namedQueryStartupCheckingEnabled;

	// Caching
	private final boolean secondLevelCacheEnabled;
	private final boolean queryCacheEnabled;
	private final QueryCacheFactory queryCacheFactory;
	private final String cacheRegionPrefix;
	private final boolean minimalPutsEnabled;
	private final boolean structuredCacheEntriesEnabled;
	private final boolean directReferenceCacheEntriesEnabled;
	private final boolean autoEvictCollectionCache;

	// Schema tooling
	private final SchemaAutoTooling schemaAutoTooling;

	// JDBC Handling
	private final boolean dataDefinitionImplicitCommit;			// not exposed on builder atm
	private final boolean dataDefinitionInTransactionSupported;	// not exposed on builder atm
	private final boolean getGeneratedKeysEnabled;
	private final int jdbcBatchSize;
	private final boolean jdbcBatchVersionedData;
	private final Integer jdbcFetchSize;
	private final boolean scrollableResultSetsEnabled;
	private final boolean commentsEnabled;
	private final ConnectionReleaseMode connectionReleaseMode;
	private final boolean wrapResultSetsEnabled;

	private final Map<String, SQLFunction> sqlFunctions;


	public SessionFactoryOptionsImpl(SessionFactoryOptionsState state) {
		this.serviceRegistry = state.getServiceRegistry();

		this.beanManagerReference = state.getBeanManagerReference();
		this.validatorFactoryReference = state.getValidatorFactoryReference();

		this.sessionFactoryName = state.getSessionFactoryName();
		this.sessionFactoryNameAlsoJndiName = state.isSessionFactoryNameAlsoJndiName();

		this.flushBeforeCompletionEnabled = state.isFlushBeforeCompletionEnabled();
		this.autoCloseSessionEnabled = state.isAutoCloseSessionEnabled();

		this.statisticsEnabled = state.isStatisticsEnabled();
		this.interceptor = state.getInterceptor();
		this.sessionFactoryObserverList = state.getSessionFactoryObservers();
		this.baselineSessionEventsListenerBuilder = state.getBaselineSessionEventsListenerBuilder();

		this.customEntityDirtinessStrategy = state.getCustomEntityDirtinessStrategy();
		this.entityNameResolvers = state.getEntityNameResolvers();
		this.entityNotFoundDelegate = state.getEntityNotFoundDelegate();
		this.identifierRollbackEnabled = state.isIdentifierRollbackEnabled();
		this.defaultEntityMode = state.getDefaultEntityMode();
		this.entityTuplizerFactory = state.getEntityTuplizerFactory();
		this.checkNullability = state.isCheckNullability();
		this.initializeLazyStateOutsideTransactions = state.isInitializeLazyStateOutsideTransactionsEnabled();
		this.multiTableBulkIdStrategy = state.getMultiTableBulkIdStrategy();
		this.batchFetchStyle = state.getBatchFetchStyle();
		this.defaultBatchFetchSize = state.getDefaultBatchFetchSize();
		this.maximumFetchDepth = state.getMaximumFetchDepth();
		this.defaultNullPrecedence = state.getDefaultNullPrecedence();
		this.orderUpdatesEnabled = state.isOrderUpdatesEnabled();
		this.orderInsertsEnabled = state.isOrderInsertsEnabled();

		this.multiTenancyStrategy = state.getMultiTenancyStrategy();
		this.currentTenantIdentifierResolver = state.getCurrentTenantIdentifierResolver();

		this.jtaTrackByThread = state.isJtaTrackByThread();

		this.querySubstitutions = state.getQuerySubstitutions();
		this.strictJpaQueryLanguageCompliance = state.isStrictJpaQueryLanguageCompliance();
		this.namedQueryStartupCheckingEnabled = state.isNamedQueryStartupCheckingEnabled();

		this.secondLevelCacheEnabled = state.isSecondLevelCacheEnabled();
		this.queryCacheEnabled = state.isQueryCacheEnabled();
		this.queryCacheFactory = state.getQueryCacheFactory();
		this.cacheRegionPrefix = state.getCacheRegionPrefix();
		this.minimalPutsEnabled = state.isMinimalPutsEnabled();
		this.structuredCacheEntriesEnabled = state.isStructuredCacheEntriesEnabled();
		this.directReferenceCacheEntriesEnabled = state.isDirectReferenceCacheEntriesEnabled();
		this.autoEvictCollectionCache = state.isAutoEvictCollectionCache();

		this.schemaAutoTooling = state.getSchemaAutoTooling();

		this.connectionReleaseMode = state.getConnectionReleaseMode();
		this.dataDefinitionImplicitCommit = state.isDataDefinitionImplicitCommit();
		this.dataDefinitionInTransactionSupported = state.isDataDefinitionInTransactionSupported();
		this.getGeneratedKeysEnabled = state.isGetGeneratedKeysEnabled();
		this.jdbcBatchSize = state.getJdbcBatchSize();
		this.jdbcBatchVersionedData = state.isJdbcBatchVersionedData();
		this.jdbcFetchSize = state.getJdbcFetchSize();
		this.scrollableResultSetsEnabled = state.isScrollableResultSetsEnabled();
		this.wrapResultSetsEnabled = state.isWrapResultSetsEnabled();
		this.commentsEnabled = state.isCommentsEnabled();

		this.sqlFunctions = state.getCustomSqlFunctionMap();
	}

	@Override
	public StandardServiceRegistry getServiceRegistry() {
		return serviceRegistry;
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
		return interceptor;
	}

	@Override
	public BaselineSessionEventsListenerBuilder getBaselineSessionEventsListenerBuilder() {
		return baselineSessionEventsListenerBuilder;
	}

	@Override
	public SessionFactoryObserver[] getSessionFactoryObservers() {
		return sessionFactoryObserverList;
	}

	@Override
	public boolean isIdentifierRollbackEnabled() {
		return identifierRollbackEnabled;
	}

	@Override
	public EntityMode getDefaultEntityMode() {
		return defaultEntityMode;
	}

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

	public boolean isAutoEvictCollectionCache() {
		return autoEvictCollectionCache;
	}

	@Override
	public SchemaAutoTooling getSchemaAutoTooling() {
		return schemaAutoTooling;
	}

	@Override
	public boolean isDataDefinitionImplicitCommit() {
		return dataDefinitionImplicitCommit;
	}

	@Override
	public boolean isDataDefinitionInTransactionSupported() {
		return dataDefinitionInTransactionSupported;
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
	public ConnectionReleaseMode getConnectionReleaseMode() {
		return connectionReleaseMode;
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
		return entityNameResolvers;
	}

	@Override
	public EntityNotFoundDelegate getEntityNotFoundDelegate() {
		return entityNotFoundDelegate;
	}

	@Override
	public Map<String, SQLFunction> getCustomSqlFunctionMap() {
		return sqlFunctions;
	}

	@Override
	public void setCheckNullability(boolean enabled) {
		this.checkNullability = enabled;
	}
}
