/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.annotations;

import java.lang.annotation.Annotation;
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

	/**
	 * @deprecated Prefer using {@link #annotatedElementSignature(JavaXMember)}
	 */
	@Deprecated
	public static String annotatedElementSignature(XProperty xProperty) {
		return getUnderlyingMember( xProperty ).toString();
	}

	public static String annotatedElementSignature(final JavaXMember jxProperty) {
		return getUnderlyingMember( jxProperty ).toString();
	}

	/**
	 * @deprecated Prefer using {@link #getUnderlyingMember(JavaXMember)}
	 */
	@Deprecated
	public static Member getUnderlyingMember(XProperty xProperty) {
		if (xProperty instanceof JavaXMember) {
			JavaXMember jx = (JavaXMember)xProperty;
			return jx.getMember();
		}
		else {
			throw new org.hibernate.HibernateException( "Can only extract Member from a XProperty which is a JavaXMember" );
		}
	}

	public static Member getUnderlyingMember(final JavaXMember jxProperty) {
		return jxProperty.getMember();
	}

	/**
	 * Locate an annotation on an annotated member, allowing for composed annotations (meta-annotations).
	 *
	 * @implNote Searches only one level deep
	 */
	public static <T extends Annotation> T findAnnotation(XAnnotatedElement xAnnotatedElement, Class<T> annotationType) {
		// first, see if we can find it directly...
		final T direct = xAnnotatedElement.getAnnotation( annotationType );
		if ( direct != null ) {
			return direct;
		}

		// or as composed...
		for ( int i = 0; i < xAnnotatedElement.getAnnotations().length; i++ ) {
			final Annotation annotation = xAnnotatedElement.getAnnotations()[ i ];
			if ( annotationType.equals( annotation.getClass() ) ) {
				// we would have found this on the direct search, so no need
				// to check its meta-annotations
				continue;
			}

			// we only check one level deep
			final T metaAnn = annotation.annotationType().getAnnotation( annotationType );
			if ( metaAnn != null ) {
				return metaAnn;
			}
		}

		return null;
	}

	/**
	 * Locate the annotation, relative to `xAnnotatedElement`, which contains
	 * the passed type of annotation.
	 *
	 * @implNote Searches only one level deep
	 */
	public static <A extends Annotation, T extends Annotation> A findContainingAnnotation(
			XAnnotatedElement xAnnotatedElement,
			Class<T> annotationType) {

		for ( int i = 0; i < xAnnotatedElement.getAnnotations().length; i++ ) {
			final Annotation annotation = xAnnotatedElement.getAnnotations()[ i ];
			// annotation = @Sequence

			final T metaAnn = annotation.annotationType().getAnnotation( annotationType );
			if ( metaAnn != null ) {
				//noinspection unchecked
				return (A) annotation;
			}
		}

		return null;
	}
}
