package org.hibernate.annotations.common.reflection;

import java.lang.annotation.Annotation;

/**
 * @author Paolo Perrotta
 * @author Davide Marchignoli
 */
public interface XAnnotatedElement {

	<T extends Annotation> T getAnnotation(Class<T> annotationType);

	<T extends Annotation> boolean isAnnotationPresent(Class<T> annotationType);

	Annotation[] getAnnotations();

	/**
	 * Returns true if the underlying artefact
	 * is the same
	 */
	boolean equals(Object x);
}
