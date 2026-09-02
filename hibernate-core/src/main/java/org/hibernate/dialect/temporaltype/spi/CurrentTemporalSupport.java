/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temporaltype.spi;

import jakarta.annotation.Nullable;

import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Defines current temporal expressions and database-side timestamp retrieval.
/// Implementations must be stable and thread-safe.
///
/// @author Steve Ebersole
/// @since 8.0
/// @see Dialect#getCurrentTemporalSupport()
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface CurrentTemporalSupport {
	/// Whether [#currentTimestamp()] uses the standard SQL `current_timestamp`
	/// function. This capability is useful when SQL text supplied outside the
	/// Dialect, such as a column default, assumes the standard spelling.
	default boolean usesStandardCurrentTimestampFunction() {
		return true;
	}

	/// Render the current SQL date expression.
	default String currentDate() {
		return "current_date";
	}

	/// Render the current SQL time expression used for `java.sql.Time`.
	default String currentTime() {
		return "current_time";
	}

	/// Render the current SQL timestamp expression used for `java.sql.Timestamp`.
	default String currentTimestamp() {
		return "current_timestamp";
	}

	/// Render the current local-time expression.
	default String currentLocalTime() {
		return currentTime();
	}

	/// Render the current local-timestamp expression.
	default String currentLocalTimestamp() {
		return currentTimestamp();
	}

	/// Render the current timestamp-with-time-zone expression.
	default String currentTimestampWithTimeZone() {
		return currentTimestamp();
	}

	/// Select the JDBC command used for database-side current timestamp retrieval,
	/// or return `null` when the database offers no supported command.
	///
	/// @see CurrentTimestampSelection
	default @Nullable CurrentTimestampSelection getCurrentTimestampSelection() {
		return null;
	}

	/// Whether the current timestamp remains stable throughout a transaction.
	default boolean isCurrentTimestampStable() {
		return false;
	}
}
