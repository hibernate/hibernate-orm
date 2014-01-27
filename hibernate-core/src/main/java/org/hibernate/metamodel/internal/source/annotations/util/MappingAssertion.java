/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.internal.source.annotations.util;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;

import org.hibernate.AnnotationException;

/**
 * Utility class that applying some mapping assertion.
 *
 * @author Strong Liu <stliu@hibernate.org>
 */
public class MappingAssertion {
	/**
	 * Check if a class is wrongly placed both {@link javax.persistence.Entity @Entity} and {@link javax.persistence.MappedSuperclass @MappedSuperclass} annotations.
	 *
	 * @param jpaEntityAnnotation The {@link javax.persistence.Entity @Entity} annotation instance.
	 * @param mappedSuperClassAnnotation The {@link javax.persistence.MappedSuperclass @MappedSuperclass} annotation instance.
	 * @param className Class name that being asserted.
	 */
	static void assertNotEntityAndMappedSuperClass(
			AnnotationInstance jpaEntityAnnotation,
			AnnotationInstance mappedSuperClassAnnotation,
			String className) {
		if ( jpaEntityAnnotation != null && mappedSuperClassAnnotation != null ) {
			throw new AnnotationException(
					"An entity cannot be annotated with both @Entity and @MappedSuperclass. " + className + " has both annotations."
			);
		}
	}

	/**
	 * Check if a class is wrongly placed both {@link javax.persistence.Entity @Entity} and {@link javax.persistence.Embeddable @Embeddable} annotations.
	 *
	 * @param jpaEntityAnnotation The {@link javax.persistence.Entity @Entity} annotation instance.
	 * @param embeddableAnnotation The {@link javax.persistence.Embeddable @Embeddable} annotation instance.
	 * @param className Class name that being asserted.
	 */
	static void assertNotEntityAndEmbeddable(AnnotationInstance jpaEntityAnnotation, AnnotationInstance embeddableAnnotation, String className) {
		if ( jpaEntityAnnotation != null && embeddableAnnotation != null ) {
			throw new AnnotationException(
					"An entity cannot be annotated with both @Entity and @Embeddable. " + className + " has both annotations."
			);
		}
	}


	/**
	 * Check if the sub-entity has {@link javax.persistence.Embeddable @Embeddable} or {@link javax.persistence.MappedSuperclass @MappedSuperclass}.
	 *
	 * From the JPA Spec, a sub entity class can not has such annotation.
	 *
	 * @param subClassInfo The sub entity {@link ClassInfo class}.
	 */
	static void assertSubEntityIsNotEmbeddableNorMappedSuperclass(ClassInfo subClassInfo) {
		if ( JandexHelper.containsSingleAnnotation( subClassInfo, JPADotNames.EMBEDDABLE ) ) {
			throw new AnnotationException( "An embeddable cannot extend an entity: " + subClassInfo );
		}
		if ( JandexHelper.containsSingleAnnotation( subClassInfo, JPADotNames.MAPPED_SUPERCLASS ) ) {
			throw new AnnotationException( "A mapped superclass cannot extend an entity: " + subClassInfo );
		}
	}
}
