/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.boot.internal;

import java.util.Map;
import java.util.Properties;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.EffectiveMappingDefaults;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.envers.boot.spi.EnversMetadataBuildingContext;
import org.hibernate.envers.configuration.Configuration;
import org.hibernate.envers.configuration.internal.EntitiesConfigurator;
import org.hibernate.envers.configuration.internal.MappingCollector;
import org.hibernate.envers.internal.entities.EntitiesConfigurations;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.revisioninfo.ModifiedEntityNamesReader;
import org.hibernate.envers.internal.revisioninfo.RevisionInfoNumberReader;
import org.hibernate.envers.internal.revisioninfo.RevisionInfoQueryCreator;
import org.hibernate.envers.internal.synchronization.AuditProcessManager;
import org.hibernate.envers.internal.tools.ReflectionTools;
import org.hibernate.envers.strategy.AuditStrategy;
import org.hibernate.envers.strategy.spi.AuditStrategyContext;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.Stoppable;

import org.jboss.logging.Logger;

/**
 * Provides central access to Envers' configuration.
 *
 * In many ways, this replaces the legacy static map Envers used originally as
 * a means to share the old AuditConfiguration.
 *
 * @author Steve Ebersole
 * @author Chris Cranford
 */
public class EnversServiceImpl implements EnversService, Configurable, Stoppable {
	private static final Logger log = Logger.getLogger( EnversServiceImpl.class );

	private static final String LEGACY_AUTO_REGISTER = "hibernate.listeners.envers.autoRegister";

	private boolean integrationEnabled;
	private boolean initialized;

	private ServiceRegistry serviceRegistry;
	private ClassLoaderService classLoaderService;

	// todo : not at all a fan of all these...
	//		2) AuditProcessManager is a glorified Map of AuditProcess instances (BeforeTransactionCompletionProcess)
	//			keyed by Transaction (Session)
	private Configuration configuration;
	private AuditProcessManager auditProcessManager;
	private EntitiesConfigurations entitiesConfigurations;

	@Override
	public void configure(Map<String, Object> configurationValues) {
		if ( configurationValues.containsKey( LEGACY_AUTO_REGISTER ) ) {
			log.debugf(
					"Encountered deprecated Envers setting [%s]; use [%s] or [%s] instead",
					LEGACY_AUTO_REGISTER,
					INTEGRATION_ENABLED,
					EnversIntegrator.AUTO_REGISTER
			);
		}
		this.integrationEnabled = ConfigurationHelper.getBoolean( INTEGRATION_ENABLED, configurationValues, true );

		log.infof( "Envers integration enabled? : %s", integrationEnabled );
	}

	@Override
	public boolean isEnabled() {
		return integrationEnabled;
	}

	@Override
	public boolean isInitialized() {
		return initialized;
	}

	@Override
	public void initialize(
			MetadataImplementor metadata,
			MappingCollector mappingCollector,
			EffectiveMappingDefaults effectiveMappingDefaults) {
		if ( initialized ) {
			throw new UnsupportedOperationException( "EnversService#initialize should be called only once" );
		}

		initialized = true;

		final InFlightMetadataCollector metadataCollector = (InFlightMetadataCollector) metadata;
		this.serviceRegistry = metadata.getMetadataBuildingOptions().getServiceRegistry();
		this.classLoaderService = serviceRegistry.getService( ClassLoaderService.class );

		final ConfigurationService cfgService = serviceRegistry.getService( ConfigurationService.class );
		final Properties properties = new Properties();
		properties.putAll( cfgService.getSettings() );

		this.configuration = new Configuration( properties, this, metadataCollector );
		this.auditProcessManager = new AuditProcessManager( configuration.getRevisionInfo().getRevisionInfoGenerator() );

		final EnversMetadataBuildingContext metadataBuildingContext = new EnversMetadataBuildingContextImpl(
				configuration,
				metadataCollector,
				effectiveMappingDefaults,
				mappingCollector
		);

		// Strategy-specific initialization
		configuration.getAuditStrategy().postInitialize(
				new AuditStrategyContext() {
					@Override
					public Class<?> getRevisionInfoClass() {
						return configuration.getRevisionInfo().getRevisionInfoClass();
					}

					@Override
					public Getter getRevisionInfoTimestampAccessor() {
						final PropertyData pd = configuration.getRevisionInfo().getRevisionInfoTimestampData();
						return ReflectionTools.getGetter( getRevisionInfoClass(), pd, serviceRegistry );
					}
				}
		);

		this.entitiesConfigurations = new EntitiesConfigurator().configure( metadataBuildingContext );
	}

	@Override
	public Configuration getConfig() {
		if ( !initialized ) {
			throw new IllegalStateException( "Service is not yet initialized" );
		}
		return configuration;
	}

	@Override
	public AuditProcessManager getAuditProcessManager() {
		if ( !initialized ) {
			throw new IllegalStateException( "Service is not yet initialized" );
		}
		return auditProcessManager;
	}

	@Override
	@Deprecated
	public AuditStrategy getAuditStrategy() {
		if ( !initialized ) {
			throw new IllegalStateException( "Service is not yet initialized" );
		}
		return configuration.getAuditStrategy();
	}

	@Override
	public EntitiesConfigurations getEntitiesConfigurations() {
		if ( !initialized ) {
			throw new IllegalStateException( "Service is not yet initialized" );
		}
		return entitiesConfigurations;
	}

	@Override
	public RevisionInfoQueryCreator getRevisionInfoQueryCreator() {
		if ( !initialized ) {
			throw new IllegalStateException( "Service is not yet initialized" );
		}
		return configuration.getRevisionInfo().getRevisionInfoQueryCreator();
	}

	@Override
	public RevisionInfoNumberReader getRevisionInfoNumberReader() {
		if ( !initialized ) {
			throw new IllegalStateException( "Service is not yet initialized" );
		}
		return configuration.getRevisionInfo().getRevisionInfoNumberReader();
	}

	@Override
	public ModifiedEntityNamesReader getModifiedEntityNamesReader() {
		if ( !initialized ) {
			throw new IllegalStateException( "Service is not yet initialized" );
		}
		return configuration.getRevisionInfo().getModifiedEntityNamesReader();
	}

	@Override
	public ClassLoaderService getClassLoaderService() {
		if ( !initialized ) {
			throw new IllegalStateException( "Service is not yet initialized" );
		}
		return classLoaderService;
	}

	@Override
	public ServiceRegistry getServiceRegistry() {
		if ( !initialized ) {
			throw new IllegalStateException( "Service is not yet initialized" );
		}
		return serviceRegistry;
	}

	@Override
	public void stop() {
		// anything to release?
	}
}
