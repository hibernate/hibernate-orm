package org.hibernate.annotations.common.reflection.java;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

import org.hibernate.annotations.common.reflection.AnnotationReader;

/**
 * Reads standard Java annotations.
 *
 * @author Paolo Perrotta
 * @author Davide Marchignoli
 */
class JavaAnnotationReader implements AnnotationReader {

	protected final AnnotatedElement element;

	public JavaAnnotationReader(AnnotatedElement el) {
		this.element = el;
	}

	public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
		return element.getAnnotation( annotationType );
	}

	public <T extends Annotation> boolean isAnnotationPresent(Class<T> annotationType) {
		return element.isAnnotationPresent( annotationType );
	}

	public Annotation[] getAnnotations() {
		return element.getAnnotations();
	}
}
