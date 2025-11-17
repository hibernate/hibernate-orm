/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * A group of {@link FetchProfileOverride}s.
 *
 * @author Gavin King
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface FetchProfileOverrides {
	FetchProfileOverride[] value();
}
