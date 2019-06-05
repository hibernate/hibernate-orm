/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.junit;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
public class TestingUtil {

	private TestingUtil() {
	}

	public static <A extends Annotation> Optional<A> findEffectiveAnnotation(
			ExtensionContext context,
			Class<A> annotationType) {
		if ( !context.getElement().isPresent() ) {
			return Optional.empty();
		}

		final AnnotatedElement annotatedElement = context.getElement().get();

		final Optional<A> direct = AnnotationSupport.findAnnotation( annotatedElement, annotationType );
		if ( direct.isPresent() ) {
			return direct;
		}

		if ( context.getTestInstance().isPresent() ) {
			return AnnotationSupport.findAnnotation( context.getRequiredTestInstance().getClass(), annotationType );
		}

		return Optional.empty();
	}

	public static <A extends Annotation> List<A> findEffectiveRepeatingAnnotation(
			ExtensionContext context,
			Class<A> annotationType,
			Class<? extends Annotation> groupAnnotationType) {
		if ( !context.getElement().isPresent() ) {
			return Collections.emptyList();
		}

		final Optional<A> effectiveAnnotation = findEffectiveAnnotation( context, annotationType );
		final Optional<? extends Annotation> effectiveGroupingAnnotation = findEffectiveAnnotation(
				context,
				groupAnnotationType
		);

		if ( effectiveAnnotation.isPresent() || effectiveGroupingAnnotation.isPresent() ) {
			if ( !effectiveGroupingAnnotation.isPresent() ) {
				return Collections.singletonList( effectiveAnnotation.get() );
			}

			final List<A> list = new ArrayList<>();
			effectiveAnnotation.ifPresent( list::add );

			final Method valueMethod;
			try {
				valueMethod = groupAnnotationType.getDeclaredMethod( "value", null );

				Collections.addAll( list, (A[]) valueMethod.invoke( effectiveGroupingAnnotation.get() ) );
			}
			catch (Exception e) {
				throw new RuntimeException( "Could not locate repeated/grouped annotations", e );
			}

			return list;
		}

		return Collections.emptyList();
	}

	public static <A extends Annotation> boolean hasEffectiveAnnotation(ExtensionContext context, Class<A> annotationType) {
		return findEffectiveAnnotation( context, annotationType ).isPresent();
	}

	@SuppressWarnings("unchecked")
	public static <T> T cast(Object thing, Class<T> type) {
		assertThat( thing, instanceOf( type ) );
		return type.cast( thing );
	}
}
