/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.mutation.spi;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Immutable selection of Hibernate's fallback strategy families for SQM
/// mutations of multi-table entities.
///
/// A custom Dialect should return a stable, non-null profile from
/// [org.hibernate.dialect.Dialect#getMultiTableMutationSupport()]. Update and
/// delete share [#mutationStrategyKind], while insert independently uses
/// [#insertStrategyKind]. Use [#forBoth(MultiTableMutationStrategyKind)] or a
/// named profile when both operations use the same family, and invoke the
/// canonical constructor when they differ.
///
/// These values select Hibernate-owned fallback implementations. Supply a
/// genuinely custom execution strategy through the query-engine mutation or
/// insert strategy setting instead of depending on Hibernate's internal
/// strategy implementations.
///
/// @param mutationStrategyKind the fallback for update and delete
/// @param insertStrategyKind the fallback for insert
///
/// @see org.hibernate.dialect.Dialect#getMultiTableMutationSupport()
/// @since 8.0
/// @author Steve Ebersole
@SPI({ USE, SUPPLY })
public record MultiTableMutationSupport(
		MultiTableMutationStrategyKind mutationStrategyKind,
		MultiTableMutationStrategyKind insertStrategyKind) {

	/// Use modifiable common-table expressions for both operation families.
	public static final MultiTableMutationSupport CTE = new MultiTableMutationSupport(
			MultiTableMutationStrategyKind.CTE,
			MultiTableMutationStrategyKind.CTE
	);
	/// Use local temporary tables for both operation families.
	public static final MultiTableMutationSupport LOCAL_TEMPORARY_TABLE = new MultiTableMutationSupport(
			MultiTableMutationStrategyKind.LOCAL_TEMPORARY_TABLE,
			MultiTableMutationStrategyKind.LOCAL_TEMPORARY_TABLE
	);
	/// Use global temporary tables for both operation families.
	public static final MultiTableMutationSupport GLOBAL_TEMPORARY_TABLE = new MultiTableMutationSupport(
			MultiTableMutationStrategyKind.GLOBAL_TEMPORARY_TABLE,
			MultiTableMutationStrategyKind.GLOBAL_TEMPORARY_TABLE
	);
	/// Use persistent tables for both operation families.
	public static final MultiTableMutationSupport PERSISTENT_TABLE = new MultiTableMutationSupport(
			MultiTableMutationStrategyKind.PERSISTENT_TABLE,
			MultiTableMutationStrategyKind.PERSISTENT_TABLE
	);

	public MultiTableMutationSupport {
		if ( mutationStrategyKind == null ) {
			throw new IllegalArgumentException( "mutationStrategyKind must not be null" );
		}
		if ( insertStrategyKind == null ) {
			throw new IllegalArgumentException( "insertStrategyKind must not be null" );
		}
	}

	/// Select one strategy kind for both update/delete and insert.
	///
	/// @param strategyKind the shared non-null strategy kind
	/// @return the canonical same-kind profile
	public static MultiTableMutationSupport forBoth(MultiTableMutationStrategyKind strategyKind) {
		if ( strategyKind == null ) {
			throw new IllegalArgumentException( "strategyKind must not be null" );
		}
		return switch ( strategyKind ) {
			case CTE -> CTE;
			case LOCAL_TEMPORARY_TABLE -> LOCAL_TEMPORARY_TABLE;
			case GLOBAL_TEMPORARY_TABLE -> GLOBAL_TEMPORARY_TABLE;
			case PERSISTENT_TABLE -> PERSISTENT_TABLE;
		};
	}
}
