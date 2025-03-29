/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.cfgxml.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.boot.CacheRegionDefinition;
import org.hibernate.boot.jaxb.cfg.spi.JaxbCfgCollectionCacheType;
import org.hibernate.boot.jaxb.cfg.spi.JaxbCfgConfigPropertyType;
import org.hibernate.boot.jaxb.cfg.spi.JaxbCfgEntityCacheType;
import org.hibernate.boot.jaxb.cfg.spi.JaxbCfgEventListenerGroupType;
import org.hibernate.boot.jaxb.cfg.spi.JaxbCfgEventListenerType;
import org.hibernate.boot.jaxb.cfg.spi.JaxbCfgHibernateConfiguration;
import org.hibernate.boot.jaxb.cfg.spi.JaxbCfgMappingReferenceType;
import org.hibernate.event.spi.EventType;

import org.jboss.logging.Logger;

/**
 * Models the information gleaned from parsing a {@code cfg.xml} file.
 * <p>
 * A {@link LoadedConfig} is built via {@link #consume}. An aggregated
 * representation can be maintained through calls to {@link #merge}.
 */
public class LoadedConfig {
	private static final Logger log = Logger.getLogger( LoadedConfig.class );

	private String sessionFactoryName;

	private final Map<String,Object> configurationValues = new ConcurrentHashMap<>( 16, 0.75f, 1 );

	private List<CacheRegionDefinition> cacheRegionDefinitions;
	private List<MappingReference> mappingReferences;
	private Map<EventType<?>,Set<String>> eventListenerMap;

	public LoadedConfig(String sessionFactoryName) {
		this.sessionFactoryName = sessionFactoryName;
	}

	public String getSessionFactoryName() {
		return sessionFactoryName;
	}

	public Map<String,Object> getConfigurationValues() {
		return configurationValues;
	}

	public List<CacheRegionDefinition> getCacheRegionDefinitions() {
		return cacheRegionDefinitions == null ? Collections.emptyList() : cacheRegionDefinitions;
	}

	public List<MappingReference> getMappingReferences() {
		return mappingReferences == null ? Collections.emptyList() : mappingReferences;
	}

	public Map<EventType<?>, Set<String>> getEventListenerMap() {
		return eventListenerMap == null ? Collections.emptyMap() : eventListenerMap;
	}

	/**
	 * Consumes the JAXB representation of a {@code cfg.xml} file and builds the
	 * LoadedConfig representation.
	 *
	 * @param jaxbCfg The JAXB representation of a {@code cfg.xml} file
	 *
	 * @return The parsed representation
	 */
	public static LoadedConfig consume(JaxbCfgHibernateConfiguration jaxbCfg) {
		final LoadedConfig cfg = new LoadedConfig( jaxbCfg.getSessionFactory().getName() );

		for ( JaxbCfgConfigPropertyType jaxbProperty : jaxbCfg.getSessionFactory().getProperty() ) {
			cfg.addConfigurationValue( jaxbProperty.getName(), jaxbProperty.getValue() );
		}

		for ( JaxbCfgMappingReferenceType jaxbMapping : jaxbCfg.getSessionFactory().getMapping() ) {
			cfg.addMappingReference( MappingReference.consume( jaxbMapping ) );
		}

		for ( Object cacheDeclaration : jaxbCfg.getSessionFactory().getClassCacheOrCollectionCache() ) {
			cfg.addCacheRegionDefinition( parseCacheRegionDefinition( cacheDeclaration ) );
		}

		if ( !jaxbCfg.getSessionFactory().getListener().isEmpty() ) {
			for ( JaxbCfgEventListenerType listener : jaxbCfg.getSessionFactory().getListener() ) {
				final EventType<?> eventType = EventType.resolveEventTypeByName( listener.getType().value() );
				cfg.addEventListener( eventType, listener.getClazz() );
			}
		}

		if ( !jaxbCfg.getSessionFactory().getEvent().isEmpty() ) {
			for ( JaxbCfgEventListenerGroupType listenerGroup : jaxbCfg.getSessionFactory().getEvent() ) {
				if ( listenerGroup.getListener().isEmpty() ) {
					continue;
				}

				final String eventTypeName = listenerGroup.getType().value();
				final EventType<?> eventType = EventType.resolveEventTypeByName( eventTypeName );

				for ( JaxbCfgEventListenerType listener : listenerGroup.getListener() ) {
					if ( listener.getType() != null ) {
						log.debugf( "Listener [%s] defined as part of a group also defined event type", listener.getClazz() );
					}
					cfg.addEventListener( eventType, listener.getClazz() );
				}
			}
		}

		return cfg;
	}

	private static String trim(String value) {
		if ( value == null ) {
			return null;
		}

		return value.trim();
	}

	private void addConfigurationValue(String propertyName, String value) {
		value = trim( value );
		configurationValues.put( propertyName, value );

		if ( !propertyName.startsWith( "hibernate." ) ) {
			configurationValues.put( "hibernate." + propertyName, value );
		}
	}

	private void addMappingReference(MappingReference mappingReference) {
		if ( mappingReferences == null ) {
			mappingReferences =  new ArrayList<>();
		}

		mappingReferences.add( mappingReference );
	}

	private static CacheRegionDefinition parseCacheRegionDefinition(Object cacheDeclaration) {
		if ( cacheDeclaration instanceof JaxbCfgEntityCacheType jaxbClassCache ) {
			return new CacheRegionDefinition(
					CacheRegionDefinition.CacheRegionType.ENTITY,
					jaxbClassCache.getClazz(),
					jaxbClassCache.getUsage().value(),
					jaxbClassCache.getRegion(),
					"all".equals( jaxbClassCache.getInclude() )
			);
		}
		else {
			final JaxbCfgCollectionCacheType jaxbCollectionCache = (JaxbCfgCollectionCacheType) cacheDeclaration;
			return new CacheRegionDefinition(
					CacheRegionDefinition.CacheRegionType.COLLECTION,
					jaxbCollectionCache.getCollection(),
					jaxbCollectionCache.getUsage().value(),
					jaxbCollectionCache.getRegion(),
					false
			);
		}
	}

	public void addCacheRegionDefinition(CacheRegionDefinition cacheRegionDefinition) {
		if ( cacheRegionDefinitions == null ) {
			cacheRegionDefinitions = new ArrayList<>();
		}
		cacheRegionDefinitions.add( cacheRegionDefinition );
	}

	public void addEventListener(EventType<?> eventType, String listenerClass) {
		if ( eventListenerMap == null ) {
			eventListenerMap = new HashMap<>();
		}

		Set<String> listenerClasses = eventListenerMap.get( eventType );
		if ( listenerClasses == null ) {
			listenerClasses = new HashSet<>();
			eventListenerMap.put( eventType, listenerClasses );
		}

		listenerClasses.add( listenerClass );
	}

	/**
	 * Merge information from loaded a {@code cfg.xml} represented by the incoming parameter
	 * into this LoadedConfig representation
	 *
	 * @param incoming The incoming config information to merge in.
	 */
	public void merge(LoadedConfig incoming) {
		if ( sessionFactoryName != null ) {
			if ( incoming.getSessionFactoryName() != null ) {
				log.debugf(
						"More than one cfg.xml file attempted to supply SessionFactory name: [%s], [%s].  Keeping initially discovered one [%s]",
						getSessionFactoryName(),
						incoming.getSessionFactoryName(),
						getSessionFactoryName()
				);
			}
		}
		else {
			sessionFactoryName = incoming.getSessionFactoryName();
		}

		addConfigurationValues( incoming.getConfigurationValues() );
		addMappingReferences( incoming.getMappingReferences() );
		addCacheRegionDefinitions( incoming.getCacheRegionDefinitions() );
		addEventListeners( incoming.getEventListenerMap() );
	}

	protected void addConfigurationValues(Map<String,Object> configurationValues) {
		if ( configurationValues == null ) {
			return;
		}

		this.configurationValues.putAll( configurationValues );
	}

	private void addMappingReferences(List<MappingReference> mappingReferences) {
		if ( mappingReferences == null ) {
			return;
		}

		if ( this.mappingReferences == null ) {
			this.mappingReferences =  new ArrayList<>();
		}
		this.mappingReferences.addAll( mappingReferences );
	}

	private void addCacheRegionDefinitions(List<CacheRegionDefinition> cacheRegionDefinitions) {
		if ( cacheRegionDefinitions == null ) {
			return;
		}

		if ( this.cacheRegionDefinitions == null ) {
			this.cacheRegionDefinitions = new ArrayList<>();
		}
		this.cacheRegionDefinitions.addAll( cacheRegionDefinitions );
	}

	private void addEventListeners(Map<EventType<?>, Set<String>> eventListenerMap) {
		if ( eventListenerMap == null ) {
			return;
		}

		if ( this.eventListenerMap == null ) {
			this.eventListenerMap = new HashMap<>();
		}

		for ( Map.Entry<EventType<?>, Set<String>> incomingEntry : eventListenerMap.entrySet() ) {
			Set<String> listenerClasses = this.eventListenerMap.get( incomingEntry.getKey() );
			if ( listenerClasses == null ) {
				listenerClasses = new HashSet<>();
				this.eventListenerMap.put( incomingEntry.getKey(), listenerClasses );
			}
			listenerClasses.addAll( incomingEntry.getValue() );
		}
	}

	public static LoadedConfig baseline() {
		return new LoadedConfig( null );
	}
}
