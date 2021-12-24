/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

/**
 * Represents an association fetching strategy. This is used
 * together with the {@code Criteria} API to specify runtime
 * fetching strategies.
 * <p>
 * For HQL queries, use the {@code FETCH} keyword instead.
 *
 * @see Criteria#setFetchMode(String, FetchMode)
 *
 * @author Gavin King
 */
public enum FetchMode  {
	/**
	 * Default to the setting configured in the mapping file.
	 */
	DEFAULT,

	/**
	 * Fetch using an outer join. Equivalent to {@code fetch="join"}.
	 */
	JOIN,
	/**
	 * Fetch eagerly, using a separate select. Equivalent to
	 * {@code fetch="select"}.
	 */
	SELECT;

	/**
	 * Fetch lazily. Equivalent to {@code outer-join="false"}.
	 *
	 * @deprecated use {@link #SELECT}
	 */
	@Deprecated
	public static final FetchMode LAZY = SELECT;
	/**
	 * Fetch eagerly, using an outer join. Equivalent to {@code outer-join="true"}.
	 *
	 * @deprecated use {@link #JOIN}
	 */
	@Deprecated
	public static final FetchMode EAGER = JOIN;
}
