/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.internal;

import org.hibernate.MappingException;
import org.hibernate.annotations.FetchProfile.FetchOverride;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.SecondPass;
import org.hibernate.mapping.FetchProfile;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.models.spi.AnnotationUsage;
import org.hibernate.models.spi.ClassDetails;

import java.util.Map;

/**
 * @author Hardy Ferentschik
 */
public class FetchOverrideSecondPass implements SecondPass {
	private final String fetchProfileName;
	private final AnnotationUsage<FetchOverride> fetch;
	private final MetadataBuildingContext buildingContext;

	public FetchOverrideSecondPass(
			String fetchProfileName,
			AnnotationUsage<FetchOverride> fetch,
			MetadataBuildingContext buildingContext) {
		this.fetchProfileName = fetchProfileName;
		this.fetch = fetch;
		this.buildingContext = buildingContext;
	}

	@Override
	public void doSecondPass(Map<String, PersistentClass> persistentClasses) throws MappingException {
		final ClassDetails entityClassDetails = fetch.getClassDetails( "entity" );
		final String attributeName = fetch.getString( "association" );

		// throws MappingException in case the property does not exist
		buildingContext.getMetadataCollector()
				.getEntityBinding( entityClassDetails.getName() )
				.getProperty( attributeName );

		final FetchProfile profile = buildingContext.getMetadataCollector().getFetchProfile( fetchProfileName );
		// we already know that the FetchProfile exists and is good to use
		profile.addFetch( new FetchProfile.Fetch(
				entityClassDetails.getName(),
				attributeName,
				fetch.getEnum( "mode" ),
				fetch.getEnum( "fetch" )
		) );
	}
}
