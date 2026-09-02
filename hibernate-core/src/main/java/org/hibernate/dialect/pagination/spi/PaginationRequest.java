/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.pagination.spi;

import java.util.Objects;

import jakarta.annotation.Nullable;

import org.hibernate.SPI;
import org.hibernate.sql.spi.ParameterMarkerStrategy;

/// Immutable input supplied by Hibernate to a [LimitHandler] for applying
/// execution-time pagination to completed SQL.
///
/// A handler may rewrite [#sql] and introduce JDBC parameters, but must not
/// retain this request. `null` first-row and maximum-row values mean that the
/// corresponding option was not specified. A [#jdbcParameterCount] of `-1`
/// means that Hibernate could not determine the existing parameter count; a
/// handler which needs numbered markers after existing parameters must account
/// for that limitation.
///
/// @param sql complete SQL before execution-time pagination
/// @param firstRow zero-based first row, or `null` when no offset was requested
/// @param maxRows maximum rows to return, or `null` when no limit was requested
/// @param jdbcParameterCount number of existing JDBC parameters, or `-1` when
/// unknown
/// @param parameterMarkerStrategy native marker strategy, or `null` for `?`
///
/// @since 8.0
/// @author Steve Ebersole
@SPI
public record PaginationRequest(
		String sql,
		@Nullable Integer firstRow,
		@Nullable Integer maxRows,
		int jdbcParameterCount,
		@Nullable ParameterMarkerStrategy parameterMarkerStrategy) {
	public PaginationRequest {
		Objects.requireNonNull( sql, "sql" );
		if ( firstRow != null && firstRow < 0 ) {
			throw new IllegalArgumentException( "firstRow must be non-negative" );
		}
		if ( maxRows != null && maxRows < 0 ) {
			throw new IllegalArgumentException( "maxRows must be non-negative" );
		}
		if ( jdbcParameterCount < -1 ) {
			throw new IllegalArgumentException( "jdbcParameterCount must be -1 or non-negative" );
		}
	}

	/// Whether an explicit first-row option was supplied, including zero.
	public boolean hasFirstRow() {
		return firstRow != null;
	}

	/// Whether a positive maximum-row limit was supplied.
	public boolean hasMaxRows() {
		return maxRows != null && maxRows > 0;
	}

	/// Whether neither an offset nor a positive limit needs to be applied.
	public boolean isEmpty() {
		return !hasFirstRow() && !hasMaxRows();
	}

	/// The requested zero-based first row, defaulting to zero.
	public int firstRowOrZero() {
		return firstRow == null ? 0 : firstRow;
	}

	/// Create the marker for a one-based JDBC parameter position in the rewritten
	/// SQL.
	public String parameterMarker(int position) {
		return parameterMarkerStrategy == null
				? "?"
				: parameterMarkerStrategy.createMarker( position, null );
	}

	/// Whether rewritten SQL must use ordinary `?` parameter markers.
	public boolean usesStandardParameterMarkers() {
		return parameterMarkerStrategy == null;
	}
}
