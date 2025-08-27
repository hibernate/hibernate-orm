/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Defines an interpolated alias occurring in a SQL
 * {@linkplain Filter#condition() filter condition}.
 * Aliases are interpolated where placeholders of the
 * form {@code {name}} occur, where {@code name} is
 * the value specified by {@link #alias}.
 * <p>
 * It's usually necessary to specify only one of
 * {@link #entity} and {@link #table} to uniquely
 * identify the alias that should be interpolated.
 *
 * @author Rob Worsnop
 */
@Target({})
@Retention(RUNTIME)
public @interface SqlFragmentAlias {
	/**
	 * The alias within the fragment.
	 */
	String alias();

	/**
	 * The table corresponding to the alias.
	 */
	String table() default "";

	/**
	 * The entity class associated with the alias.
	 */
	Class<?> entity() default void.class;
}
