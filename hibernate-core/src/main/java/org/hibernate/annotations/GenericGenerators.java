/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Array of generic generator definitions.
 *
 * @deprecated since {@link GenericGenerator} is deprecated.
 *
 * @author Paul Cowan
 */
@Target({PACKAGE, TYPE})
@Retention(RUNTIME)
@Deprecated(since = "6.5")
public @interface GenericGenerators {
	/**
	 * The aggregated generators.
	 */
	GenericGenerator[] value();
}
