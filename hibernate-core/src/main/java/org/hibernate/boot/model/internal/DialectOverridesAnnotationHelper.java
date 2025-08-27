/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.annotations.DialectOverride;
import org.hibernate.boot.models.annotations.spi.DialectOverrider;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.models.spi.AnnotationTarget;

import static org.hibernate.internal.util.collections.CollectionHelper.isNotEmpty;

/**
 * @author Sanne Grinovero
 * @author Steve Ebersole
 */
public class DialectOverridesAnnotationHelper {
	private static final Map<Class<? extends Annotation>, Class<? extends Annotation>> OVERRIDE_MAP = buildOverrideMap();

	private static Map<Class<? extends Annotation>, Class<? extends Annotation>> buildOverrideMap() {
		// not accessed concurrently
		final Map<Class<? extends Annotation>, Class<? extends Annotation>> results = new HashMap<>();
		for ( Class<?> dialectOverrideMember : DialectOverride.class.getNestMembers() ) {
			if ( dialectOverrideMember.isAnnotation() ) {
				final var overrideAnnotation =
						dialectOverrideMember.getAnnotation( DialectOverride.OverridesAnnotation.class );
				if ( overrideAnnotation != null ) {
					// The "real" annotation.  e.g. `org.hibernate.annotations.Formula`
					final var baseAnnotation = overrideAnnotation.value();
					// the "override" annotation.  e.g. `org.hibernate.annotations.DialectOverride.Formula`
					//noinspection unchecked
					final var dialectOverrideAnnotation = (Class<? extends Annotation>) dialectOverrideMember;
					results.put( baseAnnotation, dialectOverrideAnnotation );
				}
			}
		}
		return results;
	}

	public static <A extends Annotation, O extends Annotation> Class<O> getOverrideAnnotation(Class<A> annotationType) {
		final Class<O> overrideAnnotation = findOverrideAnnotation( annotationType );
		if ( overrideAnnotation == null ) {
			throw new HibernateException(
					String.format(
							Locale.ROOT,
							"Specified Annotation type (%s) does not have an override form",
							annotationType.getName()
					)
			);
		}
		else {
			return overrideAnnotation;
		}
	}

	public static <A extends Annotation, O extends Annotation> Class<O> findOverrideAnnotation(Class<A> annotationType) {
		//noinspection unchecked
		return (Class<O>) OVERRIDE_MAP.get( annotationType );
	}

	public static <T extends Annotation> T getOverridableAnnotation(
			AnnotationTarget element,
			Class<T> annotationType,
			MetadataBuildingContext context) {
		final var modelsContext = context.getBootstrapContext().getModelsContext();
		final var overrideAnnotation = OVERRIDE_MAP.get( annotationType );
		if ( overrideAnnotation != null ) {
			// the requested annotation does have a DialectOverride variant - look for matching one of those...
			final Dialect dialect = context.getMetadataCollector().getDatabase().getDialect();
			final Annotation[] overrides = element.getRepeatedAnnotationUsages( overrideAnnotation, modelsContext );
			if ( isNotEmpty( overrides ) ) {
				for ( Annotation annotation : overrides ) {
					//noinspection unchecked
					final var override = (DialectOverrider<T>) annotation;
					if ( override.matches( dialect ) ) {
						return override.override();
					}
				}
			}
		}

		// No override was found. Return the base annotation (if one)
		return element.getAnnotationUsage( annotationType, modelsContext );
	}
}
