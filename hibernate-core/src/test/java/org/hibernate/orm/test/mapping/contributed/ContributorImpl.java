/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.contributed;

import java.io.InputStream;

import org.hibernate.boot.ResourceStreamLocator;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.AdditionalMappingContributions;
import org.hibernate.boot.spi.AdditionalMappingContributor;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;

/**
 * @author Steve Ebersole
 */
public class ContributorImpl implements AdditionalMappingContributor {
	public ContributorImpl() {
	}

	@Override
	public String getContributorName() {
		return "test";
	}

	@Override
	public void contribute(
			AdditionalMappingContributions contributions,
			InFlightMetadataCollector metadata,
			ResourceStreamLocator resourceStreamLocator,
			MetadataBuildingContext buildingContext) {
		final Origin origin = new Origin( SourceType.OTHER, "test" );

		final ClassLoaderService classLoaderService = buildingContext.getBootstrapContext()
				.getServiceRegistry()
				.getService( ClassLoaderService.class );
		final InputStream inputStream = classLoaderService.locateResourceStream(
				"org/hibernate/orm/test/mapping/contributed/BasicContributorTests.hbm.xml" );

		final MappingBinder mappingBinder = new MappingBinder( buildingContext.getBootstrapContext().getServiceRegistry() );
		final Binding<JaxbHbmHibernateMapping> jaxbBinding = mappingBinder.bind( inputStream, origin );
		final JaxbHbmHibernateMapping jaxbRoot = jaxbBinding.getRoot();

		contributions.contributeBinding( jaxbRoot );
	}

}
