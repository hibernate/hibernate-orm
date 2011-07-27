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
import java.util.List;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;

import org.hibernate.MappingException;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.FetchProfiles;
import org.hibernate.metamodel.binding.FetchProfile;
import org.hibernate.metamodel.binding.FetchProfile.Fetch;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.metamodel.source.annotations.AnnotationBindingContext;
import org.hibernate.metamodel.source.annotations.HibernateDotNames;
import org.hibernate.metamodel.source.annotations.JandexHelper;

/**
 * Binds fetch profiles found in annotations.
 *
 * @author Hardy Ferentschik
 */
public class FetchProfileBinder {

	private FetchProfileBinder() {
	}

	/**
	 * Binds all {@link FetchProfiles} and {@link org.hibernate.annotations.FetchProfile} annotations to the supplied metadata.
	 *
	 * @param bindingContext the context for annotation binding
	 */
	// TODO verify that association exists. See former VerifyFetchProfileReferenceSecondPass
	public static void bind(AnnotationBindingContext bindingContext) {

		List<AnnotationInstance> annotations = bindingContext.getIndex()
				.getAnnotations( HibernateDotNames.FETCH_PROFILE );
		for ( AnnotationInstance fetchProfile : annotations ) {
			bind( bindingContext.getMetadataImplementor(), fetchProfile );
		}

		annotations = bindingContext.getIndex().getAnnotations( HibernateDotNames.FETCH_PROFILES );
		for ( AnnotationInstance fetchProfiles : annotations ) {
			AnnotationInstance[] fetchProfileAnnotations = JandexHelper.getValue(
					fetchProfiles,
					"value",
					AnnotationInstance[].class
			);
			for ( AnnotationInstance fetchProfile : fetchProfileAnnotations ) {
				bind( bindingContext.getMetadataImplementor(), fetchProfile );
			}
		}
	}

	private static void bind(MetadataImplementor metadata, AnnotationInstance fetchProfile) {
		String name = JandexHelper.getValue( fetchProfile, "name", String.class );
		Set<Fetch> fetches = new HashSet<Fetch>();
		AnnotationInstance[] overrideAnnotations = JandexHelper.getValue(
				fetchProfile,
				"fetchOverrides",
				AnnotationInstance[].class
		);
		for ( AnnotationInstance override : overrideAnnotations ) {
			FetchMode fetchMode = JandexHelper.getEnumValue( override, "mode", FetchMode.class );
			if ( !fetchMode.equals( org.hibernate.annotations.FetchMode.JOIN ) ) {
				throw new MappingException( "Only FetchMode.JOIN is currently supported" );
			}
			final String entityName = JandexHelper.getValue( override, "entity", String.class );
			final String associationName = JandexHelper.getValue( override, "association", String.class );
			fetches.add( new Fetch( entityName, associationName, fetchMode.toString().toLowerCase() ) );
		}
		metadata.addFetchProfile( new FetchProfile( name, fetches ) );
	}
}
