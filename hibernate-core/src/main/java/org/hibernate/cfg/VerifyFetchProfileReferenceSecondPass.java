/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;
import java.util.Locale;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.annotations.FetchProfile;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.MetadataSource;
import org.hibernate.mapping.PersistentClass;

/**
 * @author Hardy Ferentschik
 */
public class VerifyFetchProfileReferenceSecondPass implements SecondPass {
	private String fetchProfileName;
	private FetchProfile.FetchOverride fetch;
	private MetadataBuildingContext buildingContext;

	public VerifyFetchProfileReferenceSecondPass(
			String fetchProfileName,
			FetchProfile.FetchOverride fetch,
			MetadataBuildingContext buildingContext) {
		this.fetchProfileName = fetchProfileName;
		this.fetch = fetch;
		this.buildingContext = buildingContext;
	}

	public void doSecondPass(Map persistentClasses) throws MappingException {
		org.hibernate.mapping.FetchProfile profile = buildingContext.getMetadataCollector().getFetchProfile(
				fetchProfileName
		);
		if ( profile != null ) {
			if ( profile.getSource() != MetadataSource.ANNOTATIONS ) {
				return;
			}
		}
		else {
			profile = new org.hibernate.mapping.FetchProfile( fetchProfileName, MetadataSource.ANNOTATIONS );
			buildingContext.getMetadataCollector().addFetchProfile( profile );
		}

		PersistentClass clazz = buildingContext.getMetadataCollector().getEntityBinding( fetch.entity().getName() );
		// throws MappingException in case the property does not exist
		clazz.getProperty( fetch.association() );

		profile.addFetch(
				fetch.entity().getName(), fetch.association(), fetch.mode().toString().toLowerCase(Locale.ROOT)
		);
	}
}
