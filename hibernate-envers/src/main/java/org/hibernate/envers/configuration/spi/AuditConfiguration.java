/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers.configuration.spi;

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
import org.hibernate.property.Getter;
import org.hibernate.service.ServiceRegistry;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Stephanie Pau at Markit Group Plc
 * @author Steve Ebersole
 */
public class AuditConfiguration {
	private final ServiceRegistry serviceRegistry;

	private final GlobalConfiguration globalCfg;
	private final AuditEntitiesConfiguration auditEntCfg;
	private final AuditProcessManager auditProcessManager;
	private final AuditStrategy auditStrategy;
	private final EntitiesConfigurations entCfg;
	private final RevisionInfoQueryCreator revisionInfoQueryCreator;
	private final RevisionInfoNumberReader revisionInfoNumberReader;
	private final ModifiedEntityNamesReader modifiedEntityNamesReader;

	public AuditConfiguration(MetadataImplementor metadata, MappingCollector mappingCollector) {
		this.serviceRegistry = metadata.getMetadataBuildingOptions().getServiceRegistry();

		final ConfigurationService cfgService = serviceRegistry.getService( ConfigurationService.class );
		final Properties properties = new Properties();
		properties.putAll( cfgService.getSettings() );

		final ClassLoaderService classLoaderService = serviceRegistry.getService( ClassLoaderService.class );

		this.globalCfg = new GlobalConfiguration( properties, classLoaderService );

		final ReflectionManager reflectionManager = metadata.getMetadataBuildingOptions().getReflectionManager();
		final RevisionInfoConfiguration revInfoCfg = new RevisionInfoConfiguration( globalCfg );
		final RevisionInfoConfigurationResult revInfoCfgResult = revInfoCfg.configure( metadata, reflectionManager );

		this.auditEntCfg = new AuditEntitiesConfiguration( properties, revInfoCfgResult.getRevisionInfoEntityName() );
		this.auditProcessManager = new AuditProcessManager( revInfoCfgResult.getRevisionInfoGenerator() );
		this.revisionInfoQueryCreator = revInfoCfgResult.getRevisionInfoQueryCreator();
		this.revisionInfoNumberReader = revInfoCfgResult.getRevisionInfoNumberReader();
		this.modifiedEntityNamesReader = revInfoCfgResult.getModifiedEntityNamesReader();
		this.auditStrategy = initializeAuditStrategy(
				auditEntCfg.getAuditStrategyName(),
				revInfoCfgResult.getRevisionInfoClass(),
				revInfoCfgResult.getRevisionInfoTimestampData(),
				classLoaderService
		);
		this.entCfg = new EntitiesConfigurator().configure(
				metadata,
				serviceRegistry,
				reflectionManager,
				mappingCollector,
				globalCfg,
				auditEntCfg,
				auditStrategy,
				revInfoCfgResult.getRevisionInfoXmlMapping(),
				revInfoCfgResult.getRevisionInfoRelationMapping()
		);

	}


	public AuditEntitiesConfiguration getAuditEntCfg() {
		return auditEntCfg;
	}

	public AuditProcessManager getSyncManager() {
		return auditProcessManager;
	}

	public GlobalConfiguration getGlobalCfg() {
		return globalCfg;
	}

	public EntitiesConfigurations getEntCfg() {
		return entCfg;
	}

	public RevisionInfoQueryCreator getRevisionInfoQueryCreator() {
		return revisionInfoQueryCreator;
	}

	public RevisionInfoNumberReader getRevisionInfoNumberReader() {
		return revisionInfoNumberReader;
	}

	public ModifiedEntityNamesReader getModifiedEntityNamesReader() {
		return modifiedEntityNamesReader;
	}

	public AuditStrategy getAuditStrategy() {
		return auditStrategy;
	}

	private static AuditStrategy initializeAuditStrategy(
			String auditStrategyName,
			Class<?> revisionInfoClass,
			PropertyData revisionInfoTimestampData,
			ClassLoaderService classLoaderService) {
		AuditStrategy strategy;

		try {
			Class<?> auditStrategyClass = loadClass( auditStrategyName, classLoaderService );
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
			final Getter revisionTimestampGetter = ReflectionTools.getGetter( revisionInfoClass, revisionInfoTimestampData );
			( (ValidityAuditStrategy) strategy ).setRevisionTimestampGetter( revisionTimestampGetter );
		}

		return strategy;
	}

	/**
	 * Load a class by name, preferring our ClassLoader and then the ClassLoaderService.
	 *
	 * @param auditStrategyName The name of the class to load
	 * @param classLoaderService The ClassLoaderService
	 *
	 * @return The loaded class.
	 */
	private static Class<?> loadClass(String auditStrategyName, ClassLoaderService classLoaderService) {
		try {
			return AuditConfiguration.class.getClassLoader().loadClass( auditStrategyName );
		}
		catch (Exception e) {
			return ReflectionTools.loadClass( auditStrategyName, classLoaderService );
		}
	}

	public void destroy() {
		// Anything we need to release in here?
	}
}
