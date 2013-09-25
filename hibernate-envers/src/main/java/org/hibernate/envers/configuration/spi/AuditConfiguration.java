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

import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;

import org.hibernate.MappingException;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cfg.Configuration;
import org.hibernate.envers.configuration.internal.AuditEntitiesConfiguration;
import org.hibernate.envers.configuration.internal.EntitiesConfigurator;
import org.hibernate.envers.configuration.internal.GlobalConfiguration;
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
import org.hibernate.internal.util.ClassLoaderHelper;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.property.Getter;

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
	private ClassLoaderService classLoaderService;

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

	public ClassLoaderService getClassLoaderService() {
		return classLoaderService;
	}

	public AuditConfiguration(Configuration cfg) {
		this( cfg, null );
	}

	public AuditConfiguration(Configuration cfg, ClassLoaderService classLoaderService) {
		// TODO: Temporarily allow Envers to continuing using
		// hibernate-commons-annotations' for reflection and class loading.
		final ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader( ClassLoaderHelper.getContextClassLoader() );

		final Properties properties = cfg.getProperties();

		final ReflectionManager reflectionManager = cfg.getReflectionManager();
		this.globalCfg = new GlobalConfiguration( properties, classLoaderService );
		final RevisionInfoConfiguration revInfoCfg = new RevisionInfoConfiguration( globalCfg );
		final RevisionInfoConfigurationResult revInfoCfgResult = revInfoCfg.configure( cfg, reflectionManager );
		this.auditEntCfg = new AuditEntitiesConfiguration( properties, revInfoCfgResult.getRevisionInfoEntityName() );
		this.auditProcessManager = new AuditProcessManager( revInfoCfgResult.getRevisionInfoGenerator() );
		this.revisionInfoQueryCreator = revInfoCfgResult.getRevisionInfoQueryCreator();
		this.revisionInfoNumberReader = revInfoCfgResult.getRevisionInfoNumberReader();
		this.modifiedEntityNamesReader = revInfoCfgResult.getModifiedEntityNamesReader();
		this.classLoaderService = classLoaderService;
		this.auditStrategy = initializeAuditStrategy(
				revInfoCfgResult.getRevisionInfoClass(),
				revInfoCfgResult.getRevisionInfoTimestampData()
		);
		this.entCfg = new EntitiesConfigurator().configure(
				cfg, reflectionManager, globalCfg, auditEntCfg, auditStrategy, classLoaderService,
				revInfoCfgResult.getRevisionInfoXmlMapping(), revInfoCfgResult.getRevisionInfoRelationMapping()
		);

		Thread.currentThread().setContextClassLoader( tccl );
	}

	private AuditStrategy initializeAuditStrategy(Class<?> revisionInfoClass, PropertyData revisionInfoTimestampData) {
		AuditStrategy strategy;

		try {
			Class<?> auditStrategyClass = null;
			try {
				auditStrategyClass = this.getClass().getClassLoader().loadClass( auditEntCfg.getAuditStrategyName() );
			}
			catch (Exception e) {
				auditStrategyClass = ReflectionTools.loadClass(
						auditEntCfg.getAuditStrategyName(),
						classLoaderService
				);
			}
			strategy = (AuditStrategy) ReflectHelper.getDefaultConstructor( auditStrategyClass ).newInstance();
		}
		catch (Exception e) {
			throw new MappingException(
					String.format( "Unable to create AuditStrategy[%s] instance.", auditEntCfg.getAuditStrategyName() ),
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

	private static final Map<Configuration, AuditConfiguration> CFGS = new WeakHashMap<Configuration, AuditConfiguration>();

	public synchronized static AuditConfiguration getFor(Configuration cfg) {
		return getFor( cfg, null );
	}

	public synchronized static AuditConfiguration getFor(Configuration cfg, ClassLoaderService classLoaderService) {
		AuditConfiguration verCfg = CFGS.get( cfg );

		if ( verCfg == null ) {
			verCfg = new AuditConfiguration( cfg, classLoaderService );
			CFGS.put( cfg, verCfg );

			cfg.buildMappings();
		}

		return verCfg;
	}

	public void destroy() {
		synchronized (AuditConfiguration.class) {
			for ( Map.Entry<Configuration, AuditConfiguration> c : new HashSet<Map.Entry<Configuration, AuditConfiguration>>(
					CFGS.entrySet() ) ) {
				if ( c.getValue() == this ) { // this is nasty cleanup fix, whole static CFGS should be reworked
					CFGS.remove( c.getKey() );
				}
			}
		}
		classLoaderService = null;
	}
}
