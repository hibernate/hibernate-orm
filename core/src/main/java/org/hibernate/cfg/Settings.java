/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.cfg;

import java.util.Map;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.EntityMode;
import org.hibernate.engine.jdbc.JdbcSupport;
import org.hibernate.tuple.entity.EntityTuplizerFactory;
import org.hibernate.tuple.component.ComponentTuplizerFactory;
import org.hibernate.cache.QueryCacheFactory;
import org.hibernate.cache.RegionFactory;
import org.hibernate.connection.ConnectionProvider;
import org.hibernate.dialect.Dialect;
import org.hibernate.exception.SQLExceptionConverter;
import org.hibernate.hql.QueryTranslatorFactory;
import org.hibernate.jdbc.BatcherFactory;
import org.hibernate.jdbc.util.SQLStatementLogger;
import org.hibernate.transaction.TransactionFactory;
import org.hibernate.transaction.TransactionManagerLookup;

/**
 * Settings that affect the behaviour of Hibernate at runtime.
 *
 * @author Gavin King
 */
public final class Settings {

//	private boolean showSql;
//	private boolean formatSql;
	private SQLStatementLogger sqlStatementLogger;
	private Integer maximumFetchDepth;
	private Map querySubstitutions;
	private Dialect dialect;
	private int jdbcBatchSize;
	private int defaultBatchFetchSize;
	private boolean scrollableResultSetsEnabled;
	private boolean getGeneratedKeysEnabled;
	private String defaultSchemaName;
	private String defaultCatalogName;
	private Integer jdbcFetchSize;
	private String sessionFactoryName;
	private boolean autoCreateSchema;
	private boolean autoDropSchema;
	private boolean autoUpdateSchema;
	private boolean autoValidateSchema;
	private boolean queryCacheEnabled;
	private boolean structuredCacheEntriesEnabled;
	private boolean secondLevelCacheEnabled;
	private String cacheRegionPrefix;
	private boolean minimalPutsEnabled;
	private boolean commentsEnabled;
	private boolean statisticsEnabled;
	private boolean jdbcBatchVersionedData;
	private boolean identifierRollbackEnabled;
	private boolean flushBeforeCompletionEnabled;
	private boolean autoCloseSessionEnabled;
	private ConnectionReleaseMode connectionReleaseMode;
	private RegionFactory regionFactory;
	private QueryCacheFactory queryCacheFactory;
	private ConnectionProvider connectionProvider;
	private TransactionFactory transactionFactory;
	private TransactionManagerLookup transactionManagerLookup;
	private BatcherFactory batcherFactory;
	private QueryTranslatorFactory queryTranslatorFactory;
	private SQLExceptionConverter sqlExceptionConverter;
	private boolean wrapResultSetsEnabled;
	private boolean orderUpdatesEnabled;
	private boolean orderInsertsEnabled;
	private EntityMode defaultEntityMode;
	private boolean dataDefinitionImplicitCommit;
	private boolean dataDefinitionInTransactionSupported;
	private boolean strictJPAQLCompliance;
	private boolean namedQueryStartupCheckingEnabled;
	private EntityTuplizerFactory entityTuplizerFactory;
	private boolean checkNullability;
//	private ComponentTuplizerFactory componentTuplizerFactory; todo : HHH-3517 and HHH-1907
//	private BytecodeProvider bytecodeProvider;
	private JdbcSupport jdbcSupport;

	/**
	 * Package protected constructor
	 */
	Settings() {
	}

	// public getters ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

//	public boolean isShowSqlEnabled() {
//		return showSql;
//	}
//
//	public boolean isFormatSqlEnabled() {
//		return formatSql;
//	}

	public SQLStatementLogger getSqlStatementLogger() {
		return sqlStatementLogger;
	}

	public String getDefaultSchemaName() {
		return defaultSchemaName;
	}

	public String getDefaultCatalogName() {
		return defaultCatalogName;
	}

	public Dialect getDialect() {
		return dialect;
	}

	public int getJdbcBatchSize() {
		return jdbcBatchSize;
	}

	public int getDefaultBatchFetchSize() {
		return defaultBatchFetchSize;
	}

	public Map getQuerySubstitutions() {
		return querySubstitutions;
	}

	public boolean isIdentifierRollbackEnabled() {
		return identifierRollbackEnabled;
	}

	public boolean isScrollableResultSetsEnabled() {
		return scrollableResultSetsEnabled;
	}

	public boolean isGetGeneratedKeysEnabled() {
		return getGeneratedKeysEnabled;
	}

	public boolean isMinimalPutsEnabled() {
		return minimalPutsEnabled;
	}

	public Integer getJdbcFetchSize() {
		return jdbcFetchSize;
	}

	public ConnectionProvider getConnectionProvider() {
		return connectionProvider;
	}

	public TransactionFactory getTransactionFactory() {
		return transactionFactory;
	}

	public String getSessionFactoryName() {
		return sessionFactoryName;
	}

	public boolean isAutoCreateSchema() {
		return autoCreateSchema;
	}

	public boolean isAutoDropSchema() {
		return autoDropSchema;
	}

	public boolean isAutoUpdateSchema() {
		return autoUpdateSchema;
	}

	public Integer getMaximumFetchDepth() {
		return maximumFetchDepth;
	}

	public RegionFactory getRegionFactory() {
		return regionFactory;
	}

	public TransactionManagerLookup getTransactionManagerLookup() {
		return transactionManagerLookup;
	}

	public boolean isQueryCacheEnabled() {
		return queryCacheEnabled;
	}

	public boolean isCommentsEnabled() {
		return commentsEnabled;
	}

	public boolean isSecondLevelCacheEnabled() {
		return secondLevelCacheEnabled;
	}

	public String getCacheRegionPrefix() {
		return cacheRegionPrefix;
	}

	public QueryCacheFactory getQueryCacheFactory() {
		return queryCacheFactory;
	}

	public boolean isStatisticsEnabled() {
		return statisticsEnabled;
	}

	public boolean isJdbcBatchVersionedData() {
		return jdbcBatchVersionedData;
	}

	public boolean isFlushBeforeCompletionEnabled() {
		return flushBeforeCompletionEnabled;
	}

	public BatcherFactory getBatcherFactory() {
		return batcherFactory;
	}

	public boolean isAutoCloseSessionEnabled() {
		return autoCloseSessionEnabled;
	}

	public ConnectionReleaseMode getConnectionReleaseMode() {
		return connectionReleaseMode;
	}

	public QueryTranslatorFactory getQueryTranslatorFactory() {
		return queryTranslatorFactory;
	}

	public SQLExceptionConverter getSQLExceptionConverter() {
		return sqlExceptionConverter;
	}

	public boolean isWrapResultSetsEnabled() {
		return wrapResultSetsEnabled;
	}

	public boolean isOrderUpdatesEnabled() {
		return orderUpdatesEnabled;
	}

	public boolean isOrderInsertsEnabled() {
		return orderInsertsEnabled;
	}

	public boolean isStructuredCacheEntriesEnabled() {
		return structuredCacheEntriesEnabled;
	}

	public EntityMode getDefaultEntityMode() {
		return defaultEntityMode;
	}

	public boolean isAutoValidateSchema() {
		return autoValidateSchema;
	}

	public boolean isDataDefinitionImplicitCommit() {
		return dataDefinitionImplicitCommit;
	}

	public boolean isDataDefinitionInTransactionSupported() {
		return dataDefinitionInTransactionSupported;
	}

	public boolean isStrictJPAQLCompliance() {
		return strictJPAQLCompliance;
	}

	public boolean isNamedQueryStartupCheckingEnabled() {
		return namedQueryStartupCheckingEnabled;
	}

	public EntityTuplizerFactory getEntityTuplizerFactory() {
		return entityTuplizerFactory;
	}

//	public ComponentTuplizerFactory getComponentTuplizerFactory() {
//		return componentTuplizerFactory;
//	}

	public JdbcSupport getJdbcSupport() {
		return jdbcSupport;
	}


	// package protected setters ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

//	void setShowSqlEnabled(boolean b) {
//		showSql = b;
//	}
//
//	void setFormatSqlEnabled(boolean b) {
//		formatSql = b;
//	}

	void setSqlStatementLogger(SQLStatementLogger sqlStatementLogger) {
		this.sqlStatementLogger = sqlStatementLogger;
	}

	void setDefaultSchemaName(String string) {
		defaultSchemaName = string;
	}

	void setDefaultCatalogName(String string) {
		defaultCatalogName = string;
	}

	void setDialect(Dialect dialect) {
		this.dialect = dialect;
	}

	void setJdbcBatchSize(int i) {
		jdbcBatchSize = i;
	}

	void setDefaultBatchFetchSize(int i) {
		defaultBatchFetchSize = i;
	}

	void setQuerySubstitutions(Map map) {
		querySubstitutions = map;
	}

	void setIdentifierRollbackEnabled(boolean b) {
		identifierRollbackEnabled = b;
	}

	void setMinimalPutsEnabled(boolean b) {
		minimalPutsEnabled = b;
	}

	void setScrollableResultSetsEnabled(boolean b) {
		scrollableResultSetsEnabled = b;
	}

	void setGetGeneratedKeysEnabled(boolean b) {
		getGeneratedKeysEnabled = b;
	}

	void setJdbcFetchSize(Integer integer) {
		jdbcFetchSize = integer;
	}

	void setConnectionProvider(ConnectionProvider provider) {
		connectionProvider = provider;
	}

	void setTransactionFactory(TransactionFactory factory) {
		transactionFactory = factory;
	}

	void setSessionFactoryName(String string) {
		sessionFactoryName = string;
	}

	void setAutoCreateSchema(boolean b) {
		autoCreateSchema = b;
	}

	void setAutoDropSchema(boolean b) {
		autoDropSchema = b;
	}

	void setAutoUpdateSchema(boolean b) {
		autoUpdateSchema = b;
	}

	void setMaximumFetchDepth(Integer i) {
		maximumFetchDepth = i;
	}

	void setRegionFactory(RegionFactory regionFactory) {
		this.regionFactory = regionFactory;
	}

	void setTransactionManagerLookup(TransactionManagerLookup lookup) {
		transactionManagerLookup = lookup;
	}

	void setQueryCacheEnabled(boolean b) {
		queryCacheEnabled = b;
	}

	void setCommentsEnabled(boolean commentsEnabled) {
		this.commentsEnabled = commentsEnabled;
	}

	void setSecondLevelCacheEnabled(boolean secondLevelCacheEnabled) {
		this.secondLevelCacheEnabled = secondLevelCacheEnabled;
	}

	void setCacheRegionPrefix(String cacheRegionPrefix) {
		this.cacheRegionPrefix = cacheRegionPrefix;
	}

	void setQueryCacheFactory(QueryCacheFactory queryCacheFactory) {
		this.queryCacheFactory = queryCacheFactory;
	}

	void setStatisticsEnabled(boolean statisticsEnabled) {
		this.statisticsEnabled = statisticsEnabled;
	}

	void setJdbcBatchVersionedData(boolean jdbcBatchVersionedData) {
		this.jdbcBatchVersionedData = jdbcBatchVersionedData;
	}

	void setFlushBeforeCompletionEnabled(boolean flushBeforeCompletionEnabled) {
		this.flushBeforeCompletionEnabled = flushBeforeCompletionEnabled;
	}

	void setBatcherFactory(BatcherFactory batcher) {
		this.batcherFactory = batcher;
	}

	void setAutoCloseSessionEnabled(boolean autoCloseSessionEnabled) {
		this.autoCloseSessionEnabled = autoCloseSessionEnabled;
	}

	void setConnectionReleaseMode(ConnectionReleaseMode connectionReleaseMode) {
		this.connectionReleaseMode = connectionReleaseMode;
	}

	void setQueryTranslatorFactory(QueryTranslatorFactory queryTranslatorFactory) {
		this.queryTranslatorFactory = queryTranslatorFactory;
	}

	void setSQLExceptionConverter(SQLExceptionConverter sqlExceptionConverter) {
		this.sqlExceptionConverter = sqlExceptionConverter;
	}

	void setWrapResultSetsEnabled(boolean wrapResultSetsEnabled) {
		this.wrapResultSetsEnabled = wrapResultSetsEnabled;
	}

	void setOrderUpdatesEnabled(boolean orderUpdatesEnabled) {
		this.orderUpdatesEnabled = orderUpdatesEnabled;
	}

	void setOrderInsertsEnabled(boolean orderInsertsEnabled) {
		this.orderInsertsEnabled = orderInsertsEnabled;
	}

	void setStructuredCacheEntriesEnabled(boolean structuredCacheEntriesEnabled) {
		this.structuredCacheEntriesEnabled = structuredCacheEntriesEnabled;
	}

	void setDefaultEntityMode(EntityMode defaultEntityMode) {
		this.defaultEntityMode = defaultEntityMode;
	}

	void setAutoValidateSchema(boolean autoValidateSchema) {
		this.autoValidateSchema = autoValidateSchema;
	}

	void setDataDefinitionImplicitCommit(boolean dataDefinitionImplicitCommit) {
		this.dataDefinitionImplicitCommit = dataDefinitionImplicitCommit;
	}

	void setDataDefinitionInTransactionSupported(boolean dataDefinitionInTransactionSupported) {
		this.dataDefinitionInTransactionSupported = dataDefinitionInTransactionSupported;
	}

	void setStrictJPAQLCompliance(boolean strictJPAQLCompliance) {
		this.strictJPAQLCompliance = strictJPAQLCompliance;
	}

	void setNamedQueryStartupCheckingEnabled(boolean namedQueryStartupCheckingEnabled) {
		this.namedQueryStartupCheckingEnabled = namedQueryStartupCheckingEnabled;
	}

	void setEntityTuplizerFactory(EntityTuplizerFactory entityTuplizerFactory) {
		this.entityTuplizerFactory = entityTuplizerFactory;
	}

	public boolean isCheckNullability() {
		return checkNullability;
	}

	public void setCheckNullability(boolean checkNullability) {
		this.checkNullability = checkNullability;
	}

	//	void setComponentTuplizerFactory(ComponentTuplizerFactory componentTuplizerFactory) {
//		this.componentTuplizerFactory = componentTuplizerFactory;
//	}

	void setJdbcSupport(JdbcSupport jdbcSupport) {
		this.jdbcSupport = jdbcSupport;
	}

	//	public BytecodeProvider getBytecodeProvider() {
//		return bytecodeProvider;
//	}
//
//	void setBytecodeProvider(BytecodeProvider bytecodeProvider) {
//		this.bytecodeProvider = bytecodeProvider;
//	}
}
