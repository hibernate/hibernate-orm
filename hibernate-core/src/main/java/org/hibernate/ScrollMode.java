/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import java.sql.ResultSet;

/**
 * Specifies the type of JDBC scrollable result set to use underneath a {@link ScrollableResults}.
 *
 * @author Gavin King
 */
public enum ScrollMode {
	/**
	 * Requests a scrollable result that is only scrollable forwards.
	 *
	 * @see java.sql.ResultSet#TYPE_FORWARD_ONLY
	 */
	FORWARD_ONLY( ResultSet.TYPE_FORWARD_ONLY ),

	/**
	 * Requests a scrollable result which is sensitive to changes in the underlying data.
	 *
	 * @see java.sql.ResultSet#TYPE_SCROLL_SENSITIVE
	 */
	SCROLL_SENSITIVE( ResultSet.TYPE_SCROLL_SENSITIVE ),

	/**
	 * Requests a scrollable result which is insensitive to changes in the underlying data.
	 *
	 * Note that since the Hibernate session acts as a cache, you
	 * might need to explicitly evict objects, if you need to see
	 * changes made by other transactions.
	 *
	 * @see java.sql.ResultSet#TYPE_SCROLL_INSENSITIVE
	 */
	SCROLL_INSENSITIVE( ResultSet.TYPE_SCROLL_INSENSITIVE );

	private final int resultSetType;

	private ScrollMode(int level) {
		this.resultSetType = level;
	}

	/**
	 * Get the corresponding JDBC scroll type code constant value.
	 *
	 * @return the JDBC result set type code
	 */
	public int toResultSetType() {
		return resultSetType;
	}

	/**
	 * Determine if {@code this} mode is "less than" the provided mode.
	 *
	 * @param other The provided mode
	 *
	 * @return {@code true} if this mode is less than the other.
	 */
	public boolean lessThan(ScrollMode other) {
		return this.resultSetType < other.resultSetType;
	}

}
