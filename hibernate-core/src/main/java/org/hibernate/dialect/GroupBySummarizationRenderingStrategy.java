/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

/**
 * Strategies for rendering summarization function like rollup and cube in a group by.
 *
 * @author Christian Beikov
 */
public enum GroupBySummarizationRenderingStrategy {
	/**
	 * No support for summarization.
	 */
	NONE,
	/**
	 * Use the proprietary WITH ROLLUP or WITH CUBE clause.
	 */
	CLAUSE,
	/**
	 * Use the rollup or cube functions like specified in the SQL standard.
	 */
	FUNCTION;
}
