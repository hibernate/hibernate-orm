/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.annotations.common.reflection.java;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

import org.hibernate.annotations.common.reflection.AnnotationReader;
import org.hibernate.annotations.common.reflection.XAnnotatedElement;

/**
 * @author Paolo Perrotta
 * @author Davide Marchignoli
 */
abstract class JavaXAnnotatedElement implements XAnnotatedElement {

	private final JavaReflectionManager factory;

	private final AnnotatedElement annotatedElement;

	public JavaXAnnotatedElement(AnnotatedElement annotatedElement, JavaReflectionManager factory) {
        this.factory = factory;
		this.annotatedElement = annotatedElement;
	}

	protected JavaReflectionManager getFactory() {
		return factory;
	}

	private AnnotationReader getAnnotationReader() {
        return factory.buildAnnotationReader(annotatedElement);
	}

	public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
		return getAnnotationReader().getAnnotation( annotationType );
	}

	public <T extends Annotation> boolean isAnnotationPresent(Class<T> annotationType) {
		return getAnnotationReader().isAnnotationPresent( annotationType );
	}

	public Annotation[] getAnnotations() {
		return getAnnotationReader().getAnnotations();
	}

	AnnotatedElement toAnnotatedElement() {
		return annotatedElement;
	}

	@Override
	public boolean equals(Object obj) {
		if ( ! ( obj instanceof JavaXAnnotatedElement ) ) return false;
		JavaXAnnotatedElement other = (JavaXAnnotatedElement) obj;
		//FIXME yuk this defeat the type environment
		return annotatedElement.equals( other.toAnnotatedElement() );
	}

	@Override
	public int hashCode() {
		return annotatedElement.hashCode();
	}

	@Override
	public String toString() {
		return annotatedElement.toString();
	}
}
