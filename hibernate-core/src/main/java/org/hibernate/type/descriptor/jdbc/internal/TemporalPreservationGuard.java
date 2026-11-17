package org.hibernate.type.descriptor.jdbc.internal;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.util.Optional;
import java.util.Set;

import jakarta.annotation.Nullable;

import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.TemporalPreservationException;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.type.spi.TemporalPreservation;
import org.hibernate.dialect.type.spi.TemporalPreservation.Part;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BasicPluralJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.TemporalJdbcType;

import static org.hibernate.cfg.MappingSettings.TEMPORAL_PRESERVATION;

/// Enforces temporal preservation for Java Time values at the JDBC boundary.
///
/// With temporal preservation enabled, a Java Time value is bound, rendered
/// as a literal, or returned from a result only when the
/// [Dialect][Dialect#getTemporalPreservation] reports the exact combination
/// of Java type and JDBC type as preserving every required part, and the
/// value lies within the range reported for the combination. Anything else
/// is rejected with a [TemporalPreservationException].
///
/// With temporal preservation disabled nothing here has any effect.
///
/// @see org.hibernate.cfg.MappingSettings#TEMPORAL_PRESERVATION
///
/// @since 8.1
/// @author Steve Ebersole
@Internal
public final class TemporalPreservationGuard {
	private TemporalPreservationGuard() {
	}

	/// Whether the class is one of the Java Time classes covered by temporal
	/// preservation.
	public static boolean isJavaTimeClass(@Nullable Class<?> javaClass) {
		return javaClass == LocalDate.class
			|| javaClass == LocalTime.class
			|| javaClass == LocalDateTime.class
			|| javaClass == Instant.class
			|| javaClass == OffsetDateTime.class
			|| javaClass == OffsetTime.class
			|| javaClass == ZonedDateTime.class;
	}

	/// Whether values of the Java type are subject to this guard. True for
	/// the covered Java Time classes and for arrays and collections of them.
	public static boolean appliesTo(@Nullable JavaType<?> javaType) {
		return javaType != null
			&& ( isJavaTimeClass( javaType.getJavaTypeClass() ) || isJavaTimeContainer( javaType ) );
	}

	/// Whether the Java type is an array or collection of a covered Java Time class.
	public static boolean isJavaTimeContainer(JavaType<?> javaType) {
		return javaType instanceof BasicPluralJavaType<?> plural
			&& appliesTo( plural.getElementJavaType() );
	}

	/// Whether temporal preservation is enabled for the operation.
	public static boolean isEnabled(@Nullable WrapperOptions options) {
		if ( options == null ) {
			return false;
		}
		final var sessionFactory = options.getSessionFactory();
		return sessionFactory != null
			&& sessionFactory.getSessionFactoryOptions().isTemporalPreservationEnabled();
	}

	/// Check a non-null value about to be bound to a JDBC statement.
	public static void checkBind(
			Object value,
			JavaType<?> javaType,
			JdbcType jdbcType,
			WrapperOptions options) {
		check( value, javaType, jdbcType, options, "bind" );
	}

	/// Check a non-null value extracted from a JDBC result or output parameter.
	public static void checkExtracted(
			Object value,
			JavaType<?> javaType,
			JdbcType jdbcType,
			WrapperOptions options) {
		check( value, javaType, jdbcType, options, "read" );
	}

	/// Reject the extraction of an output parameter of a stored procedure or
	/// function. No JDBC driver has been established to return Java Time
	/// values from a [java.sql.CallableStatement] without loss, so the
	/// operation is never supported, whatever the value.
	public static void rejectCallableOutput(JavaType<?> javaType, WrapperOptions options) {
		if ( isEnabled( options ) ) {
			throw new TemporalPreservationException(
					"Cannot read a value of type '" + javaType.getTypeName()
					+ "' from an output parameter of a stored procedure or function: with '"
					+ TEMPORAL_PRESERVATION + "' enabled, output parameters of Java Time types are not supported"
			);
		}
	}

	/// Reject a fallback to a different JDBC access path after the JDBC driver
	/// failed on the path which was assessed. The fallback has not been
	/// assessed, so with temporal preservation enabled it must not be taken.
	/// Does nothing when temporal preservation is disabled.
	///
	/// @param failure what the driver threw on the assessed path
	public static void rejectFallback(JavaType<?> javaType, WrapperOptions options, Throwable failure) {
		if ( isEnabled( options ) ) {
			throw new TemporalPreservationException(
					"Cannot bind or read a value of type '" + javaType.getTypeName()
					+ "': the JDBC driver failed to handle it directly, and with '" + TEMPORAL_PRESERVATION
					+ "' enabled it is not converted to a java.sql.Timestamp instead",
					failure
			);
		}
	}

	/// Check a non-null value about to be rendered as a SQL literal, with
	/// temporal preservation known to be enabled. The literal is checked
	/// against the default mapping of its Java type.
	public static void checkLiteral(Object value, JavaType<?> javaType, WrapperOptions options) {
		if ( appliesTo( javaType ) ) {
			final var jdbcType = javaType.getRecommendedJdbcType(
					options.getTypeConfiguration().getCurrentBaseSqlTypeIndicators()
			);
			check( value, javaType, jdbcType, options.getDialect(), "render as a SQL literal" );
		}
	}

	/// Check a non-null value about to be rendered as a SQL literal through
	/// the given JDBC type, whatever renders it. Unlike
	/// [#checkLiteral(Object, JavaType, WrapperOptions)], which the standard
	/// formatter of temporal literals applies to itself, this is applied
	/// before any formatter is asked, so that a JDBC type with a formatter
	/// of its own is held to what is reported for it as well. Does nothing
	/// when temporal preservation is disabled, or when the value is not yet
	/// in the form the Java type describes.
	public static void checkLiteral(
			@Nullable Object value,
			JavaType<?> javaType,
			JdbcType jdbcType,
			@Nullable WrapperOptions options) {
		if ( value != null
				&& isEnabled( options )
				&& appliesTo( javaType )
				&& javaType.getJavaTypeClass().isInstance( value ) ) {
			check( value, javaType, jdbcType, options.getDialect(), "render as a SQL literal" );
		}
	}

	/// The instant which the value denotes, when it is an `Instant`, an
	/// `OffsetDateTime` or a `ZonedDateTime`, and `null` otherwise.
	public static @Nullable Instant instantOf(Object value) {
		if ( value instanceof Instant instant ) {
			return instant;
		}
		else if ( value instanceof OffsetDateTime offsetDateTime ) {
			return offsetDateTime.toInstant();
		}
		else if ( value instanceof ZonedDateTime zonedDateTime ) {
			return zonedDateTime.toInstant();
		}
		else {
			return null;
		}
	}

	/// Whether the default mapping of the Java type on the Dialect stores an
	/// instant as its date and time at UTC, in a column which knows of no
	/// time zone, as opposed to a column which itself denotes an instant.
	public static boolean isUtcByConvention(JavaType<?> javaType, WrapperOptions options) {
		final var jdbcType = javaType.getRecommendedJdbcType(
				options.getTypeConfiguration().getCurrentBaseSqlTypeIndicators()
		);
		return preservation( options.getDialect(), javaType, jdbcType ).instantBasis()
			== TemporalPreservation.InstantBasis.UTC_CONVENTION;
	}

	/// Reject a value with an offset about to be rendered as a SQL literal
	/// which the literal formats cannot express. They write an offset as
	/// hours and minutes, so an offset with seconds would denote a different
	/// instant. They write the year of the date in the value's own offset
	/// with four digits and no era, which for a value near either end of its
	/// range may fall outside the years 1 to 9999 although its instant does not.
	public static void checkLiteralOffset(Object value, JavaType<?> javaType) {
		if ( value instanceof TemporalAccessor temporal && temporal.isSupported( ChronoField.OFFSET_SECONDS ) ) {
			final String problem;
			if ( temporal.get( ChronoField.OFFSET_SECONDS ) % 60 != 0 ) {
				problem = "a literal whose offset has seconds";
			}
			else if ( temporal.isSupported( ChronoField.YEAR )
					&& ( temporal.get( ChronoField.YEAR ) < 1 || temporal.get( ChronoField.YEAR ) > 9999 ) ) {
				problem = "a literal whose date, in its own offset, is outside the years 1 to 9999";
			}
			else {
				return;
			}
			throw new TemporalPreservationException(
					"Cannot render as a SQL literal the value '" + value + "' of type '" + javaType.getTypeName()
					+ "': with '" + TEMPORAL_PRESERVATION + "' enabled, " + problem
					+ " is not supported; bind the value as a parameter instead"
			);
		}
	}

	/// What the combination of Java type and JDBC type preserves on the
	/// Dialect. The Dialect is asked first. Where it knows nothing of the
	/// combination, a JDBC type which reports for itself, by implementing
	/// [TemporalJdbcType], is asked next.
	public static TemporalPreservation preservation(Dialect dialect, JavaType<?> javaType, JdbcType jdbcType) {
		final var reported = dialect.getTemporalPreservation( javaType, jdbcType );
		if ( !TemporalPreservation.unknown().equals( reported ) || !( jdbcType instanceof TemporalJdbcType temporal ) ) {
			return reported;
		}
		final Optional<TemporalPreservation> stated;
		try {
			stated = temporal.getTemporalPreservation( javaType, dialect );
		}
		catch (RuntimeException e) {
			throw new MappingException(
					"TemporalJdbcType " + jdbcType.getClass().getName() + " failed to report what it preserves of '"
					+ javaType.getTypeName() + "': " + e.getMessage(),
					e
			);
		}
		//noinspection OptionalAssignedToNull
		if ( stated == null ) {
			throw new MappingException(
					"TemporalJdbcType " + jdbcType.getClass().getName() + " returned null for what it preserves of '"
					+ javaType.getTypeName() + "'; it must return an empty Optional to decline"
			);
		}
		return stated.orElse( reported );
	}

	/// Explain why values exchanged through the JDBC type would escape the
	/// checks made here, or return `null` when they would not. Values are
	/// checked by [BasicBinder] as they are bound and by [BasicExtractor] as
	/// they are read, so a JDBC type whose binder or extractor is not one of
	/// those, or replaces their entry points, is not checked at all, and one
	/// which hands out those of another JDBC type is checked against the
	/// report of that other type.
	public static @Nullable String uncheckedReason(JavaType<?> javaType, JdbcType jdbcType) {
		final var binder = jdbcType.getBinder( javaType );
		final var extractor = jdbcType.getExtractor( javaType );
		final String unchecked;
		if ( !( binder instanceof BasicBinder<?> basicBinder ) ) {
			unchecked = "binder is not a BasicBinder";
		}
		else if ( !( extractor instanceof BasicExtractor<?> basicExtractor ) ) {
			unchecked = "extractor is not a BasicExtractor";
		}
		else if ( overridesExtract( extractor.getClass() ) ) {
			unchecked = "extractor overrides BasicExtractor.extract()";
		}
		// Values are checked against what is reported for the JDBC type which the binder
		// or extractor was created for. One borrowed from another JDBC type would have
		// them checked against the report of that one.
		else if ( basicBinder.getJdbcType() != jdbcType ) {
			unchecked = "binder is that of " + basicBinder.getJdbcType().getClass().getSimpleName();
		}
		else if ( basicExtractor.getJdbcType() != jdbcType ) {
			unchecked = "extractor is that of " + basicExtractor.getJdbcType().getClass().getSimpleName();
		}
		else {
			return null;
		}
		return "with '" + TEMPORAL_PRESERVATION + "' enabled, values are checked as they pass through BasicBinder"
			+ " and BasicExtractor, and the " + unchecked + " of " + jdbcType.getClass().getName()
			+ ", so its values would not be checked";
	}

	private static boolean overridesExtract(Class<?> extractorClass) {
		for ( Class<?> type = extractorClass; type != null && type != BasicExtractor.class; type = type.getSuperclass() ) {
			for ( var method : type.getDeclaredMethods() ) {
				if ( method.getName().equals( "extract" ) && !method.isSynthetic() ) {
					return true;
				}
			}
		}
		return false;
	}

	/// Check a time of day with an offset about to be rendered as a SQL
	/// literal, with temporal preservation known to be enabled. Where the
	/// Dialect renders a time literal without an offset, the offset of the
	/// value would be dropped, and the literal would denote another time.
	public static void checkTimeLiteral(Object value, JavaType<?> javaType, Dialect dialect) {
		if ( value instanceof TemporalAccessor temporal
				&& temporal.isSupported( ChronoField.OFFSET_SECONDS )
				&& !dialect.getTemporalValueSemantics().supportsLiteralOffset() ) {
			throw new TemporalPreservationException(
					"Cannot render as a SQL literal the value '" + value + "' of type '" + javaType.getTypeName()
					+ "': with '" + TEMPORAL_PRESERVATION + "' enabled, a literal for a time with an offset is"
					+ " not supported on " + dialect.getClass().getSimpleName()
					+ ", where it would be rendered without its offset; bind the value as a parameter instead"
			);
		}
	}

	private static void check(
			Object value,
			JavaType<?> javaType,
			JdbcType jdbcType,
			WrapperOptions options,
			String operation) {
		if ( isEnabled( options ) ) {
			check( value, javaType, jdbcType, options.getDialect(), operation );
		}
	}

	private static void check(
			Object value,
			JavaType<?> javaType,
			JdbcType jdbcType,
			Dialect dialect,
			String operation) {
		final var preservation = isJavaTimeContainer( javaType )
				? TemporalPreservation.unknown()
				: preservation( dialect, javaType, jdbcType );
		// Which parts a mapping requires was settled when the factory was built.
		// Here, where the mapping is not known, the least any mapping may require
		// is required, which is also what a value without a mapping is held to.
		final String problem = unsupportedReason(
				dialect,
				javaType,
				jdbcType,
				preservation,
				Part.requiredOfNormalized( javaType.getJavaTypeClass() )
		);
		if ( problem != null ) {
			throw new TemporalPreservationException(
					"Cannot " + operation + " a value of type '" + javaType.getTypeName() + "': " + problem
			);
		}
		if ( isBefore( value, preservation.minimum() ) || isAfter( value, preservation.maximum() ) ) {
			throw new TemporalPreservationException(
					"Cannot " + operation + " the value '" + value + "' of type '" + javaType.getTypeName()
					+ "': with '" + TEMPORAL_PRESERVATION + "' enabled, " + describe( dialect, jdbcType )
					+ " preserves only values from " + bound( preservation.minimum() )
					+ " to " + bound( preservation.maximum() )
			);
		}
	}

	/// Explain why the combination does not preserve every part which makes
	/// up a value of the Java type on the Dialect, or return `null` when it does.
	public static @Nullable String unsupportedReason(Dialect dialect, JavaType<?> javaType, JdbcType jdbcType) {
		return unsupportedReason( dialect, javaType, jdbcType, Part.requiredBy( javaType.getJavaTypeClass() ) );
	}

	/// Explain why the combination does not preserve the given parts on the
	/// Dialect, or return `null` when it does.
	public static @Nullable String unsupportedReason(
			Dialect dialect,
			JavaType<?> javaType,
			JdbcType jdbcType,
			Set<Part> required) {
		return unsupportedReason(
				dialect,
				javaType,
				jdbcType,
				isJavaTimeContainer( javaType )
						? TemporalPreservation.unknown()
						: preservation( dialect, javaType, jdbcType ),
				required
		);
	}

	private static @Nullable String unsupportedReason(
			Dialect dialect,
			JavaType<?> javaType,
			JdbcType jdbcType,
			TemporalPreservation preservation,
			Set<Part> required) {
		if ( isJavaTimeContainer( javaType ) ) {
			return "with '" + TEMPORAL_PRESERVATION + "' enabled, arrays and collections of Java Time values"
				+ " stored in a single column are not supported";
		}
		final var part = preservation.firstUnsupportedPart( required );
		if ( part == null ) {
			return null;
		}
		return "with '" + TEMPORAL_PRESERVATION + "' enabled, " + describe( dialect, jdbcType )
			+ ( preservation.level( part ) == TemporalPreservation.Level.UNSUPPORTED
					? " is known to lose the " + part + " part of the value"
					: " has not been established to preserve the " + part + " part of the value" );
	}

	private static String describe(Dialect dialect, JdbcType jdbcType) {
		return jdbcType.getClass().getSimpleName() + " on " + dialect.getClass().getSimpleName();
	}

	private static String bound(@Nullable Temporal bound) {
		return bound == null ? "(unbounded)" : bound.toString();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static boolean isBefore(Object value, @Nullable Temporal minimum) {
		if ( minimum == null ) {
			return false;
		}
		final Object comparable = comparableTo( value, minimum );
		return comparable.getClass() != minimum.getClass() || ( (Comparable) comparable ).compareTo( minimum ) < 0;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static boolean isAfter(Object value, @Nullable Temporal maximum) {
		if ( maximum == null ) {
			return false;
		}
		final Object comparable = comparableTo( value, maximum );
		return comparable.getClass() != maximum.getClass() || ( (Comparable) comparable ).compareTo( maximum ) > 0;
	}

	/// A range stated as instants applies to a value with an offset or a
	/// zone by the instant it denotes.
	private static Object comparableTo(Object value, Temporal bound) {
		if ( bound instanceof Instant ) {
			if ( value instanceof OffsetDateTime offsetDateTime ) {
				return offsetDateTime.toInstant();
			}
			else if ( value instanceof ZonedDateTime zonedDateTime ) {
				return zonedDateTime.toInstant();
			}
		}
		return value;
	}
}
