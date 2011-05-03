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
package org.hibernate.metamodel.source.annotations;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.persistence.AccessType;
import javax.persistence.InheritanceType;

import com.fasterxml.classmate.ResolvedTypeWithMembers;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;

import org.hibernate.AnnotationException;
import org.hibernate.metamodel.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.source.annotations.util.ReflectionHelper;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.classloading.spi.ClassLoaderService;

/**
 * Represents the inheritance structure of the configured classes within a class hierarchy.
 *
 * @author Hardy Ferentschik
 */
public class ConfiguredClassHierarchy implements Iterable<ConfiguredClass> {
	private final AccessType defaultAccessType;
	private final InheritanceType inheritanceType;
	private final List<ConfiguredClass> configuredClasses;

	public static ConfiguredClassHierarchy create(List<ClassInfo> classes, ServiceRegistry serviceRegistry) {
		return new ConfiguredClassHierarchy( classes, serviceRegistry );
	}

	private ConfiguredClassHierarchy(List<ClassInfo> classes, ServiceRegistry serviceRegistry) {
		defaultAccessType = determineDefaultAccessType( classes );
		inheritanceType = determineInheritanceType( classes );

		ClassLoaderService classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
		Class<?> clazz = classLoaderService.classForName( classes.get( classes.size() - 1 ).name().toString() );
		ResolvedTypeWithMembers resolvedMembers = ReflectionHelper.resolveMemberTypes( clazz );

		configuredClasses = new ArrayList<ConfiguredClass>();
		ConfiguredClass parent = null;
		for ( ClassInfo info : classes ) {
			ConfiguredClass configuredClass = new ConfiguredClass(
					info, parent, defaultAccessType, inheritanceType, serviceRegistry, resolvedMembers
			);
			configuredClasses.add( configuredClass );
			parent = configuredClass;
		}
	}

	public AccessType getDefaultAccessType() {
		return defaultAccessType;
	}

	public InheritanceType getInheritanceType() {
		return inheritanceType;
	}

	/**
	 * @return An iterator iterating in top down manner over the configured classes in this hierarchy.
	 */
	public Iterator<ConfiguredClass> iterator() {
		return configuredClasses.iterator();
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "ConfiguredClassHierarchy" );
		sb.append( "{defaultAccessType=" ).append( defaultAccessType );
		sb.append( ", configuredClasses=" ).append( configuredClasses );
		sb.append( '}' );
		return sb.toString();
	}

	/**
	 * @param classes the classes in the hierarchy
	 *
	 * @return Returns the default access type for the configured class hierarchy independent of explicit
	 *         {@code AccessType} annotations. The default access type is determined by the placement of the
	 *         annotations.
	 */
	private AccessType determineDefaultAccessType(List<ClassInfo> classes) {
		AccessType accessType = null;
		for ( ClassInfo info : classes ) {
			List<AnnotationInstance> idAnnotations = info.annotations().get( JPADotNames.ID );
			if ( idAnnotations == null || idAnnotations.size() == 0 ) {
				continue;
			}
			accessType = processIdAnnotations( idAnnotations );
		}

		if ( accessType == null ) {
			return throwIdNotFoundAnnotationException( classes );
		}

		return accessType;
	}

	private AccessType processIdAnnotations(List<AnnotationInstance> idAnnotations) {
		AccessType accessType = null;
		for ( AnnotationInstance annotation : idAnnotations ) {
			AccessType tmpAccessType;
			if ( annotation.target() instanceof FieldInfo ) {
				tmpAccessType = AccessType.FIELD;
			}
			else if ( annotation.target() instanceof MethodInfo ) {
				tmpAccessType = AccessType.PROPERTY;
			}
			else {
				throw new AnnotationException( "Invalid placement of @Id annotation" );
			}

			if ( accessType == null ) {
				accessType = tmpAccessType;
			}
			else {
				if ( !accessType.equals( tmpAccessType ) ) {
					throw new AnnotationException( "Inconsistent placement of @Id annotation within hierarchy " );
				}
			}
		}
		return accessType;
	}

	private InheritanceType determineInheritanceType(List<ClassInfo> classes) {
		InheritanceType inheritanceType = null;
		for ( ClassInfo info : classes ) {
			AnnotationInstance inheritanceAnnotation = JandexHelper.getSingleAnnotation(
					info, JPADotNames.INHERITANCE
			);
			if ( inheritanceAnnotation == null ) {
				continue;
			}

			InheritanceType tmpInheritanceType = Enum.valueOf(
					InheritanceType.class, inheritanceAnnotation.value( "strategy" ).asEnum()
			);
			if ( tmpInheritanceType == null ) {
				// default inheritance type is single table
				inheritanceType = InheritanceType.SINGLE_TABLE;
			}

			if ( inheritanceType == null ) {
				inheritanceType = tmpInheritanceType;
			}
			else {
				if ( !inheritanceType.equals( tmpInheritanceType ) ) {
					throw new AnnotationException(
							"Multiple incompatible instances of @Inheritance specified within classes "
									+ hierarchyListString( classes )
					);
				}
			}
		}

		if ( inheritanceType == null ) {
			// default inheritance type is single table
			inheritanceType = InheritanceType.SINGLE_TABLE;
		}

		return inheritanceType;
	}

	private AccessType throwIdNotFoundAnnotationException(List<ClassInfo> classes) {
		StringBuilder builder = new StringBuilder();
		builder.append( "Unable to determine identifier attribute for class hierarchy " );
		builder.append( hierarchyListString( classes ) );
		throw new AnnotationException( builder.toString() );
	}

	private String hierarchyListString(List<ClassInfo> classes) {
		StringBuilder builder = new StringBuilder();
		builder.append( "[" );

		int count = 0;
		for ( ClassInfo info : classes ) {
			builder.append( info.name().toString() );
			if ( count < classes.size() ) {
				builder.append( ", " );
			}
			count++;
		}
		builder.append( "]" );
		return builder.toString();
	}
}


