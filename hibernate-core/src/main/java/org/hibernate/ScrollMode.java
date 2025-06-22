/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import java.sql.ResultSet;

/**
 * Specifies the type of JDBC scrollable {@linkplain ResultSet result set}
 * to use underneath a {@link ScrollableResults}.
 *
 * @author Gavin King
 *
 * @see org.hibernate.query.SelectionQuery#scroll(ScrollMode)
 */
public enum ScrollMode {
	/**
	 * Requests a scrollable result that is only scrollable forwards.
	 *
	 * @see ResultSet#TYPE_FORWARD_ONLY
	 */
	FORWARD_ONLY,

	/**
	 * Requests a scrollable result which is sensitive to changes
	 * in the underlying data.
	 *
	 * @see ResultSet#TYPE_SCROLL_SENSITIVE
	 */
	SCROLL_SENSITIVE,

	/**
	 * Requests a scrollable result which is insensitive to changes
	 * in the underlying data.
	 * <p>
	 * Note that since the Hibernate session acts as a cache, you
	 * might need to explicitly evict objects, if you need to see
	 * changes made by other transactions.
	 *
	 * @see ResultSet#TYPE_SCROLL_INSENSITIVE
	 */
	SCROLL_INSENSITIVE;

	/**
	 * Get the corresponding JDBC scroll type code constant value.
	 *
	 * @return the JDBC result set type code
	 */
	public int toResultSetType() {
		return switch (this) {
			case FORWARD_ONLY -> ResultSet.TYPE_FORWARD_ONLY;
			case SCROLL_SENSITIVE -> ResultSet.TYPE_SCROLL_SENSITIVE;
			case SCROLL_INSENSITIVE -> ResultSet.TYPE_SCROLL_INSENSITIVE;
		};
	}

	/**
	 * Determine if {@code this} mode is "less than" the provided mode.
	 *
	 * @param other The provided mode
	 *
	 * @return {@code true} if this mode is less than the other.
	 */
	public boolean lessThan(ScrollMode other) {
		return this.toResultSetType() < other.toResultSetType();
	}
}
