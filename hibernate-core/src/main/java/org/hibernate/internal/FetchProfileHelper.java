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
import org.hibernate.engine.profile.internal.FetchProfileAffectee;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;

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
		final String profileName = mappingProfile.getName();
		final FetchProfile fetchProfile = new FetchProfile( profileName );

		for ( org.hibernate.mapping.FetchProfile.Fetch mappingFetch : mappingProfile.getFetches() ) {
			// resolve the persister owning the fetch
			final EntityPersister owner = getEntityPersister( mappingMetamodel, fetchProfile, mappingFetch );
			( (FetchProfileAffectee) owner ).registerAffectingFetchProfile( profileName, null );

			final Association association = new Association( owner, mappingFetch.getAssociation() );
			final Fetch.Style fetchStyle = Fetch.Style.parse( mappingFetch.getStyle() );

			// validate the specified association fetch
			final ModelPart fetchablePart = owner.findByPath( association.getAssociationPath() );
			validateFetchablePart( fetchablePart, profileName, association );
			if ( fetchablePart instanceof FetchProfileAffectee ) {
				( (FetchProfileAffectee) fetchablePart ).registerAffectingFetchProfile( profileName, fetchStyle );
			}

			// then register the association with the FetchProfile
			fetchProfile.addFetch( new Fetch( association, fetchStyle ) );
		}
		return fetchProfile;
	}

	private static void validateFetchablePart(ModelPart fetchablePart, String profileName, Association association) {
		if ( fetchablePart == null ) {
			throw new HibernateException( String.format(
					"Fetch profile [%s] specified an association that does not exist - %s",
					profileName,
					association.getRole()
			) );
		}

		if ( !isAssociation( fetchablePart ) ) {
			throw new HibernateException( String.format(
					"Fetch profile [%s] specified an association that is not an association - %s",
					profileName,
					association.getRole()
			) );
		}
	}

	private static boolean isAssociation(ModelPart fetchablePart) {
		return fetchablePart instanceof EntityValuedModelPart
				|| fetchablePart instanceof PluralAttributeMapping;
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
