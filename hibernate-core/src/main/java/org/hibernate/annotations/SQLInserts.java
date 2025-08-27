/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * A grouping of {@link SQLInsert}s.
 *
 * @since 6.2
 * @author Gavin King
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface SQLInserts {
	SQLInsert[] value();
}
