/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Target;
import java.lang.annotation.Retention;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * A grouping of {@link NamedEntityGraph} definitions.
 *
 * @since 7.0
 * @author Steve Ebersole
 */
@Target({TYPE, PACKAGE, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface NamedEntityGraphs {
	/**
	 * The grouping of Hibernate named native SQL queries.
	 */
	NamedEntityGraph[] value();
}
