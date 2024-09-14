/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.util.Locale;

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
	DELETED;

	private final String defaultColumnName;

	SoftDeleteType() {
		this.defaultColumnName = name().toLowerCase( Locale.ROOT );
	}

	public String getDefaultColumnName() {
		return defaultColumnName;
	}
}
