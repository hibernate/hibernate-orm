/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temporal.spi;

import org.hibernate.SPI;
import org.hibernate.temporal.TemporalTableStrategy;
import jakarta.annotation.Nullable;

import static org.hibernate.SPI.Role.USE;

/// Describes one temporal table using names already rendered for the active
/// schema-export context.
///
/// Use every supplied name verbatim and do not quote it again. Partition names
/// are required only when [#partitioned()] is `true`; otherwise they are
/// normalized to `null`.
///
/// @param strategy the selected temporal-table storage strategy
/// @param tableName the rendered temporal table name
/// @param rowStartColumnName the rendered row-start column name
/// @param rowEndColumnName the rendered row-end column name
/// @param partitioned whether current and historical rows are partitioned
/// @param currentPartitionName the rendered current partition name
/// @param historyPartitionName the rendered history partition name
/// @author Steve Ebersole
/// @since 8.0
@SPI(USE)
public record TemporalTableDdlRequest(
		TemporalTableStrategy strategy,
		String tableName,
		String rowStartColumnName,
		String rowEndColumnName,
		boolean partitioned,
		@Nullable String currentPartitionName,
		@Nullable String historyPartitionName) {
	public TemporalTableDdlRequest {
		if ( strategy == null ) {
			throw new IllegalArgumentException( "Temporal table strategy must not be null" );
		}
		requireRenderedName( tableName, "table" );
		requireRenderedName( rowStartColumnName, "row-start column" );
		requireRenderedName( rowEndColumnName, "row-end column" );
		if ( partitioned ) {
			requireRenderedName( currentPartitionName, "current partition" );
			requireRenderedName( historyPartitionName, "history partition" );
		}
		else {
			currentPartitionName = null;
			historyPartitionName = null;
		}
	}

	private static void requireRenderedName(@Nullable String name, String role) {
		if ( name == null || name.isBlank() ) {
			throw new IllegalArgumentException( "Rendered " + role + " name must not be blank" );
		}
	}
}
