/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.internal.settings;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;

import org.hibernate.CacheMode;
import org.hibernate.Interceptor;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.StatementObserver;
import org.hibernate.annotations.CacheLayout;
import org.hibernate.audit.AuditStrategy;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.boot.model.internal.TemporalHelper;
import org.hibernate.boot.pipeline.spi.ResolvedSessionFactorySettings;
import org.hibernate.cfg.BytecodeSettings;
import org.hibernate.cache.internal.NoCachingRegionFactory;
import org.hibernate.cache.internal.StandardTimestampsCacheFactory;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.TimestampsCacheFactory;
import org.hibernate.cfg.CacheSettings;
import org.hibernate.cfg.FetchSettings;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.cfg.PersistenceSettings;
import org.hibernate.cfg.QuerySettings;
import org.hibernate.cfg.SessionEventSettings;
import org.hibernate.cfg.TransactionSettings;
import org.hibernate.cfg.ValidationSettings;
import org.hibernate.context.spi.MultiTenancy;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.jpa.internal.MutableJpaComplianceImpl;
import org.hibernate.jpa.spi.JpaCompliance;
import org.hibernate.query.criteria.ValueHandlingMode;
import org.hibernate.query.hql.HqlTranslator;
import org.hibernate.query.spi.ImmutableEntityUpdateQueryHandlingMode;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.query.sqm.sql.SqmTranslatorFactory;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.temporal.TemporalTableStrategy;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.type.descriptor.java.ObjectJavaType;

import static org.hibernate.boot.model.internal.AuditHelper.determineAuditStrategy;

/// Projects resolved bootstrap settings into settings needed for SessionFactory
/// construction.
/// The resolver keeps SessionFactory-specific decisions separate from metadata
/// resolution.  As the prototype grows, this class should own the translation
/// from the single resolved bootstrap settings root to a narrower runtime factory
/// settings contract.
///
/// @since 9.0
/// @author Steve Ebersole
public class SessionFactorySettingsResolver {
	/// Resolve SessionFactory settings from the shared bootstrap settings root and
	/// service registry.
	///
	/// @param bootstrapSettings The resolved bootstrap settings
	/// @param serviceRegistry The service registry used for factory construction
	///
	/// @return The resolved SessionFactory settings
	public static ResolvedSessionFactorySettings resolve(
			ResolvedBootstrapSettings bootstrapSettings,
			ServiceRegistry serviceRegistry) {
		Objects.requireNonNull( bootstrapSettings );
		Objects.requireNonNull( serviceRegistry );

		if ( !(serviceRegistry instanceof StandardServiceRegistry standardServiceRegistry) ) {
			throw new IllegalArgumentException(
					"SessionFactory settings resolution requires a StandardServiceRegistry"
			);
		}

		final var configurationValues = bootstrapSettings.configurationValues();
		final var cacheSettings = resolveCacheSettings( configurationValues, standardServiceRegistry );
		return new ResolvedSessionFactorySettings(
				configurationValues,
				bootstrapSettings.jpaBootstrap(),
				standardServiceRegistry,
				asString( configurationValues.get( PersistenceSettings.SESSION_FACTORY_NAME ) ),
				asString( configurationValues.get( PersistenceSettings.SESSION_FACTORY_JNDI_NAME ) ),
				asBoolean( configurationValues.get( PersistenceSettings.SESSION_FACTORY_NAME_IS_JNDI ), true ),
				resolveStatementObserver( configurationValues ),
				resolveStatementInspector( configurationValues, standardServiceRegistry ),
				CacheMode.NORMAL,
				resolvePhysicalConnectionHandlingMode( configurationValues, standardServiceRegistry ),
				resolveJdbcTimeZone( configurationValues ),
				asBoolean( configurationValues.get( TransactionSettings.FLUSH_BEFORE_COMPLETION ), true ),
				asBoolean( configurationValues.get( TransactionSettings.AUTO_CLOSE_SESSION ), false ),
				asBoolean( configurationValues.get( org.hibernate.cfg.AvailableSettings.USE_IDENTIFIER_ROLLBACK ), false ),
				asBoolean( configurationValues.get( TransactionSettings.ENABLE_LAZY_LOAD_NO_TRANS ), false ),
				resolveBidirectionalAssociationManagementEnabled( configurationValues ),
				resolveInterceptor( configurationValues, standardServiceRegistry ),
				resolveSessionFactoryObservers( configurationValues, standardServiceRegistry ),
				resolveValidatorFactoryReference( configurationValues ),
				cacheSettings.secondLevelCacheEnabled(),
				cacheSettings.queryCacheEnabled(),
				cacheSettings.queryCacheLayout(),
				cacheSettings.timestampsCacheFactory(),
				cacheSettings.cacheRegionPrefix(),
					cacheSettings.minimalPutsEnabled(),
					cacheSettings.structuredCacheEntriesEnabled(),
					cacheSettings.directReferenceCacheEntriesEnabled(),
					cacheSettings.autoEvictCollectionCache(),
					Collections.emptyMap(),
					null,
					resolveHqlTranslator( configurationValues, standardServiceRegistry ),
					resolveSqmTranslatorFactory( configurationValues, standardServiceRegistry ),
					resolveSqmMultiTableMutationStrategy( configurationValues, standardServiceRegistry ),
					resolveSqmMultiTableInsertStrategy( configurationValues, standardServiceRegistry ),
					resolveJpaCompliance( bootstrapSettings ),
					ValueHandlingMode.interpret( configurationValues.get( QuerySettings.CRITERIA_VALUE_HANDLING_MODE ) ),
					ImmutableEntityUpdateQueryHandlingMode.interpret(
							configurationValues.get( QuerySettings.IMMUTABLE_ENTITY_UPDATE_QUERY_HANDLING_MODE )
					),
					asBoolean( configurationValues.get( QuerySettings.JSON_FUNCTIONS_ENABLED ), false ),
					asBoolean( configurationValues.get( QuerySettings.XML_FUNCTIONS_ENABLED ), false ),
					asBoolean( configurationValues.get( QuerySettings.PORTABLE_INTEGER_DIVISION ), false ),
					asBoolean( configurationValues.get( QuerySettings.NATIVE_IGNORE_JDBC_PARAMETERS ), false ),
					asBoolean( configurationValues.get( QuerySettings.QUERY_STARTUP_CHECKING ), true ),
					true,
					asBoolean( configurationValues.get( PersistenceSettings.JPA_CALLBACKS_ENABLED ), true ),
					asInteger( configurationValues.get( FetchSettings.DEFAULT_BATCH_FETCH_SIZE ), -1 ),
					asInteger( configurationValues.get( FetchSettings.MAX_FETCH_DEPTH ) ),
					asBoolean( configurationValues.get( FetchSettings.USE_SUBSELECT_FETCH ), false ),
					asBoolean( configurationValues.get( JdbcSettings.USE_SQL_COMMENTS ), false ),
					resolveTemporalTableStrategy( configurationValues, standardServiceRegistry ),
					resolveAuditStrategy( configurationValues ),
					MultiTenancy.isMultiTenancyEnabled( serviceRegistry ),
					MultiTenancy.getTenantIdentifierResolver( configurationValues, standardServiceRegistry ),
					ObjectJavaType.INSTANCE,
				asString( configurationValues.get( MappingSettings.DEFAULT_CATALOG ) ),
				asString( configurationValues.get( MappingSettings.DEFAULT_SCHEMA ) )
		);
	}

	private static boolean resolveBidirectionalAssociationManagementEnabled(Map<String, Object> configurationValues) {
		final Object bidirectionalAssociationManagement =
				configurationValues.get( PersistenceSettings.BIDIRECTIONALITY_MANAGEMENT );
		return bidirectionalAssociationManagement == null
				? asBoolean( configurationValues.get( BytecodeSettings.ENHANCER_ENABLE_ASSOCIATION_MANAGEMENT ), false )
				: asBoolean( bidirectionalAssociationManagement, false );
	}

	private static Interceptor resolveInterceptor(
			Map<String, Object> configurationValues,
			StandardServiceRegistry serviceRegistry) {
		return serviceRegistry.requireService( StrategySelector.class )
				.resolveStrategy( Interceptor.class, configurationValues.get( SessionEventSettings.INTERCEPTOR ) );
	}

	private static StatementInspector resolveStatementInspector(
			Map<String, Object> configurationValues,
			StandardServiceRegistry serviceRegistry) {
		return serviceRegistry.requireService( StrategySelector.class )
				.resolveStrategy( StatementInspector.class, configurationValues.get( JdbcSettings.STATEMENT_INSPECTOR ) );
	}

	private static PhysicalConnectionHandlingMode resolvePhysicalConnectionHandlingMode(
			Map<String, Object> configurationValues,
			StandardServiceRegistry serviceRegistry) {
		final var specifiedHandlingMode = PhysicalConnectionHandlingMode.interpret(
				configurationValues.get( JdbcSettings.CONNECTION_HANDLING )
		);
		return specifiedHandlingMode != null
				? specifiedHandlingMode
				: serviceRegistry.requireService( TransactionCoordinatorBuilder.class )
						.getDefaultConnectionHandlingMode();
	}

	private static TimeZone resolveJdbcTimeZone(Map<String, Object> configurationValues) {
		final Object setting = configurationValues.get( JdbcSettings.JDBC_TIME_ZONE );
		if ( setting == null ) {
			return null;
		}
		if ( setting instanceof TimeZone timeZone ) {
			return timeZone;
		}
		if ( setting instanceof java.time.ZoneId zoneId ) {
			return TimeZone.getTimeZone( zoneId );
		}
		return TimeZone.getTimeZone( setting.toString() );
	}

	private static ResolvedCacheSettings resolveCacheSettings(
			Map<String, Object> configurationValues,
			StandardServiceRegistry serviceRegistry) {
		final var regionFactory = serviceRegistry.getService( RegionFactory.class );
		if ( regionFactory instanceof NoCachingRegionFactory ) {
			return new ResolvedCacheSettings(
					false,
					false,
					CacheLayout.AUTO,
					null,
					null,
					false,
					false,
					false,
					false
			);
		}

			final var strategySelector = serviceRegistry.requireService( StrategySelector.class );
			return new ResolvedCacheSettings(
				asBoolean( configurationValues.get( CacheSettings.USE_SECOND_LEVEL_CACHE ), true ),
				asBoolean( configurationValues.get( CacheSettings.USE_QUERY_CACHE ), false ),
				resolveCacheLayout( configurationValues.get( CacheSettings.QUERY_CACHE_LAYOUT ) ),
				strategySelector.resolveDefaultableStrategy(
						TimestampsCacheFactory.class,
						configurationValues.get( CacheSettings.QUERY_CACHE_FACTORY ),
						StandardTimestampsCacheFactory.INSTANCE
				),
				asString( configurationValues.get( CacheSettings.CACHE_REGION_PREFIX ) ),
				asBoolean(
						configurationValues.get( CacheSettings.USE_MINIMAL_PUTS ),
						regionFactory == null || regionFactory.isMinimalPutsEnabledByDefault()
				),
				asBoolean( configurationValues.get( CacheSettings.USE_STRUCTURED_CACHE ), false ),
				asBoolean( configurationValues.get( CacheSettings.USE_DIRECT_REFERENCE_CACHE_ENTRIES ), false ),
					asBoolean( configurationValues.get( CacheSettings.AUTO_EVICT_COLLECTION_CACHE ), false )
			);
		}

		private static HqlTranslator resolveHqlTranslator(
				Map<String, Object> configurationValues,
				StandardServiceRegistry serviceRegistry) {
			return serviceRegistry.requireService( StrategySelector.class )
					.resolveStrategy( HqlTranslator.class, configurationValues.get( QuerySettings.SEMANTIC_QUERY_PRODUCER ) );
		}

		private static SqmTranslatorFactory resolveSqmTranslatorFactory(
				Map<String, Object> configurationValues,
				StandardServiceRegistry serviceRegistry) {
			return serviceRegistry.requireService( StrategySelector.class )
					.resolveStrategy( SqmTranslatorFactory.class, configurationValues.get( QuerySettings.SEMANTIC_QUERY_TRANSLATOR ) );
		}

		private static SqmMultiTableMutationStrategy resolveSqmMultiTableMutationStrategy(
				Map<String, Object> configurationValues,
				StandardServiceRegistry serviceRegistry) {
			return serviceRegistry.requireService( StrategySelector.class )
					.resolveStrategy(
							SqmMultiTableMutationStrategy.class,
							configurationValues.get( QuerySettings.QUERY_MULTI_TABLE_MUTATION_STRATEGY )
					);
		}

		private static SqmMultiTableInsertStrategy resolveSqmMultiTableInsertStrategy(
				Map<String, Object> configurationValues,
				StandardServiceRegistry serviceRegistry) {
			return serviceRegistry.requireService( StrategySelector.class )
					.resolveStrategy(
							SqmMultiTableInsertStrategy.class,
							configurationValues.get( QuerySettings.QUERY_MULTI_TABLE_INSERT_STRATEGY )
					);
		}

	private static JpaCompliance resolveJpaCompliance(ResolvedBootstrapSettings bootstrapSettings) {
		return new MutableJpaComplianceImpl(
				bootstrapSettings.configurationValues(),
				bootstrapSettings.jpaBootstrap()
		);
	}

	private static TemporalTableStrategy resolveTemporalTableStrategy(
			Map<String, Object> configurationValues,
			StandardServiceRegistry serviceRegistry) {
		final var strategy = TemporalHelper.determineTemporalTableStrategy( configurationValues );
		return strategy == TemporalTableStrategy.AUTO
				? serviceRegistry.requireService( JdbcServices.class )
						.getDialect()
						.getTemporalTableSupport()
						.getDefaultTemporalTableStrategy()
				: strategy;
	}

	private static AuditStrategy resolveAuditStrategy(Map<String, Object> configurationValues) {
		return determineAuditStrategy( configurationValues );
	}

	private static CacheLayout resolveCacheLayout(Object value) {
		if ( value == null ) {
			return CacheLayout.FULL;
		}
		if ( value instanceof CacheLayout cacheLayout ) {
			return cacheLayout;
		}
		return CacheLayout.valueOf( value.toString().toUpperCase( java.util.Locale.ROOT ) );
	}

	private static Object resolveValidatorFactoryReference(Map<String, Object> configurationValues) {
		final Object jpaReference = configurationValues.get( ValidationSettings.JPA_VALIDATION_FACTORY );
		return jpaReference == null
				? configurationValues.get( ValidationSettings.JAKARTA_VALIDATION_FACTORY )
				: jpaReference;
	}

	private static StatementObserver resolveStatementObserver(Map<String, Object> configurationValues) {
		final Object setting = configurationValues.get( JdbcSettings.STATEMENT_OBSERVER );
		if ( setting == null ) {
			return null;
		}
		if ( setting instanceof StatementObserver statementObserver ) {
			return statementObserver;
		}
		if ( setting instanceof Class<?> javaType ) {
			return instantiate( javaType, StatementObserver.class );
		}
		try {
			return instantiate( Class.forName( setting.toString() ), StatementObserver.class );
		}
		catch (ClassNotFoundException e) {
			throw new IllegalArgumentException( "Unable to resolve StatementObserver - " + setting, e );
		}
	}

	private static SessionFactoryObserver[] resolveSessionFactoryObservers(
			Map<String, Object> configurationValues,
			StandardServiceRegistry serviceRegistry) {
		final Object setting = configurationValues.get( PersistenceSettings.SESSION_FACTORY_OBSERVER );
		if ( setting == null ) {
			return new SessionFactoryObserver[0];
		}

		final var observers = new ArrayList<SessionFactoryObserver>();
		addSessionFactoryObservers( setting, serviceRegistry, observers );
		return observers.toArray( SessionFactoryObserver[]::new );
	}

	private static void addSessionFactoryObservers(
			Object setting,
			StandardServiceRegistry serviceRegistry,
			ArrayList<SessionFactoryObserver> observers) {
		if ( setting instanceof SessionFactoryObserver observer ) {
			observers.add( observer );
		}
		else if ( setting instanceof SessionFactoryObserver[] observerArray ) {
			observers.addAll( java.util.List.of( observerArray ) );
		}
		else if ( setting instanceof Collection<?> collection ) {
			for ( Object item : collection ) {
				addSessionFactoryObservers( item, serviceRegistry, observers );
			}
		}
		else if ( setting instanceof Class<?> javaType ) {
			observers.add( instantiate( javaType, SessionFactoryObserver.class ) );
		}
		else {
			observers.add(
					serviceRegistry.requireService( StrategySelector.class )
							.resolveStrategy( SessionFactoryObserver.class, setting )
			);
		}
	}

	private static String asString(Object value) {
		return value == null ? null : value.toString();
	}

	private static Boolean asBoolean(Object value, boolean defaultValue) {
		if ( value == null ) {
			return defaultValue;
		}
		if ( value instanceof Boolean booleanValue ) {
			return booleanValue;
		}
		return Boolean.parseBoolean( value.toString() );
	}

	private static int asInteger(Object value, int defaultValue) {
		if ( value == null ) {
			return defaultValue;
		}
		if ( value instanceof Number number ) {
			return number.intValue();
		}
		return Integer.parseInt( value.toString() );
	}

	private static Integer asInteger(Object value) {
		if ( value == null ) {
			return null;
		}
		if ( value instanceof Number number ) {
			return number.intValue();
		}
		return Integer.valueOf( value.toString() );
	}

	private static <T> T instantiate(Class<?> javaType, Class<T> expectedType) {
		if ( !expectedType.isAssignableFrom( javaType ) ) {
			throw new IllegalArgumentException(
					javaType.getName() + " does not implement " + expectedType.getName()
			);
		}
		try {
			return expectedType.cast( javaType.getConstructor().newInstance() );
		}
		catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			throw new IllegalArgumentException( "Unable to instantiate " + javaType.getName(), e );
		}
	}

	private record ResolvedCacheSettings(
			boolean secondLevelCacheEnabled,
			boolean queryCacheEnabled,
			CacheLayout queryCacheLayout,
			TimestampsCacheFactory timestampsCacheFactory,
			String cacheRegionPrefix,
			boolean minimalPutsEnabled,
			boolean structuredCacheEntriesEnabled,
			boolean directReferenceCacheEntriesEnabled,
			boolean autoEvictCollectionCache) {
	}
}
