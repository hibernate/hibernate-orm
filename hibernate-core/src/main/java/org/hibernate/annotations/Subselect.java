/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Maps an immutable and read-only entity to a given SQL {@code select} expression.
 * <p>
 * This is an alternative to defining a database view and mapping the entity to
 * the view using the {@link jakarta.persistence.Table @Table} annotation.
 *
 * @see Synchronize
 *
 * @author Sharath Reddy
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface Subselect {
	/**
	 * The query.
	 */
	String value();
}
