/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.junit4;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.FailureExpected;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;

/**
 * Centralized utility functionality
 *
 * @author Steve Ebersole
 */
public final class Helper {
	public static final String VALIDATE_FAILURE_EXPECTED = FailureExpected.VALIDATE_FAILURE_EXPECTED;

	private Helper() {
	}

	/**
	 * Standard string content checking.
	 *
	 * @param string The string to check
	 *
	 * @return Are its content empty or the reference null?
	 */
	public static boolean isNotEmpty(String string) {
		return string != null && string.length() > 0;
	}

	/**
	 * Extract a nice test name representation for display
	 *
	 * @param frameworkMethod The test method.
	 *
	 * @return The display representation
	 */
	public static String extractTestName(FrameworkMethod frameworkMethod) {
		return frameworkMethod.getMethod().getDeclaringClass().getName() + '#' + frameworkMethod.getName();
	}

	/**
	 * Extract a nice method name representation for display
	 *
	 * @param method The method.
	 *
	 * @return The display representation
	 */
	public static String extractMethodName(Method method) {
		return method.getDeclaringClass().getName() + "#" + method.getName();
	}

	public static <T extends Annotation> T locateAnnotation(
			Class<T> annotationClass,
			FrameworkMethod frameworkMethod,
			TestClass testClass) {
		T annotation = frameworkMethod.getAnnotation( annotationClass );
		if ( annotation == null ) {
			annotation = testClass.getJavaClass().getAnnotation( annotationClass );
		}
		return annotation;
	}

	/**
	 * Locates the specified annotation both at the method site and class site.
	 *
	 * This is useful for situations where you may apply the same annotation at both the method
	 * and class level and rather than both sites being mutually exclusive, this permits both
	 * to be returned instead.
	 *
	 * @param annotationClass Annotation class
	 * @param frameworkMethod Test method.
	 * @param testClass Test class.
	 * @param <T> Annotation type.
	 *
	 * @return Collection of all annotations detected at both method or class level.
	 */
	public static <T extends Annotation> List<T> locateAllAnnotations(
			Class<T> annotationClass,
			FrameworkMethod frameworkMethod,
			TestClass testClass) {
		final List<T> annotations = new LinkedList<>();

		T annotation = frameworkMethod.getAnnotation( annotationClass );
		if ( annotation != null ) {
			annotations.add( annotation );
		}

		annotation = testClass.getJavaClass().getAnnotation( annotationClass );
		if ( annotation != null ) {
			annotations.add( annotation );
		}
		return annotations;
	}

	/**
	 * @param singularAnnotationClass Singular annotation class (e.g. {@link org.hibernate.testing.SkipForDialect}).
	 * @param pluralAnnotationClass Plural annotation class (e.g. {@link org.hibernate.testing.SkipForDialects}),
	 * assuming that the only declared method is an array of singular annotations.
	 * @param frameworkMethod Test method.
	 * @param testClass Test class.
	 * @param <S> Singular annotation type.
	 * @param <P> Plural annotation type.
	 *
	 * @return Collection of all singular annotations or an empty list.
	 */
	@SuppressWarnings("unchecked")
	public static <S extends Annotation, P extends Annotation> List<S> collectAnnotations(
			Class<S> singularAnnotationClass,
			Class<P> pluralAnnotationClass,
			FrameworkMethod frameworkMethod,
			TestClass testClass) {
		final List<S> collection = new LinkedList<S>();
		final S singularAnn = Helper.locateAnnotation( singularAnnotationClass, frameworkMethod, testClass );
		if ( singularAnn != null ) {
			collection.add( singularAnn );
		}
		final P pluralAnn = Helper.locateAnnotation( pluralAnnotationClass, frameworkMethod, testClass );
		if ( pluralAnn != null ) {
			try {
				collection.addAll( Arrays.asList( (S[]) pluralAnnotationClass.getDeclaredMethods()[0].invoke( pluralAnn ) ) );
			}
			catch (Exception e) {
				throw new RuntimeException( e );
			}
		}
		return collection;
	}

	public static String extractMessage(FailureExpected failureExpected) {
		StringBuilder buffer = new StringBuilder();
		buffer.append( '(' ).append( failureExpected.jiraKey() ).append( ')' );
		if ( isNotEmpty( failureExpected.message() ) ) {
			buffer.append( " : " ).append( failureExpected.message() );
		}
		return buffer.toString();
	}

	public static String extractIgnoreMessage(FailureExpected failureExpected, FrameworkMethod frameworkMethod) {
		return new StringBuilder( "Ignoring test [" )
				.append( Helper.extractTestName( frameworkMethod ) )
				.append( "] due to @FailureExpected - " )
				.append( extractMessage( failureExpected ) )
				.toString();
	}

	/**
	 * @see #createH2Schema(String, Map)
	 */
	public static void createH2Schema(String schemaName, Configuration cfg) {
		createH2Schema( schemaName, cfg.getProperties() );
	}

	/**
	 * Create additional H2 schema.
	 *
	 * @param schemaName New schema name.
	 * @param settings Current settings.
	 */
	public static void createH2Schema(String schemaName, Map settings) {
		settings.put(
				Environment.URL,
				settings.get( Environment.URL ) + ";INIT=CREATE SCHEMA IF NOT EXISTS " + schemaName
		);
	}
}
