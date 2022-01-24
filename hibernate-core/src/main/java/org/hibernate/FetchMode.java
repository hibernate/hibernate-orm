/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

/**
 * Represents an association fetching strategy.
 *
 * @apiNote This enumeration was previously used to override
 * the fetching strategy specified by mapping annotations when
 * using the old criteria query API. Now it is only used by
 * SPIs and internal APIs.
 *
 * @see org.hibernate.annotations.FetchMode
 *
 * @author Gavin King
 */
public enum FetchMode  {

	/**
	 * Use the default fetching strategy specified by the
	 * {@linkplain org.hibernate.annotations.Fetch mapping
	 * annotations}.
	 */
	DEFAULT,

	/**
	 * Fetch in the initial select, using an outer join.
	 *
	 * @see org.hibernate.annotations.FetchMode#JOIN
	 */
	JOIN,

	/**
	 * Fetch using a separate subsequent select.
	 *
	 * @see org.hibernate.annotations.FetchMode#SELECT
	 * @see org.hibernate.annotations.FetchMode#SUBSELECT
	 */
	SELECT

}
