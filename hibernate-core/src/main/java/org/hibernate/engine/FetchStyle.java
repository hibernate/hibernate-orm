/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine;

/**
 * Enumeration of values describing <em>how</em> fetching should occur.
 *
 * @author Steve Ebersole
 * @see FetchTiming
 */
public enum FetchStyle {
	/**
	 * Performs a separate SQL select statement to load the indicated data.
	 * <p>
	 * This can either be eager (the second select is issued immediately)
	 * or lazy (the second select is delayed until the data is needed).
	 */
	SELECT,
	/**
	 * The data to be fetched is obtained as part of an SQL join.
	 * <p>
	 * Inherently an eager style of fetching.
	 */
	JOIN,
	/**
	 * Initializes a number of indicated data items (entities or collections)
	 * in a series of grouped SQL select statements using an in-style SQL
	 * restriction to define the batch size.
	 * <p>
	 * Can be either eager or lazy.
	 */
	BATCH,
	/**
	 * Performs fetching of associated data based on the SQL restriction used
	 * to load the owner.
	 * <p>
	 * Can be either eager or lazy.
	 */
	SUBSELECT
}
