/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.mutation.spi;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.USE;

/// Selects the Hibernate-owned fallback family used for an SQM mutation of a
/// multi-table entity.
///
/// A custom Dialect should select a kind through
/// [MultiTableMutationSupport]. The kind chooses Hibernate's stock strategy
/// construction; it does not advertise general native mutation support and
/// does not replace the query-engine integration for custom execution
/// strategies.
///
/// @since 8.0
/// @author Steve Ebersole
@SPI(USE)
public enum MultiTableMutationStrategyKind {
	/// Use a modifiable common-table-expression strategy.
	CTE,
	/// Use a connection-scoped local temporary table.
	LOCAL_TEMPORARY_TABLE,
	/// Use a schema-defined global temporary table.
	GLOBAL_TEMPORARY_TABLE,
	/// Use a persistent table whose rows are isolated by session identifier.
	PERSISTENT_TABLE
}
