/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.internal.hbm;

import org.hibernate.annotations.FetchMode;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFetchProfileType;
import org.hibernate.mapping.FetchProfile;
import org.hibernate.mapping.MetadataSource;

import org.jboss.logging.Logger;

import static jakarta.persistence.FetchType.EAGER;

/**
 * @author Steve Ebersole
 */
public class FetchProfileBinder {
	private static final Logger log = Logger.getLogger( FetchProfileBinder.class );

	/**
	 * Handling for a {@code <fetch-profile/>} declaration.
	 * <p>
	 * This form handles fetch profiles defined at the {@code <hibernate-mapping/>}
	 * root.  For handling of fetch profiles defined within an entity, see
	 * {@link #processFetchProfile(HbmLocalMetadataBuildingContext, JaxbHbmFetchProfileType, String)}
	 *
	 * @param context Access to information relative to the mapping document containing this binding
	 * @param fetchProfileBinding The {@code <fetch-profile/>} binding
	 */
	public static void processFetchProfile(
			HbmLocalMetadataBuildingContext context,
			JaxbHbmFetchProfileType fetchProfileBinding) {
		processFetchProfile( context, fetchProfileBinding, null );
	}


	/**
	 * Handling for a {@code <fetch-profile/>} declaration.
	 *
	 * @param context Access to information relative to the mapping document containing this binding
	 * @param fetchProfileBinding The {@code <fetch-profile/>} binding
	 * @param containingEntityName The name of the entity containing the fetch profile declaration.  May
	 * be {@code null} to indicate a fetch profile defined at the root.
	 */
	public static void processFetchProfile(
			HbmLocalMetadataBuildingContext context,
			JaxbHbmFetchProfileType fetchProfileBinding,
			String containingEntityName) {
		FetchProfile profile = context.getMetadataCollector().getFetchProfile( fetchProfileBinding.getName() );
		if ( profile == null ) {
			log.tracef( "Creating FetchProfile: %s", fetchProfileBinding.getName() );
			profile = new FetchProfile( fetchProfileBinding.getName(), MetadataSource.HBM );
			context.getMetadataCollector().addFetchProfile( profile );
		}

		for ( JaxbHbmFetchProfileType.JaxbHbmFetch fetchBinding : fetchProfileBinding.getFetch() ) {
			String entityName = fetchBinding.getEntity();
			if ( entityName == null ) {
				entityName = containingEntityName;
			}
			if ( entityName == null ) {
				throw new org.hibernate.boot.MappingException(
						String.format(
								"Unable to determine entity for fetch-profile fetch [%s:%s]",
								profile.getName(),
								fetchBinding.getAssociation()
						),
						context.getOrigin()
				);
			}
			String association = fetchBinding.getAssociation();
			profile.addFetch( new FetchProfile.Fetch(entityName, association, fetchMode(fetchBinding.getStyle().value()), EAGER ) );
		}
	}

	private static FetchMode fetchMode(String style) {
		for ( FetchMode mode: FetchMode.values() ) {
			if ( mode.name().equalsIgnoreCase( style ) ) {
				return mode;
			}
		}
		throw new IllegalArgumentException( "Unknown FetchMode: " + style );
	}
}
