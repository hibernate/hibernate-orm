package org.hibernate.dialect.temporaltype.internal;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;

import jakarta.persistence.ParameterMode;

import org.hibernate.TemporalValueException;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.jdbc.spi.PostgreSQLDriverKind;
import org.hibernate.dialect.temporaltype.spi.TemporalCallablePrecisionContext;
import org.hibernate.dialect.temporaltype.spi.TemporalCallablePrecisionContext.Access;
import org.hibernate.dialect.temporaltype.spi.TemporalCallablePrecisionContext.Boundary;
import org.hibernate.dialect.temporaltype.spi.TemporalPrecisionContext;
import org.hibernate.dialect.temporaltype.spi.TemporalPrecisionSupport;
import org.hibernate.dialect.type.internal.PostgreSQLOffsetDateTimeJdbcType;
import org.hibernate.dialect.type.internal.PostgreSQLOffsetTimeJdbcType;
import org.hibernate.dialect.type.internal.PostgreSQLUtcTimeJdbcType;
import org.hibernate.type.descriptor.DateTimeUtils;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.InstantJavaType;
import org.hibernate.type.descriptor.java.OffsetDateTimeJavaType;
import org.hibernate.type.descriptor.java.OffsetTimeJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampUtcAsPlainJdbcTimestampJdbcType;
import org.hibernate.type.descriptor.jdbc.internal.UtcTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.internal.UtcTimestampJdbcType;

/// Bounded PostgreSQL 18/pgJDBC precision facts, tested with 18.6/42.7.14 in
/// default, text, binary and simple-query modes. Timestamp typmod reduction uses
/// ties away from PostgreSQL's epoch, after the driver's microsecond reduction.
/// Neither an attribute grid nor a raw JDBC OUT probe certifies callable transport.
///
/// @author Steve Ebersole
final class PostgreSQLTemporalPrecision {
	private static final Pattern TYPE = Pattern.compile( "(time|timestamp)(?:\\((\\d+)\\))?( with(?:out)? time zone)?" );
	private static final Instant EPOCH = Instant.parse( "2000-01-01T00:00:00Z" );
	private static final LocalDate MIN = LocalDate.of( -4712, 1, 1 );
	private static final LocalDate MAX = LocalDate.of( 294276, 12, 31 );

	private PostgreSQLTemporalPrecision() {}

	private enum Route { NATIVE_TIMESTAMP, CALENDAR_TIMESTAMP, INSTANT_TIMESTAMP, NATIVE_TIME, UTC_TIME }

	private static boolean admitted(Dialect dialect) {
		return dialect.getClass() == PostgreSQLDialect.class
				&& dialect.getVersion().getMajor() == 18
				&& ((PostgreSQLDialect) dialect).getDriverKind() == PostgreSQLDriverKind.PG_JDBC;
	}

	private static Route route(JavaType<?> java, JdbcType jdbc, ZoneId adjustment) {
		if ( java.getClass() == InstantJavaType.class && jdbc.getClass() == TimestampUtcAsPlainJdbcTimestampJdbcType.class ) {
			return Route.INSTANT_TIMESTAMP;
		}
		if ( java.getClass() == OffsetDateTimeJavaType.class ) {
			if ( jdbc.getClass() == PostgreSQLOffsetDateTimeJdbcType.class ) {
				return Route.NATIVE_TIMESTAMP;
			}
			if ( adjustment != null && jdbc.getClass() == UtcTimestampJdbcType.class
					&& ((UtcTimestampJdbcType) jdbc).getDelegate().getClass() == TimestampUtcAsPlainJdbcTimestampJdbcType.class ) {
				return Route.CALENDAR_TIMESTAMP;
			}
		}
		else if ( java.getClass() == OffsetTimeJavaType.class ) {
			if ( jdbc.getClass() == PostgreSQLOffsetTimeJdbcType.class ) {
				return Route.NATIVE_TIME;
			}
			if ( adjustment != null && jdbc.getClass() == UtcTimeJdbcType.class
					&& ((UtcTimeJdbcType) jdbc).getDelegate().getClass() == PostgreSQLUtcTimeJdbcType.class ) {
				return Route.UTC_TIME;
			}
		}
		return null;
	}

	static TemporalPrecisionSupport resolve(TemporalPrecisionContext context) {
		if ( !admitted( context.dialect() ) ) {
			return TemporalPrecisionSupport.unsupported( "PostgreSQL precision requires configured PostgreSQL 18 and PG_JDBC" );
		}
		if ( context.operation() != TemporalPrecisionContext.Operation.ATTRIBUTE ) {
			return TemporalPrecisionSupport.unsupported( "Unassessed PostgreSQL precision operation " + context.operation() );
		}
		final var route = route( context.javaType(), context.jdbcType(), context.adjustment() );
		if ( route == null ) {
			return TemporalPrecisionSupport.unsupported( "Unassessed PostgreSQL Java/JDBC precision pair" );
		}
		final boolean time = route == Route.NATIVE_TIME || route == Route.UTC_TIME;
		final boolean zone = route == Route.NATIVE_TIME || route == Route.NATIVE_TIMESTAMP;
		final var definition = TYPE.matcher( context.ddlTypeName().toLowerCase( Locale.ROOT ).trim().replaceAll( "\\s+", " " ) );
		if ( !definition.matches() || !definition.group( 1 ).equals( time ? "time" : "timestamp" )
				|| zone != " with time zone".equals( definition.group( 3 ) )
				|| context.ddlTypeCode() != context.jdbcType().getDdlTypeCode() ) {
			return TemporalPrecisionSupport.unsupported( "Unresolved or contradictory physical PostgreSQL temporal definition" );
		}
		final int physical = definition.group( 2 ) == null ? 6 : Integer.parseInt( definition.group( 2 ) );
		final int precision = context.declaredPrecision() == null ? physical : context.declaredPrecision();
		if ( physical > 6 || precision < 0 || precision > physical ) {
			return TemporalPrecisionSupport.unsupported( "PostgreSQL precision must be within 0..6 and not exceed physical precision " + physical );
		}
		return support( route, precision, "attribute" );
	}

	static TemporalPrecisionSupport resolveCallable(TemporalCallablePrecisionContext context) {
		if ( !admitted( context.dialect() ) ) {
			return TemporalPrecisionSupport.unsupported( "PostgreSQL callable precision requires configured PostgreSQL 18 and PG_JDBC" );
		}
		final var route = route( context.type().getJdbcJavaType(), context.type().getJdbcType(), context.adjustment() );
		final boolean input = context.mode() == ParameterMode.IN && context.boundary() == Boundary.INPUT
				&& context.valueAccess() == Access.INDEXED && context.registrationAccess() == Access.NONE;
		final boolean result = context.functionReturn() && context.boundary() == Boundary.OUTPUT
				&& context.valueAccess() == Access.RESULT_SET && context.registrationAccess() == Access.NONE;
		if ( route == Route.CALENDAR_TIMESTAMP && (input || result) || route == Route.NATIVE_TIME && input ) {
			return support( route, 6, "callable:" + context.boundary() + ':' + context.valueAccess() );
		}
		return TemporalPrecisionSupport.unsupported( "Unassessed PostgreSQL callable precision route; temporal OUT/INOUT requires a supported result-set mapping" );
	}

	private static TemporalPrecisionSupport support(Route route, int precision, String operation) {
		final String range = switch ( route ) {
			case NATIVE_TIMESTAMP -> "proleptic-finite-4712..294276:offset-15:59:59..+15:59:59";
			case CALENDAR_TIMESTAMP, INSTANT_TIMESTAMP -> "utc-calendar-finite-4712..294276";
			case NATIVE_TIME -> "offset-15:59:59..+15:59:59:truncate-micro6-except-end-of-day";
			case UTC_TIME -> "utc-only-time";
		};
		return TemporalPrecisionSupport.supported( "postgresql18-pgjdbc-v1:" + route + ':' + operation + ":driver-micro6-then-epoch-round:" + precision
				+ ':' + range,
				value -> quantize( value, route, precision ),
				value -> value instanceof OffsetDateTime timestamp ? timestamp.withOffsetSameInstant( ZoneOffset.UTC ) : value );
	}

	private static Object quantize(Object value, Route route, int precision) {
		check( value, route );
		// pgJDBC's OffsetTime text encoder truncates the fraction (unlike LocalTime),
		// except for its explicit end-of-day sentinel. Prepared microseconds avoid
		// dependence on that driver reduction; the storage rule records it explicitly.
		final Temporal micros = route == Route.NATIVE_TIME
				&& !((OffsetTime) value).toLocalTime().isAfter( LocalTime.of( 23, 59, 59, 999_999_499 ) )
				? DateTimeUtils.truncateToPrecision( (Temporal) value, 6 )
				: DateTimeUtils.roundToSecondPrecision( (Temporal) value, 6 );
		final int unit = (int) Math.pow( 10, 9 - precision );
		final Instant timestamp = micros instanceof Instant instant ? instant
				: micros instanceof OffsetDateTime offset ? offset.toInstant() : null;
		final boolean negativeTie = timestamp != null && timestamp.isBefore( EPOCH )
				&& precision < 6 && micros.get( ChronoField.NANO_OF_SECOND ) % unit == unit / 2;
		final Temporal result = negativeTie ? DateTimeUtils.truncateToPrecision( micros, precision )
				: DateTimeUtils.roundToSecondPrecision( micros, precision );
		check( result, route );
		return result;
	}

	private static void check(Object value, Route route) {
		final ZoneOffset offset = value instanceof OffsetDateTime timestamp ? timestamp.getOffset()
				: value instanceof OffsetTime time ? time.getOffset() : ZoneOffset.UTC;
		if ( (route == Route.NATIVE_TIMESTAMP || route == Route.NATIVE_TIME) && Math.abs( offset.getTotalSeconds() ) >= 16 * 3600 ) {
			throw new TemporalValueException( "PostgreSQL native precision requires an offset between -15:59:59 and +15:59:59" );
		}
		if ( route == Route.UTC_TIME && !ZoneOffset.UTC.equals( offset ) ) {
			throw new TemporalValueException( "PostgreSQL UTC time precision requires a UTC-adjusted carrier" );
		}
		if ( value instanceof OffsetDateTime || value instanceof Instant ) {
			final var instant = value instanceof Instant timestamp ? timestamp : ((OffsetDateTime) value).toInstant();
			final LocalDate date;
			if ( route == Route.CALENDAR_TIMESTAMP || route == Route.INSTANT_TIMESTAMP ) {
				// Match the existing Timestamp carrier's encoded calendar, including its cutover.
				final var calendar = new GregorianCalendar( TimeZone.getTimeZone( "UTC" ) );
				calendar.setTimeInMillis( Math.addExact( Math.multiplyExact( instant.getEpochSecond(), 1000 ), instant.getNano() / 1_000_000 ) );
				final int year = calendar.get( Calendar.ERA ) == GregorianCalendar.BC ? 1 - calendar.get( Calendar.YEAR ) : calendar.get( Calendar.YEAR );
				date = LocalDate.of( year, calendar.get( Calendar.MONTH ) + 1, calendar.get( Calendar.DAY_OF_MONTH ) );
			}
			else {
				date = instant.atOffset( ZoneOffset.UTC ).toLocalDate();
			}
			if ( date.isBefore( MIN ) || date.isAfter( MAX ) ) {
				throw new TemporalValueException( "PostgreSQL timestamp precision requires finite encoded dates -4712-01-01..294276-12-31" );
			}
		}
	}
}
