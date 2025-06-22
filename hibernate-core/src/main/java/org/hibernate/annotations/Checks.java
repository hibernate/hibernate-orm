/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * A list of {@link Check}s.
 *
 * @deprecated since {@link Check} is deprecated.
 *
 * @author Gavin King
 */
@Target({TYPE, METHOD, FIELD})
@Retention(RUNTIME)
@Deprecated(since = "7")
public @interface Checks {
	Check[] value();
}
