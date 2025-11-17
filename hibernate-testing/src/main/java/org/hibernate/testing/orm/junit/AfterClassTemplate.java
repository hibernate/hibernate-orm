/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.junit;

import org.hibernate.Incubating;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Similar to JUnit's {@link org.junit.jupiter.api.AfterAll} but called after
 * each template invocation in a {@link @ClassTemplate} test class.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Incubating
public @interface AfterClassTemplate {
}
