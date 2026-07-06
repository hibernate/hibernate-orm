/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.metadata;

import org.hibernate.HibernateException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.pipeline.internal.BootstrapPipeline;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.jpa.boot.spi.ProviderChecker;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;

import java.util.Collection;
import java.util.Properties;

public class JpaMetadataDescriptor implements MetadataDescriptor {

	private Properties properties = new Properties();
	private Metadata metadata = null;

	public JpaMetadataDescriptor(
			final String persistenceUnit,
			final Properties properties) {
		final var integrationSettings = properties == null ? new Properties() : properties;
		final var persistenceUnitDescriptor = locatePersistenceUnit( persistenceUnit, integrationSettings );
		final var metadataResolution = BootstrapPipeline.resolveMetadata( persistenceUnitDescriptor, integrationSettings );
		metadata = metadataResolution.metadata();
		this.properties.putAll( metadataResolution.configurationValues() );
	}

	public Metadata createMetadata() {
		return metadata;
	}

	public Properties getProperties() {
		return properties;
	}

	private static class PersistenceProvider extends HibernatePersistenceProvider {
		public PersistenceUnitDescriptor getPersistenceUnitDescriptor(
				String persistenceUnit,
				Properties properties) {
			final Collection<PersistenceUnitDescriptor> persistenceUnitDescriptors =
					locatePersistenceUnits( properties, null, null );
			if ( persistenceUnit == null && persistenceUnitDescriptors.size() > 1 ) {
				throw new HibernateException( "No persistence unit name provided and multiple persistence units found." );
			}
			for ( var persistenceUnitDescriptor : persistenceUnitDescriptors ) {
				if ( ( persistenceUnit == null || persistenceUnitDescriptor.getName().equals( persistenceUnit ) )
						&& ProviderChecker.isProvider( persistenceUnitDescriptor, properties ) ) {
					return persistenceUnitDescriptor;
				}
			}
			throw new HibernateException( "Persistence unit not found: '" + persistenceUnit + "'." );
		}
	}

	private static PersistenceUnitDescriptor locatePersistenceUnit(
			final String persistenceUnit,
			final Properties properties) {
		return new PersistenceProvider().getPersistenceUnitDescriptor(
				persistenceUnit,
				properties);
	}

}
