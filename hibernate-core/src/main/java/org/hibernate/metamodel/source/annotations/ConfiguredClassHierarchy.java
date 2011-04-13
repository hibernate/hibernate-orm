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

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;

import org.hibernate.AnnotationException;

/**
 * Represents the inheritance structure of the configured classes within a class hierarchy.
 *
 * @author Hardy Ferentschik
 */
public class ConfiguredClassHierarchy implements Iterable<ConfiguredClass> {
	private final AccessType defaultAccessType;
	private final InheritanceType inheritanceType;
	private final List<ConfiguredClass> configuredClasses;

	ConfiguredClassHierarchy(List<ClassInfo> classes) {
		configuredClasses = new ArrayList<ConfiguredClass>();
		for ( ClassInfo info : classes ) {
			configuredClasses.add( new ConfiguredClass( info, this ) );
		}
		defaultAccessType = determineDefaultAccessType();
		inheritanceType = determineInheritanceType();
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
	public String toString
			() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "ConfiguredClassHierarchy" );
		sb.append( "{defaultAccessType=" ).append( defaultAccessType );
		sb.append( ", configuredClasses=" ).append( configuredClasses );
		sb.append( '}' );
		return sb.toString();
	}

	/**
	 * @return Returns the default access type for the configured class hierarchy independent of explicit
	 *         {@code AccessType} annotations. The default access type is determined by the placement of the
	 *         annotations.
	 */
	private AccessType determineDefaultAccessType() {
		Iterator<ConfiguredClass> iter = iterator();
		AccessType accessType = null;
		while ( iter.hasNext() ) {
			ConfiguredClass configuredClass = iter.next();
			ClassInfo info = configuredClass.getClassInfo();
			List<AnnotationInstance> idAnnotations = info.annotations().get( JPADotNames.ID );
			if ( idAnnotations == null || idAnnotations.size() == 0 ) {
				continue;
			}
			accessType = processIdAnnotations( idAnnotations );
		}

		if ( accessType == null ) {
			return throwIdNotFoundAnnotationException();
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
					throw new AnnotationException( "Inconsistent placement of @Id annotation within hierarchy " + hierarchyListString() );
				}
			}
		}
		return accessType;
	}

	private InheritanceType determineInheritanceType() {
		Iterator<ConfiguredClass> iter = iterator();
		InheritanceType inheritanceType = null;
		while ( iter.hasNext() ) {
			ConfiguredClass configuredClass = iter.next();
			ClassInfo info = configuredClass.getClassInfo();
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
							"Multiple incompatible instances of @Inheritance specified within hierarchy " + hierarchyListString()
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

	private AccessType throwIdNotFoundAnnotationException() {
		StringBuilder builder = new StringBuilder();
		builder.append( "Unable to find Id property for class hierarchy " );
		builder.append( hierarchyListString() );
		throw new AnnotationException( builder.toString() );
	}

	private String hierarchyListString() {
		Iterator<ConfiguredClass> iter;
		StringBuilder builder = new StringBuilder();
		builder.append( "[" );
		iter = iterator();
		while ( iter.hasNext() ) {
			builder.append( iter.next().getClassInfo().name().toString() );
			if ( iter.hasNext() ) {
				builder.append( ", " );
			}
		}
		builder.append( "]" );
		return builder.toString();
	}
}


