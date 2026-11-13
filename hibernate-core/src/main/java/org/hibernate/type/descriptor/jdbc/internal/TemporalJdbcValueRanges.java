package org.hibernate.type.descriptor.jdbc.internal;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.*;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.hibernate.TemporalValueException;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;

/// Finite SQL and JDBC carrier boundaries for new direct-enabled handling.
/// These checks use the configured Dialect, never live JDBC metadata or an ambient
/// timezone. Legacy explicit conversion retains its established behavior.
///
/// SQL infinities are outside this delivery's accepted domain, even when a driver
/// can map a Java extreme value to an infinity sentinel without changing equality.
///
/// @author Steve Ebersole
public final class TemporalJdbcValueRanges {
	private static final LocalDate ORACLE_MIN = LocalDate.of( -4711, 1, 1 );
	private static final LocalDate ORACLE_MAX = LocalDate.of( 9999, 12, 31 );
	// pgJDBC's typed finite lower bound is 4713 BCE in ISO era numbering.
	private static final LocalDate POSTGRESQL_MIN = LocalDate.of( -4712, 1, 1 );
	private static final LocalDate POSTGRESQL_DATE_MAX = LocalDate.of( 5874897, 12, 31 );
	private static final LocalDate POSTGRESQL_TIMESTAMP_MAX = LocalDate.of( 294276, 12, 31 );

	private TemporalJdbcValueRanges() {}

	public static boolean isTemporalSqlType(int code) {
		return code == java.sql.Types.TIME || code == java.sql.Types.TIME_WITH_TIMEZONE || code == org.hibernate.type.SqlTypes.TIME_UTC
				|| code == java.sql.Types.DATE || code == java.sql.Types.TIMESTAMP
				|| code == java.sql.Types.TIMESTAMP_WITH_TIMEZONE || code == org.hibernate.type.SqlTypes.TIMESTAMP_UTC;
	}

	private static Dialect dialect(WrapperOptions options) {
		if ( options == null ) return null;
		final var factory = options.getSessionFactory();
		if ( factory == null || factory.getSessionFactoryOptions() == null
				|| !factory.getSessionFactoryOptions().isPreferJavaTimeJdbcTypesEnabled() ) return null;
		return options.getDialect();
	}

	public static boolean isNewHandling(WrapperOptions options) { return dialect( options ) != null; }

	public static void check(Object value, WrapperOptions options) {
		if ( !(value instanceof LocalDate || value instanceof LocalDateTime || value instanceof OffsetDateTime
				|| value instanceof ZonedDateTime || value instanceof Instant) ) return;
		final var dialect = dialect( options );
		if ( dialect == null ) return;
		if ( value instanceof LocalDate date ) checkDate( date, true, dialect, value );
		else if ( value instanceof LocalDateTime timestamp ) checkDate( timestamp.toLocalDate(), false, dialect, value );
		else if ( value instanceof OffsetDateTime timestamp ) {
			if ( dialect instanceof PostgreSQLDialect ) checkInstant( timestamp.toInstant(), dialect );
			else checkDate( timestamp.toLocalDate(), false, dialect, value );
		}
		else if ( value instanceof ZonedDateTime timestamp ) {
			if ( dialect instanceof PostgreSQLDialect ) checkInstant( timestamp.toInstant(), dialect );
			else checkDate( timestamp.toLocalDate(), false, dialect, value );
		}
		else checkInstant( (Instant) value, dialect );
	}

	private static void checkInstant(Instant instant, Dialect dialect) {
		try { checkDate( instant.atOffset( ZoneOffset.UTC ).toLocalDate(), false, dialect, instant ); }
		catch (DateTimeException ex) { throw new TemporalValueException( "Instant " + instant + " exceeds the finite Java Time SQL timestamp carrier range" ); }
	}

	private static void checkDate(LocalDate date, boolean dateOnly, Dialect dialect, Object value) {
		final LocalDate min, max;
		if ( dialect instanceof OracleDialect ) { min = ORACLE_MIN; max = ORACLE_MAX; }
		else if ( dialect instanceof PostgreSQLDialect ) { min = POSTGRESQL_MIN; max = dateOnly ? POSTGRESQL_DATE_MAX : POSTGRESQL_TIMESTAMP_MAX; }
		else if ( dialect instanceof H2Dialect ) { min = LocalDate.MIN; max = LocalDate.MAX; }
		else return;
		if ( date.isBefore( min ) || date.isAfter( max ) ) {
			throw new TemporalValueException( dialect.getClass().getSimpleName() + " temporal value " + value
					+ " exceeds the supported finite " + (dateOnly ? "DATE" : "TIMESTAMP") + " range " + min + ".." + max );
		}
	}

	public static void checkTimestamp(Instant instant, WrapperOptions options) {
		if ( instant == null ) return;
		final var dialect = dialect( options );
		if ( dialect == null ) return;
		final long millis;
		try { millis = Math.addExact( Math.multiplyExact( instant.getEpochSecond(), 1000 ), instant.getNano() / 1000000 ); }
		catch (ArithmeticException ex) { throw new TemporalValueException( "Instant " + instant + " exceeds java.sql.Timestamp's finite millisecond carrier range; use supported direct handling" ); }
		if ( dialect instanceof OracleDialect || dialect instanceof PostgreSQLDialect ) {
			// These drivers encode Timestamp's UTC Calendar fields, including its
			// historical calendar, rather than the proleptic java.time date fields.
			final var calendar = new GregorianCalendar( TimeZone.getTimeZone( "UTC" ) );
			calendar.setTimeInMillis( millis );
			final int year = calendar.get( Calendar.ERA ) == GregorianCalendar.BC ? 1 - calendar.get( Calendar.YEAR ) : calendar.get( Calendar.YEAR );
			checkDate( LocalDate.of( year, calendar.get( Calendar.MONTH ) + 1, calendar.get( Calendar.DAY_OF_MONTH ) ), false, dialect, instant );
		}
		else checkInstant( instant, dialect );
	}

	public static <T> ValueBinder<T> instantBinder(JavaType<T> javaType, ValueBinder<T> delegate) {
		return new ValueBinder<>() {
			private void check(T value, WrapperOptions options) {
				if ( value != null && isNewHandling( options ) ) checkTimestamp( javaType.unwrap( value, Instant.class, options ), options );
			}
			@Override public void bind(PreparedStatement statement, T value, int index, WrapperOptions options) throws SQLException {
				check( value, options ); delegate.bind( statement, value, index, options );
			}
			@Override public void bind(CallableStatement statement, T value, String name, WrapperOptions options) throws SQLException {
				check( value, options ); delegate.bind( statement, value, name, options );
			}
			@Override public Object getBindValue(T value, WrapperOptions options) throws SQLException {
				check( value, options ); return delegate.getBindValue( value, options );
			}
		};
	}

	public static <T> ValueExtractor<T> instantExtractor(JavaType<T> javaType, ValueExtractor<T> delegate) {
		return new ValueExtractor<>() {
			private T checked(T value, WrapperOptions options) {
				if ( value != null && isNewHandling( options ) ) checkTimestamp( javaType.unwrap( value, Instant.class, options ), options );
				return value;
			}
			@Override public T extract(ResultSet result, int index, WrapperOptions options) throws SQLException {
				return checked( delegate.extract( result, index, options ), options );
			}
			@Override public T extract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return checked( delegate.extract( statement, index, options ), options );
			}
			@Override public T extract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return checked( delegate.extract( statement, name, options ), options );
			}
		};
	}
}
