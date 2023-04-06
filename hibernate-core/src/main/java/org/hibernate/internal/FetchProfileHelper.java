/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal;

import org.hibernate.HibernateException;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.profile.Association;
import org.hibernate.engine.profile.Fetch;
import org.hibernate.engine.profile.FetchProfile;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.type.Type;

import java.util.HashMap;
import java.util.Map;

/**
 * Create {@link FetchProfile} references from {@link org.hibernate.mapping.FetchProfile} references
 *
 * @author Gavin King
 */
public class FetchProfileHelper {

	public static Map<String, FetchProfile> getFetchProfiles(
			MetadataImplementor bootMetamodel,
			MappingMetamodel mappingMetamodel) {
		final Map<String, FetchProfile> fetchProfiles = new HashMap<>();
		for ( org.hibernate.mapping.FetchProfile mappingProfile : bootMetamodel.getFetchProfiles() ) {
			final FetchProfile fetchProfile = createFetchProfile( mappingMetamodel, mappingProfile );
			fetchProfiles.put( fetchProfile.getName(), fetchProfile );
		}
		return fetchProfiles;
	}

	private static FetchProfile createFetchProfile(
			MappingMetamodel mappingMetamodel,
			org.hibernate.mapping.FetchProfile mappingProfile) {
		final FetchProfile fetchProfile = new FetchProfile( mappingProfile.getName() );
		for ( org.hibernate.mapping.FetchProfile.Fetch mappingFetch : mappingProfile.getFetches() ) {
			// resolve the persister owning the fetch
			final EntityPersister owner = getEntityPersister( mappingMetamodel, fetchProfile, mappingFetch );

			// validate the specified association fetch
			final Type associationType = owner.getPropertyType( mappingFetch.getAssociation() );
			if ( associationType == null || !associationType.isAssociationType() ) {
				throw new HibernateException( "Fetch profile [" + fetchProfile.getName()
						+ "] specified an association that does not exist [" + mappingFetch.getAssociation() + "]" );
			}

			// resolve the style
			final Fetch.Style fetchStyle = Fetch.Style.parse( mappingFetch.getStyle() );

			// then construct the fetch instance...
			fetchProfile.addFetch( new Association( owner, mappingFetch.getAssociation() ), fetchStyle );
			((Loadable) owner).registerAffectingFetchProfile( fetchProfile.getName() );
		}
		return fetchProfile;
	}

	private static EntityPersister getEntityPersister(
			MappingMetamodel mappingMetamodel,
			FetchProfile fetchProfile,
			org.hibernate.mapping.FetchProfile.Fetch mappingFetch) {
		final String entityName = mappingMetamodel.getImportedName( mappingFetch.getEntity() );
		if ( entityName != null ) {
			EntityPersister persister = mappingMetamodel.getEntityDescriptor( entityName );
			if ( persister != null ) {
				return persister;
			}
		}
		throw new HibernateException( "Unable to resolve entity reference [" + mappingFetch.getEntity()
				+ "] in fetch profile [" + fetchProfile.getName() + "]" );
	}

}
