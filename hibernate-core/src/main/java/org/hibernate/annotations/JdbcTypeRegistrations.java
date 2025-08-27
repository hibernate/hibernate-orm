/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Grouping of {@link JdbcTypeRegistration}
 *
 * See notes on {@link JdbcTypeRegistration} about using on packages
 * versus use on classes
 *
 * @since 6.0
 */
@Target({PACKAGE, TYPE})
@Inherited
@Retention(RUNTIME)
public @interface JdbcTypeRegistrations {
	JdbcTypeRegistration[] value();
}
