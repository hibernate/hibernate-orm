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
import org.hibernate.boot.jaxb.cfg.spi.JaxbCfgEntityCacheType;
import org.hibernate.boot.jaxb.cfg.spi.JaxbCfgHibernateConfiguration;
import org.hibernate.event.spi.EventType;

import static org.hibernate.boot.BootLogging.BOOT_LOGGER;


/**
 * Models the information gleaned from parsing a {@code cfg.xml} file.
 * <p>
 * A {@link LoadedConfig} is built via {@link #consume}. An aggregated
 * representation can be maintained through calls to {@link #merge}.
 */
public class LoadedConfig {

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
		final var cfg = new LoadedConfig( jaxbCfg.getSessionFactory().getName() );

		for ( var jaxbProperty : jaxbCfg.getSessionFactory().getProperty() ) {
			cfg.addConfigurationValue( jaxbProperty.getName(), jaxbProperty.getValue() );
		}

		for ( var jaxbMapping : jaxbCfg.getSessionFactory().getMapping() ) {
			cfg.addMappingReference( MappingReference.consume( jaxbMapping ) );
		}

		for ( Object cacheDeclaration : jaxbCfg.getSessionFactory().getClassCacheOrCollectionCache() ) {
			cfg.addCacheRegionDefinition( parseCacheRegionDefinition( cacheDeclaration ) );
		}

		final var eventListeners = jaxbCfg.getSessionFactory().getListener();
		if ( !eventListeners.isEmpty() ) {
			for ( var listener : eventListeners ) {
				final var eventType = EventType.resolveEventTypeByName( listener.getType().value() );
				cfg.addEventListener( eventType, listener.getClazz() );
			}
		}

		final var listenerGroups = jaxbCfg.getSessionFactory().getEvent();
		if ( !listenerGroups.isEmpty() ) {
			for ( var listenerGroup : listenerGroups ) {
				if ( !listenerGroup.getListener().isEmpty() ) {
					final String eventTypeName = listenerGroup.getType().value();
					final var eventType = EventType.resolveEventTypeByName( eventTypeName );
					for ( var listener : listenerGroup.getListener() ) {
						final String listenerClassName = listener.getClazz();
						if ( listener.getType() != null ) {
							BOOT_LOGGER.listenerDefinedAlsoDefinedEventType( listenerClassName );
						}
						cfg.addEventListener( eventType, listenerClassName );
					}
				}
			}
		}

		return cfg;
	}

	private static String trim(String value) {
		return value == null ? null : value.trim();

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
		else if ( cacheDeclaration instanceof JaxbCfgCollectionCacheType jaxbCollectionCache ) {
			return new CacheRegionDefinition(
					CacheRegionDefinition.CacheRegionType.COLLECTION,
					jaxbCollectionCache.getCollection(),
					jaxbCollectionCache.getUsage().value(),
					jaxbCollectionCache.getRegion(),
					false
			);
		}
		else {
			throw new IllegalArgumentException( "Unrecognized cache declaration" );
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
		final String sessionFactoryName = incoming.getSessionFactoryName();
		if ( this.sessionFactoryName != null ) {
			if ( sessionFactoryName != null ) {
				BOOT_LOGGER.moreThanOneCfgXmlSuppliedSessionFactoryName(
						this.sessionFactoryName,
						sessionFactoryName,
						this.sessionFactoryName
				);
			}
		}
		else {
			this.sessionFactoryName = sessionFactoryName;
		}

		addConfigurationValues( incoming.getConfigurationValues() );
		addMappingReferences( incoming.getMappingReferences() );
		addCacheRegionDefinitions( incoming.getCacheRegionDefinitions() );
		addEventListeners( incoming.getEventListenerMap() );
	}

	protected void addConfigurationValues(Map<String,Object> configurationValues) {
		if ( configurationValues != null ) {
			this.configurationValues.putAll( configurationValues );
		}
	}

	private void addMappingReferences(List<MappingReference> mappingReferences) {
		if ( mappingReferences != null ) {
			if ( this.mappingReferences == null ) {
				this.mappingReferences = new ArrayList<>();
			}
			this.mappingReferences.addAll( mappingReferences );
		}
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
		if ( eventListenerMap != null ) {
			if ( this.eventListenerMap == null ) {
				this.eventListenerMap = new HashMap<>();
			}

			for ( var incomingEntry : eventListenerMap.entrySet() ) {
				var listenerClasses = this.eventListenerMap.get( incomingEntry.getKey() );
				if ( listenerClasses == null ) {
					listenerClasses = new HashSet<>();
					this.eventListenerMap.put( incomingEntry.getKey(), listenerClasses );
				}
				listenerClasses.addAll( incomingEntry.getValue() );
			}
		}
	}

	public static LoadedConfig baseline() {
		return new LoadedConfig( null );
	}
}
