/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.contributed;

import java.io.InputStream;

import org.hibernate.boot.ResourceStreamLocator;
import org.hibernate.boot.archive.internal.RepeatableInputStreamAccess;
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
		final String resourceLocation = "org/hibernate/orm/test/mapping/contributed/BasicContributorTests.hbm.xml";
		final Origin origin = new Origin( SourceType.OTHER, "test" );

		final ClassLoaderService classLoaderService = buildingContext.getBootstrapContext()
				.getServiceRegistry()
				.getService( ClassLoaderService.class );
		final InputStream inputStream = classLoaderService.locateResourceStream(
				resourceLocation );

		final MappingBinder mappingBinder = new MappingBinder( buildingContext.getBootstrapContext().getServiceRegistry() );
		final Binding<JaxbHbmHibernateMapping> jaxbBinding = mappingBinder.bind( new RepeatableInputStreamAccess( resourceLocation, inputStream), origin );
		final JaxbHbmHibernateMapping jaxbRoot = jaxbBinding.getRoot();

		contributions.contributeBinding( jaxbRoot );
	}

}
