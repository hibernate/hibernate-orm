/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.pagination.spi;

import java.util.List;

import jakarta.annotation.Nullable;

import org.hibernate.SPI;

/// Immutable JDBC execution instructions produced by a [LimitHandler] together
/// with paginated SQL.
///
/// Hibernate binds [#parametersAtStart] in list order, then the query's original
/// parameters, and then [#parametersAtEnd] in list order. It applies [#maxRows]
/// through `PreparedStatement.setMaxRows()` and advances [#rowsToSkip] rows in
/// the result set. A handler must encode each responsibility exactly once; for
/// example, an offset represented in SQL must not also be returned as rows to
/// skip.
///
/// @param parametersAtStart integer pagination values bound before original
/// query parameters
/// @param parametersAtEnd integer pagination values bound after original query
/// parameters
/// @param maxRows JDBC maximum rows, or `null` when no JDBC maximum is needed
/// @param rowsToSkip rows Hibernate must skip in the returned result set
///
/// @since 8.0
/// @author Steve Ebersole
@SPI
public record PaginationJdbcInstructions(
		List<Integer> parametersAtStart,
		List<Integer> parametersAtEnd,
		@Nullable Integer maxRows,
		int rowsToSkip) {
	/// No additional pagination parameters or JDBC/result-set work.
	public static final PaginationJdbcInstructions NONE =
			new PaginationJdbcInstructions( List.of(), List.of(), null, 0 );

	public PaginationJdbcInstructions {
		parametersAtStart = List.copyOf( parametersAtStart );
		parametersAtEnd = List.copyOf( parametersAtEnd );
		if ( maxRows != null && maxRows < 0 ) {
			throw new IllegalArgumentException( "maxRows must be non-negative" );
		}
		if ( rowsToSkip < 0 ) {
			throw new IllegalArgumentException( "rowsToSkip must be non-negative" );
		}
	}

	/// One-based position of the first original query parameter after pagination
	/// parameters inserted at the start.
	public int parameterPositionStart() {
		return parametersAtStart.size() + 1;
	}
}
