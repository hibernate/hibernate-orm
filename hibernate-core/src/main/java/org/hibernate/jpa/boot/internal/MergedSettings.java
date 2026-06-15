/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.boot.internal;

import org.hibernate.boot.CacheRegionDefinition;
import org.hibernate.cfg.Environment;
import org.hibernate.internal.util.PropertiesHelper;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.hibernate.cfg.PersistenceSettings.PERSISTENCE_UNIT_NAME;

public class MergedSettings {
	private final Map<String, Object> configurationValues =
			new ConcurrentHashMap<>(16, 0.75f, 1);

	private List<CacheRegionDefinition> cacheRegionDefinitions;

	/**
	 * {@code MergedSettings} is initialized with {@code hibernate.properties}
	 */
	MergedSettings() {
		getConfigurationValues().putAll( PropertiesHelper.map( Environment.getProperties() ) );
	}

	List<CacheRegionDefinition> getCacheRegionDefinitions() {
		return cacheRegionDefinitions;
	}

	void processPersistenceUnitDescriptorProperties(PersistenceUnitDescriptor persistenceUnit) {
		final var properties = persistenceUnit.getProperties();
		if ( properties != null ) {
			getConfigurationValues().putAll( PropertiesHelper.map( properties ) );
		}
		getConfigurationValues().put( PERSISTENCE_UNIT_NAME, persistenceUnit.getName() );
	}

	public Map<String, Object> getConfigurationValues() {
		return configurationValues;
	}

	void addCacheRegionDefinition(CacheRegionDefinition cacheRegionDefinition) {
		if ( cacheRegionDefinitions == null ) {
			cacheRegionDefinitions = new ArrayList<>();
		}
		cacheRegionDefinitions.add( cacheRegionDefinition );
	}
}
