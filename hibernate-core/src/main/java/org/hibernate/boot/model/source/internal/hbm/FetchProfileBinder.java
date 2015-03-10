/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
