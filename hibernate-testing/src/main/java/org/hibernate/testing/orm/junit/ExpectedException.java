/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Annotation that can be used, in conjunction with {@link ExpectedExceptionExtension},
 * to indicate that a specific test is expected to fail in a particular way
 * (throw the specified exception) as its "success condition".
 *
 * @see ExpectedExceptionExtension
 *
 * @author Steve Ebersole
 */
@Inherited
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)

@ExtendWith( ExpectedExceptionExtension.class )
public @interface ExpectedException {
	Class<? extends Throwable> value();
}
