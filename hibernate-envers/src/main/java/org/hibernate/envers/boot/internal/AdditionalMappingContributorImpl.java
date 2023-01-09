/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.envers.boot.internal;

import org.hibernate.HibernateException;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.spi.AdditionalMappingContributions;
import org.hibernate.boot.spi.AdditionalMappingContributor;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.envers.configuration.internal.MappingCollector;
import org.hibernate.service.ServiceRegistry;

import static org.hibernate.cfg.AvailableSettings.XML_MAPPING_ENABLED;

/**
 * @author Steve Ebersole
 */
public class AdditionalMappingContributorImpl implements AdditionalMappingContributor {
	@Override
	public String getContributorName() {
		return "envers";
	}

	@Override
	public void contribute(
			AdditionalMappingContributions contributions,
			InFlightMetadataCollector metadata,
			MappingBinder jaxbBinder,
			MetadataBuildingContext buildingContext) {
		final MetadataBuildingOptions metadataBuildingOptions = metadata.getMetadataBuildingOptions();
		final ServiceRegistry serviceRegistry = metadataBuildingOptions.getServiceRegistry();
		final EnversService enversService = serviceRegistry.getService( EnversService.class );

		if ( !enversService.isEnabled() ) {
			// short-circuit if envers integration has been disabled.
			return;
		}

		if ( !metadataBuildingOptions.isXmlMappingEnabled() ) {
			throw new HibernateException( "Hibernate Envers currently requires XML mapping to be enabled."
					+ " Please don't disable setting `" + XML_MAPPING_ENABLED
					+ "`; alternatively disable Hibernate Envers." );
		}

		final MappingCollector mappingCollector = new MappingCollector() {
			private final Origin origin = new Origin( SourceType.OTHER, "envers" );

			@Override
			public void addDocument(JaxbHbmHibernateMapping mapping) {
				contributions.contributeBinding( mapping, origin );
			}
		};

		enversService.initialize( metadata, mappingCollector );
	}
}
