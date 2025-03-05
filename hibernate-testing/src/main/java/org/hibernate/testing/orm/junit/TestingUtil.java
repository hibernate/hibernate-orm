/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.junit;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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

	public static <A extends Annotation> Collection<A> collectAnnotations(
			ExtensionContext context,
			Class<A> annotationType,
			Class<? extends Annotation> groupAnnotationType) {
		return collectAnnotations(
				context,
				annotationType,
				groupAnnotationType,
				(methodAnnotation, methodAnnotations, classAnnotation, classAnnotations) -> {
					final List<A> list = new ArrayList<>();
					if ( methodAnnotation != null ) {
						list.add( methodAnnotation );
					}
					else if ( classAnnotation != null ) {
						list.add( classAnnotation );
					}
					if ( methodAnnotations != null ) {
						list.addAll( Arrays.asList( methodAnnotations ) );
					}
					else if ( classAnnotations != null ) {
						list.addAll( Arrays.asList( classAnnotations ) );
					}
					return list;
				}
		);
	}

	public static <A extends Annotation> Collection<A> collectAnnotations(
			ExtensionContext context,
			Class<A> annotationType,
			Class<? extends Annotation> groupAnnotationType,
			TestAnnotationCollector<A> collector) {
		if ( !context.getElement().isPresent() ) {
			return Collections.emptyList();
		}

		final AnnotatedElement annotatedElement = context.getElement().get();
		final A methodAnnotation = annotatedElement.getAnnotation( annotationType );
		final A classAnnotation = context.getTestInstance().map( i -> i.getClass().getAnnotation( annotationType ) ).orElse( null );
		final Annotation methodPluralAnn = annotatedElement.getAnnotation( groupAnnotationType );
		final Annotation classPluralAnn = context.getTestInstance().map( i -> i.getClass().getAnnotation( groupAnnotationType ) ).orElse( null );
		final A[] methodAnnotations;
		final A[] classAnnotations;
		if ( methodPluralAnn != null ) {
			try {
				methodAnnotations = (A[]) groupAnnotationType.getDeclaredMethod( "value", null ).invoke( methodPluralAnn );
			}
			catch (Exception e) {
				throw new RuntimeException( e );
			}
		}
		else {
			methodAnnotations = null;
		}
		if ( classPluralAnn != null ) {
			try {
				classAnnotations = (A[]) groupAnnotationType.getDeclaredMethod( "value", null ).invoke( classPluralAnn );
			}
			catch (Exception e) {
				throw new RuntimeException( e );
			}
		}
		else {
			classAnnotations = null;
		}

		return collector.collect( methodAnnotation, methodAnnotations, classAnnotation, classAnnotations );
	}

	public static <A extends Annotation> boolean hasEffectiveAnnotation(ExtensionContext context, Class<A> annotationType) {
		return findEffectiveAnnotation( context, annotationType ).isPresent();
	}

	@SuppressWarnings("unchecked")
	public static <T> T cast(Object thing, Class<T> type) {
		assertThat( thing, instanceOf( type ) );
		return type.cast( thing );
	}

	public interface TestAnnotationCollector<S> {
		Collection<S> collect(S methodAnnotation, S[] methodAnnotations, S classAnnotation, S[] classAnnotations);
	}
}
