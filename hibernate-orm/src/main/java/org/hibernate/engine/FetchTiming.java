/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine;

/**
 * Enumeration of values describing <b>WHEN</b> fetching should occur.
 * 
 * @author Steve Ebersole
 * @see FetchStyle
 */
public enum FetchTiming {
	/**
	 * Perform fetching immediately.  Also called eager fetching
	 */
	IMMEDIATE,
	/**
	 * Performing fetching later, when needed.  Also called lazy fetching.
	 */
	DELAYED
}
