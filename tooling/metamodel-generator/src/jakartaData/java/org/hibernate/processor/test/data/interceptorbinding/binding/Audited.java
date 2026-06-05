/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.interceptorbinding.binding;

import jakarta.interceptor.InterceptorBinding;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@InterceptorBinding
@Target({ TYPE, METHOD })
@Retention(RUNTIME)
public @interface Audited {
	Mode mode() default Mode.STANDARD;
	Class<?> entity() default Object.class;
	Class<?> arrayType() default Object.class;
	Nested nested() default @Nested;
	Nested[] nestedArray() default {};

	enum Mode {
		STANDARD,
		STRICT
	}

	@interface Nested {
		Class<?> value() default Object.class;
	}
}
