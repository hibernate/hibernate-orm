/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temporal.spi;

import java.util.List;

import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;
import org.hibernate.temporal.TemporalTableStrategy;
import jakarta.annotation.Nullable;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Defines the database grammar and capabilities used for temporal table DDL
/// and historical queries.
///
/// Implement this contract with immutable, thread-safe state. Use rendered
/// names from [TemporalTableDdlRequest] verbatim, return declarative auxiliary
/// commands instead of mutating boot metadata, and make restriction choices
/// only from the ephemeral [TemporalRestrictionRequest].
///
/// @author Gavin King
/// @author Steve Ebersole
/// @since 8.0
/// @see Dialect#getTemporalTableSupport()
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface TemporalTableSupport {
	/// Report whether the database provides native system-versioned tables.
	boolean supportsNativeTemporalTables();

	/// Return the [org.hibernate.type.SqlTypes] code used for temporal
	/// effectivity columns.
	int getTemporalColumnType();

	/// Return the default fractional-second precision of temporal effectivity
	/// columns.
	int getTemporalColumnPrecision();

	/// Render the trailing table options for the supplied table. Use every name
	/// in the request verbatim and return `null` when no options are needed.
	@Nullable String getTemporalTableOptions(TemporalTableDdlRequest request);

	/// Report whether a partitioned temporal table must omit its primary key.
	boolean suppressesTemporalTablePrimaryKeys(boolean partitioned);

	/// Report whether the strategy can partition current and historical rows.
	boolean supportsTemporalTablePartitioning();

	/// Describe, in deterministic order, the schema commands required in
	/// addition to the temporal table itself. Return an immutable list and do
	/// not mutate boot metadata.
	List<TemporalTableAuxiliaryObject> getTemporalTableAuxiliaryObjects(TemporalTableDdlRequest request);

	/// Render declarations placed inside the temporal table definition. Use
	/// every request name verbatim and return `null` when none are needed.
	@Nullable String getExtraTemporalTableDeclarations(TemporalTableDdlRequest request);

	/// Report whether Hibernate should add its effectivity check constraint for
	/// the selected storage strategy.
	boolean createTemporalTableCheckConstraint(TemporalTableStrategy strategy);

	/// Return the SQL operator placed before a historical instant expression.
	String getAsOfOperator(TemporalTableStrategy strategy);

	/// Report whether historical queries use [#getAsOfOperator] for the selected
	/// strategy.
	boolean useAsOfOperator(TemporalTableStrategy strategy);

	/// Report whether current-data queries use [#getAsOfOperator] without an
	/// explicit historical instant.
	boolean useAsOfOperatorForCurrent(TemporalTableStrategy strategy);

	/// Decide whether Hibernate should render effectivity-column restrictions
	/// from the supplied ephemeral query facts. Do not retain the request.
	boolean useTemporalRestriction(TemporalRestrictionRequest request);

	/// Return the column option which excludes a column from native temporal
	/// versioning, or `null` when no option is needed.
	@Nullable String getTemporalExclusionColumnOption();

	/// Return the concrete strategy selected when configuration requests
	/// [TemporalTableStrategy#AUTO].
	TemporalTableStrategy getDefaultTemporalTableStrategy();
}
