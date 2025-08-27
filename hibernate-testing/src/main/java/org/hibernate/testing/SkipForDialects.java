/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Plural annotation for {@link SkipForDialect}.
 * Useful when more than one dialect needs to be skipped because of a different reason.
 *
 * @author Lukasz Antoniak
 * @deprecated Use JUnit 5 and {@link org.hibernate.testing.orm.junit.SkipForDialectGroup} instead.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
@Deprecated(forRemoval = true)
public @interface SkipForDialects {
	SkipForDialect[] value();
}
