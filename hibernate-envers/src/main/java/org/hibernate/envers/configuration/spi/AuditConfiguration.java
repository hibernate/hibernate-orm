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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import org.hibernate.MappingException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.envers.configuration.internal.AuditEntitiesConfiguration;
import org.hibernate.envers.configuration.internal.EntitiesConfigurator;
import org.hibernate.envers.configuration.internal.GlobalConfiguration;
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
import org.hibernate.metamodel.Metadata;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.AttributeBindingContainer;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.domain.Attribute;
import org.hibernate.metamodel.spi.domain.AttributeContainer;
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

	//public AuditConfiguration(Configuration cfg) {
	//	this( cfg, null );
	//}

	public AuditConfiguration(AuditConfigurationContext context) {
		this.globalCfg = context.getGlobalConfiguration();
		this.auditEntCfg = context.getAuditEntitiesConfiguration();

		this.auditProcessManager = new AuditProcessManager( context.getRevisionInfoConfigurationResult().getRevisionInfoGenerator() );
		this.revisionInfoQueryCreator = context.getRevisionInfoConfigurationResult().getRevisionInfoQueryCreator();
		this.revisionInfoNumberReader = context.getRevisionInfoConfigurationResult().getRevisionInfoNumberReader();
		this.modifiedEntityNamesReader = context.getRevisionInfoConfigurationResult().getModifiedEntityNamesReader();

		this.auditStrategy = initializeAuditStrategy(
				context.getRevisionInfoConfigurationResult().getRevisionInfoClass(),
				context.getRevisionInfoConfigurationResult().getRevisionInfoTimestampData()
		);
		this.entCfg = new EntitiesConfigurator().configure(
				context,
				auditStrategy,
				context.getRevisionInfoConfigurationResult().getRevisionInfoXmlMapping(),
				context.getRevisionInfoConfigurationResult().getRevisionInfoRelationMapping()
		);

	}

	private AuditStrategy initializeAuditStrategy(Class<?> revisionInfoClass, PropertyData revisionInfoTimestampData) {
		AuditStrategy strategy;

		try {
			Class<?> auditStrategyClass;
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

	private static final Map<UUID, AuditConfiguration> CFGS = new WeakHashMap<UUID, AuditConfiguration>();

	//public synchronized static AuditConfiguration register(MetadataImplementor metadata) {
	//	return register( metadata, null );
	//}

	public synchronized static AuditConfiguration register(
			AuditConfigurationContext context,  Metadata metadata) {
		AuditConfiguration verCfg = CFGS.get( metadata.getUUID() );

		if ( verCfg == null ) {
			verCfg = new AuditConfiguration( context );
			CFGS.put( metadata.getUUID(), verCfg );
		}

		return verCfg;
	}

	public void destroy() {
		synchronized (AuditConfiguration.class) {
			for ( Map.Entry<UUID, AuditConfiguration> c : new HashSet<Map.Entry<UUID, AuditConfiguration>>(
					CFGS.entrySet() ) ) {
				if ( c.getValue() == this ) { // this is nasty cleanup fix, whole static CFGS should be reworked
					CFGS.remove( c.getKey() );
				}
			}
		}
		classLoaderService = null;
	}

	public interface AuditConfigurationContext {
		//InFlightMetadataCollector metadataCollector,
		//AdditionalJaxbRootProducer.AdditionalJaxbRootProducerContext context,
		//GlobalConfiguration globalCfg,
		//AuditEntitiesConfiguration verEntCfg,
		//AuditStrategy auditStrategy,

		Metadata getMetadata();

		EntityBinding getEntityBinding(String entityName);

		EntityBinding getEntityBinding(ClassInfo classInfo);

		IndexView getJandexIndex();

		ClassInfo getClassInfo(String className);

		ClassInfo getClassInfo(DotName classDotName);

		ClassInfo getClassInfo(AttributeContainer attributeContainer);

		ClassLoaderService getClassLoaderService();

		<T> T getAnnotationProxy(AnnotationInstance annotationInstance, Class<T> annotationClass);

		Map<DotName, List<AnnotationInstance>> locateAttributeAnnotations(Attribute attribute);
		// return coreConfiguration.locateAttributeAnnotations;

		GlobalConfiguration getGlobalConfiguration();

		AuditEntitiesConfiguration getAuditEntitiesConfiguration();

		public RevisionInfoConfigurationResult getRevisionInfoConfigurationResult();

		void addDocument(org.w3c.dom.Document document);
	}
}
