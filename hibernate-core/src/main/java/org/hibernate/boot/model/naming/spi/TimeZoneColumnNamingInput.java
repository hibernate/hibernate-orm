/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.spi;

import java.util.Optional;

import org.hibernate.SPI;
import org.hibernate.relational.naming.spi.LogicalName;

import static java.util.Objects.requireNonNull;

/// Immutable facts for naming the offset companion of a temporal column.
/// Used only when two-column time-zone storage needs an implicit companion name.
/// A nonempty effective companion name bypasses the naming callback entirely.
/// All reference components are non-null; [#sourceColumnName()] may be empty.
///
/// @param owner The mapped entity receiving the temporal value, not a mapped superclass declaring it
/// @param attributePath The nonempty user attribute path relative to the owner, such as `eventTime`,
/// excluding synthetic `instant`, `utcTime`, and `zoneOffset` members
/// @param temporalColumn The actual temporal column's settled logical/physical name pair, after
/// effective temporal overrides and enclosing column naming patterns
/// @param table The actual destination table for the companion column, with settled dependency names
/// @param companionDeclared Whether a [org.hibernate.annotations.TimeZoneColumn] or an effective
/// [jakarta.persistence.AttributeOverride] for the synthetic `zoneOffset` member is present.
/// `true` includes an annotation with its default empty name and an override with an empty
/// column name. `false` means neither companion declaration exists. It does not mean that
/// the companion has an explicit name: nonempty names bypass this callback
/// @param sourceColumnName The original temporal source name before overrides and enclosing
/// patterns: either its direct explicit [jakarta.persistence.Column#name()] or an already-resolved
/// implicit name. Empty if source naming was not needed, for example because a temporal override
/// supplied the actual column name. This is a dependency, not a calculated companion default
///
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public record TimeZoneColumnNamingInput(
		EntityNamingInput owner,
		String attributePath,
		NamingNamePair temporalColumn,
		TableNamingInput table,
		boolean companionDeclared,
		Optional<LogicalName> sourceColumnName) {
	public TimeZoneColumnNamingInput {
		requireNonNull( owner, "owner" );
		requireNonNull( attributePath, "attributePath" );
		requireNonNull( temporalColumn, "temporalColumn" );
		requireNonNull( table, "table" );
		requireNonNull( sourceColumnName, "sourceColumnName" );
		if ( attributePath.isEmpty() ) {
			throw new IllegalArgumentException( "Temporal attribute path must not be empty" );
		}
	}

	/// Whether mapping metadata declares the companion column, even without a name.
	/// For example, bare `@TimeZoneColumn` yields `true`, while `@Column(name="event_time")`
	/// on the temporal attribute alone yields `false`. An effective `zoneOffset` override
	/// with an empty name also yields `true`.
	///
	/// The default naming implementation preserves the distinct existing conventions:
	/// `false` uses the original temporal source name plus `_tz`; `true` delegates to
	/// basic naming of the synthetic `zoneOffset` path. Thus, with Standard naming,
	/// `eventTime` produces `eventTime_tz` or `eventTime_zoneOffset`, respectively.
	/// Custom strategies may choose the same algorithm for both cases.
	///
	/// @return Whether a companion declaration is present, not whether its name is explicit
	public boolean companionDeclared() {
		return companionDeclared;
	}

}
