/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.jpa.boot.spi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.boot.spi.CacheRegionDefinition;
import org.hibernate.boot.spi.JaccDefinition;
import org.hibernate.jaxb.spi.cfg.JaxbHibernateConfiguration;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.ValueHolder;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.service.BootstrapServiceRegistry;
import org.hibernate.service.ConfigLoader;

import static org.hibernate.jaxb.spi.cfg.JaxbHibernateConfiguration.JaxbSessionFactory.JaxbMapping;

/**
 * Helper class for handling building a unified map of "JPA settings".  Extracted to separate class so others can
 * leverage.
 *
 * @author Steve Ebersole
 */
public class JpaUnifiedSettingsBuilder {
	/**
	 * JPA settings can name one or more {@code cfg.xml} file to read for setting information.  {@code cfg.xml}
	 * files, however, can name things other than just settings (properties).  If can name metadata mappings,
	 * cache configurations, etc.  This interface collects those "other" artifacts for use by the caller of this
	 * helper so that the {@code cfg.xml} does not need to be parsed a second time to get that information later.
	 */
	public static interface CfgXmlMappingArtifacts {
		public List<JaxbMapping> getMappings();
		public List<CacheRegionDefinition> getCacheRegionDefinitions();
		public List<JaccDefinition> getJaccDefinitions();
	}

	/**
	 * Aggregated result of processing all pertinent JPA setting sources.
	 */
	public static interface Result {
		public Map<?, ?> getSettings();
		public CfgXmlMappingArtifacts getCfgXmlMappingArtifacts();
	}

	public static Result mergePropertySources(
			PersistenceUnitDescriptor persistenceUnit,
			Map integrationSettings,
			final BootstrapServiceRegistry bootstrapServiceRegistry) {
		ResultImpl result = new ResultImpl();
		result.consume( persistenceUnit, integrationSettings, bootstrapServiceRegistry );
		return result;
	}

	private static class CfgXmlMappingArtifactsImpl implements CfgXmlMappingArtifacts {
		private final List<JaxbMapping> mappings = new ArrayList<JaxbMapping>();
		private final List<CacheRegionDefinition> cacheRegionDefinitions = new ArrayList<CacheRegionDefinition>();
		private final List<JaccDefinition> jaccDefinitions = new ArrayList<JaccDefinition>();

		@Override
		public List<JaxbMapping> getMappings() {
			return mappings;
		}

		@Override
		public List<CacheRegionDefinition> getCacheRegionDefinitions() {
			return cacheRegionDefinitions;
		}

		@Override
		public List<JaccDefinition> getJaccDefinitions() {
			return jaccDefinitions;
		}
	}

	private static class ResultImpl implements Result {
		private final Map settings = new ConcurrentHashMap();
		private final CfgXmlMappingArtifactsImpl cfgXmlMappingArtifacts = new CfgXmlMappingArtifactsImpl();

		@Override
		public Map<?, ?> getSettings() {
			return settings;
		}

		@Override
		public CfgXmlMappingArtifacts getCfgXmlMappingArtifacts() {
			return cfgXmlMappingArtifacts;
		}


		@SuppressWarnings("unchecked")
		public void consume(
				PersistenceUnitDescriptor persistenceUnit,
				Map integrationSettings,
				final BootstrapServiceRegistry bootstrapServiceRegistry) {
			// first, apply persistence.xml-defined settings
			final Map merged = new HashMap();
			if ( persistenceUnit.getProperties() != null ) {
				merged.putAll( persistenceUnit.getProperties() );
			}

			merged.put( AvailableSettings.PERSISTENCE_UNIT_NAME, persistenceUnit.getName() );

			// see if the persistence.xml settings named a Hibernate config file....
			final ValueHolder<ConfigLoader> configLoaderHolder = new ValueHolder<ConfigLoader>(
					new ValueHolder.DeferredInitializer<ConfigLoader>() {
						@Override
						public ConfigLoader initialize() {
							return new ConfigLoader( bootstrapServiceRegistry );
						}
					}
			);

			{
				final String cfgXmlResourceName = (String) merged.remove( AvailableSettings.CFG_FILE );
				if ( StringHelper.isNotEmpty( cfgXmlResourceName ) ) {
					// it does, so load those properties
					JaxbHibernateConfiguration configurationElement = configLoaderHolder.getValue()
							.loadConfigXmlResource( cfgXmlResourceName );
					processHibernateConfigurationElement( configurationElement, merged );
				}
			}

			// see if integration settings named a Hibernate config file....
			{
				final String cfgXmlResourceName = (String) integrationSettings.get( AvailableSettings.CFG_FILE );
				if ( StringHelper.isNotEmpty( cfgXmlResourceName ) ) {
					integrationSettings.remove( AvailableSettings.CFG_FILE );
					// it does, so load those properties
					JaxbHibernateConfiguration configurationElement = configLoaderHolder.getValue().loadConfigXmlResource(
							cfgXmlResourceName
					);
					processHibernateConfigurationElement( configurationElement, merged );
				}
			}

			// finally, apply integration-supplied settings (per JPA spec, integration settings should override other sources)
			merged.putAll( integrationSettings );

			if ( ! merged.containsKey( AvailableSettings.VALIDATION_MODE ) ) {
				if ( persistenceUnit.getValidationMode() != null ) {
					merged.put( AvailableSettings.VALIDATION_MODE, persistenceUnit.getValidationMode() );
				}
			}

			if ( ! merged.containsKey( AvailableSettings.SHARED_CACHE_MODE ) ) {
				if ( persistenceUnit.getSharedCacheMode() != null ) {
					merged.put( AvailableSettings.SHARED_CACHE_MODE, persistenceUnit.getSharedCacheMode() );
				}
			}

			applyAllNonNull( merged );
		}

		private void applyAllNonNull(Map<?,?> values) {
			for ( Map.Entry entry : values.entrySet() ) {
				if ( entry.getValue() != null ) {
					settings.put( entry.getKey(), entry.getValue() );
				}
			}
		}

		@SuppressWarnings("unchecked")
		private void processHibernateConfigurationElement(
				JaxbHibernateConfiguration configurationElement,
				Map mergeMap) {
			if ( ! mergeMap.containsKey( org.hibernate.cfg.AvailableSettings.SESSION_FACTORY_NAME ) ) {
				String cfgName = configurationElement.getSessionFactory().getName();
				if ( cfgName != null ) {
					mergeMap.put( org.hibernate.cfg.AvailableSettings.SESSION_FACTORY_NAME, cfgName );
				}
			}

			for ( JaxbHibernateConfiguration.JaxbSessionFactory.JaxbProperty jaxbProperty : configurationElement.getSessionFactory().getProperty() ) {
				mergeMap.put( jaxbProperty.getName(), jaxbProperty.getValue() );
			}

			for ( JaxbMapping jaxbMapping : configurationElement.getSessionFactory().getMapping() ) {
				cfgXmlMappingArtifacts.mappings.add( jaxbMapping );
			}

			for ( Object cacheDeclaration : configurationElement.getSessionFactory().getClassCacheOrCollectionCache() ) {
				if ( JaxbHibernateConfiguration.JaxbSessionFactory.JaxbClassCache.class.isInstance( cacheDeclaration ) ) {
					final JaxbHibernateConfiguration.JaxbSessionFactory.JaxbClassCache jaxbClassCache
							= (JaxbHibernateConfiguration.JaxbSessionFactory.JaxbClassCache) cacheDeclaration;
					cfgXmlMappingArtifacts.cacheRegionDefinitions.add(
							new CacheRegionDefinition(
									CacheRegionDefinition.CacheRegionType.ENTITY,
									jaxbClassCache.getClazz(),
									jaxbClassCache.getUsage().value(),
									jaxbClassCache.getRegion(),
									"all".equals( jaxbClassCache.getInclude() )
							)
					);
				}
				else {
					final JaxbHibernateConfiguration.JaxbSessionFactory.JaxbCollectionCache jaxbCollectionCache
							= (JaxbHibernateConfiguration.JaxbSessionFactory.JaxbCollectionCache) cacheDeclaration;
					cfgXmlMappingArtifacts.cacheRegionDefinitions.add(
							new CacheRegionDefinition(
									CacheRegionDefinition.CacheRegionType.COLLECTION,
									jaxbCollectionCache.getCollection(),
									jaxbCollectionCache.getUsage().value(),
									jaxbCollectionCache.getRegion(),
									false
							)
					);
				}
			}

			if ( configurationElement.getSecurity() != null ) {
				final String contextId = configurationElement.getSecurity().getContext();
				for ( JaxbHibernateConfiguration.JaxbSecurity.JaxbGrant grant : configurationElement.getSecurity().getGrant() ) {
					cfgXmlMappingArtifacts.jaccDefinitions.add(
							new JaccDefinition(
									contextId,
									grant.getRole(),
									grant.getEntityName(),
									grant.getActions()
							)
					);
				}
			}
		}
	}

}
