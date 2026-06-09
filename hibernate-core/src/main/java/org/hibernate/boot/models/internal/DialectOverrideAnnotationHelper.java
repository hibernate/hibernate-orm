/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.internal;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.annotations.DialectOverride;
import org.hibernate.boot.models.annotations.spi.DialectOverrider;
import org.hibernate.dialect.Dialect;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.models.spi.ModelsContext;

import static org.hibernate.internal.util.collections.CollectionHelper.isNotEmpty;

/**
 * Dialect-specific annotation override resolution for the boot model pipeline.
 */
public final class DialectOverrideAnnotationHelper {
	private static final Map<Class<? extends Annotation>, Class<? extends Annotation>> OVERRIDE_MAP = buildOverrideMap();

	private DialectOverrideAnnotationHelper() {
	}

	private static Map<Class<? extends Annotation>, Class<? extends Annotation>> buildOverrideMap() {
		final Map<Class<? extends Annotation>, Class<? extends Annotation>> results = new HashMap<>();
		for ( Class<?> dialectOverrideMember : DialectOverride.class.getNestMembers() ) {
			if ( dialectOverrideMember.isAnnotation() ) {
				final var overrideAnnotation =
						dialectOverrideMember.getAnnotation( DialectOverride.OverridesAnnotation.class );
				if ( overrideAnnotation != null ) {
					//noinspection unchecked
					results.put(
							overrideAnnotation.value(),
							(Class<? extends Annotation>) dialectOverrideMember
					);
				}
			}
		}
		return results;
	}

	public static <A extends Annotation> A getOverridableAnnotation(
			AnnotationTarget target,
			Class<A> annotationType,
			Dialect dialect,
			ModelsContext modelsContext) {
		final Class<? extends Annotation> overrideAnnotation = OVERRIDE_MAP.get( annotationType );
		if ( overrideAnnotation != null ) {
			final Annotation[] overrides = target.getRepeatedAnnotationUsages( overrideAnnotation, modelsContext );
			if ( isNotEmpty( overrides ) ) {
				for ( Annotation annotation : overrides ) {
					//noinspection unchecked
					final DialectOverrider<A> override = (DialectOverrider<A>) annotation;
					if ( override.matches( dialect ) ) {
						return override.override();
					}
				}
			}
		}
		return target.getAnnotationUsage( annotationType, modelsContext );
	}

	public static <A extends Annotation> A[] getOverridableAnnotationUsages(
			AnnotationTarget target,
			Class<A> annotationType,
			Dialect dialect,
			ModelsContext modelsContext) {
		final ArrayList<A> result = new ArrayList<>();
		final Class<? extends Annotation> overrideAnnotation = OVERRIDE_MAP.get( annotationType );
		if ( overrideAnnotation != null ) {
			final Annotation[] overrides = target.getRepeatedAnnotationUsages( overrideAnnotation, modelsContext );
			if ( isNotEmpty( overrides ) ) {
				for ( Annotation annotation : overrides ) {
					//noinspection unchecked
					final DialectOverrider<A> override = (DialectOverrider<A>) annotation;
					if ( override.matches( dialect ) ) {
						result.add( override.override() );
					}
				}
			}
		}
		final A[] baseAnnotations = target.getRepeatedAnnotationUsages( annotationType, modelsContext );
		if ( !result.isEmpty() ) {
			return result.toArray( baseAnnotations );
		}
		if ( isNotEmpty( baseAnnotations ) ) {
			for ( A baseAnnotation : baseAnnotations ) {
				result.add( baseAnnotation );
			}
		}
		return result.toArray( baseAnnotations );
	}
}
