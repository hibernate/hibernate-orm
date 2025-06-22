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
 * Marks an arbitrary class as available for use in HQL queries by its unqualified name.
 * <p>
 * By default, non-entity class names must be fully-qualified in the query language.
 *
 * @author Gavin King
 *
 * @since 6.2
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface Imported {
	/**
	 * Provide an alias for the class, to avoid collisions.
	 */
	String rename() default "";
}
