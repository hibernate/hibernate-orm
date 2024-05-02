/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.internal;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.annotations.DialectOverride;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.models.spi.AnnotationUsage;
import org.hibernate.models.spi.ClassDetails;

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

	public static <T extends Annotation> AnnotationUsage<T> getOverridableAnnotation(
			AnnotationTarget element,
			Class<T> annotationType,
			MetadataBuildingContext context) {
		final Class<? extends Annotation> overrideAnnotation = OVERRIDE_MAP.get( annotationType );
		if ( overrideAnnotation != null ) {
			// the requested annotation does have a DialectOverride variant - look for matching one of those...
			final Dialect dialect = context.getMetadataCollector().getDatabase().getDialect();
			final DatabaseVersion version = dialect.getVersion();

			final List<? extends AnnotationUsage<? extends Annotation>> overrides = element.getRepeatedAnnotationUsages( overrideAnnotation );
			for ( AnnotationUsage<? extends Annotation> override : overrides ) {
				if ( overrideMatchesDialect( override, dialect ) ) {
					// we found an override match...
					// the override's `override` attribute is the thing to return
					return override.getNestedUsage( "override" );
				}
			}
		}

		// no override was found.  return the base annotation (if one)
		return element.getSingleAnnotationUsage( annotationType );
	}

	public static boolean overrideMatchesDialect(AnnotationUsage<? extends Annotation> override, Dialect dialect) {
		final ClassDetails overrideDialect = override.getClassDetails( "dialect" );
		final Class<? extends Dialect> overrideDialectJavaType = overrideDialect.toJavaClass();
		if ( !overrideDialectJavaType.isAssignableFrom( dialect.getClass() ) ) {
			return false;
		}

		final AnnotationUsage<DialectOverride.Version> beforeAnn = override.getNestedUsage( "before" );
		final AnnotationUsage<DialectOverride.Version> sameOrAfterAnn = override.getNestedUsage( "sameOrAfter" );
		final DatabaseVersion version = dialect.getVersion();

		if ( version.isBefore( beforeAnn.getInteger( "major" ), beforeAnn.getInteger( "minor" ) )
				&& version.isSameOrAfter( sameOrAfterAnn.getInteger( "major" ), sameOrAfterAnn.getInteger( "minor" ) ) ) {
			return true;
		}

		return false;
	}
}
