/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temporaltype.spi;

import jakarta.annotation.Nullable;
import jakarta.persistence.TemporalType;

import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;
import org.hibernate.query.common.TemporalUnit;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;
import static java.util.Objects.requireNonNull;

/// Defines SQL extraction and timestamp arithmetic patterns and unit names.
/// Preserve the documented placeholder ordering when overriding a pattern.
///
/// @author Steve Ebersole
/// @since 8.0
/// @see Dialect#getTemporalOperationSupport()
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface TemporalOperationSupport {
	/// Return an extraction pattern containing `?1` for the field and `?2` for the value.
	default String extractPattern(TemporalUnit unit) {
		requireNonNull( unit, "unit" );
		return "extract(?1 from ?2)";
	}

	/// Translate an extraction field name.
	default String translateExtractField(TemporalUnit unit) {
		requireNonNull( unit, "unit" );
		return switch ( unit ) {
			case DAY_OF_MONTH -> "dd";
			case DAY_OF_YEAR -> "dy";
			case DAY_OF_WEEK -> "dw";
			case OFFSET, NATIVE, NANOSECOND, DATE, TIME, WEEK_OF_MONTH, WEEK_OF_YEAR ->
					throw new IllegalArgumentException( "illegal field: " + unit );
			default -> unit.toString();
		};
	}

	/// Translate a duration unit name.
	default String translateDurationField(TemporalUnit unit) {
		requireNonNull( unit, "unit" );
		return switch ( unit ) {
			case NATIVE -> "nanosecond";
			case DAY_OF_MONTH, DAY_OF_YEAR, DAY_OF_WEEK, WEEK_OF_MONTH, WEEK_OF_YEAR, OFFSET,
					TIMEZONE_HOUR, TIMEZONE_MINUTE, DATE, TIME ->
					throw new IllegalArgumentException( "illegal unit: " + unit );
			default -> unit.toString();
		};
	}

	/// Return a timestamp-add pattern containing `?1`, `?2`, and `?3`.
	default String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, @Nullable IntervalType intervalType) {
		requireNonNull( unit, "unit" );
		requireNonNull( temporalType, "temporalType" );
		throw new UnsupportedOperationException( "Timestamp addition is not supported" );
	}

	/// Return a timestamp-difference pattern containing `?1`, `?2`, and `?3`.
	default String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		requireNonNull( unit, "unit" );
		requireNonNull( fromTemporalType, "fromTemporalType" );
		requireNonNull( toTemporalType, "toTemporalType" );
		throw new UnsupportedOperationException( "Timestamp difference is not supported" );
	}

	/// The number of nanoseconds represented by one native fractional-second
	/// unit used for datetime arithmetic.
	///
	/// Return a positive power of ten no greater than `1_000_000_000`, and keep
	/// the value consistent with [#timestampaddPattern] and
	/// [#timestampdiffPattern].
	@SPI({ USE, IMPLEMENT })
	default long fractionalSecondPrecisionInNanos() {
		return 1;
	}
}
