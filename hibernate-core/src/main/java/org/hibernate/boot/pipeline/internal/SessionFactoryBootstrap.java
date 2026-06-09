/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.internal;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.persistence.PersistenceConfiguration;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.SchemaManagementAction;

import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.pipeline.internal.source.MappingSourceContributions;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.pipeline.internal.settings.ResolvedBootstrapSettings;
import org.hibernate.boot.pipeline.internal.settings.ResolvedMappingSettings;
import org.hibernate.boot.pipeline.internal.settings.SettingsResolver;
import org.hibernate.cfg.PersistenceSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.service.spi.ServiceException;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tool.schema.Action;
import org.hibernate.tool.schema.spi.DelayedDropRegistryNotAvailableImpl;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;

import static org.hibernate.cfg.CacheSettings.JAKARTA_SHARED_CACHE_MODE;
import static org.hibernate.cfg.JdbcSettings.JAKARTA_JTA_DATASOURCE;
import static org.hibernate.cfg.JdbcSettings.JAKARTA_NON_JTA_DATASOURCE;
import static org.hibernate.cfg.PersistenceSettings.JAKARTA_TRANSACTION_TYPE;
import static org.hibernate.cfg.SchemaToolingSettings.JAKARTA_HBM2DDL_DATABASE_ACTION;
import static org.hibernate.cfg.SchemaToolingSettings.JAKARTA_HBM2DDL_SCRIPTS_ACTION;
import static org.hibernate.cfg.TransactionSettings.FLUSH_BEFORE_COMPLETION;
import static org.hibernate.cfg.ValidationSettings.JAKARTA_VALIDATION_MODE;
import static org.hibernate.internal.util.config.ConfigurationHelper.getBoolean;
import static org.hibernate.jpa.internal.JpaLogger.JPA_LOGGER;

/// Canonical coordinator for SessionFactory bootstrap.
///
/// This class is the shared bridge between public bootstrap entry points and
/// the lower-level boot orchestration pipeline.  Public entry points should
/// normalize their inputs to one of the accepted source descriptions and
/// delegate here rather than constructing settings, metadata, or runtime
/// factory pieces directly.
///
/// The overloads accepting {@link PersistenceConfiguration} and
/// {@link PersistenceUnitDescriptor} are entry-point adapters.  They resolve
/// bootstrap settings, create the service registries, collect mapping-source
/// contributions, and then delegate to the resolved-input form,
/// {@link #build(SessionFactoryBootstrapRequest)}.
///
/// The resolved-input overload is the narrow core operation.  Callers that
/// already have resolved settings, source contributions, and a service registry
/// may use it directly.
///
/// @see MetadataResolver
/// @see SessionFactoryPipeline
///
/// @since 9.0
/// @author Steve Ebersole
public class SessionFactoryBootstrap {
	/// Build a [SessionFactory] from a Jakarta Persistence [PersistenceConfiguration].
	///
	/// This is the canonical path for programmatic PersistenceConfiguration
	/// bootstrap.  It accepts both plain Jakarta configurations and Hibernate's
	/// [HibernatePersistenceConfiguration] extension.  Hibernate-specific
	/// mapping-source contributions are used when the supplied configuration is a
	/// Hibernate configuration.
	///
	/// The bootstrap-owned service registries are attached to the returned
	/// SessionFactory and are destroyed when the SessionFactory is closed.
	///
	/// @param persistenceConfiguration the persistence configuration to bootstrap
	///
	/// @return the built SessionFactory
	///
	/// @throws PersistenceException if bootstrap fails
	public static SessionFactory build(PersistenceConfiguration persistenceConfiguration) {
		final var bootstrapServiceRegistry = new BootstrapServiceRegistryBuilder().build();
		try {
			final var bootstrapRequest = createEntryPointBootstrapRequest( persistenceConfiguration, bootstrapServiceRegistry );
			return build( bootstrapRequest );
		}
		catch (Exception e) {
			bootstrapServiceRegistry.close();
			if ( e instanceof ServiceException serviceException ) {
				throw serviceException;
			}
			throw new PersistenceException(
					"Unable to build Hibernate SessionFactory  [persistence unit: "
							+ persistenceConfiguration.name() + "] ",
					e
			);
		}
	}

	/// Perform schema management for a Jakarta Persistence [PersistenceConfiguration].
	///
	/// This follows the same settings and source-contribution resolution as
	/// [#build(PersistenceConfiguration)], but stops after metadata
	/// resolution and invokes Hibernate's schema-management coordinator.  The
	/// service registries created for this operation are always destroyed before
	/// the method returns.
	///
	/// @param persistenceConfiguration the persistence configuration to use for
	/// schema management
	///
	/// @throws PersistenceException if schema management fails
	public static void generateSchema(PersistenceConfiguration persistenceConfiguration) {
		final var bootstrapServiceRegistry = new BootstrapServiceRegistryBuilder().build();
		StandardServiceRegistry standardServiceRegistry = null;
		try {
			final var bootstrapRequest = createEntryPointBootstrapRequest( persistenceConfiguration, bootstrapServiceRegistry );
			standardServiceRegistry = bootstrapRequest.standardServiceRegistry();
			generateSchema( bootstrapRequest );
		}
		catch (Exception e) {
			throw new PersistenceException(
					"Error performing schema management  [persistence unit: "
							+ persistenceConfiguration.name() + "] ",
					e
			);
		}
		finally {
			if ( standardServiceRegistry != null ) {
				StandardServiceRegistryBuilder.destroy( standardServiceRegistry );
			}
			else {
				bootstrapServiceRegistry.close();
			}
		}
	}

	/// Build a [SessionFactory] from a persistence-unit descriptor and
	/// integration settings.
	///
	/// This is the canonical path for persistence-unit based bootstrap, including
	/// persistence.xml descriptors and container-provided
	/// `PersistenceUnitInfo` descriptors wrapped as
	/// [PersistenceUnitDescriptor].  The integration settings override or
	/// supplement values supplied by the descriptor according to the caller's
	/// entry-point contract.
	///
	/// The bootstrap-owned service registries are attached to the returned
	/// SessionFactory and are destroyed when the SessionFactory is closed.
	///
	/// @param persistenceUnitDescriptor descriptor for the persistence unit
	/// @param integrationSettings settings supplied by the bootstrap entry point
	///
	/// @return the built SessionFactory
	///
	/// @throws PersistenceException if bootstrap fails
	///
	/// @see org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor
	/// @see org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor
	public static SessionFactory build(
			PersistenceUnitDescriptor persistenceUnitDescriptor,
			Map<?, ?> integrationSettings) {
		final var bootstrapServiceRegistry = buildBootstrapServiceRegistry( persistenceUnitDescriptor );
		try {
			final var bootstrapRequest = createEntryPointBootstrapRequest(
					persistenceUnitDescriptor,
					integrationSettings,
					bootstrapServiceRegistry
			);
			return build( bootstrapRequest );
		}
		catch (Exception e) {
			bootstrapServiceRegistry.close();
			if ( e instanceof MappingException mappingException ) {
				throw mappingException;
			}
			if ( e instanceof ServiceException serviceException ) {
				throw serviceException;
			}
			throw new PersistenceException(
					"Unable to build Hibernate SessionFactory  [persistence unit: "
							+ persistenceUnitDescriptor.getName() + "] ",
					e
			);
		}
	}

	/// Perform schema management for a persistence-unit descriptor and integration
	/// settings.
	///
	/// This follows the same descriptor and settings resolution as
	/// [#build(PersistenceUnitDescriptor, Map)], but stops after metadata
	/// resolution and invokes Hibernate's schema-management coordinator.  The
	/// service registries created for this operation are always destroyed before
	/// the method returns.
	///
	/// @param persistenceUnitDescriptor descriptor for the persistence unit
	/// @param integrationSettings settings supplied by the bootstrap entry point
	///
	/// @throws PersistenceException if schema management fails
	public static void generateSchema(
			PersistenceUnitDescriptor persistenceUnitDescriptor,
			Map<?, ?> integrationSettings) {
		final var bootstrapServiceRegistry = buildBootstrapServiceRegistry( persistenceUnitDescriptor );
		StandardServiceRegistry standardServiceRegistry = null;
		try {
			final var bootstrapRequest = createEntryPointBootstrapRequest(
					persistenceUnitDescriptor,
					integrationSettings,
					bootstrapServiceRegistry
			);
			standardServiceRegistry = bootstrapRequest.standardServiceRegistry();
			generateSchema( bootstrapRequest );
		}
		catch (Exception e) {
			throw new PersistenceException(
					"Error performing schema management  [persistence unit: "
					+ persistenceUnitDescriptor.getName() + "] ",
					e
			);
		}
		finally {
			if ( standardServiceRegistry != null ) {
				StandardServiceRegistryBuilder.destroy( standardServiceRegistry );
			}
			else {
				bootstrapServiceRegistry.close();
			}
		}
	}

	/// Resolve metadata from a persistence-unit descriptor and integration settings.
	///
	/// The returned metadata is suitable for metadata-only callers and retains
	/// the resolved metadata product so later SessionFactory construction can
	/// continue through the pipeline-aware path.
	public static MetadataBootstrap resolveMetadata(
			PersistenceUnitDescriptor persistenceUnitDescriptor,
			Map<?, ?> integrationSettings) {
		final var bootstrapServiceRegistry = buildBootstrapServiceRegistry( persistenceUnitDescriptor );
		StandardServiceRegistry standardServiceRegistry = null;
		try {
			final var bootstrapRequest = createEntryPointBootstrapRequest(
					persistenceUnitDescriptor,
					integrationSettings,
					bootstrapServiceRegistry
			);
			standardServiceRegistry = bootstrapRequest.standardServiceRegistry();
			final var resolvedMetadata = MetadataResolver.resolve(
					bootstrapRequest.bootstrapSettings(),
					bootstrapRequest.mappingSettings(),
					bootstrapRequest.sourceContributions(),
					standardServiceRegistry
			);
			final var serviceRegistryCloser = new OwnedServiceRegistryCloser(
					(ServiceRegistryImplementor) standardServiceRegistry
			);
			return new MetadataBootstrap(
					new ResolvedMetadataImplementor(
							bootstrapRequest.bootstrapSettings(),
							resolvedMetadata,
							serviceRegistryCloser
					),
					bootstrapRequest.bootstrapSettings().configurationValues(),
					serviceRegistryCloser
			);
		}
		catch (Exception e) {
			if ( standardServiceRegistry != null ) {
				StandardServiceRegistryBuilder.destroy( standardServiceRegistry );
			}
			else {
				bootstrapServiceRegistry.close();
			}
			throw new PersistenceException(
					"Unable to resolve Hibernate metadata  [persistence unit: "
							+ persistenceUnitDescriptor.getName() + "] ",
					e
			);
		}
	}

	/// Build a [SessionFactoryImplementor] from fully resolved bootstrap
	/// inputs.
	///
	/// This is the core orchestration operation used by the entry-point adapter
	/// overloads after they have resolved settings, collected source
	/// contributions, and created the service registry.  It resolves metadata and
	/// then delegates final runtime factory creation to
	/// [SessionFactoryPipeline].
	///
	/// @param request resolved inputs for SessionFactory bootstrap
	///
	/// @return the built SessionFactory
	public static SessionFactoryImplementor build(SessionFactoryBootstrapRequest request) {
		Objects.requireNonNull( request );
		final var resolvedMetadata = MetadataResolver.resolve(
				request.bootstrapSettings(),
				request.mappingSettings(),
				request.sourceContributions(),
				request.metadataCustomizations(),
				request.serviceRegistry()
		);
		return SessionFactoryPipeline.build(
				request.sessionFactorySettings(),
				resolvedMetadata,
				request.serviceRegistry(),
				request.additionalSessionFactoryObservers()
		);
	}

	private static SessionFactory build(EntryPointBootstrapRequest bootstrapRequest) {
		final var sessionFactorySettings = SettingsResolver.resolveSessionFactorySettings(
				bootstrapRequest.bootstrapSettings(),
				bootstrapRequest.standardServiceRegistry()
		);
		return build(
				new SessionFactoryBootstrapRequest(
						bootstrapRequest.bootstrapSettings(),
						bootstrapRequest.mappingSettings(),
						bootstrapRequest.sourceContributions(),
						MetadataCustomizations.NONE,
						sessionFactorySettings,
						bootstrapRequest.standardServiceRegistry(),
						new SessionFactoryObserver[] { ServiceRegistryCloser.INSTANCE }
				)
		);
	}

	private static void generateSchema(EntryPointBootstrapRequest bootstrapRequest) {
		final var resolvedMetadata = MetadataResolver.resolve(
				bootstrapRequest.bootstrapSettings(),
				bootstrapRequest.mappingSettings(),
				bootstrapRequest.sourceContributions(),
				bootstrapRequest.standardServiceRegistry()
		);
		SchemaManagementToolCoordinator.process(
				resolvedMetadata.metadata(),
				bootstrapRequest.standardServiceRegistry(),
				bootstrapRequest.bootstrapSettings().configurationValues(),
				DelayedDropRegistryNotAvailableImpl.INSTANCE
		);
	}

	private static EntryPointBootstrapRequest createEntryPointBootstrapRequest(
			PersistenceConfiguration persistenceConfiguration,
			org.hibernate.boot.registry.BootstrapServiceRegistry bootstrapServiceRegistry) {
		final var configurationValues = configurationValues( persistenceConfiguration );
		if ( getBoolean( FLUSH_BEFORE_COMPLETION, configurationValues ) ) {
			JPA_LOGGER.definingFlushBeforeCompletionIgnoredInHem( FLUSH_BEFORE_COMPLETION );
			configurationValues.put( FLUSH_BEFORE_COMPLETION, String.valueOf( false ) );
		}
		final var bootstrapSettings = SettingsResolver.resolveBootstrapSettings(
				configurationValues,
				true
		);
		final var mappingSettings = SettingsResolver.resolveMappingSettings(
				bootstrapSettings,
				persistenceConfiguration.defaultToOneFetchType()
		);
		final var standardServiceRegistry = StandardServiceRegistryBuilder.forJpa( bootstrapServiceRegistry )
				.applySettings( bootstrapSettings.configurationValues() )
				.build();
		final var sourceContributions = resolveMappingSourceContributions(
				persistenceConfiguration,
				bootstrapSettings,
				mappingSettings,
				standardServiceRegistry
		);
		return new EntryPointBootstrapRequest(
				bootstrapSettings,
				mappingSettings,
				standardServiceRegistry,
				sourceContributions
		);
	}

	private static EntryPointBootstrapRequest createEntryPointBootstrapRequest(
			PersistenceUnitDescriptor persistenceUnitDescriptor,
			Map<?, ?> integrationSettings,
			org.hibernate.boot.registry.BootstrapServiceRegistry bootstrapServiceRegistry) {
		final var bootstrapSettings = SettingsResolver.resolveBootstrapSettings(
				persistenceUnitDescriptor,
				integrationSettings
		);
		final var mappingSettings = SettingsResolver.resolveMappingSettings(
				bootstrapSettings,
				persistenceUnitDescriptor.getDefaultToOneFetchType()
		);
		if ( getBoolean( FLUSH_BEFORE_COMPLETION, bootstrapSettings.configurationValues() ) ) {
			JPA_LOGGER.definingFlushBeforeCompletionIgnoredInHem( FLUSH_BEFORE_COMPLETION );
			bootstrapSettings.configurationValues().put( FLUSH_BEFORE_COMPLETION, String.valueOf( false ) );
		}
		final var standardServiceRegistry = StandardServiceRegistryBuilder.forJpa( bootstrapServiceRegistry )
				.applySettings( bootstrapSettings.configurationValues() )
				.build();
		return new EntryPointBootstrapRequest(
				bootstrapSettings,
				mappingSettings,
				standardServiceRegistry,
				MappingSourceContributions.from(
						persistenceUnitDescriptor,
						bootstrapSettings,
						standardServiceRegistry.requireService( ClassLoaderService.class )
				)
		);
	}

	private static MappingSourceContributions resolveMappingSourceContributions(
			PersistenceConfiguration persistenceConfiguration,
			ResolvedBootstrapSettings bootstrapSettings,
			ResolvedMappingSettings mappingSettings,
			StandardServiceRegistry serviceRegistry) {
		if ( persistenceConfiguration instanceof HibernatePersistenceConfiguration hibernatePersistenceConfiguration ) {
			return MappingSourceContributions.from(
					hibernatePersistenceConfiguration,
					bootstrapSettings,
					mappingSettings,
					serviceRegistry.requireService( ClassLoaderService.class )
			);
		}
		else {
			return MappingSourceContributions.from( persistenceConfiguration );
		}
	}

	private static Map<String, Object> configurationValues(PersistenceConfiguration persistenceConfiguration) {
		final var configurationValues = new LinkedHashMap<String, Object>();
		persistenceConfiguration.properties().forEach( (key, value) -> {
			if ( key != null ) {
				configurationValues.put( key.toString(), value );
			}
		} );
		collectSchemaManagementActions( persistenceConfiguration, configurationValues::putIfAbsent );
		configurationValues.put( PersistenceSettings.PERSISTENCE_UNIT_NAME, persistenceConfiguration.name() );
		putIfNonNull( configurationValues, JAKARTA_TRANSACTION_TYPE, persistenceConfiguration.transactionType() );
		putIfNonNull( configurationValues, JAKARTA_JTA_DATASOURCE, persistenceConfiguration.jtaDataSource() );
		putIfNonNull( configurationValues, JAKARTA_NON_JTA_DATASOURCE, persistenceConfiguration.nonJtaDataSource() );
		putIfNonNull( configurationValues, JAKARTA_VALIDATION_MODE, persistenceConfiguration.validationMode() );
		putIfNonNull( configurationValues, JAKARTA_SHARED_CACHE_MODE, persistenceConfiguration.sharedCacheMode() );
		return configurationValues;
	}

	private static void collectSchemaManagementActions(
			PersistenceConfiguration persistenceConfiguration,
			java.util.function.BiConsumer<String, Object> collector) {
		collectSchemaManagementAction(
				persistenceConfiguration.schemaManagementDatabaseAction(),
				JAKARTA_HBM2DDL_DATABASE_ACTION,
				collector
		);
		collectSchemaManagementAction(
				persistenceConfiguration.schemaManagementScriptsAction(),
				JAKARTA_HBM2DDL_SCRIPTS_ACTION,
				collector
		);
	}

	private static void collectSchemaManagementAction(
			SchemaManagementAction action,
			String settingName,
			java.util.function.BiConsumer<String, Object> collector) {
		if ( action != null && action != SchemaManagementAction.NONE ) {
			collector.accept( settingName, Action.interpretJpaSetting( action ) );
		}
	}

	private static void putIfNonNull(Map<String, Object> configurationValues, String name, Object value) {
		if ( value != null ) {
			configurationValues.putIfAbsent( name, value );
		}
	}

	private static org.hibernate.boot.registry.BootstrapServiceRegistry buildBootstrapServiceRegistry(
			PersistenceUnitDescriptor persistenceUnitDescriptor) {
		final var builder = new BootstrapServiceRegistryBuilder();
		final var persistenceUnitClassLoader = persistenceUnitDescriptor.getClassLoader();
		if ( persistenceUnitClassLoader != null ) {
			builder.applyClassLoader( persistenceUnitClassLoader );
		}
		return builder.build();
	}

	/// Intermediate request produced by adapting raw entry-point inputs.
	///
	/// Unlike [SessionFactoryBootstrapRequest], this request does not include
	/// SessionFactory-level settings.  It represents the common resolved inputs
	/// needed by both one-shot SessionFactory construction and schema generation.
	private record EntryPointBootstrapRequest(
			ResolvedBootstrapSettings bootstrapSettings,
			ResolvedMappingSettings mappingSettings,
			StandardServiceRegistry standardServiceRegistry,
			MappingSourceContributions sourceContributions) {
	}

	private static class ServiceRegistryCloser implements SessionFactoryObserver {
		private static final ServiceRegistryCloser INSTANCE = new ServiceRegistryCloser();

		@Override
		public void sessionFactoryClosed(SessionFactory sessionFactory) {
			final var factoryImplementor = (SessionFactoryImplementor) sessionFactory;
			close( factoryImplementor.getServiceRegistry() );
		}

		private void close(ServiceRegistryImplementor serviceRegistry) {
			serviceRegistry.destroy();
			final var basicRegistry =
					(ServiceRegistryImplementor)
							serviceRegistry.getParentServiceRegistry();
			if ( basicRegistry != null ) {
				basicRegistry.destroy();
			}
		}
	}

	private static class OwnedServiceRegistryCloser implements SessionFactoryObserver, Runnable {
		private final ServiceRegistryImplementor serviceRegistry;
		private final AtomicBoolean closed = new AtomicBoolean();

		private OwnedServiceRegistryCloser(ServiceRegistryImplementor serviceRegistry) {
			this.serviceRegistry = serviceRegistry;
		}

		@Override
		public void sessionFactoryClosed(SessionFactory sessionFactory) {
			run();
		}

		@Override
		public void run() {
			if ( closed.compareAndSet( false, true ) ) {
				ServiceRegistryCloser.INSTANCE.close( serviceRegistry );
			}
		}
	}
}
