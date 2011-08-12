/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
