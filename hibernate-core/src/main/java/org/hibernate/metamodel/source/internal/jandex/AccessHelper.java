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
package org.hibernate.metamodel.source.internal.jandex;

import java.util.List;
import java.util.Map;
import javax.persistence.AccessType;

import org.hibernate.AssertionFailure;
import org.hibernate.MappingException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.source.internal.annotations.util.JPADotNames;
import org.hibernate.metamodel.source.internal.annotations.util.JandexHelper;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;

/**
 * @author Strong Liu
 */
public class AccessHelper implements JPADotNames {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			AccessHelper.class.getName()
	);

	static AccessType getAccessFromDefault(IndexBuilder indexBuilder) {
		AnnotationInstance annotationInstance = JandexHelper.getSingleAnnotation(
				indexBuilder.getAnnotations(),
				PseudoJpaDotNames.DEFAULT_ACCESS
		);
		if ( annotationInstance == null ) {
			return null;
		}
		else {
			return JandexHelper.getEnumValue(
					annotationInstance,
					"value",
					AccessType.class,
					indexBuilder.getServiceRegistry().getService( ClassLoaderService.class )
			);
		}

	}

	static AccessType getAccessFromIdPosition(DotName className, IndexBuilder indexBuilder) {
		Map<DotName, List<AnnotationInstance>> indexedAnnotations = indexBuilder.getIndexedAnnotations( className );
		Map<DotName, List<AnnotationInstance>> ormAnnotations = indexBuilder.getClassInfoAnnotationsMap( className );
		AccessType accessType = getAccessFromIdPosition( ormAnnotations );
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

	private static AccessType getAccessFromIdPosition(Map<DotName, List<AnnotationInstance>> annotations) {
		if ( annotations == null || annotations.isEmpty() || !( annotations.containsKey( ID ) ) ) {
			return null;
		}
		List<AnnotationInstance> idAnnotationInstances = annotations.get( ID );
		if ( CollectionHelper.isNotEmpty( idAnnotationInstances ) ) {
			return processIdAnnotations( idAnnotationInstances );
		}
		return null;
	}

	private static AccessType processIdAnnotations(List<AnnotationInstance> idAnnotations) {
		AccessType accessType = null;
		for ( AnnotationInstance annotation : idAnnotations ) {
			AnnotationTarget tmpTarget = annotation.target();
			if ( tmpTarget == null ) {
				throw new AssertionFailure( "@Id has no AnnotationTarget, this is mostly a internal error." );
			}
			if ( accessType == null ) {
				accessType = annotationTargetToAccessType( tmpTarget );
			}
			else {
				if ( !accessType.equals( annotationTargetToAccessType( tmpTarget ) ) ) {
					throw new MappingException( "Inconsistent placement of @Id annotation within hierarchy " );
				}
			}
		}
		return accessType;
	}

	static AccessType annotationTargetToAccessType(AnnotationTarget target) {
		return ( target instanceof MethodInfo ) ? AccessType.PROPERTY : AccessType.FIELD;
	}

	static AccessType getEntityAccess(DotName className, IndexBuilder indexBuilder) {
		Map<DotName, List<AnnotationInstance>> indexedAnnotations = indexBuilder.getIndexedAnnotations( className );
		Map<DotName, List<AnnotationInstance>> ormAnnotations = indexBuilder.getClassInfoAnnotationsMap( className );
		AccessType accessType = getAccess( ormAnnotations, indexBuilder );
		if ( accessType == null ) {
			accessType = getAccess( indexedAnnotations, indexBuilder );
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

	private static AccessType getAccess(Map<DotName, List<AnnotationInstance>> annotations, IndexBuilder indexBuilder) {
		if ( annotations == null || annotations.isEmpty() || !isEntityObject( annotations ) ) {
			return null;
		}
		List<AnnotationInstance> accessAnnotationInstances = annotations.get( JPADotNames.ACCESS );
		if ( CollectionHelper.isNotEmpty( accessAnnotationInstances ) ) {
			for ( AnnotationInstance annotationInstance : accessAnnotationInstances ) {
				if ( annotationInstance.target() != null && annotationInstance.target() instanceof ClassInfo ) {
					return JandexHelper.getEnumValue(
							annotationInstance,
							"value",
							AccessType.class,
							indexBuilder.getServiceRegistry().getService( ClassLoaderService.class )
					);
				}
			}
		}
		return null;
	}

	private static boolean isEntityObject(Map<DotName, List<AnnotationInstance>> annotations) {
		return annotations.containsKey( ENTITY ) || annotations.containsKey( MAPPED_SUPERCLASS ) || annotations
				.containsKey( EMBEDDABLE );
	}

	/**
	 * Get {@link javax.persistence.AccessType } from {@link javax.persistence.Access @Access} on the attribute of the given class
	 */
	static AccessType getAccessFromAttributeAnnotation(DotName className, String attributeName, IndexBuilder indexBuilder) {
		Map<DotName, List<AnnotationInstance>> indexedAnnotations = indexBuilder.getIndexedAnnotations( className );
		if ( indexedAnnotations != null && indexedAnnotations.containsKey( ACCESS ) ) {
			List<AnnotationInstance> annotationInstances = indexedAnnotations.get( ACCESS );
			if ( CollectionHelper.isNotEmpty( annotationInstances ) ) {
				for ( AnnotationInstance annotationInstance : annotationInstances ) {
					AnnotationTarget indexedPropertyTarget = annotationInstance.target();
					if ( indexedPropertyTarget == null ) {
						continue;
					}
					if ( JandexHelper.getPropertyName( indexedPropertyTarget ).equals( attributeName ) ) {
						AccessType accessType = JandexHelper.getEnumValue(
								annotationInstance,
								"value",
								AccessType.class,
								indexBuilder.getServiceRegistry().getService( ClassLoaderService.class )
						);
						/**
						 * here we ignore @Access(FIELD) on property (getter) and @Access(PROPERTY) on field
						 */
						AccessType targetAccessType = annotationTargetToAccessType( indexedPropertyTarget );
						if ( accessType.equals( targetAccessType ) ) {
							return targetAccessType;
						}
						else {
							LOG.warn(
									String.format(
											"%s.%s has @Access on %s, but it tries to assign the access type to %s, this is not allowed by JPA spec, and will be ignored.",
											className,
											attributeName,
											targetAccessType,
											accessType
									)
							);
						}
					}
				}
			}
		}
		return null;
	}
}
