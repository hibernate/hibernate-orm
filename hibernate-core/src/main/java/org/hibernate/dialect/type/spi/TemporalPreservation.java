package org.hibernate.dialect.type.spi;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.Set;

import jakarta.annotation.Nullable;

import org.hibernate.Incubating;
import org.hibernate.SPI;

import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// What a [Dialect][org.hibernate.dialect.Dialect] reports about how one exact
/// combination of Java type and JDBC type stores a Java Time value.
///
/// The report is made per [Part] of the value. A part is *preserved* when the
/// stored SQL value is correct for that part according to the semantics of the
/// database itself, for every value within [#minimum()] and [#maximum()]. It
/// is not enough for Hibernate to read back the value it wrote.
///
/// When temporal preservation is enabled, every part which the Java type
/// [requires][Part#requiredBy(Class)] must be [Level#SUPPORTED]. Both
/// [Level#UNSUPPORTED] and [Level#UNKNOWN] lead to rejection.
///
/// @param date whether the calendar date is preserved
/// @param time whether the time of day is preserved
/// @param instant whether the instant is preserved
/// @param offset whether the offset from UTC is preserved
/// @param zoneId whether the region-based zone identity is preserved
/// @param instantBasis how the stored value denotes an instant, when [#instant()]
///        is supported
/// @param minimum the least preserved value, of the same Java Time class as the
///        values of the assessed Java type, or `null` when every value the Java
///        class can represent is preserved at the low end
/// @param maximum the greatest preserved value, or `null` likewise
///
/// @see org.hibernate.dialect.Dialect#getTemporalPreservation
/// @see org.hibernate.cfg.MappingSettings#TEMPORAL_PRESERVATION
///
/// @since 8.1
/// @author Steve Ebersole
@Incubating(since = "8.1")
@SPI({ USE, SUPPLY })
public record TemporalPreservation(
		Level date,
		Level time,
		Level instant,
		Level offset,
		Level zoneId,
		@Nullable InstantBasis instantBasis,
		@Nullable Temporal minimum,
		@Nullable Temporal maximum) {

	private static final TemporalPreservation UNKNOWN = new TemporalPreservation(
			Level.UNKNOWN,
			Level.UNKNOWN,
			Level.UNKNOWN,
			Level.UNKNOWN,
			Level.UNKNOWN,
			null,
			null,
			null
	);

	public TemporalPreservation {
		if ( date == null || time == null || instant == null || offset == null || zoneId == null ) {
			throw new IllegalArgumentException( "The level of every part must be given" );
		}
		if ( minimum != null && maximum != null && minimum.getClass() != maximum.getClass() ) {
			throw new IllegalArgumentException( "The minimum and maximum must be of the same class" );
		}
		if ( instant == Level.SUPPORTED && instantBasis == null ) {
			throw new IllegalArgumentException( "A supported instant requires its basis" );
		}
	}

	/// Nothing is known about the combination. This is the answer for anything
	/// which has not been assessed, and leads to rejection.
	public static TemporalPreservation unknown() {
		return UNKNOWN;
	}

	/// The calendar date is preserved for the given range of values.
	public static TemporalPreservation ofDate(@Nullable LocalDate minimum, @Nullable LocalDate maximum) {
		return new TemporalPreservation(
				Level.SUPPORTED,
				Level.UNKNOWN,
				Level.UNKNOWN,
				Level.UNKNOWN,
				Level.UNKNOWN,
				null,
				minimum,
				maximum
		);
	}

	/// The time of day is preserved for every value.
	public static TemporalPreservation ofTime() {
		return new TemporalPreservation(
				Level.UNKNOWN,
				Level.SUPPORTED,
				Level.UNKNOWN,
				Level.UNKNOWN,
				Level.UNKNOWN,
				null,
				null,
				null
		);
	}

	/// The calendar date and time of day are preserved for the given range of values.
	public static TemporalPreservation ofDateTime(@Nullable LocalDateTime minimum, @Nullable LocalDateTime maximum) {
		return new TemporalPreservation(
				Level.SUPPORTED,
				Level.SUPPORTED,
				Level.UNKNOWN,
				Level.UNKNOWN,
				Level.UNKNOWN,
				null,
				minimum,
				maximum
		);
	}

	/// The instant is preserved for the given range of values.
	public static TemporalPreservation ofInstant(
			InstantBasis basis,
			@Nullable Instant minimum,
			@Nullable Instant maximum) {
		return new TemporalPreservation(
				Level.UNKNOWN,
				Level.UNKNOWN,
				Level.SUPPORTED,
				Level.UNKNOWN,
				Level.UNKNOWN,
				basis,
				minimum,
				maximum
		);
	}

	/// The level reported for the given part.
	public Level level(Part part) {
		return switch ( part ) {
			case DATE -> date;
			case TIME -> time;
			case INSTANT -> instant;
			case OFFSET -> offset;
			case ZONE_ID -> zoneId;
		};
	}

	/// The first part required by the given Java Time class which is not
	/// [Level#SUPPORTED], or `null` when every required part is supported.
	/// A class which is not a recognized Java Time class requires every part,
	/// so that nothing is reported as preserved by accident.
	public @Nullable Part firstUnsupportedPart(Class<?> javaTimeClass) {
		return firstUnsupportedPart( Part.requiredBy( javaTimeClass ) );
	}

	/// The first of the given parts which is not [Level#SUPPORTED], or `null`
	/// when every one of them is supported.
	public @Nullable Part firstUnsupportedPart(Set<Part> required) {
		for ( var part : Part.values() ) {
			if ( required.contains( part ) && level( part ) != Level.SUPPORTED ) {
				return part;
			}
		}
		return null;
	}

	/// A part of a Java Time value.
	public enum Part {
		/// The calendar date: year, month and day.
		DATE,
		/// The time of day.
		TIME,
		/// The point on the time-line.
		INSTANT,
		/// The offset from UTC.
		OFFSET,
		/// The identity of a region-based zone, such as `Europe/Paris`.
		/// An instant together with an offset does not preserve it.
		ZONE_ID;

		private static final Set<Part> OF_DATE = Set.of( DATE );
		private static final Set<Part> OF_TIME = Set.of( TIME );
		private static final Set<Part> OF_DATE_TIME = Set.of( DATE, TIME );
		private static final Set<Part> OF_INSTANT = Set.of( INSTANT );
		private static final Set<Part> OF_OFFSET_TIME = Set.of( TIME, OFFSET );
		private static final Set<Part> OF_OFFSET_DATE_TIME = Set.of( INSTANT, OFFSET );
		private static final Set<Part> OF_ZONED_DATE_TIME = Set.of( INSTANT, OFFSET, ZONE_ID );
		private static final Set<Part> ALL = Set.of( values() );

		/// The parts which make up a value of the given Java Time class. A
		/// class which is not a recognized Java Time class requires every part.
		/// The parts required of a value of the given Java Time class when
		/// only its instant matters, as selected with
		/// [org.hibernate.annotations.TemporalPreservationPolicy.Type#NORMALIZED].
		/// For any class other than `OffsetDateTime` and `ZonedDateTime` these
		/// are the parts which make up the value.
		public static Set<Part> requiredOfNormalized(Class<?> javaTimeClass) {
			return javaTimeClass == OffsetDateTime.class || javaTimeClass == ZonedDateTime.class
					? OF_INSTANT
					: requiredBy( javaTimeClass );
		}

		public static Set<Part> requiredBy(Class<?> javaTimeClass) {
			if ( javaTimeClass == LocalDate.class ) {
				return OF_DATE;
			}
			else if ( javaTimeClass == LocalTime.class ) {
				return OF_TIME;
			}
			else if ( javaTimeClass == LocalDateTime.class ) {
				return OF_DATE_TIME;
			}
			else if ( javaTimeClass == Instant.class ) {
				return OF_INSTANT;
			}
			else if ( javaTimeClass == OffsetTime.class ) {
				return OF_OFFSET_TIME;
			}
			else if ( javaTimeClass == OffsetDateTime.class ) {
				return OF_OFFSET_DATE_TIME;
			}
			else if ( javaTimeClass == ZonedDateTime.class ) {
				return OF_ZONED_DATE_TIME;
			}
			else {
				return ALL;
			}
		}
	}

	/// How much is known about the preservation of a part.
	public enum Level {
		/// The combination has not been assessed for this part.
		UNKNOWN,
		/// The combination is known to lose this part.
		UNSUPPORTED,
		/// The combination is known to preserve this part.
		SUPPORTED
	}

	/// How a stored value denotes an instant.
	public enum InstantBasis {
		/// The SQL type itself carries an instant, for example
		/// `timestamp with time zone`.
		NATIVE,
		/// The SQL type carries date and time fields only, and they denote
		/// an instant by the documented convention that they are in UTC.
		/// Other consumers of the column must apply the same convention.
		UTC_CONVENTION
	}
}
