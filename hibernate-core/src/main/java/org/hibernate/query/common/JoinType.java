/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
