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
import org.hibernate.dialect.PostgresPlusDialect;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;

/// Guards the finite date and timestamp domains of the built-in temporal JDBC handling.
/// Checks run on binding and extraction so an unsupported input or a driver-produced
/// infinity sentinel fails with [TemporalValueException] instead of silently changing
/// the value. They neither round fractional seconds nor perform timezone adjustment,
/// and do not replace the Java/JDBC pair's preservation assessment.
///
/// Activation follows the factory's effective direct-Java-Time handling flag, which
/// is also controlled by temporal preservation. Legacy explicit conversion and calls
/// without the necessary factory context bypass these guards. Bounds depend only on
/// the configured [Dialect], never live JDBC metadata or an ambient timezone:
///
/// - Oracle has its own finite date/timestamp interval.
/// - PostgreSQL has distinct DATE and TIMESTAMP upper bounds.
/// - EDB uses the common DATE/TIMESTAMP interval because its natural DATE may be
///   timestamp-backed; a wider explicit column does not expand this handler contract.
/// - H2 uses the Java date domain, subject to the selected JDBC carrier's limits.
///
/// Other dialects have no provider-specific date bounds here; that absence is not a
/// declaration of support. SQL infinities remain outside the accepted finite domain
/// even when a driver maps Java extremes to sentinels with equal round-trip values.
/// Direct Java Time values use proleptic date fields, while timestamp fallback paths
/// additionally account for millisecond overflow and historical calendar encoding.
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

	/// Recognizes SQL date, time and timestamp codes, including Hibernate's UTC codes.
	/// This classifies a SQL carrier; it does not assert that a provider supports it or
	/// that every associated value is range-checked by [#check(Object, WrapperOptions)].
	///
	/// @param code the SQL type code to classify
	/// @return whether the code denotes one of the temporal SQL carriers handled here
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

	/// Determines whether these guards are active for the current operation.
	///
	/// @param options the JDBC operation context, possibly null
	/// @return true when a factory enables direct Java Time handling and a dialect is available
	public static boolean isNewHandling(WrapperOptions options) { return dialect( options ) != null; }

	/// Checks the finite domain of a Java Time value before binding or after extraction.
	/// Local dates use DATE bounds, local date-times use TIMESTAMP bounds, and instants
	/// are checked as UTC dates. PostgreSQL-family offset/zoned values are checked by
	/// instant; other dialects use their local date fields. This interpretation is solely
	/// for checking bounds and does not modify the value.
	///
	/// @param value the value to check; null, time-only values, and other Java types are ignored
	/// @param options the operation context; checks are bypassed when [#isNewHandling(WrapperOptions)] is false
	/// @throws TemporalValueException if the value exceeds the applicable finite domain
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
		// EDB's natural DATE may be timestamp-backed (edb_redwood_date). Use
		// the common finite domain without querying metadata or session settings.
		else if ( dialect instanceof PostgresPlusDialect ) { min = POSTGRESQL_MIN; max = POSTGRESQL_TIMESTAMP_MAX; }
		else if ( dialect instanceof PostgreSQLDialect ) { min = POSTGRESQL_MIN; max = dateOnly ? POSTGRESQL_DATE_MAX : POSTGRESQL_TIMESTAMP_MAX; }
		else if ( dialect instanceof H2Dialect ) { min = LocalDate.MIN; max = LocalDate.MAX; }
		else return;
		if ( date.isBefore( min ) || date.isAfter( max ) ) {
			throw new TemporalValueException( dialect.getClass().getSimpleName() + " temporal value " + value
					+ " exceeds the supported finite " + (dateOnly ? "DATE" : "TIMESTAMP") + " range " + min + ".." + max );
		}
	}

	/// Checks an instant destined for a millisecond-based JDBC timestamp carrier.
	/// In addition to detecting overflow, Oracle and PostgreSQL-family paths check the
	/// fields encoded by a UTC [GregorianCalendar], including its historical cutover.
	/// These may differ from the proleptic fields checked by [#check(Object, WrapperOptions)]. No default JVM
	/// timezone or database metadata is consulted.
	///
	/// @param instant the instant represented by the JDBC timestamp, or null
	/// @param options the operation context; checks are bypassed when [#isNewHandling(WrapperOptions)] is false
	/// @throws TemporalValueException if millisecond conversion overflows or the encoded
	/// timestamp fields exceed the provider's finite domain
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

	/// Decorates an instant-based timestamp binder with [#checkTimestamp(Instant, WrapperOptions)] validation.
	/// When guards are active, validation precedes indexed/named binding and bind-value
	/// conversion. The delegate retains responsibility for the actual encoding and nulls.
	///
	/// @param javaType descriptor capable of unwrapping non-null values to [Instant]
	/// @param delegate the binder implementing the timestamp carrier operations
	/// @param <T> the Java value type accepted by the binder
	/// @return a validating wrapper without modifying the delegate
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

	/// Decorates an instant-based timestamp extractor with [#checkTimestamp(Instant, WrapperOptions)] validation.
	/// The delegate extracts first; active guards then validate each non-null result
	/// before it reaches the caller. Result-set and indexed/named callable extraction
	/// share the same check. Nulls and compatibility-mode results pass through unchanged.
	///
	/// @param javaType descriptor capable of unwrapping extracted values to [Instant]
	/// @param delegate the extractor implementing the timestamp carrier operations
	/// @param <T> the extracted Java value type
	/// @return a validating wrapper without modifying the delegate
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
