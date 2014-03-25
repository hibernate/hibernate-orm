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
package org.hibernate.metamodel.source.internal.annotations.global;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.MappingException;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.FetchProfiles;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.metamodel.source.internal.annotations.AnnotationBindingContext;
import org.hibernate.metamodel.source.internal.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.source.internal.annotations.util.JandexHelper;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.FetchProfile;
import org.hibernate.metamodel.spi.binding.FetchProfile.Fetch;
import org.hibernate.metamodel.spi.domain.Attribute;
import org.jboss.jandex.AnnotationInstance;

/**
 * Binds fetch profiles found in annotations.
 *
 * @author Hardy Ferentschik
 */
public class FetchProfileProcessor {

	private FetchProfileProcessor() {
	}

	/**
	 * Binds all {@link FetchProfiles} and {@link org.hibernate.annotations.FetchProfile} annotations to the supplied metadata.
	 *
	 * @param bindingContext the context for annotation binding
	 */
	public static void bind(AnnotationBindingContext bindingContext) {

		Collection<AnnotationInstance> annotations = bindingContext.getJandexAccess()
				.getIndex()
				.getAnnotations( HibernateDotNames.FETCH_PROFILE );
		for ( AnnotationInstance fetchProfile : annotations ) {
			bind( bindingContext, fetchProfile );
		}

		annotations = bindingContext.getJandexAccess().getIndex().getAnnotations( HibernateDotNames.FETCH_PROFILES );
		for ( AnnotationInstance fetchProfiles : annotations ) {
			AnnotationInstance[] fetchProfileAnnotations = JandexHelper.getValue(
					fetchProfiles,
					"value",
					AnnotationInstance[].class,
					bindingContext.getServiceRegistry().getService( ClassLoaderService.class )
			);
			for ( AnnotationInstance fetchProfile : fetchProfileAnnotations ) {
				bind( bindingContext, fetchProfile );
			}
		}
	}

	private static void bind(AnnotationBindingContext bindingContext, AnnotationInstance fetchProfile) {
		final ClassLoaderService classLoaderService = bindingContext.getServiceRegistry().getService( ClassLoaderService.class );
		String name = JandexHelper.getValue( fetchProfile, "name", String.class, classLoaderService);
		Set<Fetch> fetches = new HashSet<Fetch>();
		AnnotationInstance[] overrideAnnotations = JandexHelper.getValue(
				fetchProfile,
				"fetchOverrides",
				AnnotationInstance[].class,
				classLoaderService
		);
		for ( AnnotationInstance override : overrideAnnotations ) {
			FetchMode fetchMode = JandexHelper.getEnumValue( override, "mode", FetchMode.class, classLoaderService );
			if ( !fetchMode.equals( org.hibernate.annotations.FetchMode.JOIN ) ) {
				throw new MappingException( "Only FetchMode.JOIN is currently supported" );
			}
			
			final String entityName = JandexHelper.getValue( override, "entity", String.class, classLoaderService );
			final String associationName = JandexHelper.getValue( override, "association", String.class, classLoaderService );
			
			EntityBinding entityBinding = bindingContext.getMetadataCollector().getEntityBinding( entityName );
			if ( entityBinding == null ) {
				throw new MappingException( "FetchProfile " + name + " references an unknown entity: " + entityName );
			}
			Attribute attributeBinding = entityBinding.getAttributeContainer().locateAttribute( associationName );
			if ( attributeBinding == null ) {
				throw new MappingException( "FetchProfile " + name + " references an unknown association: " + associationName );
			}
			
			fetches.add( new Fetch( entityName, associationName, fetchMode.toString().toLowerCase() ) );
		}
		bindingContext.getMetadataCollector().addFetchProfile( new FetchProfile( name, fetches ) );
	}
}
