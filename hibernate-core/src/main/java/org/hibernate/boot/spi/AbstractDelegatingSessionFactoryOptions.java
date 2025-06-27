/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.spi;

import java.util.Map;
import java.util.TimeZone;
import java.util.function.Supplier;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import org.hibernate.CacheMode;
import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.EntityNameResolver;
import org.hibernate.FlushMode;
import org.hibernate.Interceptor;
import org.hibernate.LockOptions;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.context.spi.TenantSchemaMapper;
import org.hibernate.type.TimeZoneStorageStrategy;
import org.hibernate.annotations.CacheLayout;
import org.hibernate.boot.SchemaAutoTooling;
import org.hibernate.boot.TempTableDdlTransactionHandling;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.cache.spi.TimestampsCacheFactory;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.internal.BaselineSessionEventsListenerBuilder;
import org.hibernate.jpa.spi.JpaCompliance;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.query.spi.ImmutableEntityUpdateQueryHandlingMode;
import org.hibernate.query.criteria.ValueHandlingMode;
import org.hibernate.query.hql.HqlTranslator;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.query.sqm.sql.SqmTranslatorFactory;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.format.FormatMapper;

import jakarta.persistence.criteria.Nulls;

import static org.hibernate.internal.log.DeprecationLogger.DEPRECATION_LOGGER;

/**
 * Convenience base class for custom implementations of {@link SessionFactoryOptions}
 * using delegation.
 *
 * @implNote Declared non-abstract to ensure that all {@link SessionFactoryOptions} methods
 *           have at least a default implementation.
 *
 * @author Steve Ebersole
 */
public class AbstractDelegatingSessionFactoryOptions implements SessionFactoryOptions {
	private final SessionFactoryOptions delegate;

	public AbstractDelegatingSessionFactoryOptions(SessionFactoryOptions delegate) {
		this.delegate = delegate;
	}

	protected SessionFactoryOptions delegate() {
		return delegate;
	}

	@Override
	public String getUuid() {
		return delegate().getUuid();
	}

	@Override
	public StandardServiceRegistry getServiceRegistry() {
		return delegate.getServiceRegistry();
	}

	@Override
	public boolean isJpaBootstrap() {
		return delegate.isJpaBootstrap();
	}

	@Override
	public boolean isJtaTransactionAccessEnabled() {
		return delegate.isJtaTransactionAccessEnabled();
	}

	/**
	 * @deprecated with no replacement.
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	@Override
	public boolean isAllowRefreshDetachedEntity() {
		DEPRECATION_LOGGER.deprecatedRefreshLockDetachedEntity();
		return delegate.isAllowRefreshDetachedEntity();
	}

	@Override
	public Object getBeanManagerReference() {
		return delegate.getBeanManagerReference();
	}

	@Override
	public Object getValidatorFactoryReference() {
		return delegate.getValidatorFactoryReference();
	}

	@Override
	public String getSessionFactoryName() {
		return delegate.getSessionFactoryName();
	}

	@Override
	public Boolean isSessionFactoryNameAlsoJndiName() {
		return delegate.isSessionFactoryNameAlsoJndiName();
	}

	@Override
	public boolean isFlushBeforeCompletionEnabled() {
		return delegate.isFlushBeforeCompletionEnabled();
	}

	@Override
	public boolean isAutoCloseSessionEnabled() {
		return delegate.isAutoCloseSessionEnabled();
	}

	@Override
	public boolean isStatisticsEnabled() {
		return delegate.isStatisticsEnabled();
	}

	@Override
	public Interceptor getInterceptor() {
		return delegate.getInterceptor();
	}

	@Override
	public SqmMultiTableMutationStrategy getCustomSqmMultiTableMutationStrategy() {
		return delegate.getCustomSqmMultiTableMutationStrategy();
	}

	@Override
	public SqmMultiTableInsertStrategy getCustomSqmMultiTableInsertStrategy() {
		return delegate.getCustomSqmMultiTableInsertStrategy();
	}

	@Override
	public StatementInspector getStatementInspector() {
		return delegate.getStatementInspector();
	}

	@Override
	public SessionFactoryObserver[] getSessionFactoryObservers() {
		return delegate.getSessionFactoryObservers();
	}

	@Override
	public BaselineSessionEventsListenerBuilder getBaselineSessionEventsListenerBuilder() {
		return delegate.getBaselineSessionEventsListenerBuilder();
	}

	@Override
	public boolean isIdentifierRollbackEnabled() {
		return delegate.isIdentifierRollbackEnabled();
	}

	@Override
	public boolean isCheckNullability() {
		return delegate.isCheckNullability();
	}

	@Override
	public boolean isInitializeLazyStateOutsideTransactionsEnabled() {
		return delegate.isInitializeLazyStateOutsideTransactionsEnabled();
	}

	@Override @Deprecated
	public TempTableDdlTransactionHandling getTempTableDdlTransactionHandling() {
		return delegate.getTempTableDdlTransactionHandling();
	}

	@Override
	public boolean isDelayBatchFetchLoaderCreationsEnabled() {
		return delegate.isDelayBatchFetchLoaderCreationsEnabled();
	}

	@Override
	public int getDefaultBatchFetchSize() {
		return delegate.getDefaultBatchFetchSize();
	}

	@Override
	public Integer getMaximumFetchDepth() {
		return delegate.getMaximumFetchDepth();
	}

	@Override
	public boolean isSubselectFetchEnabled() {
		return delegate.isSubselectFetchEnabled();
	}

	@Override
	public Nulls getDefaultNullPrecedence() {
		return delegate.getDefaultNullPrecedence();
	}

	@Override
	public boolean isOrderUpdatesEnabled() {
		return delegate.isOrderUpdatesEnabled();
	}

	@Override
	public boolean isOrderInsertsEnabled() {
		return delegate.isOrderInsertsEnabled();
	}

	@Override
	public boolean isMultiTenancyEnabled() {
		return delegate.isMultiTenancyEnabled();
	}

	@Override
	public CurrentTenantIdentifierResolver<Object> getCurrentTenantIdentifierResolver() {
		return delegate.getCurrentTenantIdentifierResolver();
	}

	@Override
	public TenantSchemaMapper<Object> getTenantSchemaMapper() {
		return delegate.getTenantSchemaMapper();
	}

	@Override
	public JavaType<Object> getDefaultTenantIdentifierJavaType() {
		return delegate.getDefaultTenantIdentifierJavaType();
	}

	@Override
	public boolean isJtaTrackByThread() {
		return delegate.isJtaTrackByThread();
	}

	@Override
	public boolean isNamedQueryStartupCheckingEnabled() {
		return delegate.isNamedQueryStartupCheckingEnabled();
	}

	@Override
	public boolean isAllowOutOfTransactionUpdateOperations() {
		return delegate.isAllowOutOfTransactionUpdateOperations();
	}

	@Override @Deprecated
	public boolean isReleaseResourcesOnCloseEnabled() {
		return delegate.isReleaseResourcesOnCloseEnabled();
	}

	@Override
	public boolean isSecondLevelCacheEnabled() {
		return delegate.isSecondLevelCacheEnabled();
	}

	@Override
	public boolean isQueryCacheEnabled() {
		return delegate.isQueryCacheEnabled();
	}

	@Override
	public CacheLayout getQueryCacheLayout() {
		return delegate.getQueryCacheLayout();
	}

	@Override
	public TimestampsCacheFactory getTimestampsCacheFactory() {
		return delegate.getTimestampsCacheFactory();
	}

	@Override
	public String getCacheRegionPrefix() {
		return delegate.getCacheRegionPrefix();
	}

	@Override
	public boolean isMinimalPutsEnabled() {
		return delegate.isMinimalPutsEnabled();
	}

	@Override
	public boolean isStructuredCacheEntriesEnabled() {
		return delegate.isStructuredCacheEntriesEnabled();
	}

	@Override
	public boolean isDirectReferenceCacheEntriesEnabled() {
		return delegate.isDirectReferenceCacheEntriesEnabled();
	}

	@Override
	public boolean isAutoEvictCollectionCache() {
		return delegate.isAutoEvictCollectionCache();
	}

	@Override @Deprecated
	public SchemaAutoTooling getSchemaAutoTooling() {
		return delegate.getSchemaAutoTooling();
	}

	@Override
	public int getJdbcBatchSize() {
		return delegate.getJdbcBatchSize();
	}

	@Override
	public boolean isScrollableResultSetsEnabled() {
		return delegate.isScrollableResultSetsEnabled();
	}

	@Override
	public boolean isGetGeneratedKeysEnabled() {
		return delegate.isGetGeneratedKeysEnabled();
	}

	@Override
	public Integer getJdbcFetchSize() {
		return delegate.getJdbcFetchSize();
	}

	@Override
	public PhysicalConnectionHandlingMode getPhysicalConnectionHandlingMode() {
		return delegate.getPhysicalConnectionHandlingMode();
	}

	@Override
	public boolean doesConnectionProviderDisableAutoCommit() {
		return delegate.doesConnectionProviderDisableAutoCommit();
	}

	@Override
	public boolean isCommentsEnabled() {
		return delegate.isCommentsEnabled();
	}

	@Override
	public CustomEntityDirtinessStrategy getCustomEntityDirtinessStrategy() {
		return delegate.getCustomEntityDirtinessStrategy();
	}

	@Override
	public EntityNameResolver[] getEntityNameResolvers() {
		return delegate.getEntityNameResolvers();
	}

	@Override
	public EntityNotFoundDelegate getEntityNotFoundDelegate() {
		return delegate.getEntityNotFoundDelegate();
	}

	@Override
	public Map<String, SqmFunctionDescriptor> getCustomSqlFunctionMap() {
		return delegate.getCustomSqlFunctionMap();
	}

	@Override
	public void setCheckNullability(boolean enabled) {
		delegate.setCheckNullability( enabled );
	}

	@Override
	public boolean isPreferUserTransaction() {
		return delegate.isPreferUserTransaction();
	}

	@Override
	public Supplier<? extends Interceptor> getStatelessInterceptorImplementorSupplier() {
		return delegate.getStatelessInterceptorImplementorSupplier();
	}

	@Override
	public HqlTranslator getCustomHqlTranslator() {
		return delegate.getCustomHqlTranslator();
	}

	@Override
	public SqmTranslatorFactory getCustomSqmTranslatorFactory() {
		return delegate.getCustomSqmTranslatorFactory();
	}

	@Override
	public TimeZone getJdbcTimeZone() {
		return delegate.getJdbcTimeZone();
	}

	@Override
	public ValueHandlingMode getCriteriaValueHandlingMode() {
		return delegate.getCriteriaValueHandlingMode();
	}

	@Override
	public boolean isCriteriaCopyTreeEnabled() {
		return delegate.isCriteriaCopyTreeEnabled();
	}

	@Override
	public boolean isCriteriaPlanCacheEnabled() {
		return delegate.isCriteriaPlanCacheEnabled();
	}

	public boolean getNativeJdbcParametersIgnored() {
		return delegate.getNativeJdbcParametersIgnored();
	}

	@Override
	public JpaCompliance getJpaCompliance() {
		return delegate.getJpaCompliance();
	}

	@Override
	public boolean isFailOnPaginationOverCollectionFetchEnabled() {
		return delegate.isFailOnPaginationOverCollectionFetchEnabled();
	}

	@Override
	public ImmutableEntityUpdateQueryHandlingMode getImmutableEntityUpdateQueryHandlingMode() {
		return delegate.getImmutableEntityUpdateQueryHandlingMode();
	}

	@Override
	public boolean allowImmutableEntityUpdate() {
		return delegate.allowImmutableEntityUpdate();
	}

	@Override
	public String getDefaultCatalog() {
		return delegate.getDefaultCatalog();
	}

	@Override
	public String getDefaultSchema() {
		return delegate.getDefaultSchema();
	}

	@Override
	public boolean inClauseParameterPaddingEnabled() {
		return delegate.inClauseParameterPaddingEnabled();
	}

	@Override
	public boolean isJsonFunctionsEnabled() {
		return delegate.isJsonFunctionsEnabled();
	}

	@Override
	public boolean isXmlFunctionsEnabled() {
		return delegate.isXmlFunctionsEnabled();
	}

	@Override
	public boolean isPortableIntegerDivisionEnabled() {
		return delegate.isPortableIntegerDivisionEnabled();
	}

	@Override
	public int getQueryStatisticsMaxSize() {
		return delegate.getQueryStatisticsMaxSize();
	}

	@Override
	public boolean areJPACallbacksEnabled() {
		return delegate.areJPACallbacksEnabled();
	}

	@Override
	public boolean isCollectionsInDefaultFetchGroupEnabled() {
		return delegate.isCollectionsInDefaultFetchGroupEnabled();
	}

	@Override
	public boolean isUnownedAssociationTransientCheck() {
		return delegate.isUnownedAssociationTransientCheck();
	}

	@Override
	public boolean isUseOfJdbcNamedParametersEnabled() {
		return delegate().isUseOfJdbcNamedParametersEnabled();
	}

	@Override
	public SqmFunctionRegistry getCustomSqmFunctionRegistry() {
		return delegate().getCustomSqmFunctionRegistry();
	}

	@Override
	public int getPreferredSqlTypeCodeForBoolean() {
		return delegate.getPreferredSqlTypeCodeForBoolean();
	}

	@Override
	public int getPreferredSqlTypeCodeForDuration() {
		return delegate.getPreferredSqlTypeCodeForDuration();
	}

	@Override
	public int getPreferredSqlTypeCodeForUuid() {
		return delegate.getPreferredSqlTypeCodeForUuid();
	}

	@Override
	public int getPreferredSqlTypeCodeForInstant() {
		return delegate.getPreferredSqlTypeCodeForInstant();
	}

	@Override
	public int getPreferredSqlTypeCodeForArray() {
		return delegate.getPreferredSqlTypeCodeForArray();
	}

	@Override
	public TimeZoneStorageStrategy getDefaultTimeZoneStorageStrategy() {
		return delegate.getDefaultTimeZoneStorageStrategy();
	}

	@Override
	public boolean isPreferJavaTimeJdbcTypesEnabled() {
		return delegate.isPreferJavaTimeJdbcTypesEnabled();
	}

	@Override
	public boolean isPreferNativeEnumTypesEnabled() {
		return delegate.isPreferNativeEnumTypesEnabled();
	}

	@Override
	public FormatMapper getJsonFormatMapper() {
		return delegate.getJsonFormatMapper();
	}

	@Override
	public FormatMapper getXmlFormatMapper() {
		return delegate.getXmlFormatMapper();
	}

	@Override
	public boolean isXmlFormatMapperLegacyFormatEnabled() {
		return delegate.isXmlFormatMapperLegacyFormatEnabled();
	}

	@Override
	public boolean isPassProcedureParameterNames() {
		return delegate.isPassProcedureParameterNames();
	}

	@Override
	public boolean isPreferJdbcDatetimeTypesInNativeQueriesEnabled() {
		return delegate.isPreferJdbcDatetimeTypesInNativeQueriesEnabled();
	}

	@Override
	public CacheStoreMode getCacheStoreMode(Map<String, Object> properties) {
		return delegate.getCacheStoreMode( properties );
	}

	@Override
	public CacheRetrieveMode getCacheRetrieveMode(Map<String, Object> properties) {
		return delegate.getCacheRetrieveMode( properties );
	}

	@Override
	public Map<String, Object> getDefaultSessionProperties() {
		return delegate.getDefaultSessionProperties();
	}

	@Override
	public CacheMode getInitialSessionCacheMode() {
		return delegate.getInitialSessionCacheMode();
	}

	@Override
	public FlushMode getInitialSessionFlushMode() {
		return delegate.getInitialSessionFlushMode();
	}

	@Override
	public LockOptions getDefaultLockOptions() {
		return delegate.getDefaultLockOptions();
	}
}
