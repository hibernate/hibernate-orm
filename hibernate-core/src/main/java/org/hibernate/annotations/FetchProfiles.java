/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Collects together multiple fetch profiles.
 *
 * @author Hardy Ferentschik
 */
@Target({ TYPE, PACKAGE })
@Retention(RUNTIME)
public @interface FetchProfiles {
	/**
	 * The aggregated fetch profiles.
	 */
	FetchProfile[] value();
}
