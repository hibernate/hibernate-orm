/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFetchProfileType;
import org.hibernate.mapping.FetchProfile;
import org.hibernate.mapping.MetadataSource;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class FetchProfileBinder {
	private static final Logger log = Logger.getLogger( FetchProfileBinder.class );

	/**
	 * Handling for a {@code <fetch-profile/>} declaration.
	 * <p/>
	 * This form handles fetch profiles defined at the {@code <hibernate-mapping/>}
	 * root.  For handling of fetch profiles defined within an entity, see
	 * {@link #processFetchProfile(HbmLocalMetadataBuildingContext, org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFetchProfileType, String)}
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
			log.debugf( "Creating FetchProfile : %s", fetchProfileBinding.getName() );
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
			profile.addFetch( entityName, fetchBinding.getAssociation(), fetchBinding.getStyle().value() );
		}
	}

}
