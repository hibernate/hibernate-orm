/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation allowing to specify which classes should be passed to the annotation processor for compilation.
 *
 * @author Hardy Ferentschik
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface WithClasses {
	Class<?>[] value();

	/**
	 * @return an array of classes which should be complied prior to compiling the actual test classes
	 */
	Class<?>[] preCompile() default { };

	String[] sources() default {};
}
