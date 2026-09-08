/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.type.spi;

import org.hibernate.Length;
import org.hibernate.SPI;
import org.hibernate.engine.jdbc.Size;

import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Immutable description of a Dialect's resolved type-sizing limits and
/// defaults.
///
/// Providers should return one stable profile from
/// [org.hibernate.dialect.Dialect#getTypeSizingProfile()]. Parameterized
/// lengths are the largest values accepted in VARCHAR-, NVARCHAR-, or
/// VARBINARY-like type declarations. Physical capacities are the largest
/// values representable by the database, even when Hibernate must select a
/// different DDL spelling above the parameterized-length limit. Character
/// limits and capacities are character counts; binary limits and capacities
/// are byte counts.
///
/// Decimal, float, and double precision retain the database-specific digit
/// interpretation used by the corresponding SQL type. Timestamp precision and
/// interval-second scale are decimal fractional-second digits. The LOB length
/// is the generated BLOB or CLOB length.
///
/// A builder contains resolved values, not callbacks to its supplying Dialect.
/// When refining an inherited family profile, set every dimension which
/// differs. Setters are independent and do not propagate changes between
/// VARCHAR, NVARCHAR, VARBINARY, parameterized-length, or capacity values.
///
/// @see org.hibernate.dialect.Dialect#getTypeSizingProfile()
///
/// @since 8.0
/// @author Steve Ebersole
@SPI({ USE, SUPPLY })
public final class TypeSizingProfile {
	/// The declaration form or physical value family is unavailable.
	public static final int UNSUPPORTED = -1;

	/// The base-Dialect sizing profile.
	public static final TypeSizingProfile STANDARD = new TypeSizingProfile(
			38,
			6,
			6,
			9,
			Size.DEFAULT_LOB_LENGTH,
			24,
			53,
			Length.LONG32,
			Length.LONG32,
			Length.LONG32,
			Length.LONG32,
			Length.LONG32,
			Length.LONG32
	);

	private final int defaultDecimalPrecision;
	private final int defaultTimestampPrecision;
	private final int maxTimestampPrecision;
	private final int defaultIntervalSecondScale;
	private final long defaultLobLength;
	private final int floatPrecision;
	private final int doublePrecision;
	private final int maxVarcharLength;
	private final int maxNVarcharLength;
	private final int maxVarbinaryLength;
	private final int maxVarcharCapacity;
	private final int maxNVarcharCapacity;
	private final int maxVarbinaryCapacity;

	private TypeSizingProfile(
			int defaultDecimalPrecision,
			int defaultTimestampPrecision,
			int maxTimestampPrecision,
			int defaultIntervalSecondScale,
			long defaultLobLength,
			int floatPrecision,
			int doublePrecision,
			int maxVarcharLength,
			int maxNVarcharLength,
			int maxVarbinaryLength,
			int maxVarcharCapacity,
			int maxNVarcharCapacity,
			int maxVarbinaryCapacity) {
		this.defaultDecimalPrecision = positive( "defaultDecimalPrecision", defaultDecimalPrecision );
		this.defaultTimestampPrecision = nonnegative( "defaultTimestampPrecision", defaultTimestampPrecision );
		this.maxTimestampPrecision = positive( "maxTimestampPrecision", maxTimestampPrecision );
		this.defaultIntervalSecondScale = nonnegative( "defaultIntervalSecondScale", defaultIntervalSecondScale );
		this.defaultLobLength = positive( "defaultLobLength", defaultLobLength );
		this.floatPrecision = positive( "floatPrecision", floatPrecision );
		this.doublePrecision = positive( "doublePrecision", doublePrecision );
		this.maxVarcharLength = size( "maxVarcharLength", maxVarcharLength );
		this.maxNVarcharLength = size( "maxNVarcharLength", maxNVarcharLength );
		this.maxVarbinaryLength = size( "maxVarbinaryLength", maxVarbinaryLength );
		this.maxVarcharCapacity = capacity( "maxVarcharCapacity", maxVarcharCapacity, "maxVarcharLength", maxVarcharLength );
		this.maxNVarcharCapacity = capacity( "maxNVarcharCapacity", maxNVarcharCapacity, "maxNVarcharLength", maxNVarcharLength );
		this.maxVarbinaryCapacity = capacity( "maxVarbinaryCapacity", maxVarbinaryCapacity, "maxVarbinaryLength", maxVarbinaryLength );
	}

	private static int positive(String name, int value) {
		if ( value <= 0 ) {
			throw new IllegalArgumentException( name + " must be positive: " + value );
		}
		return value;
	}

	private static long positive(String name, long value) {
		if ( value <= 0 ) {
			throw new IllegalArgumentException( name + " must be positive: " + value );
		}
		return value;
	}

	private static int nonnegative(String name, int value) {
		if ( value < 0 ) {
			throw new IllegalArgumentException( name + " must not be negative: " + value );
		}
		return value;
	}

	private static int size(String name, int value) {
		if ( value == UNSUPPORTED || value > 0 ) {
			return value;
		}
		throw new IllegalArgumentException( name + " must be positive or UNSUPPORTED: " + value );
	}

	private static int capacity(String name, int value, String lengthName, int length) {
		size( name, value );
		if ( length > 0 && value < length ) {
			throw new IllegalArgumentException( name + " must not be smaller than " + lengthName
					+ ": " + value + " < " + length );
		}
		return value;
	}

	/// Create a builder initialized with [#STANDARD].
	public static Builder builder() {
		return new Builder( STANDARD );
	}

	/// Create a builder containing every resolved value from `base`.
	public static Builder builder(TypeSizingProfile base) {
		if ( base == null ) {
			throw new IllegalArgumentException( "base TypeSizingProfile must not be null" );
		}
		return new Builder( base );
	}

	/// Return the default exact-numeric precision in database-specific digits.
	public int defaultDecimalPrecision() {
		return defaultDecimalPrecision;
	}

	/// Return the default number of decimal fractional-second timestamp digits.
	public int defaultTimestampPrecision() {
		return defaultTimestampPrecision;
	}

	/// Return the maximum number of decimal fractional-second timestamp digits.
	public int maxTimestampPrecision() {
		return maxTimestampPrecision;
	}

	/// Return the default number of decimal fractional-second interval digits.
	public int defaultIntervalSecondScale() {
		return defaultIntervalSecondScale;
	}

	/// Return the generated BLOB or CLOB length.
	public long defaultLobLength() {
		return defaultLobLength;
	}

	/// Return the database precision which represents a Java `float`.
	public int floatPrecision() {
		return floatPrecision;
	}

	/// Return the database precision which represents a Java `double`.
	public int doublePrecision() {
		return doublePrecision;
	}

	/// Return the largest VARCHAR-like declaration parameter, in characters.
	public int maxVarcharLength() {
		return maxVarcharLength;
	}

	/// Return the largest NVARCHAR-like declaration parameter, in characters.
	public int maxNVarcharLength() {
		return maxNVarcharLength;
	}

	/// Return the largest VARBINARY-like declaration parameter, in bytes.
	public int maxVarbinaryLength() {
		return maxVarbinaryLength;
	}

	/// Return the largest VARCHAR-like physical value, in characters.
	public int maxVarcharCapacity() {
		return maxVarcharCapacity;
	}

	/// Return the largest NVARCHAR-like physical value, in characters.
	public int maxNVarcharCapacity() {
		return maxNVarcharCapacity;
	}

	/// Return the largest VARBINARY-like physical value, in bytes.
	public int maxVarbinaryCapacity() {
		return maxVarbinaryCapacity;
	}

	/// Mutable construction state for an immutable profile snapshot.
	///
	/// Each setter changes only its named value and returns this builder.
	public static final class Builder {
		private int defaultDecimalPrecision;
		private int defaultTimestampPrecision;
		private int maxTimestampPrecision;
		private int defaultIntervalSecondScale;
		private long defaultLobLength;
		private int floatPrecision;
		private int doublePrecision;
		private int maxVarcharLength;
		private int maxNVarcharLength;
		private int maxVarbinaryLength;
		private int maxVarcharCapacity;
		private int maxNVarcharCapacity;
		private int maxVarbinaryCapacity;

		private Builder(TypeSizingProfile base) {
			defaultDecimalPrecision = base.defaultDecimalPrecision;
			defaultTimestampPrecision = base.defaultTimestampPrecision;
			maxTimestampPrecision = base.maxTimestampPrecision;
			defaultIntervalSecondScale = base.defaultIntervalSecondScale;
			defaultLobLength = base.defaultLobLength;
			floatPrecision = base.floatPrecision;
			doublePrecision = base.doublePrecision;
			maxVarcharLength = base.maxVarcharLength;
			maxNVarcharLength = base.maxNVarcharLength;
			maxVarbinaryLength = base.maxVarbinaryLength;
			maxVarcharCapacity = base.maxVarcharCapacity;
			maxNVarcharCapacity = base.maxNVarcharCapacity;
			maxVarbinaryCapacity = base.maxVarbinaryCapacity;
		}

		/// Set the default exact-numeric precision in database-specific digits.
		public Builder defaultDecimalPrecision(int value) {
			defaultDecimalPrecision = value;
			return this;
		}

		/// Set the default number of decimal fractional-second timestamp digits.
		public Builder defaultTimestampPrecision(int value) {
			defaultTimestampPrecision = value;
			return this;
		}

		/// Set the maximum number of decimal fractional-second timestamp digits.
		public Builder maxTimestampPrecision(int value) {
			maxTimestampPrecision = value;
			return this;
		}

		/// Set the default number of decimal fractional-second interval digits.
		public Builder defaultIntervalSecondScale(int value) {
			defaultIntervalSecondScale = value;
			return this;
		}

		/// Set the generated BLOB or CLOB length.
		public Builder defaultLobLength(long value) {
			defaultLobLength = value;
			return this;
		}

		/// Set the database precision which represents a Java `float`.
		public Builder floatPrecision(int value) {
			floatPrecision = value;
			return this;
		}

		/// Set the database precision which represents a Java `double`.
		public Builder doublePrecision(int value) {
			doublePrecision = value;
			return this;
		}

		/// Set the largest VARCHAR-like declaration parameter, in characters.
		public Builder maxVarcharLength(int value) {
			maxVarcharLength = value;
			return this;
		}

		/// Set the largest NVARCHAR-like declaration parameter, in characters.
		public Builder maxNVarcharLength(int value) {
			maxNVarcharLength = value;
			return this;
		}

		/// Set the largest VARBINARY-like declaration parameter, in bytes.
		public Builder maxVarbinaryLength(int value) {
			maxVarbinaryLength = value;
			return this;
		}

		/// Set the largest VARCHAR-like physical value, in characters.
		public Builder maxVarcharCapacity(int value) {
			maxVarcharCapacity = value;
			return this;
		}

		/// Set the largest NVARCHAR-like physical value, in characters.
		public Builder maxNVarcharCapacity(int value) {
			maxNVarcharCapacity = value;
			return this;
		}

		/// Set the largest VARBINARY-like physical value, in bytes.
		public Builder maxVarbinaryCapacity(int value) {
			maxVarbinaryCapacity = value;
			return this;
		}

		/// Build an immutable snapshot of the current values.
		public TypeSizingProfile build() {
			return new TypeSizingProfile(
					defaultDecimalPrecision,
					defaultTimestampPrecision,
					maxTimestampPrecision,
					defaultIntervalSecondScale,
					defaultLobLength,
					floatPrecision,
					doublePrecision,
					maxVarcharLength,
					maxNVarcharLength,
					maxVarbinaryLength,
					maxVarcharCapacity,
					maxNVarcharCapacity,
					maxVarbinaryCapacity
			);
		}
	}
}
