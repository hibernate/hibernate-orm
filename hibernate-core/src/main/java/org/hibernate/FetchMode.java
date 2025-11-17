/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
@Internal
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
