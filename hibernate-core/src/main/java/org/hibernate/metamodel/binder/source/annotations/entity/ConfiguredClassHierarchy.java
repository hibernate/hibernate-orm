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
package org.hibernate.metamodel.binder.source.annotations.entity;

import javax.persistence.AccessType;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;

import org.hibernate.AnnotationException;
import org.hibernate.metamodel.binder.source.annotations.AnnotationsBindingContext;
import org.hibernate.metamodel.binder.source.annotations.JPADotNames;
import org.hibernate.metamodel.binder.source.annotations.JandexHelper;
import org.hibernate.metamodel.binding.InheritanceType;
import org.hibernate.metamodel.source.annotations.entity.EmbeddableClass;
import org.hibernate.metamodel.source.annotations.entity.EntityClass;

/**
 * Contains information about the access and inheritance type for all classes within a class hierarchy.
 *
 * @author Hardy Ferentschik
 */
public class ConfiguredClassHierarchy<T extends ConfiguredClass> implements Iterable<T> {
	private final AccessType defaultAccessType;
	private final InheritanceType inheritanceType;
	private final List<T> configuredClasses;

	public static ConfiguredClassHierarchy<EntityClass> createEntityClassHierarchy(List<ClassInfo> classInfoList, AnnotationsBindingContext context) {
		AccessType defaultAccessType = determineDefaultAccessType( classInfoList );
		InheritanceType inheritanceType = determineInheritanceType( classInfoList );
		return new ConfiguredClassHierarchy<EntityClass>(
				classInfoList,
				context,
				defaultAccessType,
				inheritanceType,
				EntityClass.class
		);
	}

	public static ConfiguredClassHierarchy<EmbeddableClass> createEmbeddableClassHierarchy(
			List<ClassInfo> classes,
			AccessType accessType,
			AnnotationsBindingContext context) {
		return new ConfiguredClassHierarchy<EmbeddableClass>(
				classes,
				context,
				accessType,
				InheritanceType.NO_INHERITANCE,
				EmbeddableClass.class
		);
	}

	private ConfiguredClassHierarchy(
			List<ClassInfo> classInfoList,
			AnnotationsBindingContext context,
			AccessType defaultAccessType,
			InheritanceType inheritanceType,
			Class<T> configuredClassType) {
		this.defaultAccessType = defaultAccessType;
		this.inheritanceType = inheritanceType;

		// the resolved type for the top level class in the hierarchy
		context.resolveAllTypes( classInfoList.get( classInfoList.size() - 1 ).name().toString() );

		configuredClasses = new ArrayList<T>();
		T parent = null;
		for ( ClassInfo info : classInfoList ) {
			T configuredClass;
			if ( EntityClass.class.equals( configuredClassType ) ) {
				configuredClass = (T) new EntityClass(
						info, (EntityClass) parent, defaultAccessType, inheritanceType, context
				);
			}
			else {
				configuredClass = (T) new EmbeddableClass(
						info, (EmbeddableClass) parent, defaultAccessType, context
				);
			}
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
	public Iterator<T> iterator() {
		return configuredClasses.iterator();
	}

	/**
	 * @return Returns the top level configured class
	 */
	public T getRoot() {
		return configuredClasses.get( 0 );
	}

	/**
	 * @return Returns the leaf configured class
	 */
	public T getLeaf() {
		return configuredClasses.get( configuredClasses.size() - 1 );
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
	private static AccessType determineDefaultAccessType(List<ClassInfo> classes) {
        AccessType accessTypeByEmbeddedIdPlacement = null;
        AccessType accessTypeByIdPlacement = null;
		for ( ClassInfo info : classes ) {
			List<AnnotationInstance> idAnnotations = info.annotations().get( JPADotNames.ID );
            List<AnnotationInstance> embeddedIdAnnotations = info.annotations().get( JPADotNames.EMBEDDED_ID );

            if ( embeddedIdAnnotations != null && !embeddedIdAnnotations.isEmpty() ) {
                accessTypeByEmbeddedIdPlacement = determineAccessTypeByIdPlacement( embeddedIdAnnotations );
            }
			if ( idAnnotations != null && !idAnnotations.isEmpty() ) {
				accessTypeByIdPlacement = determineAccessTypeByIdPlacement( idAnnotations );
			}
		}
        if ( accessTypeByEmbeddedIdPlacement != null ) {
            return accessTypeByEmbeddedIdPlacement;
        } else if (accessTypeByIdPlacement != null ){
            return accessTypeByIdPlacement;
        } else {
            return throwIdNotFoundAnnotationException( classes );
        }


//
//
//		if ( accessType == null ) {
//			return throwIdNotFoundAnnotationException( classes );
//		}
//
//		return accessType;
	}

	private static AccessType determineAccessTypeByIdPlacement(List<AnnotationInstance> idAnnotations) {
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

	private static InheritanceType determineInheritanceType(List<ClassInfo> classes) {
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

	private static AccessType throwIdNotFoundAnnotationException(List<ClassInfo> classes) {
		StringBuilder builder = new StringBuilder();
		builder.append( "Unable to determine identifier attribute for class hierarchy consisting of the classe(s) " );
		builder.append( hierarchyListString( classes ) );
		throw new AnnotationException( builder.toString() );
	}

	private static String hierarchyListString(List<ClassInfo> classes) {
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
