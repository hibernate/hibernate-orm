/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.internal;

import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.type.spi.TypeConfiguration;

import org.jboss.logging.Logger;

/**
 * A Registry of TypeConfiguration references based on the
 * TypeConfiguration's UUID.
 *
 * @author Steve Ebersole
 */
public class TypeConfigurationRegistry {
	private static final Logger LOG = Logger.getLogger( TypeConfigurationRegistry.class );

	/**
	 * Singleton access
	 */
	public static final TypeConfigurationRegistry INSTANCE = new TypeConfigurationRegistry();

	private TypeConfigurationRegistry() {
	}

	private ConcurrentHashMap<String,TypeConfiguration> configurationMap;

	public void registerTypeConfiguration(TypeConfiguration typeConfiguration) {
		if ( configurationMap == null ) {
			configurationMap = new ConcurrentHashMap<>();
		}
		configurationMap.put( typeConfiguration.getUuid(), typeConfiguration );
	}

	public TypeConfiguration findTypeConfiguration(String uuid) {
		if ( configurationMap == null ) {
			return null;
		}

		return configurationMap.get( uuid );
	}

	public void deregisterTypeConfiguration(TypeConfiguration typeConfiguration) {
		final TypeConfiguration existing = configurationMap.remove( typeConfiguration.getUuid() );
		if ( existing != typeConfiguration ) {
			LOG.debugf(
					"Different TypeConfiguration [%s] passed to #deregisterTypeConfiguration than previously registered [%s] under that UUID [%s]",
					typeConfiguration,
					existing,
					typeConfiguration.getUuid()
			);
		}
	}
}
