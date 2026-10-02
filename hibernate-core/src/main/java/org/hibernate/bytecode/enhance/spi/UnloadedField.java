package org.hibernate.bytecode.enhance.spi;

import java.lang.annotation.Annotation;

public interface UnloadedField {

	boolean hasAnnotation(Class<? extends Annotation> annotationType);
}
