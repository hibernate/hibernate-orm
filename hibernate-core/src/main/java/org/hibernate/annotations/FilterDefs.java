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
 * Array of filter definitions.
 *
 * @author Matthew Inger
 * @author Emmanuel Bernard
 */
@Target({PACKAGE, TYPE})
@Retention(RUNTIME)
public @interface FilterDefs {
	/**
	 * The aggregated filter definitions.
	 */
	FilterDef[] value();
}
