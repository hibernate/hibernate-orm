/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.envers.configuration;

import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;

import org.hibernate.MappingException;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.cfg.Configuration;
import org.hibernate.envers.entities.EntitiesConfigurations;
import org.hibernate.envers.entities.PropertyData;
import org.hibernate.envers.revisioninfo.ModifiedEntityNamesReader;
import org.hibernate.envers.revisioninfo.RevisionInfoNumberReader;
import org.hibernate.envers.revisioninfo.RevisionInfoQueryCreator;
import org.hibernate.envers.strategy.AuditStrategy;
import org.hibernate.envers.strategy.ValidityAuditStrategy;
import org.hibernate.envers.synchronization.AuditProcessManager;
import org.hibernate.envers.tools.reflection.ReflectionTools;
import org.hibernate.property.Getter;
import org.hibernate.service.classloading.spi.ClassLoaderService;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Stephanie Pau at Markit Group Plc
 */
public class AuditConfiguration {
	private final GlobalConfiguration globalCfg;
	private final AuditEntitiesConfiguration auditEntCfg;
	private final AuditProcessManager auditProcessManager;
	private final AuditStrategy auditStrategy;
	private final EntitiesConfigurations entCfg;
	private final RevisionInfoQueryCreator revisionInfoQueryCreator;
	private final RevisionInfoNumberReader revisionInfoNumberReader;
	private final ModifiedEntityNamesReader modifiedEntityNamesReader;
	private final ClassLoaderService classLoaderService;

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

	public AuditConfiguration(Configuration cfg) {
		this( cfg, null );
	}

	public AuditConfiguration(Configuration cfg, ClassLoaderService classLoaderService) {
		Properties properties = cfg.getProperties();

		ReflectionManager reflectionManager = cfg.getReflectionManager();
		globalCfg = new GlobalConfiguration( properties );
		RevisionInfoConfiguration revInfoCfg = new RevisionInfoConfiguration( globalCfg );
		RevisionInfoConfigurationResult revInfoCfgResult = revInfoCfg.configure( cfg, reflectionManager );
		auditEntCfg = new AuditEntitiesConfiguration( properties, revInfoCfgResult.getRevisionInfoEntityName() );
		auditProcessManager = new AuditProcessManager( revInfoCfgResult.getRevisionInfoGenerator() );
		revisionInfoQueryCreator = revInfoCfgResult.getRevisionInfoQueryCreator();
		revisionInfoNumberReader = revInfoCfgResult.getRevisionInfoNumberReader();
		modifiedEntityNamesReader = revInfoCfgResult.getModifiedEntityNamesReader();
		this.classLoaderService = classLoaderService;
		auditStrategy = initializeAuditStrategy(
				revInfoCfgResult.getRevisionInfoClass(),
				revInfoCfgResult.getRevisionInfoTimestampData()
		);
		entCfg = new EntitiesConfigurator().configure(
				cfg, reflectionManager, globalCfg, auditEntCfg, auditStrategy,
				revInfoCfgResult.getRevisionInfoXmlMapping(), revInfoCfgResult.getRevisionInfoRelationMapping()
		);
	}

	private AuditStrategy initializeAuditStrategy(Class<?> revisionInfoClass, PropertyData revisionInfoTimestampData) {
		AuditStrategy strategy;

		try {

			Class<?> auditStrategyClass = null;
			if ( classLoaderService != null ) {
				auditStrategyClass = classLoaderService.classForName( auditEntCfg.getAuditStrategyName() );
			}
			else {
				auditStrategyClass = Thread.currentThread()
						.getContextClassLoader()
						.loadClass( auditEntCfg.getAuditStrategyName() );
			}

			strategy = (AuditStrategy) auditStrategyClass.newInstance();
		}
		catch ( Exception e ) {
			throw new MappingException(
					String.format( "Unable to create AuditStrategy[%s] instance.", auditEntCfg.getAuditStrategyName() ),
					e
			);
		}

		if ( strategy instanceof ValidityAuditStrategy ) {
			// further initialization required
			Getter revisionTimestampGetter = ReflectionTools.getGetter( revisionInfoClass, revisionInfoTimestampData );
			( (ValidityAuditStrategy) strategy ).setRevisionTimestampGetter( revisionTimestampGetter );
		}

		return strategy;
	}

	//

	private static Map<Configuration, AuditConfiguration> cfgs
			= new WeakHashMap<Configuration, AuditConfiguration>();

	public synchronized static AuditConfiguration getFor(Configuration cfg) {
		return getFor( cfg, null );
	}

	public synchronized static AuditConfiguration getFor(Configuration cfg, ClassLoaderService classLoaderService) {
		AuditConfiguration verCfg = cfgs.get( cfg );

		if ( verCfg == null ) {
			verCfg = new AuditConfiguration( cfg, classLoaderService );
			cfgs.put( cfg, verCfg );

			cfg.buildMappings();
		}

		return verCfg;
	}
}
