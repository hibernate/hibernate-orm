/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.models.internal.OrmAnnotationDescriptor;
import org.hibernate.models.spi.AnnotationDescriptor;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractAnnotationTypeAnnotations;

/**
 * Much the same as {@linkplain org.hibernate.models.internal.OrmAnnotationDescriptor} except for
 * special cases where we do care about annotations on the annotation type
 *
 * @author Steve Ebersole
 */
public class SpecializedAnnotationDescriptor<A extends Annotation,C extends A> extends OrmAnnotationDescriptor<A,C> {
	private final Map<Class<? extends Annotation>, ? extends Annotation> usagesMap;

	public SpecializedAnnotationDescriptor(Class<A> annotationType, Class<C> concreteType) {
		super( annotationType, concreteType );
		usagesMap = extractAnnotationsMap( annotationType );
	}

	public SpecializedAnnotationDescriptor(Class<A> annotationType, Class<C> concreteType, AnnotationDescriptor<? extends Annotation> container) {
		super( annotationType, concreteType, container );
		usagesMap = extractAnnotationsMap( annotationType );
	}

	@Override
	public Map<Class<? extends Annotation>, ? extends Annotation> getUsageMap() {
		return usagesMap;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Map<Class<? extends Annotation>, ? extends Annotation> extractAnnotationsMap(Class<A> annotationType) {
		final List<Annotation> annotationTypeAnnotations = extractAnnotationTypeAnnotations( annotationType );
		if ( CollectionHelper.isEmpty( annotationTypeAnnotations ) ) {
			return Collections.emptyMap();
		}

		final HashMap result = new HashMap<>();
		annotationTypeAnnotations.forEach( (annotationTypeAnnotation) -> {
			result.put( annotationTypeAnnotation.annotationType(), annotationTypeAnnotation );
		} );
		return result;
	}
}
