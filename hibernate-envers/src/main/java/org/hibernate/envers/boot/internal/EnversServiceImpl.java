/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.boot.internal;

import java.util.Map;
import java.util.Properties;

import org.hibernate.MappingException;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.envers.configuration.internal.AuditEntitiesConfiguration;
import org.hibernate.envers.configuration.internal.EntitiesConfigurator;
import org.hibernate.envers.configuration.internal.GlobalConfiguration;
import org.hibernate.envers.configuration.internal.MappingCollector;
import org.hibernate.envers.configuration.internal.RevisionInfoConfiguration;
import org.hibernate.envers.configuration.internal.RevisionInfoConfigurationResult;
import org.hibernate.envers.internal.entities.EntitiesConfigurations;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.revisioninfo.ModifiedEntityNamesReader;
import org.hibernate.envers.internal.revisioninfo.RevisionInfoNumberReader;
import org.hibernate.envers.internal.revisioninfo.RevisionInfoQueryCreator;
import org.hibernate.envers.internal.synchronization.AuditProcessManager;
import org.hibernate.envers.internal.tools.ReflectionTools;
import org.hibernate.envers.strategy.AuditStrategy;
import org.hibernate.envers.strategy.ValidityAuditStrategy;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.internal.util.xml.XMLHelper;
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
 */
public class EnversServiceImpl implements EnversService, Configurable, Stoppable {
	private static final Logger log = Logger.getLogger( EnversServiceImpl.class );

	private boolean integrationEnabled;
	private boolean initialized;

	private ServiceRegistry serviceRegistry;
	private ClassLoaderService classLoaderService;

	// todo : not at all a fan of all these...
	//		1) GlobalConfiguration, AuditEntitiesConfiguration and AuditStrategy are
	// 			all "configuration" objects.  They seem unnecessarily split apart from
	//			each other.  Why 3?  Why not just one?
	//		2) AuditProcessManager is a glorified Map of AuditProcess instances (BeforeTransactionCompletionProcess)
	//			keyed by Transaction (Session)
	//		3) Make sure that the info kept here is all really needed at run time, and not just at
	//			"mapping time"
	private GlobalConfiguration globalConfiguration;
	private AuditEntitiesConfiguration auditEntitiesConfiguration;
	private AuditProcessManager auditProcessManager;
	private AuditStrategy auditStrategy;
	private EntitiesConfigurations entitiesConfigurations;
	private RevisionInfoQueryCreator revisionInfoQueryCreator;
	private RevisionInfoNumberReader revisionInfoNumberReader;
	private ModifiedEntityNamesReader modifiedEntityNamesReader;

	private XMLHelper xmlHelper;

	@Override
	public void configure(Map configurationValues) {
		final boolean legacySetting = ConfigurationHelper.getBoolean( LEGACY_AUTO_REGISTER, configurationValues, true );
		this.integrationEnabled = ConfigurationHelper.getBoolean( INTEGRATION_ENABLED, configurationValues, legacySetting );

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
	public void initialize(final MetadataImplementor metadata, final MappingCollector mappingCollector) {
		if ( initialized ) {
			throw new UnsupportedOperationException( "EnversService#initialize should be called only once" );
		}

		initialized = true;


		this.serviceRegistry = metadata.getMetadataBuildingOptions().getServiceRegistry();
		this.classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
		this.xmlHelper = new XMLHelper( classLoaderService );

		doInitialize( metadata, mappingCollector, serviceRegistry );
	}

	private void doInitialize(
			final MetadataImplementor metadata,
			final MappingCollector mappingCollector,
			ServiceRegistry serviceRegistry) {
		final ConfigurationService cfgService = serviceRegistry.getService( ConfigurationService.class );
		final Properties properties = new Properties();
		properties.putAll( cfgService.getSettings() );

		this.globalConfiguration = new GlobalConfiguration( this, properties );

		final ReflectionManager reflectionManager = metadata.getMetadataBuildingOptions()
				.getReflectionManager();
		final RevisionInfoConfiguration revInfoCfg = new RevisionInfoConfiguration( globalConfiguration );
		final RevisionInfoConfigurationResult revInfoCfgResult = revInfoCfg.configure(
				metadata,
				reflectionManager
		);

		EnversServiceImpl.this.auditEntitiesConfiguration = new AuditEntitiesConfiguration(
				properties,
				revInfoCfgResult.getRevisionInfoEntityName()
		);
		this.auditProcessManager = new AuditProcessManager( revInfoCfgResult.getRevisionInfoGenerator() );
		this.revisionInfoQueryCreator = revInfoCfgResult.getRevisionInfoQueryCreator();
		this.revisionInfoNumberReader = revInfoCfgResult.getRevisionInfoNumberReader();
		this.modifiedEntityNamesReader = revInfoCfgResult.getModifiedEntityNamesReader();
		this.auditStrategy = initializeAuditStrategy(
				auditEntitiesConfiguration.getAuditStrategyName(),
				revInfoCfgResult.getRevisionInfoClass(),
				revInfoCfgResult.getRevisionInfoTimestampData(),
				serviceRegistry
		);
		this.entitiesConfigurations = new EntitiesConfigurator().configure(
				metadata,
				serviceRegistry,
				reflectionManager,
				mappingCollector,
				globalConfiguration,
				auditEntitiesConfiguration,
				auditStrategy,
				revInfoCfgResult.getRevisionInfoXmlMapping(),
				revInfoCfgResult.getRevisionInfoRelationMapping()
		);
	}

	private static AuditStrategy initializeAuditStrategy(
			String auditStrategyName,
			Class<?> revisionInfoClass,
			PropertyData revisionInfoTimestampData,
			ServiceRegistry serviceRegistry) {
		AuditStrategy strategy;

		try {
			final Class<?> auditStrategyClass = loadClass( auditStrategyName, serviceRegistry );
			strategy = (AuditStrategy) ReflectHelper.getDefaultConstructor( auditStrategyClass ).newInstance();
		}
		catch (Exception e) {
			throw new MappingException(
					String.format( "Unable to create AuditStrategy [%s] instance.", auditStrategyName ),
					e
			);
		}

		if ( strategy instanceof ValidityAuditStrategy ) {
			// further initialization required
			final Getter revisionTimestampGetter = ReflectionTools.getGetter(
					revisionInfoClass,
					revisionInfoTimestampData,
					serviceRegistry
			);
			( (ValidityAuditStrategy) strategy ).setRevisionTimestampGetter( revisionTimestampGetter );
		}

		return strategy;
	}

	@Override
	public XMLHelper getXmlHelper() {
		return xmlHelper;
	}

	/**
	 * Load a class by name, preferring our ClassLoader and then the ClassLoaderService.
	 *
	 * @param auditStrategyName The name of the class to load
	 * @param serviceRegistry The ServiceRegistry
	 *
	 * @return The loaded class.
	 */
	private static Class<?> loadClass(String auditStrategyName, ServiceRegistry serviceRegistry) {
		try {
			return EnversServiceImpl.class.getClassLoader().loadClass( auditStrategyName );
		}
		catch (Exception e) {
			return ReflectionTools.loadClass( auditStrategyName, serviceRegistry.getService( ClassLoaderService.class ) );
		}
	}

	@Override
	public GlobalConfiguration getGlobalConfiguration() {
		if ( !initialized ) {
			throw new IllegalStateException( "Service is not yet initialized" );
		}
		return globalConfiguration;
	}

	@Override
	public AuditEntitiesConfiguration getAuditEntitiesConfiguration() {
		if ( !initialized ) {
			throw new IllegalStateException( "Service is not yet initialized" );
		}
		return auditEntitiesConfiguration;
	}

	@Override
	public AuditProcessManager getAuditProcessManager() {
		if ( !initialized ) {
			throw new IllegalStateException( "Service is not yet initialized" );
		}
		return auditProcessManager;
	}

	@Override
	public AuditStrategy getAuditStrategy() {
		if ( !initialized ) {
			throw new IllegalStateException( "Service is not yet initialized" );
		}
		return auditStrategy;
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
		return revisionInfoQueryCreator;
	}

	@Override
	public RevisionInfoNumberReader getRevisionInfoNumberReader() {
		if ( !initialized ) {
			throw new IllegalStateException( "Service is not yet initialized" );
		}
		return revisionInfoNumberReader;
	}

	@Override
	public ModifiedEntityNamesReader getModifiedEntityNamesReader() {
		if ( !initialized ) {
			throw new IllegalStateException( "Service is not yet initialized" );
		}
		return modifiedEntityNamesReader;
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
