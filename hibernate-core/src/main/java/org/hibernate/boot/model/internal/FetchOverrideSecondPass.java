/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.annotations.FetchProfile.FetchOverride;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.SecondPass;
import org.hibernate.mapping.FetchProfile;
import org.hibernate.mapping.PersistentClass;

/**
 * @author Hardy Ferentschik
 */
public class FetchOverrideSecondPass implements SecondPass {
	private final String fetchProfileName;
	private final FetchOverride fetch;
	private final MetadataBuildingContext buildingContext;

	public FetchOverrideSecondPass(
			String fetchProfileName,
			FetchOverride fetch,
			MetadataBuildingContext buildingContext) {
		this.fetchProfileName = fetchProfileName;
		this.fetch = fetch;
		this.buildingContext = buildingContext;
	}

	@Override
	public void doSecondPass(Map<String, PersistentClass> persistentClasses) throws MappingException {
		final Class<?> entityClassDetails = fetch.entity();
		final String attributeName = fetch.association();

		// throws MappingException in case the property does not exist
		buildingContext.getMetadataCollector()
				.getEntityBinding( entityClassDetails.getName() )
				.getProperty( attributeName );

		final FetchProfile profile = buildingContext.getMetadataCollector().getFetchProfile( fetchProfileName );
		// we already know that the FetchProfile exists and is good to use
		profile.addFetch( new FetchProfile.Fetch(
				entityClassDetails.getName(),
				attributeName,
				fetch.mode(),
				fetch.fetch()
		) );
	}
}
