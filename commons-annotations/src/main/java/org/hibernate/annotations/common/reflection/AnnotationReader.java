package org.hibernate.annotations.common.reflection;

import java.lang.annotation.Annotation;

/**
 * @author Paolo Perrotta
 */
public interface AnnotationReader {

    public <T extends Annotation> T getAnnotation(Class<T> annotationType);

    public <T extends Annotation> boolean isAnnotationPresent(Class<T> annotationType);

    public Annotation[] getAnnotations();
}