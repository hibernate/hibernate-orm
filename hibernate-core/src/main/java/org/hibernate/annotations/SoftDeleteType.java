/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

/**
 * Enumeration of defines styles of soft-delete
 *
 * @author Steve Ebersole
 */
public enum SoftDeleteType {
	/**
	 * Tracks rows which are active.  The values stored in the database:<dl>
	 *     <dt>{@code true}</dt>
	 *     <dd>indicates that the row is active (non-deleted)</dd>
	 *     <dt>{@code false}</dt>
	 *     <dd>indicates that the row is inactive (deleted)</dd>
	 * </dl>
	 *
	 * @implNote Causes the {@linkplain SoftDelete#converter() conversion} to be wrapped in a negation.
	 */
	ACTIVE,

	/**
	 * Tracks rows which are deleted. The values stored in the database:<dl>
	 *     <dt>{@code true}</dt>
	 * 	   <dd>indicates that the row is deleted</dd>
	 * 	   <dt>{@code false}</dt>
	 * 	   <dd>indicates that the row is non-deleted</dd>
	 * </dl>
	 */
	DELETED,

	/**
	 * Tracks rows which are deleted by the timestamp at which they were deleted.  <dl>
	 *     <dt>{@code null}</dt>
	 *     <dd>indicates that the row is non-deleted</dd>
	 *     <dt>non-{@code null}</dt>
	 *     <dd>indicates that the row is deleted, at the given timestamp</dd>
	 * </dl>
	 */
	TIMESTAMP;

	/**
	 * The default column name used with this strategy.
	 * @see SoftDelete#columnName
	 */
	public String getDefaultColumnName() {
		return switch ( this ) {
			case ACTIVE -> "active";
			case DELETED, TIMESTAMP -> "deleted";
		};
	}
}
