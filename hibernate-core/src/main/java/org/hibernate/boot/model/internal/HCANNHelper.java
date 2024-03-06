/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;

import org.hibernate.Internal;
import org.hibernate.annotations.common.reflection.XAnnotatedElement;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.annotations.common.reflection.java.JavaXMember;

/**
 * Manage the various fun-ness of dealing with HCANN...
 *
 * @author Steve Ebersole
 */
@Internal
public final class HCANNHelper {

	public static boolean hasAnnotation(
			AnnotatedElement element,
			Class<? extends Annotation> annotationToCheck) {
		if ( element == null ) {
			return false;
		}

		return element.isAnnotationPresent( annotationToCheck );
	}

	public static boolean hasAnnotation(
			AnnotatedElement element,
			Class<? extends Annotation> annotationToCheck,
			Class<? extends Annotation> annotationToCheck2) {
		if ( element == null ) {
			return false;
		}

		return element.isAnnotationPresent( annotationToCheck )
			|| element.isAnnotationPresent( annotationToCheck2 );
	}

	public static boolean hasAnnotation(
			XAnnotatedElement element,
			Class<? extends Annotation> annotationToCheck) {
		if ( element == null ) {
			return false;
		}

		return element.isAnnotationPresent( annotationToCheck );
	}

	public static boolean hasAnnotation(
			XAnnotatedElement element,
			Class<? extends Annotation> annotationToCheck,
			Class<? extends Annotation> annotationToCheck2) {
		if ( element == null ) {
			return false;
		}

		return element.isAnnotationPresent( annotationToCheck )
			|| element.isAnnotationPresent( annotationToCheck2);
	}

	public static boolean hasAnnotation(
			XAnnotatedElement element,
			Class<? extends Annotation>... annotationsToCheck) {
		assert annotationsToCheck != null && annotationsToCheck.length > 0;

		if ( element == null ) {
			return false;
		}

		for ( int i = 0; i < annotationsToCheck.length; i++ ) {
			if ( element.isAnnotationPresent( annotationsToCheck[i] ) ) {
				return true;
			}
		}

		return false;
	}

	/**
	 * @deprecated Prefer using {@link #annotatedElementSignature(JavaXMember)}
	 */
	@Deprecated
	public static String annotatedElementSignature(XProperty property) {
		return getUnderlyingMember( property ).toString();
	}

	public static String annotatedElementSignature(final JavaXMember member) {
		return getUnderlyingMember( member ).toString();
	}

	/**
	 * @deprecated Prefer using {@link #getUnderlyingMember(JavaXMember)}
	 */
	@Deprecated
	public static Member getUnderlyingMember(XProperty property) {
		if ( property instanceof JavaXMember ) {
			JavaXMember member = (JavaXMember) property;
			return member.getMember();
		}
		else {
			throw new org.hibernate.HibernateException( "Can only extract Member from a XProperty which is a JavaXMember" );
		}
	}

	public static Member getUnderlyingMember(final JavaXMember member) {
		return member.getMember();
	}

}
