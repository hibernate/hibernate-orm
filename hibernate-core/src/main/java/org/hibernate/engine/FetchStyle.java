/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine;

/**
 * Enumeration of values describing <b>HOW</b> fetching should occur.
 *
 * @author Steve Ebersole
 * @see FetchTiming
 */
public enum FetchStyle {
	/**
	 * Performs a separate SQL select to load the indicated data.  This can either be eager (the second select is
	 * issued immediately) or lazy (the second select is delayed until the data is needed).
	 */
	SELECT,
	/**
	 * Inherently an eager style of fetching.  The data to be fetched is obtained as part of an SQL join.
	 */
	JOIN,
	/**
	 * Initializes a number of indicated data items (entities or collections) in a series of grouped sql selects
	 * using an in-style sql restriction to define the batch size.  Again, can be either eager or lazy.
	 */
	BATCH,
	/**
	 * Performs fetching of associated data (currently limited to only collections) based on the sql restriction
	 * used to load the owner.  Again, can be either eager or lazy.
	 */
	SUBSELECT
}
