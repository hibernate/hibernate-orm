package org.hibernate.type.descriptor.jdbc.internal;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;

import jakarta.annotation.Nullable;

import org.hibernate.Internal;
import org.hibernate.TemporalPreservationException;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.type.spi.TemporalPreservation;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BasicPluralJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

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
				: dialect.getTemporalPreservation( javaType, jdbcType );
		final String problem = unsupportedReason( dialect, javaType, jdbcType, preservation );
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

	/// Explain why the combination is not supported for temporal preservation
	/// on the Dialect, or return `null` when it is supported.
	public static @Nullable String unsupportedReason(Dialect dialect, JavaType<?> javaType, JdbcType jdbcType) {
		return unsupportedReason(
				dialect,
				javaType,
				jdbcType,
				isJavaTimeContainer( javaType )
						? TemporalPreservation.unknown()
						: dialect.getTemporalPreservation( javaType, jdbcType )
		);
	}

	private static @Nullable String unsupportedReason(
			Dialect dialect,
			JavaType<?> javaType,
			JdbcType jdbcType,
			TemporalPreservation preservation) {
		if ( isJavaTimeContainer( javaType ) ) {
			return "with '" + TEMPORAL_PRESERVATION + "' enabled, arrays and collections of Java Time values"
				+ " stored in a single column are not supported";
		}
		final var part = preservation.firstUnsupportedPart( javaType.getJavaTypeClass() );
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
		return minimum != null
			&& ( value.getClass() != minimum.getClass() || ( (Comparable) value ).compareTo( minimum ) < 0 );
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static boolean isAfter(Object value, @Nullable Temporal maximum) {
		return maximum != null
			&& ( value.getClass() != maximum.getClass() || ( (Comparable) value ).compareTo( maximum ) > 0 );
	}
}
