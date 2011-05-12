/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc..
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
package org.hibernate.metamodel.source.annotations.xml.mocker;

import java.util.List;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

import org.hibernate.AnnotationException;
import org.hibernate.metamodel.source.annotation.xml.XMLAccessType;
import org.hibernate.metamodel.source.annotations.JPADotNames;
import org.hibernate.metamodel.source.annotations.util.JandexHelper;

/**
 * @author Strong Liu
 */
class AccessHelper implements JPADotNames {
	static XMLAccessType getAccessFromIdPosition(DotName className, IndexBuilder indexBuilder) {
		Map<DotName, List<AnnotationInstance>> indexedAnnotations = indexBuilder.getIndexedAnnotations( className );
		Map<DotName, List<AnnotationInstance>> ormAnnotations = indexBuilder.getClassInfoAnnotationsMap( className );
		XMLAccessType accessType = getAccessFromIdPosition( ormAnnotations );
		if ( accessType == null ) {
			accessType = getAccessFromIdPosition( indexedAnnotations );
		}
		if ( accessType == null ) {
			ClassInfo parent = indexBuilder.getClassInfo( className );
			if ( parent == null ) {
				parent = indexBuilder.getIndexedClassInfo( className );
			}
			if ( parent != null ) {
				DotName parentClassName = parent.superName();
				accessType = getAccessFromIdPosition( parentClassName, indexBuilder );
			}

		}

		return accessType;
	}

	private static XMLAccessType getAccessFromIdPosition(Map<DotName, List<AnnotationInstance>> annotations) {
		if ( annotations == null || annotations.isEmpty() || !( annotations.containsKey( ID ) ) ) {
			return null;
		}
		List<AnnotationInstance> idAnnotationInstances = annotations.get( ID );
		if ( MockHelper.isNotEmpty( idAnnotationInstances ) ) {
			return processIdAnnotations( idAnnotationInstances );
		}
		return null;
	}

	private static XMLAccessType processIdAnnotations(List<AnnotationInstance> idAnnotations) {
		XMLAccessType accessType = null;
		for ( AnnotationInstance annotation : idAnnotations ) {
			AnnotationTarget tmpTarget = annotation.target();
			if ( tmpTarget == null ) {
				continue;
			}
			if ( accessType == null ) {
				accessType = annotationTargetToAccessType( tmpTarget );
			}
			else {
				if ( !accessType.equals( annotationTargetToAccessType( tmpTarget ) ) ) {
					throw new AnnotationException( "Inconsistent placement of @Id annotation within hierarchy " );
				}
			}
		}
		return accessType;
	}

	static XMLAccessType annotationTargetToAccessType(AnnotationTarget target) {
		return ( target instanceof MethodInfo ) ? XMLAccessType.PROPERTY : XMLAccessType.FIELD;
	}

	static XMLAccessType getEntityAccess(DotName className, IndexBuilder indexBuilder) {
		Map<DotName, List<AnnotationInstance>> indexedAnnotations = indexBuilder.getIndexedAnnotations( className );
		Map<DotName, List<AnnotationInstance>> ormAnnotations = indexBuilder.getClassInfoAnnotationsMap( className );
		XMLAccessType accessType = getAccess( ormAnnotations );
		if ( accessType == null ) {
			accessType = getAccess( indexedAnnotations );
		}
		if ( accessType == null ) {
			ClassInfo parent = indexBuilder.getClassInfo( className );
			if ( parent == null ) {
				parent = indexBuilder.getIndexedClassInfo( className );
			}
			if ( parent != null ) {
				DotName parentClassName = parent.superName();
				accessType = getEntityAccess( parentClassName, indexBuilder );
			}
		}
		return accessType;

	}

	private static XMLAccessType getAccess(Map<DotName, List<AnnotationInstance>> annotations) {
		if ( annotations == null || annotations.isEmpty() || !isEntityObject( annotations ) ) {
			return null;
		}
		List<AnnotationInstance> accessAnnotationInstances = annotations.get( JPADotNames.ACCESS );
		if ( MockHelper.isNotEmpty( accessAnnotationInstances ) ) {
			for ( AnnotationInstance annotationInstance : accessAnnotationInstances ) {
				if ( annotationInstance.target() != null && annotationInstance.target() instanceof ClassInfo ) {
					return XMLAccessType.valueOf( annotationInstance.value().asEnum() );
				}
			}
		}
		return null;
	}

	private static boolean isEntityObject(Map<DotName, List<AnnotationInstance>> annotations) {
		return annotations.containsKey( ENTITY ) || annotations.containsKey( MAPPED_SUPERCLASS ) || annotations
				.containsKey( EMBEDDABLE );
	}

	static XMLAccessType getAccessFromAttributeAnnotation(DotName className, String attributeName, IndexBuilder indexBuilder) {
		Map<DotName, List<AnnotationInstance>> indexedAnnotations = indexBuilder.getIndexedAnnotations( className );
		if ( indexedAnnotations != null && indexedAnnotations.containsKey( ACCESS ) ) {
			List<AnnotationInstance> annotationInstances = indexedAnnotations.get( ACCESS );
			if ( MockHelper.isNotEmpty( annotationInstances ) ) {
				for ( AnnotationInstance annotationInstance : annotationInstances ) {
					AnnotationTarget indexedPropertyTarget = annotationInstance.target();
					if ( indexedPropertyTarget == null ) {
						continue;
					}
					if ( JandexHelper.getPropertyName( indexedPropertyTarget ).equals( attributeName ) ) {
						XMLAccessType accessType = XMLAccessType.valueOf( annotationInstance.value().asEnum() );
						XMLAccessType targetAccessType = ( indexedPropertyTarget instanceof MethodInfo ) ? XMLAccessType.PROPERTY : XMLAccessType.FIELD;
						if ( accessType == targetAccessType ) {
							return targetAccessType;
						}
					}
				}
			}
		}
		return null;
	}
}
