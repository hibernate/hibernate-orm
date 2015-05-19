/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

/**
 * Represents an association fetching strategy. This is used
 * together with the <tt>Criteria</tt> API to specify runtime
 * fetching strategies.<br>
 * <br>
 * For HQL queries, use the <tt>FETCH</tt> keyword instead.
 *
 * @see Criteria#setFetchMode(java.lang.String, FetchMode)
 *
 * @author Gavin King
 */
public enum FetchMode  {
	/**
	 * Default to the setting configured in the mapping file.
	 */
	DEFAULT,

	/**
	 * Fetch using an outer join. Equivalent to <tt>fetch="join"</tt>.
	 */
	JOIN,
	/**
	 * Fetch eagerly, using a separate select. Equivalent to
	 * <tt>fetch="select"</tt>.
	 */
	SELECT;

	/**
	 * Fetch lazily. Equivalent to <tt>outer-join="false"</tt>.
	 *
	 * @deprecated use <tt>FetchMode.SELECT</tt>
	 */
	@Deprecated
	public static final FetchMode LAZY = SELECT;
	/**
	 * Fetch eagerly, using an outer join. Equivalent to <tt>outer-join="true"</tt>.
	 *
	 * @deprecated use <tt>FetchMode.JOIN</tt>
	 */
	@Deprecated
	public static final FetchMode EAGER = JOIN;
}
