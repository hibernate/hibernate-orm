/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.common;

/**
 * The kind of fetch to use for the {@code FETCH} clause.
 *
 * @author Christian Beikov
 */
public enum FetchClauseType {
	/**
	 * Exact row count like for {@code LIMIT} clause or {@code FETCH FIRST n ROWS ONLY}.
	 */
	ROWS_ONLY,
	/**
	 * Also fetches ties if the last value is not unique {@code FETCH FIRST n ROWS WITH TIES}.
	 */
	ROWS_WITH_TIES,
	/**
	 * Row count in percent {@code FETCH FIRST n PERCENT ROWS ONLY}.
	 */
	PERCENT_ONLY,
	/**
	 * Also fetches ties if the last value is not unique {@code FETCH FIRST n PERCENT ROWS WITH TIES}.
	 */
	PERCENT_WITH_TIES
}
