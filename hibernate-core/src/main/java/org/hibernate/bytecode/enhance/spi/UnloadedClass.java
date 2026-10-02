package org.hibernate.bytecode.enhance.spi;

import java.lang.annotation.Annotation;

public interface UnloadedClass {

	boolean hasAnnotation(Class<? extends Annotation> annotationType);

	String getName();
}
