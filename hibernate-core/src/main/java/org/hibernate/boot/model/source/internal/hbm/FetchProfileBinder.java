/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.internal.hbm;

import org.hibernate.annotations.FetchMode;
import org.hibernate.boot.MappingException;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFetchProfileType;
import org.hibernate.mapping.FetchProfile;
import org.hibernate.mapping.MetadataSource;


import static jakarta.persistence.FetchType.EAGER;
import static org.hibernate.boot.BootLogging.BOOT_LOGGER;

/**
 * @author Steve Ebersole
 */
public class FetchProfileBinder {

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
		final var profile = fetchProfile( context, fetchProfileBinding );
		for ( var fetchBinding : fetchProfileBinding.getFetch() ) {
			final String entityName = entityName( containingEntityName, fetchBinding );
			if ( entityName == null ) {
				throw new MappingException(
						String.format(
								"Unable to determine entity for fetch profile fetch [%s:%s]",
								profile.getName(),
								fetchBinding.getAssociation()
						),
						context.getOrigin()
				);
			}
			profile.addFetch( new FetchProfile.Fetch(
					entityName,
					fetchBinding.getAssociation(),
					fetchMode( fetchBinding.getStyle().value() ),
					EAGER
			) );
		}
	}

	private static String entityName(String containingEntityName, JaxbHbmFetchProfileType.JaxbHbmFetch fetchBinding) {
		final String entityName = fetchBinding.getEntity();
		return entityName == null ? containingEntityName : entityName;
	}

	private static FetchProfile fetchProfile(
			HbmLocalMetadataBuildingContext context, JaxbHbmFetchProfileType fetchProfileBinding) {
		final var collector = context.getMetadataCollector();
		final var profile = collector.getFetchProfile( fetchProfileBinding.getName() );
		if ( profile == null ) {
			BOOT_LOGGER.creatingFetchProfile( fetchProfileBinding.getName() );
			final var newProfile = new FetchProfile( fetchProfileBinding.getName(), MetadataSource.HBM );
			collector.addFetchProfile( newProfile );
			return newProfile;
		}
		else {
			return profile;
		}
	}

	private static FetchMode fetchMode(String style) {
		for ( var mode: FetchMode.values() ) {
			if ( mode.name().equalsIgnoreCase( style ) ) {
				return mode;
			}
		}
		throw new IllegalArgumentException( "Unknown FetchMode: " + style );
	}
}
