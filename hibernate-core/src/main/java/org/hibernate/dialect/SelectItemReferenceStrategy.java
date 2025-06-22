/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

/**
 * Strategies for referring to a select item.
 *
 * @author Christian Beikov
 */
public enum SelectItemReferenceStrategy {
	/**
	 * The default strategy i.e. render the expression again.
	 */
	EXPRESSION,
	/**
	 * Refer to the item via its alias.
	 */
	ALIAS,
	/**
	 * Refer to the item via its position.
	 */
	POSITION
}
