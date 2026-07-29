/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.StatementObserver;
import org.hibernate.binder.internal.TenantIdBinder;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.model.relational.internal.SqlStringGenerationContextImpl;
import org.hibernate.boot.pipeline.spi.ResolvedSessionFactorySettings;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.cfg.internal.DomainDataRegionConfigImpl;
import org.hibernate.cache.cfg.spi.DomainDataRegionConfig;
import org.hibernate.cache.internal.DisabledCaching;
import org.hibernate.cache.internal.EnabledCaching;
import org.hibernate.cache.internal.NoCachingRegionFactory;
import org.hibernate.cache.spi.CacheFactory;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.extension.internal.ExtensionIntegrationServiceImpl;
import org.hibernate.engine.query.internal.NativeQueryInterpreterStandardImpl;
import org.hibernate.engine.query.spi.NativeQueryInterpreter;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.internal.IgnoredStatementObserver;
import org.hibernate.mapping.RootClass;
import org.hibernate.stat.internal.StatisticsImpl;
import org.hibernate.stat.spi.StatisticsFactory;
import org.hibernate.type.descriptor.java.JavaType;

import static org.hibernate.cfg.StatisticsSettings.STATS_BUILDER;

/// Builds constructor-ready runtime components that do not require the
/// SessionFactory instance.
///
/// @since 9.0
/// @author Steve Ebersole
public final class SessionFactoryRuntimeComponentsBuilder {
	private SessionFactoryRuntimeComponentsBuilder() {
	}

	public static SessionFactoryRuntimeComponents build(
			MetadataImplementor metadata,
			ResolvedSessionFactorySettings settings,
			BootstrapContext bootstrapContext,
			JdbcServices jdbcServices) {
		final var filterDefinitions = new HashMap<>( metadata.getFilterDefinitions() );
		return new SessionFactoryRuntimeComponents(
				bootstrapContext.getTypeConfiguration(),
				bootstrapContext.getModelsContext(),
				bootstrapContext.getClassLoaderService(),
				bootstrapContext.getClassLoaderAccess(),
				bootstrapContext.getManagedBeanRegistry(),
				bootstrapContext.getRepresentationStrategySelector(),
				nativeQueryInterpreter(
						bootstrapContext.getClassLoaderService(),
						settings.nativeJdbcParametersIgnored()
				),
				ExtensionIntegrationServiceImpl.create(
						Set.of(),
						bootstrapContext.getClassLoaderService()
				),
				cacheFactory( bootstrapContext.getClassLoaderService() ),
				cacheRegionConfigs( metadata ),
				statisticsFactory(
						settings.configurationValues().get( STATS_BUILDER ),
						bootstrapContext.getClassLoaderService()
				),
				sqlStringGenerationContext(
						metadata,
						jdbcServices,
						settings.defaultCatalog(),
						settings.defaultSchema()
				),
				statementObserver( settings.statementObserver() ),
				settings.sessionFactoryObservers(),
				filterDefinitions,
				autoEnabledFilters( filterDefinitions ),
				tenantIdentifierType( filterDefinitions, settings.defaultTenantIdentifierJavaType() )
		);
	}

	public static SessionFactoryRuntimeComponents build(
			MetadataImplementor metadata,
			SessionFactoryOptions options,
			BootstrapContext bootstrapContext,
			JdbcServices jdbcServices) {
		final var filterDefinitions = new HashMap<>( metadata.getFilterDefinitions() );
		return new SessionFactoryRuntimeComponents(
				bootstrapContext.getTypeConfiguration(),
				bootstrapContext.getModelsContext(),
				bootstrapContext.getClassLoaderService(),
				bootstrapContext.getClassLoaderAccess(),
				bootstrapContext.getManagedBeanRegistry(),
				bootstrapContext.getRepresentationStrategySelector(),
				nativeQueryInterpreter(
						bootstrapContext.getClassLoaderService(),
						options.getNativeJdbcParametersIgnored()
				),
				ExtensionIntegrationServiceImpl.create(
						Set.of(),
						bootstrapContext.getClassLoaderService()
				),
				cacheFactory( bootstrapContext.getClassLoaderService() ),
				cacheRegionConfigs( metadata ),
				statisticsFactory(
						options.getServiceRegistry()
								.requireService( ConfigurationService.class )
								.getSettings()
								.get( STATS_BUILDER ),
						bootstrapContext.getClassLoaderService()
				),
				sqlStringGenerationContext(
						metadata,
						jdbcServices,
						options.getDefaultCatalog(),
						options.getDefaultSchema()
				),
				statementObserver( options.getStatementObserver() ),
				options.getSessionFactoryObservers(),
				filterDefinitions,
				autoEnabledFilters( filterDefinitions ),
				tenantIdentifierType( filterDefinitions, options.getDefaultTenantIdentifierJavaType() )
		);
	}

	private static SqlStringGenerationContext sqlStringGenerationContext(
			MetadataImplementor metadata,
			JdbcServices jdbcServices,
			String defaultCatalog,
			String defaultSchema) {
		return SqlStringGenerationContextImpl.fromExplicit(
				jdbcServices.getJdbcEnvironment(),
				metadata.getDatabase(),
				defaultCatalog,
				defaultSchema
		);
	}

	private static NativeQueryInterpreter nativeQueryInterpreter(
			ClassLoaderService classLoaderService,
			boolean nativeJdbcParametersIgnored) {
		final var discovered = classLoaderService.loadJavaServices( NativeQueryInterpreter.class ).iterator();
		if ( !discovered.hasNext() ) {
			return new NativeQueryInterpreterStandardImpl( nativeJdbcParametersIgnored );
		}

		final var interpreter = discovered.next();
		if ( discovered.hasNext() ) {
			throw new HibernateException(
					"Multiple NativeQueryInterpreter service registrations found via ServiceLoader"
			);
		}
		return interpreter;
	}

	private static CacheFactory cacheFactory(ClassLoaderService classLoaderService) {
		final var discovered = classLoaderService.loadJavaServices( CacheFactory.class ).iterator();
		if ( discovered.hasNext() ) {
			final var cacheFactory = discovered.next();
			if ( discovered.hasNext() ) {
				throw new HibernateException(
						"Multiple CacheFactory service registrations found via ServiceLoader"
				);
			}
			return cacheFactory;
		}

		return sessionFactory -> {
			final var regionFactory = sessionFactory.getServiceRegistry().requireService( RegionFactory.class );
			return regionFactory instanceof NoCachingRegionFactory
					? new DisabledCaching( sessionFactory )
					: new EnabledCaching( sessionFactory );
		};
	}

	private static Set<DomainDataRegionConfig> cacheRegionConfigs(MetadataImplementor metadata) {
		final Map<String, DomainDataRegionConfigImpl.Builder> regionConfigBuilders = new HashMap<>();

		for ( var entity : metadata.getEntityBindings() ) {
			final var accessType = AccessType.fromExternalName( entity.getCacheConcurrencyStrategy() );
			if ( accessType != null ) {
				if ( entity.isCached() ) {
					regionConfigBuilders.computeIfAbsent(
							entity.getRootClass().getCacheRegionName(),
							DomainDataRegionConfigImpl.Builder::new
					).addEntityConfig( entity, accessType );
				}

				if ( entity instanceof RootClass rootClass
						&& entity.hasNaturalId()
						&& entity.getNaturalIdCacheRegionName() != null ) {
					regionConfigBuilders.computeIfAbsent(
							entity.getNaturalIdCacheRegionName(),
							DomainDataRegionConfigImpl.Builder::new
					).addNaturalIdConfig( rootClass, accessType );
				}
			}
		}

		for ( var collection : metadata.getCollectionBindings() ) {
			final var accessType = AccessType.fromExternalName( collection.getCacheConcurrencyStrategy() );
			if ( accessType != null ) {
				regionConfigBuilders.computeIfAbsent(
						collection.getCacheRegionName(),
						DomainDataRegionConfigImpl.Builder::new
				).addCollectionConfig( collection, accessType );
			}
		}

		if ( regionConfigBuilders.isEmpty() ) {
			return Set.of();
		}

		final var regionConfigs = new HashSet<DomainDataRegionConfig>();
		for ( var builder : regionConfigBuilders.values() ) {
			regionConfigs.add( builder.build() );
		}
		return regionConfigs;
	}

	private static StatisticsFactory statisticsFactory(
			Object configuredFactory,
			ClassLoaderService classLoaderService) {
		if ( configuredFactory instanceof StatisticsFactory statisticsFactory ) {
			return statisticsFactory;
		}
		if ( configuredFactory != null ) {
			try {
				final Class<?> factoryClass = configuredFactory instanceof Class<?> specifiedClass
						? specifiedClass
						: classLoaderService.classForName( configuredFactory.toString() );
				return (StatisticsFactory) factoryClass.getConstructor().newInstance();
			}
			catch (Exception e) {
				throw new HibernateException(
						"Unable to instantiate specified StatisticsFactory implementation [" + configuredFactory + "]",
						e
				);
			}
		}

		final var discovered = classLoaderService.loadJavaServices( StatisticsFactory.class ).iterator();
		if ( discovered.hasNext() ) {
			final var statisticsFactory = discovered.next();
			if ( discovered.hasNext() ) {
				throw new HibernateException(
						"Multiple StatisticsFactory service registrations found via ServiceLoader; "
								+ "specify one explicitly via '" + STATS_BUILDER + "'"
				);
			}
			return statisticsFactory;
		}
		return StatisticsImpl::new;
	}

	private static StatementObserver statementObserver(StatementObserver statementObserver) {
		return statementObserver == null ? IgnoredStatementObserver.IGNORE : statementObserver;
	}

	private static Collection<FilterDefinition> autoEnabledFilters(Map<String, FilterDefinition> filterDefinitions) {
		final var autoEnabledFilters = new ArrayList<FilterDefinition>();
		for ( var filter : filterDefinitions.values() ) {
			if ( filter.isAutoEnabled() ) {
				autoEnabledFilters.add( filter );
			}
		}
		return autoEnabledFilters;
	}

	private static JavaType<Object> tenantIdentifierType(
			Map<String, FilterDefinition> filterDefinitions,
			JavaType<Object> defaultTenantIdentifierJavaType) {
		final var tenantFilter = filterDefinitions.get( TenantIdBinder.FILTER_NAME );
		if ( tenantFilter == null ) {
			return defaultTenantIdentifierJavaType;
		}
		else {
			final var jdbcMapping = tenantFilter.getParameterJdbcMapping( TenantIdBinder.PARAMETER_NAME );
			assert jdbcMapping != null;
			//NOTE: this is completely unsound
			//noinspection unchecked
			return (JavaType<Object>) jdbcMapping.getJavaTypeDescriptor();
		}
	}
}
