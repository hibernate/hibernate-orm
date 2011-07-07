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
package org.hibernate.metamodel.binder.source.annotations;

import javax.persistence.AccessType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.metamodel.binder.source.annotations.entity.ConfiguredClassHierarchy;
import org.hibernate.metamodel.source.annotations.entity.EmbeddableClass;
import org.hibernate.metamodel.source.annotations.entity.EntityClass;

/**
 * Given a (jandex) annotation index build processes all classes with JPA relevant annotations and pre-orders
 * JPA entities respectively their inheritance hierarchy.
 *
 * @author Hardy Ferentschik
 */
public class ConfiguredClassHierarchyBuilder {

	/**
	 * Pre-processes the annotated entities from the index and put them into a structure which can
	 * bound to the Hibernate metamodel.
	 *
	 * @param bindingContext The binding context, giving access to needed services and information
	 *
	 * @return a set of {@code ConfiguredClassHierarchy}s. One for each "leaf" entity.
	 */
	public static Set<ConfiguredClassHierarchy<EntityClass>> createEntityHierarchies(AnnotationsBindingContext bindingContext) {
		Map<ClassInfo, List<ClassInfo>> processedClassInfos = new HashMap<ClassInfo, List<ClassInfo>>();

		for ( ClassInfo info : bindingContext.getIndex().getKnownClasses() ) {
			if ( !isEntityClass( info ) ) {
				continue;
			}

			if ( processedClassInfos.containsKey( info ) ) {
				continue;
			}

			List<ClassInfo> configuredClassList = new ArrayList<ClassInfo>();
			ClassInfo tmpClassInfo = info;
			Class<?> clazz = bindingContext.locateClassByName( tmpClassInfo.toString() );
			while ( clazz != null && !clazz.equals( Object.class ) ) {
				tmpClassInfo = bindingContext.getIndex().getClassByName( DotName.createSimple( clazz.getName() ) );
				clazz = clazz.getSuperclass();
				if ( tmpClassInfo == null ) {
					continue;
				}

				if ( existsHierarchyWithClassInfoAsLeaf( processedClassInfos, tmpClassInfo ) ) {
					List<ClassInfo> classInfoList = processedClassInfos.get( tmpClassInfo );
					for ( ClassInfo tmpInfo : configuredClassList ) {
						classInfoList.add( tmpInfo );
						processedClassInfos.put( tmpInfo, classInfoList );
					}
					break;
				}
				else {
					configuredClassList.add( 0, tmpClassInfo );
					processedClassInfos.put( tmpClassInfo, configuredClassList );
				}
			}
		}

		Set<ConfiguredClassHierarchy<EntityClass>> hierarchies = new HashSet<ConfiguredClassHierarchy<EntityClass>>();
		List<List<ClassInfo>> processedList = new ArrayList<List<ClassInfo>>();
		for ( List<ClassInfo> classInfoList : processedClassInfos.values() ) {
			if ( !processedList.contains( classInfoList ) ) {
				hierarchies.add( ConfiguredClassHierarchy.createEntityClassHierarchy( classInfoList, bindingContext ) );
				processedList.add( classInfoList );
			}
		}

		return hierarchies;
	}

	/**
	 * Builds the configured class hierarchy for a an embeddable class.
	 *
	 * @param embeddableClass the top level embedded class
	 * @param accessType the access type inherited from the class in which the embeddable gets embedded
	 * @param context the annotation binding context with access to the service registry and the annotation index
	 *
	 * @return a set of {@code ConfiguredClassHierarchy}s. One for each "leaf" entity.
	 */
	public static ConfiguredClassHierarchy<EmbeddableClass> createEmbeddableHierarchy(Class<?> embeddableClass, AccessType accessType, AnnotationsBindingContext context) {

		ClassInfo embeddableClassInfo = context.getClassInfo( embeddableClass.getName() );
		if ( embeddableClassInfo == null ) {
			throw new AssertionFailure(
					String.format(
							"The specified class %s cannot be found in the annotation index",
							embeddableClass.getName()
					)
			);
		}

		if ( JandexHelper.getSingleAnnotation( embeddableClassInfo, JPADotNames.EMBEDDABLE ) == null ) {
			throw new AssertionFailure(
					String.format(
							"The specified class %s is not annotated with @Embeddable",
							embeddableClass.getName()
					)
			);
		}

		List<ClassInfo> classInfoList = new ArrayList<ClassInfo>();
		ClassInfo tmpClassInfo;
		Class<?> clazz = embeddableClass;
		while ( clazz != null && !clazz.equals( Object.class ) ) {
			tmpClassInfo = context.getIndex().getClassByName( DotName.createSimple( clazz.getName() ) );
			clazz = clazz.getSuperclass();
			if ( tmpClassInfo == null ) {
				continue;
			}

			classInfoList.add( 0, tmpClassInfo );
		}

		return ConfiguredClassHierarchy.createEmbeddableClassHierarchy( classInfoList, accessType, context );
	}

	/**
	 * Checks whether the passed jandex class info needs to be processed.
	 *
	 * @param info the jandex class info
	 *
	 * @return {@code true} if the class represented by {@code info} is relevant for the JPA mappings, {@code false} otherwise.
	 */
	private static boolean isEntityClass(ClassInfo info) {
		boolean isConfiguredClass = true;
		AnnotationInstance jpaEntityAnnotation = JandexHelper.getSingleAnnotation( info, JPADotNames.ENTITY );
		AnnotationInstance mappedSuperClassAnnotation = JandexHelper.getSingleAnnotation(
				info, JPADotNames.MAPPED_SUPERCLASS
		);

		// we are only interested in building the class hierarchies for @Entity or @MappedSuperclass
		if ( jpaEntityAnnotation == null && mappedSuperClassAnnotation == null ) {
			return false;
		}

		// some sanity checks
		String className = info.toString();
		assertNotEntityAndMappedSuperClass( jpaEntityAnnotation, mappedSuperClassAnnotation, className );

		AnnotationInstance embeddableAnnotation = JandexHelper.getSingleAnnotation(
				info, JPADotNames.EMBEDDABLE
		);
		assertNotEntityAndEmbeddable( jpaEntityAnnotation, embeddableAnnotation, className );

		return isConfiguredClass;
	}

	private static boolean existsHierarchyWithClassInfoAsLeaf(Map<ClassInfo, List<ClassInfo>> processedClassInfos, ClassInfo tmpClassInfo) {
		if ( !processedClassInfos.containsKey( tmpClassInfo ) ) {
			return false;
		}

		List<ClassInfo> classInfoList = processedClassInfos.get( tmpClassInfo );
		return classInfoList.get( classInfoList.size() - 1 ).equals( tmpClassInfo );
	}

	private static void assertNotEntityAndMappedSuperClass(AnnotationInstance jpaEntityAnnotation, AnnotationInstance mappedSuperClassAnnotation, String className) {
		if ( jpaEntityAnnotation != null && mappedSuperClassAnnotation != null ) {
			throw new AnnotationException(
					"An entity cannot be annotated with both @Entity and @MappedSuperclass. " + className + " has both annotations."
			);
		}
	}

	private static void assertNotEntityAndEmbeddable(AnnotationInstance jpaEntityAnnotation, AnnotationInstance embeddableAnnotation, String className) {
		if ( jpaEntityAnnotation != null && embeddableAnnotation != null ) {
			throw new AnnotationException(
					"An entity cannot be annotated with both @Entity and @Embeddable. " + className + " has both annotations."
			);
		}
	}
}


