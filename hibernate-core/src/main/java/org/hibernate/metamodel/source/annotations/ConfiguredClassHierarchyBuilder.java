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
import java.util.List;
import javax.persistence.AccessType;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import org.hibernate.AssertionFailure;

import org.hibernate.metamodel.source.annotations.entity.EmbeddableClass;
import org.hibernate.metamodel.source.annotations.entity.EmbeddableHierarchy;

/**
 * @author Hardy Ferentschik
 */
public class ConfiguredClassHierarchyBuilder {

	/**
	 * Builds the configured class hierarchy for a an embeddable class.
	 *
	 * @param embeddableClass the top level embedded class
	 * @param accessType the access type inherited from the class in which the embeddable gets embedded
	 * @param context the annotation binding context with access to the service registry and the annotation index
	 *
	 * @return a set of {@code ConfiguredClassHierarchy}s. One for each "leaf" entity.
	 */
	public static EmbeddableHierarchy<EmbeddableClass> createEmbeddableHierarchy(Class<?> embeddableClass, AccessType accessType, AnnotationBindingContext context) {

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

		return EmbeddableHierarchy.createEmbeddableClassHierarchy( classInfoList, accessType, context );
	}
}


