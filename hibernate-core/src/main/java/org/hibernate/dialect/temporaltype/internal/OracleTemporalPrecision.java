package org.hibernate.dialect.temporaltype.internal;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;

import jakarta.persistence.ParameterMode;

import org.hibernate.TemporalValueException;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.temporaltype.spi.TemporalCallablePrecisionContext;
import org.hibernate.dialect.temporaltype.spi.TemporalCallablePrecisionContext.Access;
import org.hibernate.dialect.temporaltype.spi.TemporalCallablePrecisionContext.Boundary;
import org.hibernate.dialect.temporaltype.spi.TemporalPrecisionContext;
import org.hibernate.dialect.temporaltype.spi.TemporalPrecisionSupport;
import org.hibernate.dialect.temporaltype.spi.TemporalValueSemantics.PrecisionAdjustment;
import org.hibernate.type.descriptor.java.InstantJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.OffsetDateTimeJavaType;
import org.hibernate.type.descriptor.java.OffsetTimeJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.TimeUtcAsOffsetTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampUtcAsPlainJdbcTimestampJdbcType;
import org.hibernate.type.descriptor.jdbc.internal.UtcTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.internal.UtcTimestampJdbcType;

/// Oracle 23.26 / JDBC 23.26 precision facts, verified with 23.26.1 / 23.26.4.
/// Column assignment rounds directly from nanoseconds. Indexed callable transport
/// carries nanoseconds independently of the precision imposed by a SQL routine.
/// The time-only route uses Oracle TIMESTAMP WITH TIME ZONE as its physical encoding.
///
/// @author Steve Ebersole
final class OracleTemporalPrecision {
	private static final Pattern TYPE = Pattern.compile( "timestamp(?:\\((\\d+)\\))?( with time zone)?" );

	private OracleTemporalPrecision() {}

	private enum Route { ADJUSTED_TIMESTAMP, UTC_TIME, INSTANT_TIMESTAMP }

	private static boolean admitted(Dialect dialect) {
		return dialect.getClass() == OracleDialect.class
				&& dialect.getVersion().getMajor() == 23 && dialect.getVersion().getMinor() == 26
				&& ((OracleDialect) dialect).getDriverMajorVersion() == 23
				&& ((OracleDialect) dialect).getDriverMinorVersion() == 26;
	}

	private static Route route(JavaType<?> java, JdbcType jdbc, ZoneId adjustment) {
		if ( java.getClass() == InstantJavaType.class && jdbc.getClass() == TimestampUtcAsPlainJdbcTimestampJdbcType.class ) {
			return Route.INSTANT_TIMESTAMP;
		}
		if ( adjustment != null ) {
			if ( java.getClass() == OffsetDateTimeJavaType.class && jdbc.getClass() == UtcTimestampJdbcType.class
					&& ((UtcTimestampJdbcType) jdbc).getDelegate().getClass() == TimestampUtcAsPlainJdbcTimestampJdbcType.class ) {
				return Route.ADJUSTED_TIMESTAMP;
			}
			if ( java.getClass() == OffsetTimeJavaType.class && jdbc.getClass() == UtcTimeJdbcType.class
					&& ((UtcTimeJdbcType) jdbc).getDelegate().getClass() == TimeUtcAsOffsetTimeJdbcType.class ) {
				return Route.UTC_TIME;
			}
		}
		return null;
	}

	static TemporalPrecisionSupport resolve(TemporalPrecisionContext context) {
		if ( !admitted( context.dialect() ) ) {
			return TemporalPrecisionSupport.unsupported( "Oracle precision requires configured Oracle 23.26 and JDBC driver 23.26" );
		}
		if ( context.operation() != TemporalPrecisionContext.Operation.ATTRIBUTE ) {
			return TemporalPrecisionSupport.unsupported( "Unassessed Oracle precision operation " + context.operation() );
		}
		final var route = route( context.javaType(), context.jdbcType(), context.adjustment() );
		if ( route == null ) {
			return TemporalPrecisionSupport.unsupported( "Unassessed Oracle Java/JDBC precision pair" );
		}
		final var definition = TYPE.matcher( context.ddlTypeName().toLowerCase( Locale.ROOT ).trim().replaceAll( "\\s+", " " ) );
		if ( !definition.matches() || (route == Route.UTC_TIME) != (definition.group( 2 ) != null)
				|| context.ddlTypeCode() != context.jdbcType().getDdlTypeCode() ) {
			return TemporalPrecisionSupport.unsupported( "Unresolved or contradictory physical Oracle temporal definition" );
		}
		final int physical = definition.group( 1 ) == null ? 6 : Integer.parseInt( definition.group( 1 ) );
		final int precision = context.declaredPrecision() == null ? physical : context.declaredPrecision();
		if ( physical > 9 || precision < 0 || precision > physical ) {
			return TemporalPrecisionSupport.unsupported( "Oracle precision must be within 0..9 and not exceed physical precision " + physical );
		}
		return support( route, precision, "attribute" );
	}

	static TemporalPrecisionSupport resolveCallable(TemporalCallablePrecisionContext context) {
		if ( !admitted( context.dialect() ) ) {
			return TemporalPrecisionSupport.unsupported( "Oracle callable precision requires configured Oracle 23.26 and JDBC driver 23.26" );
		}
		final var route = route( context.type().getJdbcJavaType(), context.type().getJdbcType(), context.adjustment() );
		final boolean input = context.boundary() == Boundary.INPUT && context.valueAccess() == Access.INDEXED
				&& context.registrationAccess() == Access.NONE;
		final boolean output = context.boundary() == Boundary.OUTPUT && context.valueAccess() == Access.INDEXED
				&& context.registrationAccess() == Access.INDEXED;
		if ( context.jdbcTypeCode() == context.type().getJdbcType().getJdbcTypeCode()
				&& (route == Route.ADJUSTED_TIMESTAMP && (input && context.mode() == ParameterMode.IN || output && context.functionReturn())
				|| route == Route.UTC_TIME && !context.functionReturn() && (input || output)) ) {
			return support( route, 9, "callable:" + context.mode() + ':' + context.boundary() + ':' + context.valueAccess() );
		}
		return TemporalPrecisionSupport.unsupported( "Unassessed Oracle callable precision mode/access/pair" );
	}

	private static TemporalPrecisionSupport support(Route route, int precision, String operation) {
		final var quantizer = new DecimalTemporalPrecision( precision, 9, PrecisionAdjustment.ROUND, true );
		return TemporalPrecisionSupport.supported( "oracle23.26-jdbc23.26-v1:" + route + ':' + operation
				+ ":utc-calendar-finite-4711..9999:" + quantizer.signature(), value -> {
			check( value, route );
			final var result = quantizer.apply( value );
			check( result, route );
			return result;
		}, value -> value instanceof OffsetDateTime timestamp ? timestamp.withOffsetSameInstant( ZoneOffset.UTC ) : value );
	}

	private static void check(Object value, Route route) {
		if ( route == Route.UTC_TIME ) {
			if ( !ZoneOffset.UTC.equals( ((OffsetTime) value).getOffset() ) ) {
				throw new TemporalValueException( "Oracle time precision requires a UTC-adjusted carrier" );
			}
			return;
		}
		final var instant = value instanceof Instant timestamp ? timestamp : ((OffsetDateTime) value).toInstant();
		// Preserve the existing plain Timestamp carrier's GregorianCalendar encoding,
		// including BC years and the historical cutover; do not reinterpret old data.
		final var calendar = new GregorianCalendar( TimeZone.getTimeZone( "UTC" ) );
		calendar.setTimeInMillis( Math.addExact( Math.multiplyExact( instant.getEpochSecond(), 1000 ), instant.getNano() / 1_000_000 ) );
		final int year = calendar.get( Calendar.ERA ) == GregorianCalendar.BC ? 1 - calendar.get( Calendar.YEAR ) : calendar.get( Calendar.YEAR );
		if ( year < -4711 || year > 9999 ) {
			throw new TemporalValueException( "Oracle timestamp precision requires finite encoded dates -4711-01-01..9999-12-31" );
		}
	}
}
