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

import java.util.Arrays;
import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.Index;

import org.hibernate.MappingException;
import org.hibernate.annotations.FetchMode;
import org.hibernate.mapping.MetadataSource;
import org.hibernate.metamodel.binding.FetchProfile;
import org.hibernate.metamodel.source.annotations.HibernateDotNames;
import org.hibernate.metamodel.source.internal.MetadataImpl;

/**
 * Binds fetch profiles found in annotations.
 *
 * @author Hardy Ferentschik
 */
public class FetchProfileBinder {
	private FetchProfileBinder() {
	}

	/**
	 * Binds all {@link org.hibernate.annotations.FetchProfiles} and {@link org.hibernate.annotations.FetchProfile}
	 * annotations to the specified meta data instance.
	 *
	 * @param meta the global metadata
	 * @param index the annotation index repository
	 */
	// TODO how to handle fetch profiles defined in hbm and annotations. Which overrides which?
	// TODO verify that association exists. See former VerifyFetchProfileReferenceSecondPass
	public static void bind(MetadataImpl meta, Index index) {
		// check @FetchProfiles
		List<AnnotationInstance> fetchProfilesAnnotations = index.getAnnotations( HibernateDotNames.FETCH_PROFILES );
		for ( AnnotationInstance fetchProfilesAnnotation : fetchProfilesAnnotations ) {
			AnnotationInstance fetchProfiles[] = fetchProfilesAnnotation.value().asNestedArray();
			bindFetchProfileAnnotations( meta, Arrays.asList( fetchProfiles ) );
		}

		// check @FetchProfile
		List<AnnotationInstance> fetchProfileAnnotations = index.getAnnotations( HibernateDotNames.FETCH_PROFILE );
		bindFetchProfileAnnotations( meta, fetchProfileAnnotations );
	}

	private static void bindFetchProfileAnnotations(MetadataImpl meta, List<AnnotationInstance> fetchProfileAnnotations) {
		for ( AnnotationInstance fetchProfileAnnotation : fetchProfileAnnotations ) {
			String name = fetchProfileAnnotation.value( "name" ).asString();
			FetchProfile profile = meta.findOrCreateFetchProfile( name, MetadataSource.ANNOTATIONS );

			AnnotationInstance overrides[] = fetchProfileAnnotation.value( "fetchOverrides" ).asNestedArray();
			for ( AnnotationInstance overrideAnnotation : overrides ) {
				FetchMode fetchMode = Enum.valueOf( FetchMode.class, overrideAnnotation.value( "mode" ).asEnum() );
				if ( !fetchMode.equals( org.hibernate.annotations.FetchMode.JOIN ) ) {
					throw new MappingException( "Only FetchMode.JOIN is currently supported" );
				}

				String entityClassName = overrideAnnotation.value( "entity" ).asClass().name().toString();
				String associationName = overrideAnnotation.value( "association" ).asString();
				profile.addFetch(
						entityClassName, associationName, fetchMode.toString().toLowerCase()
				);
			}
		}
	}
}


