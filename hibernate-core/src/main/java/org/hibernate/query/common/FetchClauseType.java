/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.common;

/// The form of SQL `fetch` clause requested by query translation.
///
/// Treat the four forms independently when describing database support. In
/// particular, ordinary row-count support does not imply percent or ties
/// support.
///
/// @see org.hibernate.dialect.sql.ast.spi.FetchClauseSupport
///
/// @author Christian Beikov
public enum FetchClauseType {
	/// Exact row count such as `limit` or `fetch first n rows only`.
	ROWS_ONLY,
	/// Row count which also fetches ties when the last value is not unique.
	ROWS_WITH_TIES,
	/// Row count expressed as a percentage.
	PERCENT_ONLY,
	/// Percentage which also fetches ties when the last value is not unique.
	PERCENT_WITH_TIES
}
