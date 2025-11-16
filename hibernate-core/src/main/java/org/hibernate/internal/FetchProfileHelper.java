/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import org.hibernate.HibernateException;
import org.hibernate.annotations.FetchMode;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.profile.Association;
import org.hibernate.engine.profile.DefaultFetchProfile;
import org.hibernate.engine.profile.Fetch;
import org.hibernate.engine.profile.FetchProfile;
import org.hibernate.engine.profile.internal.FetchProfileAffectee;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.RuntimeMetamodels;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;

import java.util.HashMap;
import java.util.Map;

import static org.hibernate.engine.profile.DefaultFetchProfile.HIBERNATE_DEFAULT_PROFILE;

/**
 * Create {@link FetchProfile} references from {@link org.hibernate.mapping.FetchProfile} references
 *
 * @author Gavin King
 */
class FetchProfileHelper {

	@SuppressWarnings("unused")
	public static Map<String, FetchProfile> getFetchProfiles(
			MetadataImplementor bootMetamodel,
			RuntimeMetamodels runtimeMetamodels) {
		final Map<String, FetchProfile> fetchProfiles = new HashMap<>();
		addFetchProfiles( bootMetamodel, runtimeMetamodels, fetchProfiles );
		return fetchProfiles;
	}

	static void addFetchProfiles(
			MetadataImplementor bootMetamodel,
			RuntimeMetamodels runtimeMetamodels,
			Map<String, FetchProfile> fetchProfiles) {
		final MappingMetamodel mappingMetamodel = runtimeMetamodels.getMappingMetamodel();
		for ( var mappingProfile : bootMetamodel.getFetchProfiles() ) {
			final var fetchProfile = createFetchProfile( mappingMetamodel, mappingProfile );
			fetchProfiles.put( fetchProfile.getName(), fetchProfile );
		}
		fetchProfiles.put( HIBERNATE_DEFAULT_PROFILE, new DefaultFetchProfile( mappingMetamodel ) );
	}

	private static FetchProfile createFetchProfile(
			MappingMetamodel mappingMetamodel,
			org.hibernate.mapping.FetchProfile mappingProfile) {
		final String profileName = mappingProfile.getName();
		final var fetchProfile = new FetchProfile( profileName );
		for ( var mappingFetch : mappingProfile.getFetches() ) {
			// resolve the persister owning the fetch
			final var owner = getEntityPersister( mappingMetamodel, fetchProfile, mappingFetch );
			if ( owner instanceof FetchProfileAffectee fetchProfileAffectee ) {
				fetchProfileAffectee.registerAffectingFetchProfile( profileName );
			}

			final var association = new Association( owner, mappingFetch.getAssociation() );
			final var fetchStyle = fetchStyle( mappingFetch.getMethod() );
			final var fetchTiming = FetchTiming.forType( mappingFetch.getType() );

			// validate the specified association fetch
			final var fetchablePart = owner.findByPath( association.getAssociationPath() );
			validateFetchablePart( fetchablePart, profileName, association );
			if ( fetchablePart instanceof FetchProfileAffectee fetchProfileAffectee ) {
				fetchProfileAffectee.registerAffectingFetchProfile( profileName );
			}

			// then register the association with the FetchProfile
			fetchProfile.addFetch( new Fetch( association, fetchStyle, fetchTiming ) );
		}
		return fetchProfile;
	}

	private static FetchStyle fetchStyle(FetchMode fetchMode) {
		return switch ( fetchMode ) {
			case JOIN -> FetchStyle.JOIN;
			case SELECT -> FetchStyle.SELECT;
			case SUBSELECT -> FetchStyle.SUBSELECT;
		};
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
			final var persister = mappingMetamodel.getEntityDescriptor( entityName );
			if ( persister != null ) {
				return persister;
			}
		}
		throw new HibernateException( "Unable to resolve entity reference [" + mappingFetch.getEntity()
				+ "] in fetch profile [" + fetchProfile.getName() + "]" );
	}

}
