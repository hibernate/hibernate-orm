/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.boot.internal;

import org.hibernate.HibernateException;
import org.hibernate.boot.ResourceStreamLocator;
import org.hibernate.boot.spi.AdditionalMappingContributions;
import org.hibernate.boot.spi.AdditionalMappingContributor;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataBuildingOptions;
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
			ResourceStreamLocator resourceStreamLocator,
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

		enversService.initialize( metadata, contributions::contributeBinding, contributions.getEffectiveMappingDefaults() );
	}
}
