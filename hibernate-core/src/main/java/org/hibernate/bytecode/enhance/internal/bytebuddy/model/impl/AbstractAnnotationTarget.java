/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy.model.impl;

import java.lang.annotation.Annotation;

import org.hibernate.bytecode.enhance.internal.bytebuddy.model.AnnotationTarget;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractAnnotationTarget implements AnnotationTarget {
	private final AnnotationList annotationAccess;

	public AbstractAnnotationTarget(AnnotationList annotationAccess) {
		this.annotationAccess = annotationAccess;
	}

	@Override
	public <A extends Annotation> boolean hasAnnotation(Class<A> type) {
		return annotationAccess.isAnnotationPresent( type );
	}

	@Override
	public <A extends Annotation> A getAnnotation(Class<A> type) {
		final AnnotationDescription.Loadable<A> reference = annotationAccess.ofType( type );
		if ( reference == null ) {
			return null;
		}
		return reference.load();
	}
}
