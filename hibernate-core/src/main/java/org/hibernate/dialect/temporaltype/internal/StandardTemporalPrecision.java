package org.hibernate.dialect.temporaltype.internal;

import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Instant;

import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.function.UnaryOperator;

import org.hibernate.TemporalValueException;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.temporaltype.spi.TemporalValueSemantics.PrecisionAdjustment;
import org.hibernate.dialect.temporaltype.spi.TemporalPrecisionContext;
import org.hibernate.dialect.temporaltype.spi.TemporalPrecisionSupport;
import org.hibernate.type.descriptor.java.InstantJavaType;
import org.hibernate.type.descriptor.java.LocalDateJavaType;
import org.hibernate.type.descriptor.java.LocalDateTimeJavaType;
import org.hibernate.type.descriptor.java.LocalTimeJavaType;
import org.hibernate.type.descriptor.java.OffsetDateTimeJavaType;
import org.hibernate.type.descriptor.java.OffsetTimeJavaType;
import org.hibernate.type.descriptor.java.ZonedDateTimeJavaType;
import org.hibernate.type.descriptor.jdbc.LocalDateJdbcType;
import org.hibernate.type.descriptor.jdbc.LocalDateTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.LocalTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.OffsetDateTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.OffsetTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.TimeUtcAsOffsetTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampUtcAsOffsetDateTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampUtcAsPlainJdbcTimestampJdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampUtcAsZonedJdbcTimestampJdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampWithTimeZoneJdbcType;
import org.hibernate.type.descriptor.jdbc.internal.UtcTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.internal.UtcTimestampJdbcType;

/// Stock precision facts. Each recognized execution route has its own fixed-point
/// regressions; unknown replacement classes do not inherit a precision assertion.
/// Provider additions belong here or in the provider's contribution only after
/// their physical sizing and execution route have been established.
///
/// @author Steve Ebersole
public final class StandardTemporalPrecision {
	private static final Pattern FRACTIONAL_TYPE = Pattern.compile(
			"(time|timestamp)(?:\\((\\d+)\\))?( with(?:out)? time zone)?" );
	private static final Set<Class<?>> JAVA_TYPES = Set.of(
			LocalDateJavaType.class, LocalTimeJavaType.class, LocalDateTimeJavaType.class,
			InstantJavaType.class, OffsetDateTimeJavaType.class, ZonedDateTimeJavaType.class, OffsetTimeJavaType.class );

	private StandardTemporalPrecision() {
	}

	public static TemporalPrecisionSupport resolve(TemporalPrecisionContext context) {
		if ( !JAVA_TYPES.contains( context.javaType().getClass() ) ) {
			return TemporalPrecisionSupport.unsupported( "No precision contribution for the actual Java descriptor" );
		}
		// DATE has no fractional component. The existing preservation assessment
		// still owns calendar/range support; this exact stock pair adds no reduction.
		if ( context.javaType().getClass() == LocalDateJavaType.class
				&& context.jdbcType().getClass() == LocalDateJdbcType.class
				&& context.ddlTypeName().equalsIgnoreCase( "date" ) ) {
			return TemporalPrecisionSupport.supported( "sql-date-v1", UnaryOperator.identity(), UnaryOperator.identity() );
		}
		if ( context.dialect() instanceof H2Dialect ) {
			return h2( context );
		}
		if ( context.dialect().getClass() == HSQLDialect.class ) {
			return hsql( context );
		}
		return TemporalPrecisionSupport.unsupported( "No established precision execution profile for "
				+ context.dialect().getClass().getName() );
	}

	private static TemporalPrecisionSupport hsql(TemporalPrecisionContext context) {
		// JDBC reports major/minor versions; the regression envelope is HSQLDB 2.7 (tested with 2.7.4).
		if ( context.dialect().getVersion().isBefore( 2, 7 ) ) {
			return TemporalPrecisionSupport.unsupported( "HSQL precision profile requires configured version 2.7 or later" );
		}
		if ( context.operation() != TemporalPrecisionContext.Operation.ATTRIBUTE
				&& context.operation() != TemporalPrecisionContext.Operation.VERSION ) {
			return TemporalPrecisionSupport.unsupported( "Unassessed HSQL precision operation " + context.operation() );
		}
		final var java = context.javaType().getClass();
		final var jdbc = context.jdbcType().getClass();
		final String encoding;
		if ( java == LocalDateTimeJavaType.class && jdbc == LocalDateTimeJdbcType.class ) {
			encoding = "local-fields";
		}
		else if ( java == InstantJavaType.class && jdbc == TimestampUtcAsPlainJdbcTimestampJdbcType.class ) {
			encoding = "utc-calendar";
		}
		else {
			return TemporalPrecisionSupport.unsupported( "Unassessed HSQL Java/JDBC precision pair" );
		}
		// HSQLDB 2.7.4: typed local fields and the UTC Calendar timestamp route
		// retain prepared nanoseconds and truncate to the declared timestamp grid.
		// VERSION here certifies storage only, not a database timestamp producer.
		final var definition = FRACTIONAL_TYPE.matcher( context.ddlTypeName().toLowerCase( Locale.ROOT )
				.trim().replaceAll( "\\s+", " " ) );
		if ( !definition.matches() || !"timestamp".equals( definition.group( 1 ) )
				|| " with time zone".equals( definition.group( 3 ) ) ) {
			return TemporalPrecisionSupport.unsupported( "Unresolved physical HSQL timestamp column definition" );
		}
		final int physical = definition.group( 2 ) == null ? 6 : Integer.parseInt( definition.group( 2 ) );
		final int precision = context.declaredPrecision() == null ? physical : context.declaredPrecision();
		if ( physical < 0 || physical > 9 || precision < 0 || precision > physical ) {
			return TemporalPrecisionSupport.unsupported( "HSQL temporal precision must be within 0..9 and not exceed physical column precision " + physical );
		}
		final var quantizer = new DecimalTemporalPrecision( precision, 9, PrecisionAdjustment.TRUNCATE, true );
		return TemporalPrecisionSupport.supported( "hsql-v1:1582-10-15..9999-12-31:" + encoding + ':' + quantizer.signature(),
				value -> {
					// HSQL's typed LocalDateTime extraction uses a GregorianCalendar,
					// losing proleptic fields before its cutover. Certify only the
					// established finite interval, for both timestamp carriers.
					final var date = value instanceof LocalDateTime local ? local.toLocalDate()
							: ((Instant) value).atOffset( ZoneOffset.UTC ).toLocalDate();
					if ( date.isBefore( LocalDate.of( 1582, 10, 15 ) ) || date.isAfter( LocalDate.of( 9999, 12, 31 ) ) ) {
						throw new TemporalValueException( "HSQL timestamp precision profile supports finite dates 1582-10-15..9999-12-31" );
					}
					return quantizer.apply( value );
				}, UnaryOperator.identity() );
	}

	private static TemporalPrecisionSupport h2(TemporalPrecisionContext context) {
		final var java = context.javaType().getClass();
		final var jdbc = context.jdbcType().getClass();

		UnaryOperator<Object> reconstruction = null;
		String encoding = null;
		if ( java == LocalTimeJavaType.class && jdbc == LocalTimeJdbcType.class
				|| java == LocalDateTimeJavaType.class && jdbc == LocalDateTimeJdbcType.class
				|| java == OffsetTimeJavaType.class && jdbc == OffsetTimeJdbcType.class
				|| java == OffsetDateTimeJavaType.class && jdbc == OffsetDateTimeJdbcType.class ) {
			reconstruction = UnaryOperator.identity();
			encoding = "fields";
		}
		else if ( (java == OffsetDateTimeJavaType.class || java == ZonedDateTimeJavaType.class)
				&& jdbc == TimestampWithTimeZoneJdbcType.class ) {
			reconstruction = value -> value instanceof ZonedDateTime zoned
					? zoned.toOffsetDateTime().toZonedDateTime() : value;
			encoding = "offset";
		}
		else {
			final var carrier = jdbc == UtcTimestampJdbcType.class
					? ((UtcTimestampJdbcType) context.jdbcType()).getDelegate().getClass() : jdbc;
			if ( java == InstantJavaType.class && (carrier == TimestampUtcAsOffsetDateTimeJdbcType.class
					|| carrier == TimestampUtcAsZonedJdbcTimestampJdbcType.class
					|| carrier == TimestampUtcAsPlainJdbcTimestampJdbcType.class) ) {
				reconstruction = UnaryOperator.identity();
				encoding = "instant";
			}
			else if ( (java == OffsetDateTimeJavaType.class || java == ZonedDateTimeJavaType.class)
					&& (carrier == TimestampUtcAsOffsetDateTimeJdbcType.class
					|| context.adjustment() != null && (carrier == TimestampUtcAsZonedJdbcTimestampJdbcType.class
					|| carrier == TimestampUtcAsPlainJdbcTimestampJdbcType.class)) ) {
				reconstruction = value -> value instanceof OffsetDateTime offset
						? offset.withOffsetSameInstant( ZoneOffset.UTC )
						: ((ZonedDateTime) value).withZoneSameInstant( ZoneOffset.UTC );
				encoding = "utc";
			}
			else if ( java == OffsetTimeJavaType.class
					&& (jdbc == TimeUtcAsOffsetTimeJdbcType.class || jdbc == UtcTimeJdbcType.class
					&& ((UtcTimeJdbcType) context.jdbcType()).getDelegate().getClass() == TimeUtcAsOffsetTimeJdbcType.class) ) {
				reconstruction = value -> ((OffsetTime) value).withOffsetSameInstant( ZoneOffset.UTC );
				encoding = "utc-time";
			}
		}
		if ( reconstruction == null ) {
			return TemporalPrecisionSupport.unsupported( "Unassessed H2 Java/JDBC precision pair" );
		}
		final Integer precision;
		if ( context.operation() == TemporalPrecisionContext.Operation.EXPRESSION ) {
			// H2 typed extraction can carry all nine digits of the expression result;
			// a read-only formula has no attribute assignment grid to impose.
			precision = 9;
		}
		else {
			final var definition = FRACTIONAL_TYPE.matcher( context.ddlTypeName().toLowerCase( Locale.ROOT )
					.trim().replaceAll( "\\s+", " " ) );
			if ( !definition.matches() ) {
				return TemporalPrecisionSupport.unsupported( "Unresolved physical H2 temporal column definition" );
			}
			final boolean time = java == LocalTimeJavaType.class || java == OffsetTimeJavaType.class;
			if ( !definition.group( 1 ).equals( time ? "time" : "timestamp" ) ) {
				return TemporalPrecisionSupport.unsupported( "Physical column definition contradicts the temporal JDBC carrier" );
			}
			if ( (jdbc == OffsetDateTimeJdbcType.class || jdbc == OffsetTimeJdbcType.class
					|| jdbc == TimestampWithTimeZoneJdbcType.class)
					&& !" with time zone".equals( definition.group( 3 ) ) ) {
				return TemporalPrecisionSupport.unsupported( "Physical column definition omits the JDBC carrier's required timezone" );
			}
			final int physical = definition.group( 2 ) == null ? time ? 0 : 6 : Integer.parseInt( definition.group( 2 ) );
			precision = context.declaredPrecision() == null ? physical : context.declaredPrecision();
			if ( precision > physical ) {
				return TemporalPrecisionSupport.unsupported( "Declared fractional precision exceeds the physical column precision " + physical );
			}
		}
		if ( precision < 0 || precision > 9 ) {
			return TemporalPrecisionSupport.unsupported( "H2 temporal precision must resolve to 0..9" );
		}
		final var quantizer = new DecimalTemporalPrecision( precision, 9,
				context.dialect().getTemporalValueSemantics().precisionAdjustment(), context.adjustment() == null );
		return TemporalPrecisionSupport.supported( "h2-v1:" + encoding + ':' + quantizer.signature(), quantizer, reconstruction );
	}
}
