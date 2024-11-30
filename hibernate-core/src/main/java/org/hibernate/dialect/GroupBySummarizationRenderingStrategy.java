/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
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
