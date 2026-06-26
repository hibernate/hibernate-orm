/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.spi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;

import org.hibernate.CacheMode;
import org.hibernate.GraphParserMode;
import org.hibernate.Interceptor;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.StatementObserver;
import org.hibernate.annotations.CacheLayout;
import org.hibernate.audit.AuditStrategy;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.cache.spi.TimestampsCacheFactory;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.context.spi.TenantCredentialsMapper;
import org.hibernate.context.spi.TenantSchemaMapper;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.spi.JpaCompliance;
import org.hibernate.query.criteria.ValueHandlingMode;
import org.hibernate.query.hql.spi.HqlTranslator;
import org.hibernate.query.spi.ImmutableEntityUpdateQueryHandlingMode;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.query.sqm.sql.spi.SqmTranslatorFactory;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.temporal.TemporalTableStrategy;
import org.hibernate.type.TimeZoneStorageStrategy;
import org.hibernate.type.descriptor.java.JavaType;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;

/// Resolved settings used while building the runtime SessionFactory.
/// This is intentionally focused on values needed by the next SessionFactory
/// construction slice.  It is not meant to mirror ORM's large
/// `SessionFactoryOptions` surface; additional named values should be added
/// only when a concrete build or runtime consumer requires them.
///
/// @see SessionFactoryImplementor#getSessionFactoryOptions()
///
/// @since 9.0
/// @author Steve Ebersole
public record ResolvedSessionFactorySettings(
		/// The normalized raw configuration values available to runtime factory
		/// option resolution.
		Map<String, Object> configurationValues,

		/// Whether this factory build originated from a Jakarta Persistence entry
		/// point.
		boolean jpaBootstrap,

		/// The standard service registry used for factory-service construction.
		StandardServiceRegistry serviceRegistry,

		/// Explicit SessionFactory name, if one was configured.
		String sessionFactoryName,

		/// Explicit JNDI name for binding the SessionFactory, if one was configured.
		String sessionFactoryJndiName,

		/// Whether the SessionFactory name should also be treated as a JNDI name.
		Boolean sessionFactoryNameAlsoJndiName,

		/// Statement observer applied to SQL statements emitted by this factory.
		StatementObserver statementObserver,

		/// SQL statement inspector applied before JDBC execution.
		StatementInspector statementInspector,

		/// Initial cache mode for newly opened sessions.
		CacheMode initialSessionCacheMode,

		/// Default JPA cache retrieve mode for newly opened sessions and find operations.
		CacheRetrieveMode defaultCacheRetrieveMode,

		/// Default JPA cache store mode for newly opened sessions and find operations.
		CacheStoreMode defaultCacheStoreMode,

		/// Parser mode used for Hibernate entity graph text parsing.
		GraphParserMode graphParserMode,

		/// Physical JDBC connection handling mode for sessions.
		PhysicalConnectionHandlingMode physicalConnectionHandlingMode,

		/// JDBC time zone override, if configured.
		TimeZone jdbcTimeZone,

		/// Whether sessions should flush before transaction completion.
		boolean flushBeforeCompletionEnabled,

		/// Whether sessions should auto-close after transaction completion.
		boolean autoCloseSessionEnabled,

		/// Whether identifiers should be reset on delete.
		boolean identifierRollbackEnabled,

		/// Whether lazy state may be initialized outside an active transaction.
		boolean initializeLazyStateOutsideTransactionsEnabled,

		/// Whether unowned associations are checked for transient references during cascading.
		boolean unownedAssociationTransientCheck,

		/// Whether flush-time bidirectional association management is enabled.
		boolean bidirectionalAssociationManagementEnabled,

		/// Whether runtime statistics collection is enabled.
		boolean statisticsEnabled,

		/// Factory-wide interceptor applied to Sessions unless overridden.
		Interceptor interceptor,

		/// Observers notified about SessionFactory lifecycle events.
		SessionFactoryObserver[] sessionFactoryObservers,

		/// Bean Validation ValidatorFactory reference supplied through configuration,
		/// if one is available.
		Object validatorFactoryReference,

		/// Whether second-level caching is enabled for this factory.
		boolean secondLevelCacheEnabled,

		/// Whether query result caching is enabled for this factory.
		boolean queryCacheEnabled,

		/// Query cache entry layout.
		CacheLayout queryCacheLayout,

		/// Factory used to build the timestamps cache.
		TimestampsCacheFactory timestampsCacheFactory,

		/// Prefix applied to cache region names.
		String cacheRegionPrefix,

		/// Whether cache puts should avoid overwriting equivalent entries.
		boolean minimalPutsEnabled,

		/// Whether cache entries use the structured cache-entry format.
		boolean structuredCacheEntriesEnabled,

		/// Whether direct-reference cache entries are enabled.
		boolean directReferenceCacheEntriesEnabled,

		/// Whether collection caches are auto-evicted for bidirectional association changes.
		boolean autoEvictCollectionCache,

		/// Custom SQL-function registrations applied before contributor and dialect functions.
		Map<String, SqmFunctionDescriptor> customSqlFunctionMap,

		/// Custom SQM function registry overlay, if configured.
		SqmFunctionRegistry customSqmFunctionRegistry,

		/// Custom HQL translator, if configured.
		HqlTranslator customHqlTranslator,

		/// Custom SQM translator factory, if configured.
		SqmTranslatorFactory customSqmTranslatorFactory,

		/// Custom multi-table mutation strategy, if configured.
		SqmMultiTableMutationStrategy customSqmMultiTableMutationStrategy,

		/// Custom multi-table insert strategy, if configured.
		SqmMultiTableInsertStrategy customSqmMultiTableInsertStrategy,

		/// JPA compliance settings visible to query/runtime model creation.
		JpaCompliance jpaCompliance,

		/// Criteria literal/bind handling mode.
		ValueHandlingMode criteriaValueHandlingMode,

		/// Handling mode for bulk updates against immutable entities.
		ImmutableEntityUpdateQueryHandlingMode immutableEntityUpdateQueryHandlingMode,

		/// Preferred JDBC type code for boolean values in runtime query/type resolution.
		int preferredSqlTypeCodeForBoolean,

		/// Preferred JDBC type code for duration values in runtime query/type resolution.
		int preferredSqlTypeCodeForDuration,

		/// Default storage strategy for time-zone-aware temporal values.
		TimeZoneStorageStrategy defaultTimeZoneStorageStrategy,

		/// Whether stored procedure parameter names should be passed through JDBC calls.
		boolean passProcedureParameterNames,

		/// Whether Java time values should prefer direct JDBC 4.2 mappings.
		boolean preferJavaTimeJdbcTypesEnabled,

		/// Whether native query result discovery should prefer JDBC datetime types.
		boolean preferJdbcDatetimeTypesInNativeQueriesEnabled,

		/// Whether HQL JSON functions are enabled.
		boolean jsonFunctionsEnabled,

		/// Whether HQL XML functions are enabled.
		boolean xmlFunctionsEnabled,

		/// Whether integer division should be portable across dialects.
		boolean portableIntegerDivisionEnabled,

		/// Whether native-query JDBC ordinal parameters should be ignored.
		boolean nativeJdbcParametersIgnored,

		/// Whether HQL and Criteria safe-mode validation is enabled.
		boolean safeModeEnabled,

		/// Whether named queries should be validated during SessionFactory startup.
		boolean namedQueryStartupCheckingEnabled,

		/// Whether enhanced collection attributes are included in the default fetch
		/// group.
		boolean collectionsInDefaultFetchGroupEnabled,

		/// Whether Jakarta Persistence entity callbacks are enabled.
		boolean jpaCallbacksEnabled,

		/// Default batch-fetch size for entity and collection persisters.
		int defaultBatchFetchSize,

		/// Maximum outer-join fetch depth, if configured.
		Integer maximumFetchDepth,

		/// Whether subselect fetching is enabled by default.
		boolean subselectFetchEnabled,

		/// Whether generated SQL should include comments.
		boolean commentsEnabled,

		/// Strategy used for temporal table mappings.
		TemporalTableStrategy temporalTableStrategy,

		/// Strategy used for audited mappings.
		AuditStrategy auditStrategy,

		/// Whether a multi-tenant connection provider is available.
		boolean multiTenancyEnabled,

			/// Current tenant identifier resolver, if one is configured.
			CurrentTenantIdentifierResolver<Object> currentTenantIdentifierResolver,

			/// Maps tenant identifiers to database schemas for schema-based tenancy.
			TenantSchemaMapper<Object> tenantSchemaMapper,

			/// Maps tenant identifiers to database credentials for credentials-based tenancy.
			TenantCredentialsMapper<Object> tenantCredentialsMapper,

			/// Fallback Java type for tenant identifiers when no tenant-id mapping is present.
			JavaType<Object> defaultTenantIdentifierJavaType,

		/// Default catalog used when generating SQL for unqualified mapping names.
		String defaultCatalog,

		/// Default schema used when generating SQL for unqualified mapping names.
		String defaultSchema) {

	/// Exposes immutable snapshots.
	public ResolvedSessionFactorySettings {
		configurationValues = Collections.unmodifiableMap( new LinkedHashMap<>(
				Objects.requireNonNull( configurationValues )
			) );
			Objects.requireNonNull( serviceRegistry );
			customSqlFunctionMap = Collections.unmodifiableMap( new LinkedHashMap<>(
					Objects.requireNonNull( customSqlFunctionMap )
			) );
			sessionFactoryObservers = sessionFactoryObservers == null
					? new SessionFactoryObserver[0]
					: sessionFactoryObservers.clone();
			Objects.requireNonNull( queryCacheLayout );
			Objects.requireNonNull( jpaCompliance );
			Objects.requireNonNull( criteriaValueHandlingMode );
			Objects.requireNonNull( immutableEntityUpdateQueryHandlingMode );
			Objects.requireNonNull( defaultTimeZoneStorageStrategy );
			Objects.requireNonNull( defaultCacheRetrieveMode );
			Objects.requireNonNull( defaultCacheStoreMode );
			Objects.requireNonNull( graphParserMode );
			Objects.requireNonNull( defaultTenantIdentifierJavaType );
		}

	@Override
	public SessionFactoryObserver[] sessionFactoryObservers() {
		return sessionFactoryObservers.clone();
	}

	public ResolvedSessionFactorySettings withSessionFactoryObservers(SessionFactoryObserver[] sessionFactoryObservers) {
		return new ResolvedSessionFactorySettings(
				configurationValues,
				jpaBootstrap,
				serviceRegistry,
				sessionFactoryName,
				sessionFactoryJndiName,
				sessionFactoryNameAlsoJndiName,
				statementObserver,
				statementInspector,
				initialSessionCacheMode,
				defaultCacheRetrieveMode,
				defaultCacheStoreMode,
				graphParserMode,
				physicalConnectionHandlingMode,
				jdbcTimeZone,
				flushBeforeCompletionEnabled,
				autoCloseSessionEnabled,
				identifierRollbackEnabled,
				initializeLazyStateOutsideTransactionsEnabled,
				unownedAssociationTransientCheck,
				bidirectionalAssociationManagementEnabled,
				statisticsEnabled,
				interceptor,
				sessionFactoryObservers,
				validatorFactoryReference,
				secondLevelCacheEnabled,
				queryCacheEnabled,
				queryCacheLayout,
				timestampsCacheFactory,
				cacheRegionPrefix,
				minimalPutsEnabled,
				structuredCacheEntriesEnabled,
				directReferenceCacheEntriesEnabled,
				autoEvictCollectionCache,
				customSqlFunctionMap,
				customSqmFunctionRegistry,
				customHqlTranslator,
				customSqmTranslatorFactory,
				customSqmMultiTableMutationStrategy,
				customSqmMultiTableInsertStrategy,
				jpaCompliance,
				criteriaValueHandlingMode,
				immutableEntityUpdateQueryHandlingMode,
				preferredSqlTypeCodeForBoolean,
				preferredSqlTypeCodeForDuration,
				defaultTimeZoneStorageStrategy,
				passProcedureParameterNames,
				preferJavaTimeJdbcTypesEnabled,
				preferJdbcDatetimeTypesInNativeQueriesEnabled,
				jsonFunctionsEnabled,
				xmlFunctionsEnabled,
				portableIntegerDivisionEnabled,
				nativeJdbcParametersIgnored,
				safeModeEnabled,
				namedQueryStartupCheckingEnabled,
				collectionsInDefaultFetchGroupEnabled,
				jpaCallbacksEnabled,
				defaultBatchFetchSize,
				maximumFetchDepth,
				subselectFetchEnabled,
				commentsEnabled,
				temporalTableStrategy,
				auditStrategy,
					multiTenancyEnabled,
					currentTenantIdentifierResolver,
					tenantSchemaMapper,
					tenantCredentialsMapper,
					defaultTenantIdentifierJavaType,
					defaultCatalog,
				defaultSchema
		);
	}
}
