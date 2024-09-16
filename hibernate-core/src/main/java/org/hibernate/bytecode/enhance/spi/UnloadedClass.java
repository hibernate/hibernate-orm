/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.enhance.spi;

import java.lang.annotation.Annotation;

public interface UnloadedClass {

	boolean hasAnnotation(Class<? extends Annotation> annotationType);

	String getName();
}
