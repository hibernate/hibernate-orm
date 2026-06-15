/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.internal;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Objects;

import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.pipeline.spi.ResolvedSessionFactorySettings;
import org.hibernate.boot.pipeline.spi.SessionFactoryConstructionIdentity;
import org.hibernate.boot.spi.SessionFactoryOptions;

/// **Temporary** adapter from the PoC's resolved factory settings to ORM's legacy
/// [SessionFactoryOptions] SPI.
/// This adapter deliberately supports only the subset audited for the immediate
/// `SessionFactoryImpl` constructor path.  Unsupported method calls fail
/// loudly so each new runtime dependency can be audited before it becomes part of
/// [ResolvedSessionFactorySettings].
///
/// @author Steve Ebersole
///
/// @deprecated This is a temporary adapter which is useful during migration to new SF bootstrap design.
@Deprecated(forRemoval = true)
public final class SessionFactoryOptionsAdapter {
	private SessionFactoryOptionsAdapter() {
	}

	public static SessionFactoryOptions create(ResolvedSessionFactorySettings settings) {
		return create( settings, SessionFactoryConstructionIdentity.resolve( settings ) );
	}

	public static SessionFactoryOptions create(
			ResolvedSessionFactorySettings settings,
			SessionFactoryConstructionIdentity identity) {
		return create( settings, identity, new SessionFactoryObserver[0] );
	}

	public static SessionFactoryOptions create(
			ResolvedSessionFactorySettings settings,
			SessionFactoryConstructionIdentity identity,
			SessionFactoryObserver[] builtInObservers) {
		return create( settings, identity, builtInObservers, new SessionFactoryObserver[0] );
	}

	public static SessionFactoryOptions create(
			ResolvedSessionFactorySettings settings,
			SessionFactoryConstructionIdentity identity,
			SessionFactoryObserver[] builtInObservers,
			SessionFactoryObserver[] additionalObservers) {
		Objects.requireNonNull( settings );
		Objects.requireNonNull( identity );
		Objects.requireNonNull( builtInObservers );
		Objects.requireNonNull( additionalObservers );
		return (SessionFactoryOptions) Proxy.newProxyInstance(
				SessionFactoryOptions.class.getClassLoader(),
				new Class<?>[] { SessionFactoryOptions.class },
				new Handler( settings, identity, builtInObservers.clone(), additionalObservers.clone() )
		);
	}

	private record Handler(
			ResolvedSessionFactorySettings settings,
			SessionFactoryConstructionIdentity identity,
			SessionFactoryObserver[] builtInObservers,
			SessionFactoryObserver[] additionalObservers) implements InvocationHandler {
		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				switch ( method.getName() ) {
					case "toString" -> {
						return toString();
					}
					case "hashCode" -> {
						return System.identityHashCode( proxy );
					}
					case "equals" -> {
						return proxy == args[0];
					}
			}
			return switch ( method.getName() ) {
				case "getUuid" -> identity.uuid();
				case "getServiceRegistry" -> settings.serviceRegistry();
				case "isJpaBootstrap" -> settings.jpaBootstrap();
				case "getSessionFactoryName" -> settings.sessionFactoryName();
				case "isSessionFactoryNameAlsoJndiName" -> settings.sessionFactoryNameAlsoJndiName();
				case "isStatisticsEnabled" -> false;
				case "getStatementObserver" -> settings.statementObserver();
				case "getStatementInspector" -> settings.statementInspector();
				case "getInitialSessionCacheMode" -> settings.initialSessionCacheMode();
				case "getInitialSessionFlushMode" -> org.hibernate.FlushMode.AUTO;
				case "getDefaultLockOptions" -> org.hibernate.LockOptions.NONE;
				case "getDefaultSessionProperties" -> java.util.Collections.emptyMap();
				case "getPhysicalConnectionHandlingMode" -> settings.physicalConnectionHandlingMode();
				case "getJdbcTimeZone" -> settings.jdbcTimeZone();
				case "isFlushBeforeCompletionEnabled" -> settings.flushBeforeCompletionEnabled();
				case "isAutoCloseSessionEnabled" -> settings.autoCloseSessionEnabled();
				case "isJtaTransactionAccessEnabled" -> true;
				case "isPreferUserTransaction" -> false;
				case "isAllowOutOfTransactionUpdateOperations" -> false;
				case "isJtaTrackByThread" -> true;
				case "isIdentifierRollbackEnabled" -> settings.identifierRollbackEnabled();
				case "isBidirectionalAssociationManagementEnabled" -> settings.bidirectionalAssociationManagementEnabled();
				case "getInterceptor" -> settings.interceptor();
				case "getStatelessInterceptorImplementorSupplier" -> null;
				case "buildSessionEventListeners" -> new org.hibernate.SessionEventListener[0];
				case "getSessionFactoryObservers" -> sessionFactoryObservers();
				case "getValidatorFactoryReference" -> settings.validatorFactoryReference();
				case "getCustomEntityDirtinessStrategy" -> org.hibernate.boot.internal.DefaultCustomEntityDirtinessStrategy.INSTANCE;
				case "getEntityNameResolvers" -> new org.hibernate.EntityNameResolver[0];
				case "getEntityNotFoundDelegate" -> null;
				case "isCheckNullability" -> true;
				case "setCheckNullability" -> null;
				case "isSecondLevelCacheEnabled" -> settings.secondLevelCacheEnabled();
				case "isQueryCacheEnabled" -> settings.queryCacheEnabled();
				case "getQueryCacheLayout" -> settings.queryCacheLayout();
				case "getTimestampsCacheFactory" -> settings.timestampsCacheFactory();
				case "getCacheRegionPrefix" -> settings.cacheRegionPrefix();
				case "isMinimalPutsEnabled" -> settings.minimalPutsEnabled();
				case "isStructuredCacheEntriesEnabled" -> settings.structuredCacheEntriesEnabled();
				case "isDirectReferenceCacheEntriesEnabled" -> settings.directReferenceCacheEntriesEnabled();
				case "isAutoEvictCollectionCache" -> settings.autoEvictCollectionCache();
				case "getCustomSqlFunctionMap" -> settings.customSqlFunctionMap();
				case "getCustomSqmFunctionRegistry" -> settings.customSqmFunctionRegistry();
				case "getCustomHqlTranslator" -> settings.customHqlTranslator();
				case "getCustomSqmTranslatorFactory" -> settings.customSqmTranslatorFactory();
				case "getCustomSqmMultiTableMutationStrategy" -> settings.customSqmMultiTableMutationStrategy();
				case "getCustomSqmMultiTableInsertStrategy" -> settings.customSqmMultiTableInsertStrategy();
				case "resolveCustomSqmMultiTableMutationStrategy" -> null;
				case "resolveCustomSqmMultiTableInsertStrategy" -> null;
				case "getJpaCompliance" -> settings.jpaCompliance();
				case "getCriteriaValueHandlingMode" -> settings.criteriaValueHandlingMode();
				case "getImmutableEntityUpdateQueryHandlingMode" -> settings.immutableEntityUpdateQueryHandlingMode();
				case "allowImmutableEntityUpdate" -> settings.immutableEntityUpdateQueryHandlingMode()
						!= org.hibernate.query.spi.ImmutableEntityUpdateQueryHandlingMode.EXCEPTION;
				case "isJsonFunctionsEnabled" -> settings.jsonFunctionsEnabled();
				case "isXmlFunctionsEnabled" -> settings.xmlFunctionsEnabled();
				case "isPortableIntegerDivisionEnabled" -> settings.portableIntegerDivisionEnabled();
				case "getNativeJdbcParametersIgnored" -> settings.nativeJdbcParametersIgnored();
				case "isNamedQueryStartupCheckingEnabled" -> settings.namedQueryStartupCheckingEnabled();
				case "getQueryStatisticsMaxSize" -> 100;
				case "isCollectionsInDefaultFetchGroupEnabled" -> settings.collectionsInDefaultFetchGroupEnabled();
				case "areJPACallbacksEnabled" -> settings.jpaCallbacksEnabled();
				case "getDefaultBatchFetchSize" -> settings.defaultBatchFetchSize();
				case "getMaximumFetchDepth" -> settings.maximumFetchDepth();
				case "isSubselectFetchEnabled" -> settings.subselectFetchEnabled();
				case "getJdbcBatchSize" -> 0;
				case "getJdbcFetchSize" -> null;
				case "isScrollableResultSetsEnabled" -> true;
				case "isGetGeneratedKeysEnabled" -> true;
				case "isOrderUpdatesEnabled" -> false;
				case "isOrderInsertsEnabled" -> false;
				case "isCommentsEnabled" -> settings.commentsEnabled();
				case "doesConnectionProviderDisableAutoCommit" -> false;
				case "getTemporalTableStrategy" -> settings.temporalTableStrategy();
				case "getAuditStrategy" -> settings.auditStrategy();
				case "isMultiTenancyEnabled" -> settings.multiTenancyEnabled();
				case "getCurrentTenantIdentifierResolver" -> settings.currentTenantIdentifierResolver();
				case "getDefaultTenantIdentifierJavaType" -> settings.defaultTenantIdentifierJavaType();
				case "getDefaultCatalog" -> settings.defaultCatalog();
				case "getDefaultSchema" -> settings.defaultSchema();
					default -> {
						if ( method.isDefault() ) {
							yield InvocationHandler.invokeDefault( proxy, method, args );
						}
						throw new UnsupportedOperationException(
								"SessionFactoryOptions method not resolved by the minimal adapter: "
										+ method.getName()
						);
					}
				};
		}

		@Override
		public String toString() {
			return "SessionFactoryOptionsAdapter[" + identity.uuid() + "]";
		}

		private SessionFactoryObserver[] sessionFactoryObservers() {
			final var configuredObservers = settings.sessionFactoryObservers();
			if ( configuredObservers.length == 0 && additionalObservers.length == 0 ) {
				return builtInObservers.clone();
			}
			if ( builtInObservers.length == 0 ) {
				return concat( configuredObservers, additionalObservers );
			}
			return concat( concat( builtInObservers, configuredObservers ), additionalObservers );
		}

		private static SessionFactoryObserver[] concat(
				SessionFactoryObserver[] first,
				SessionFactoryObserver[] second) {
			if ( first.length == 0 ) {
				return second.clone();
			}
			if ( second.length == 0 ) {
				return first.clone();
			}
			final var observers = Arrays.copyOf(
					first,
					first.length + second.length
			);
			System.arraycopy(
					second,
					0,
					observers,
					first.length,
					second.length
			);
			return observers;
		}
	}
}
