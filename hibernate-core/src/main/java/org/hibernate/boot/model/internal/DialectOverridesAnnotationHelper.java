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
import org.hibernate.models.spi.ModelsContext;

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

		final Class<?>[] dialectOverrideMembers = DialectOverride.class.getNestMembers();
		for ( Class<?> dialectOverrideMember : dialectOverrideMembers ) {
			if ( !dialectOverrideMember.isAnnotation() ) {
				continue;
			}

			final DialectOverride.OverridesAnnotation overrideAnnotation = dialectOverrideMember.getAnnotation( DialectOverride.OverridesAnnotation.class );
			if ( overrideAnnotation == null ) {
				continue;
			}

			// The "real" annotation.  e.g. `org.hibernate.annotations.Formula`
			final Class<? extends Annotation> baseAnnotation = overrideAnnotation.value();

			// the "override" annotation.  e.g. `org.hibernate.annotations.DialectOverride.Formula`
			//noinspection unchecked
			final Class<? extends Annotation> dialectOverrideAnnotation = (Class<? extends Annotation>) dialectOverrideMember;

			results.put( baseAnnotation, dialectOverrideAnnotation );
		}

		return results;
	}

	public static <A extends Annotation, O extends Annotation> Class<O> getOverrideAnnotation(Class<A> annotationType) {
		final Class<O> overrideAnnotation = findOverrideAnnotation( annotationType );
		if ( overrideAnnotation != null ) {
			return overrideAnnotation;
		}
		throw new HibernateException(
				String.format(
						Locale.ROOT,
						"Specified Annotation type (%s) does not have an override form",
						annotationType.getName()
				)
		);
	}

	public static <A extends Annotation, O extends Annotation> Class<O> findOverrideAnnotation(Class<A> annotationType) {
		//noinspection unchecked
		return (Class<O>) OVERRIDE_MAP.get( annotationType );
	}

	public static <T extends Annotation> T getOverridableAnnotation(
			AnnotationTarget element,
			Class<T> annotationType,
			MetadataBuildingContext context) {
		final ModelsContext modelsContext = context.getBootstrapContext().getModelsContext();
		final Class<? extends Annotation> overrideAnnotation = OVERRIDE_MAP.get( annotationType );

		if ( overrideAnnotation != null ) {
			// the requested annotation does have a DialectOverride variant - look for matching one of those...
			final Dialect dialect = context.getMetadataCollector().getDatabase().getDialect();

			final Annotation[] overrides = element.getRepeatedAnnotationUsages( overrideAnnotation, modelsContext );
			if ( isNotEmpty( overrides ) ) {
				for ( int i = 0; i < overrides.length; i++ ) {
					//noinspection unchecked
					final DialectOverrider<T> override = (DialectOverrider<T>) overrides[i];
					if ( override.matches( dialect ) ) {
						return override.override();
					}
				}
			}
		}

		// no override was found.  return the base annotation (if one)
		return element.getAnnotationUsage( annotationType, modelsContext );
	}

	public static boolean overrideMatchesDialect(DialectOverrider<?> override, Dialect dialect) {
		return override.matches( dialect );
	}
}
