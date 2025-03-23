/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.common;

/**
 * Enumerates the possible kinds of join in HQL and ANSI SQL.
 *
 * @apiNote This enumeration competes with
 *          {@link jakarta.persistence.criteria.JoinType},
 *          adding {@link #FULL} and {@link #CROSS} joins.
 *
 * @author Gavin King
 *
 * @since 7
 *
 * @see jakarta.persistence.criteria.JoinType
 */
public enum JoinType {
	/**
	 * @see jakarta.persistence.criteria.JoinType#INNER
	 */
	INNER,
	/**
	 * @see jakarta.persistence.criteria.JoinType#LEFT
	 */
	LEFT,
	/**
	 * @see jakarta.persistence.criteria.JoinType#RIGHT
	 */
	RIGHT,
	FULL,
	CROSS
}
