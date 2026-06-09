/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.boot.internal;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.PersistenceConfiguration;
import jakarta.persistence.SchemaManagementAction;

import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.models.source.BootstrapSourceContributions;
import org.hibernate.boot.orchestration.MetadataResolver;
import org.hibernate.boot.orchestration.SessionFactoryBootstrap;
import org.hibernate.boot.orchestration.SessionFactoryBootstrapRequest;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.settings.BootstrapSettingsResolver;
import org.hibernate.boot.settings.ResolvedBootstrapSettings;
import org.hibernate.boot.settings.SessionFactorySettingsResolver;
import org.hibernate.cfg.PersistenceSettings;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
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

/// Adapts Jakarta PersistenceConfiguration-style entry points to the shared
/// SessionFactory bootstrap process.
///
/// @since 9.0
/// @author Steve Ebersole
public class PersistenceConfigurationBootstrapAdapter {
	/// Build a EntityManagerFactory from PersistenceConfiguration.
	///
	/// @see org.hibernate.jpa.HibernatePersistenceProvider#createEntityManagerFactory(PersistenceConfiguration)
	public static EntityManagerFactory build(PersistenceConfiguration persistenceConfiguration) {
		final var bootstrapServiceRegistry = new BootstrapServiceRegistryBuilder().build();
		try {
			final var bootstrapContext = createBootstrapContext( persistenceConfiguration, bootstrapServiceRegistry );
			final var sessionFactorySettings = SessionFactorySettingsResolver.resolve(
					bootstrapContext.bootstrapSettings(),
					bootstrapContext.standardServiceRegistry()
			);
			return SessionFactoryBootstrap.build(
					new SessionFactoryBootstrapRequest(
							bootstrapContext.bootstrapSettings(),
							bootstrapContext.sourceContributions(),
							sessionFactorySettings,
							bootstrapContext.standardServiceRegistry(),
							new SessionFactoryObserver[] { ServiceRegistryCloser.INSTANCE }
					)
			);
		}
		catch (Exception e) {
			bootstrapServiceRegistry.close();
			throw new PersistenceException(
					"Unable to build Hibernate SessionFactory  [persistence unit: "
							+ persistenceConfiguration.name() + "] ",
					e
			);
		}
	}

	/// Perform schema generation from PersistenceConfiguration.
	///
	/// @see org.hibernate.jpa.HibernatePersistenceProvider#generateSchema(PersistenceConfiguration)
	public static void generateSchema(PersistenceConfiguration persistenceConfiguration) {
		final var bootstrapServiceRegistry = new BootstrapServiceRegistryBuilder().build();
		StandardServiceRegistry standardServiceRegistry = null;
		try {
			final var bootstrapContext = createBootstrapContext( persistenceConfiguration, bootstrapServiceRegistry );
			standardServiceRegistry = bootstrapContext.standardServiceRegistry();
			final var resolvedMetadata = MetadataResolver.resolve(
					bootstrapContext.bootstrapSettings(),
					bootstrapContext.sourceContributions(),
					standardServiceRegistry
			);
			SchemaManagementToolCoordinator.process(
					resolvedMetadata.metadata(),
					standardServiceRegistry,
					bootstrapContext.configurationValues(),
					DelayedDropRegistryNotAvailableImpl.INSTANCE
			);
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

	private static BootstrapContext createBootstrapContext(
			PersistenceConfiguration persistenceConfiguration,
			org.hibernate.boot.registry.BootstrapServiceRegistry bootstrapServiceRegistry) {
		final var configurationValues = configurationValues( persistenceConfiguration );
		if ( getBoolean( FLUSH_BEFORE_COMPLETION, configurationValues ) ) {
			JPA_LOGGER.definingFlushBeforeCompletionIgnoredInHem( FLUSH_BEFORE_COMPLETION );
			configurationValues.put( FLUSH_BEFORE_COMPLETION, String.valueOf( false ) );
		}
		final var bootstrapSettings = BootstrapSettingsResolver.resolve(
				configurationValues,
				true,
				persistenceConfiguration.defaultToOneFetchType()
		);
		final var standardServiceRegistry = StandardServiceRegistryBuilder.forJpa( bootstrapServiceRegistry )
				.applySettings( bootstrapSettings.configurationValues() )
				.build();
		final var sourceContributions = resolveBootstrapSourceContributions(
				persistenceConfiguration,
				bootstrapSettings,
				standardServiceRegistry
		);
		return new BootstrapContext(
				configurationValues,
				bootstrapSettings,
				standardServiceRegistry,
				sourceContributions
		);
	}

	private static BootstrapSourceContributions resolveBootstrapSourceContributions(
			PersistenceConfiguration persistenceConfiguration,
			ResolvedBootstrapSettings bootstrapSettings,
			StandardServiceRegistry serviceRegistry) {
		if ( persistenceConfiguration instanceof HibernatePersistenceConfiguration hibernatePersistenceConfiguration ) {
			return BootstrapSourceContributions.from(
					hibernatePersistenceConfiguration,
					bootstrapSettings,
					serviceRegistry.requireService( ClassLoaderService.class )
			);
		}
		else {
			return BootstrapSourceContributions.from( persistenceConfiguration );
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

	private record BootstrapContext(
			Map<String, Object> configurationValues,
			ResolvedBootstrapSettings bootstrapSettings,
			StandardServiceRegistry standardServiceRegistry,
			BootstrapSourceContributions sourceContributions) {
	}
}
