/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.hibernate.Incubating;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.internal.MappingSources;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.boot.pipeline.internal.source.MappingSourceContributions;
import org.hibernate.boot.pipeline.internal.MetadataCustomizations;
import org.hibernate.boot.pipeline.internal.MetadataResolver;
import org.hibernate.boot.pipeline.internal.SessionFactoryBootstrap;
import org.hibernate.boot.pipeline.internal.SessionFactoryBootstrapRequest;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.pipeline.internal.settings.ResolvedBootstrapSettings;
import org.hibernate.boot.pipeline.internal.settings.ResolvedMappingSettings;
import org.hibernate.boot.pipeline.internal.settings.SettingsResolver;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tool.schema.spi.DelayedDropRegistryNotAvailableImpl;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;

import jakarta.persistence.FetchType;

/// Native Hibernate bootstrap entry point.
///
/// This is the replacement shape for native bootstrap through `Configuration`,
/// `MetadataSources`, and `MetadataBuilder`.  It collects mapping sources,
/// settings, and metadata-build options, then builds a SessionFactory through the
/// new bootstrap pipeline.
///
/// This is the native Hibernate alternative to the JPA SE bootstrap APIs.
///
/// @see org.hibernate.jpa.HibernatePersistenceConfiguration
/// @see jakarta.persistence.Persistence
/// @see jakarta.persistence.PersistenceConfiguration
///
/// @since 9.0
/// @author Steve Ebersole
@Incubating
public class HibernateBootstrap {
	private final MappingSources mappingSources = new MappingSources();
	private final LinkedHashMap<String, Object> settings = new LinkedHashMap<>();
	private final LinkedHashMap<String, Class<?>> queryImports = new LinkedHashMap<>();
	private final List<TypeContributor> typeContributors = new ArrayList<>();
	private final List<FunctionContributor> functionContributors = new ArrayList<>();
	private final List<CacheRegionDefinition> cacheRegionDefinitions = new ArrayList<>();

	private FetchType defaultToOneFetchType = FetchType.EAGER;

	private final StandardServiceRegistry providedServiceRegistry;

	private HibernateBootstrap(StandardServiceRegistry providedServiceRegistry) {
		this.providedServiceRegistry = providedServiceRegistry;
	}

	/// Create a native bootstrap that owns its service registry.
	public static HibernateBootstrap create() {
		return new HibernateBootstrap( null );
	}

	/// Create a native bootstrap using a caller-owned service registry.
	public static HibernateBootstrap create(StandardServiceRegistry serviceRegistry) {
		return new HibernateBootstrap( Objects.requireNonNull( serviceRegistry ) );
	}

	/// Apply a configuration setting.
	public HibernateBootstrap applySetting(String name, Object value) {
		if ( value == null ) {
			settings.remove( name );
		}
		else {
			settings.put( name, value );
		}
		return this;
	}

	/// Apply configuration settings.
	public HibernateBootstrap applySettings(Map<?, ?> settings) {
		if ( settings != null ) {
			settings.forEach( (key, value) -> {
				if ( key != null ) {
					applySetting( key.toString(), value );
				}
			} );
		}
		return this;
	}

	/// Add a managed class.
	public HibernateBootstrap addManagedClass(Class<?> managedClass) {
		mappingSources.addManagedClass( managedClass );
		return this;
	}

	/// Add managed classes.
	public HibernateBootstrap addManagedClasses(Class<?>... managedClasses) {
		mappingSources.addManagedClasses( managedClasses );
		return this;
	}

	/// Add a managed class name without loading the class.
	public HibernateBootstrap addManagedClassName(String managedClassName) {
		mappingSources.addManagedClassName( managedClassName );
		return this;
	}

	/// Add managed class names without loading the classes.
	public HibernateBootstrap addManagedClassNames(String... managedClassNames) {
		mappingSources.addManagedClassNames( managedClassNames );
		return this;
	}

	/// Add package-level metadata by package name.
	public HibernateBootstrap addPackage(String packageName) {
		mappingSources.addPackage( packageName );
		return this;
	}

	/// Add package-level metadata by package reference.
	public HibernateBootstrap addPackage(Package packageRef) {
		mappingSources.addPackage( packageRef );
		return this;
	}

	/// Add a classpath mapping resource name.
	public HibernateBootstrap addMappingResource(String mappingResource) {
		mappingSources.addMappingResource( mappingResource );
		return this;
	}

	/// Add classpath mapping resource names.
	public HibernateBootstrap addMappingResources(String... mappingResources) {
		mappingSources.addMappingResources( mappingResources );
		return this;
	}

	/// Add a mapping file path.
	public HibernateBootstrap addMappingFile(Path mappingFile) {
		mappingSources.addMappingFile( mappingFile );
		return this;
	}

	/// Add a mapping file.
	public HibernateBootstrap addMappingFile(File mappingFile) {
		mappingSources.addMappingFile( mappingFile );
		return this;
	}

	/// Add a mapping file URI.
	public HibernateBootstrap addMappingUri(URI mappingFileUri) {
		mappingSources.addMappingUri( mappingFileUri );
		return this;
	}

	/// Add a mapping file URL.
	public HibernateBootstrap addMappingUrl(URL mappingFileUrl) {
		mappingSources.addMappingUrl( mappingFileUrl );
		return this;
	}

	/// Add a query import.
	public HibernateBootstrap addQueryImport(String importedName, Class<?> target) {
		queryImports.put( importedName, target );
		return this;
	}

	/// Apply the default fetch type for to-one associations that request the
	/// Jakarta Persistence `DEFAULT` fetch type.
	public HibernateBootstrap applyDefaultToOneFetchType(FetchType defaultToOneFetchType) {
		this.defaultToOneFetchType = defaultToOneFetchType == null ? FetchType.EAGER : defaultToOneFetchType;
		return this;
	}

	/// Apply a type contributor while resolving metadata.
	public HibernateBootstrap applyTypeContributor(TypeContributor typeContributor) {
		if ( typeContributor != null ) {
			typeContributors.add( typeContributor );
		}
		return this;
	}

	/// Apply type contributors while resolving metadata.
	public HibernateBootstrap applyTypeContributors(TypeContributor... typeContributors) {
		if ( typeContributors != null ) {
			for ( var typeContributor : typeContributors ) {
				applyTypeContributor( typeContributor );
			}
		}
		return this;
	}

	/// Apply a function contributor while resolving metadata.
	public HibernateBootstrap applyFunctionContributor(FunctionContributor functionContributor) {
		if ( functionContributor != null ) {
			functionContributors.add( functionContributor );
		}
		return this;
	}

	/// Apply function contributors while resolving metadata.
	public HibernateBootstrap applyFunctionContributors(FunctionContributor... functionContributors) {
		if ( functionContributors != null ) {
			for ( var functionContributor : functionContributors ) {
				applyFunctionContributor( functionContributor );
			}
		}
		return this;
	}

	/// Apply a cache-region declaration while resolving metadata.
	public HibernateBootstrap applyCacheRegionDefinition(CacheRegionDefinition cacheRegionDefinition) {
		if ( cacheRegionDefinition != null ) {
			cacheRegionDefinitions.add( cacheRegionDefinition );
		}
		return this;
	}

	/// Build a SessionFactory through the native bootstrap pipeline.
	public SessionFactory buildSessionFactory() {
		final var bootstrapSettings = SettingsResolver.resolveBootstrapSettings( settings, false );
		final var standardServiceRegistry = providedServiceRegistry == null
				? new StandardServiceRegistryBuilder()
						.applySettings( bootstrapSettings.configurationValues() )
						.build()
				: providedServiceRegistry;
		try {
			final var mappingSettings = resolveMappingSettings( bootstrapSettings );
			final var sessionFactorySettings = SettingsResolver.resolveSessionFactorySettings(
					bootstrapSettings,
					standardServiceRegistry
			);
			return SessionFactoryBootstrap.build(
					new SessionFactoryBootstrapRequest(
							bootstrapSettings,
							mappingSettings,
							MappingSourceContributions.from( mappingSources ),
							metadataCustomizations(),
							sessionFactorySettings,
							standardServiceRegistry,
							providedServiceRegistry == null
									? new SessionFactoryObserver[] { ServiceRegistryCloser.INSTANCE }
									: new SessionFactoryObserver[0]
					)
			);
		}
		catch (RuntimeException e) {
			if ( providedServiceRegistry == null ) {
				StandardServiceRegistryBuilder.destroy( standardServiceRegistry );
			}
			throw e;
		}
	}

	/// Perform schema management through the native bootstrap pipeline.
	public void generateSchema() {
		final var bootstrapSettings = SettingsResolver.resolveBootstrapSettings( settings, false );
		final var standardServiceRegistry = providedServiceRegistry == null
				? new StandardServiceRegistryBuilder()
						.applySettings( bootstrapSettings.configurationValues() )
						.build()
				: providedServiceRegistry;
		try {
			final var mappingSettings = resolveMappingSettings( bootstrapSettings );
			final var resolvedMetadata = MetadataResolver.resolve(
					bootstrapSettings,
					mappingSettings,
					MappingSourceContributions.from( mappingSources ),
					metadataCustomizations(),
					standardServiceRegistry
			);
			SchemaManagementToolCoordinator.process(
					resolvedMetadata.metadata(),
					standardServiceRegistry,
					bootstrapSettings.configurationValues(),
					DelayedDropRegistryNotAvailableImpl.INSTANCE
			);
		}
		finally {
			if ( providedServiceRegistry == null ) {
				StandardServiceRegistryBuilder.destroy( standardServiceRegistry );
			}
		}
	}

	private ResolvedMappingSettings resolveMappingSettings(ResolvedBootstrapSettings bootstrapSettings) {
		return SettingsResolver.resolveMappingSettings( bootstrapSettings, defaultToOneFetchType );
	}

	private MetadataCustomizations metadataCustomizations() {
		return new MetadataCustomizations(
				queryImports,
				typeContributors,
				functionContributors,
				cacheRegionDefinitions
		);
	}

	private static class ServiceRegistryCloser implements SessionFactoryObserver {
		private static final ServiceRegistryCloser INSTANCE = new ServiceRegistryCloser();

		@Override
		public void sessionFactoryClosed(SessionFactory sessionFactory) {
			final var factoryImplementor = (SessionFactoryImplementor) sessionFactory;
			final var serviceRegistry = factoryImplementor.getServiceRegistry();
			serviceRegistry.destroy();
			final var basicRegistry =
					(ServiceRegistryImplementor)
							serviceRegistry.getParentServiceRegistry();
			if ( basicRegistry != null ) {
				basicRegistry.destroy();
			}
		}
	}
}
