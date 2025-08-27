/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies the base value for the {@linkplain jakarta.persistence.OrderColumn
 * order column} of a persistent list or array, that is, the order column value
 * of the first element of the list or array.
 * <ul>
 * <li>When a row is read from the database, this base value is subtracted
 *     from the order column value to determine an index in the list or array.
 * <li>When an element is written to the database, the base value is added to
 *     the list or array index to determine the order column value.
 *</ul>
 * <p>
 * By default, the base value for an order column is zero, as required by JPA.
 * <p>
 * This annotation is usually used in conjunction with the JPA-defined
 * {@link jakarta.persistence.OrderColumn}.
 *
 * @see jakarta.persistence.OrderColumn
 *
 * @author Steve Ebersole
 */
@Retention( RUNTIME )
public @interface ListIndexBase {
	/**
	 * The list index base.  Default is 0.
	 */
	int value() default 0;
}
