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
package org.hibernate.metamodel.source.annotations.entity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.persistence.AccessType;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;

import org.hibernate.AnnotationException;
import org.hibernate.metamodel.binding.InheritanceType;
import org.hibernate.metamodel.source.annotations.AnnotationBindingContext;
import org.hibernate.metamodel.source.annotations.JPADotNames;
import org.hibernate.metamodel.source.annotations.util.JandexHelper;

/**
 * Contains information about the access and inheritance type for all classes within a class hierarchy.
 *
 * @author Hardy Ferentschik
 */
public class ConfiguredClassHierarchy implements Iterable<EntityClass> {
	private final AccessType defaultAccessType;
	private final InheritanceType inheritanceType;
	private final List<EntityClass> entityClasses;

	public static ConfiguredClassHierarchy create(List<ClassInfo> classes, AnnotationBindingContext context) {
		return new ConfiguredClassHierarchy( classes, context );
	}

	private ConfiguredClassHierarchy(List<ClassInfo> classInfoList, AnnotationBindingContext context) {
		defaultAccessType = determineDefaultAccessType( classInfoList );
		inheritanceType = determineInheritanceType( classInfoList );

		// the resolved type for the top level class in the hierarchy
		context.resolveAllTypes( classInfoList.get( classInfoList.size() - 1 ).name().toString() );

		entityClasses = new ArrayList<EntityClass>();
		EntityClass parent = null;
		for ( ClassInfo info : classInfoList ) {
			EntityClass entityClass = new EntityClass(
					info, parent, defaultAccessType, inheritanceType, context
			);
			entityClasses.add( entityClass );
			parent = entityClass;
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
	public Iterator<EntityClass> iterator() {
		return entityClasses.iterator();
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "ConfiguredClassHierarchy" );
		sb.append( "{defaultAccessType=" ).append( defaultAccessType );
		sb.append( ", configuredClasses=" ).append( entityClasses );
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
			accessType = determineAccessTypeByIdPlacement( idAnnotations );
		}

		if ( accessType == null ) {
			return throwIdNotFoundAnnotationException( classes );
		}

		return accessType;
	}

	private AccessType determineAccessTypeByIdPlacement(List<AnnotationInstance> idAnnotations) {
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
		if ( classes.size() == 1 ) {
			return InheritanceType.NO_INHERITANCE;
		}

		InheritanceType inheritanceType = null;
		for ( ClassInfo info : classes ) {
			AnnotationInstance inheritanceAnnotation = JandexHelper.getSingleAnnotation(
					info, JPADotNames.INHERITANCE
			);
			if ( inheritanceAnnotation == null ) {
				continue;
			}

			javax.persistence.InheritanceType jpaInheritanceType = Enum.valueOf(
					javax.persistence.InheritanceType.class, inheritanceAnnotation.value( "strategy" ).asEnum()
			);
			InheritanceType tmpInheritanceType = InheritanceType.get( jpaInheritanceType );
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
		builder.append( "Unable to determine identifier attribute for class hierarchy consisting of the classe(s) " );
		builder.append( hierarchyListString( classes ) );
		throw new AnnotationException( builder.toString() );
	}

	private String hierarchyListString(List<ClassInfo> classes) {
		StringBuilder builder = new StringBuilder();
		builder.append( "[" );

		int count = 0;
		for ( ClassInfo info : classes ) {
			builder.append( info.name().toString() );
			if ( count < classes.size() - 1 ) {
				builder.append( ", " );
			}
			count++;
		}
		builder.append( "]" );
		return builder.toString();
	}
}
