/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.pagination.spi;

import java.util.Objects;

import org.hibernate.SPI;

/// Immutable result returned by [LimitHandler#processSql], pairing completed SQL
/// with all JDBC and result-set work required by its pagination.
///
/// The SQL and instructions form one protocol result and must remain
/// consistent. In particular, values represented by SQL parameters must appear
/// in the appropriate instruction list.
///
/// @param sql completed, possibly rewritten SQL
/// @param jdbcInstructions JDBC binding, maximum-row, and row-skipping work
///
/// @since 8.0
/// @author Steve Ebersole
@SPI
public record PaginationResult(String sql, PaginationJdbcInstructions jdbcInstructions) {
	public PaginationResult {
		Objects.requireNonNull( sql, "sql" );
		Objects.requireNonNull( jdbcInstructions, "jdbcInstructions" );
	}

	/// Return the given SQL with no pagination-specific JDBC work.
	public static PaginationResult unchanged(String sql) {
		return new PaginationResult( sql, PaginationJdbcInstructions.NONE );
	}
}
