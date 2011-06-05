/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.annotations.global;

import java.util.HashSet;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.Index;

import org.hibernate.MappingException;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.FetchProfiles;
import org.hibernate.metamodel.binding.FetchProfile;
import org.hibernate.metamodel.binding.FetchProfile.Fetch;
import org.hibernate.metamodel.source.annotations.HibernateDotNames;
import org.hibernate.metamodel.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.source.spi.MetadataImplementor;

/**
 * Binds fetch profiles found in annotations.
 *
 * @author Hardy Ferentschik
 */
public class FetchProfileBinder {

	/**
	 * Binds all {@link FetchProfiles} and {@link org.hibernate.annotations.FetchProfile} annotations to the supplied metadata.
	 *
	 * @param metadata the global metadata
	 * @param jandex the jandex index
	 */
	// TODO verify that association exists. See former VerifyFetchProfileReferenceSecondPass
	public static void bind(MetadataImplementor metadata, Index jandex) {
		for ( AnnotationInstance fetchProfile : jandex.getAnnotations( HibernateDotNames.FETCH_PROFILE ) ) {
			bind( metadata, fetchProfile );
		}
		for ( AnnotationInstance fetchProfiles : jandex.getAnnotations( HibernateDotNames.FETCH_PROFILES ) ) {
			for ( AnnotationInstance fetchProfile : JandexHelper.getValueAsArray( fetchProfiles, "value" ) ) {
				bind( metadata, fetchProfile );
			}
		}
	}

	private static void bind(MetadataImplementor metadata, AnnotationInstance fetchProfile) {
		String name = JandexHelper.getValueAsString( fetchProfile, "name" );
		Set<Fetch> fetches = new HashSet<Fetch>();
		for ( AnnotationInstance override : JandexHelper.getValueAsArray( fetchProfile, "fetchOverrides" ) ) {
			FetchMode fetchMode = JandexHelper.getValueAsEnum( override, "mode", FetchMode.class );
			if ( !fetchMode.equals( org.hibernate.annotations.FetchMode.JOIN ) ) {
				throw new MappingException( "Only FetchMode.JOIN is currently supported" );
			}
			fetches.add(
					new Fetch(
							JandexHelper.getValueAsString( override, "entity" ), JandexHelper.getValueAsString(
									override,
									"association"
							),
							fetchMode.toString().toLowerCase()
					)
			);
		}
		metadata.addFetchProfile( new FetchProfile( name, fetches ) );
	}

	private FetchProfileBinder() {
	}
}
