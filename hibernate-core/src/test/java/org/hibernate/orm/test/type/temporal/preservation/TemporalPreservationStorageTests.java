package org.hibernate.orm.test.type.temporal.preservation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.TemporalPreservationException;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.cfg.QuerySettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.type.internal.StandardTemporalPreservations;
import org.hibernate.orm.test.type.temporal.preservation.TemporalPreservationBootMatrix.WithInstant;
import org.hibernate.orm.test.type.temporal.preservation.TemporalPreservationBootMatrix.WithLocalDate;
import org.hibernate.orm.test.type.temporal.preservation.TemporalPreservationBootMatrix.WithLocalDateTime;
import org.hibernate.orm.test.type.temporal.preservation.TemporalPreservationBootMatrix.WithLocalTime;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Verifies against a live database that, with temporal preservation enabled,
/// what is stored for a Java Time value is correct according to the database
/// itself, and that values outside the supported range are rejected.
///
/// The database's own view is taken with SQL written by hand in this test:
/// a literal in the database's own syntax which the stored value must equal,
/// a row inserted with such a literal which must read back as the Java value,
/// and for instants the number of seconds since the epoch as computed by the
/// database. None of that SQL passes through Hibernate's handling of Java
/// Time values.
///
/// @author Steve Ebersole
class TemporalPreservationStorageTests {
	private static final AtomicInteger IDS = new AtomicInteger();
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern( "uuuu-MM-dd" );
	private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern( "uuuu-MM-dd HH:mm:ss.SSSSSS" );
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern( "HH:mm:ss" );

	@Nested
	@ServiceRegistry(settings = {
			@Setting(name = MappingSettings.TEMPORAL_PRESERVATION, value = "true"),
			@Setting(name = QuerySettings.CRITERIA_VALUE_HANDLING_MODE, value = "inline")
	})
	@RequiresDialect(H2Dialect.class)
	@RequiresDialect(value = PostgreSQLDialect.class, matchSubTypes = false)
	@RequiresDialect(MySQLDialect.class)
	@RequiresDialect(OracleDialect.class)
	@DomainModel(annotatedClasses = { WithLocalDate.class, WithLocalDateTime.class, WithInstant.class })
	@SessionFactory
	class DatesAndInstants {
		@AfterEach
		void cleanup(SessionFactoryScope scope) {
			scope.dropData();
		}

		@Test
		void localDate(SessionFactoryScope scope) {
			final LocalDate first = firstDay( scope );
			for ( var value : new LocalDate[] {
					first,
					LocalDate.of( 1582, 10, 15 ),
					LocalDate.of( 1900, 1, 1 ),
					LocalDate.of( 1970, 1, 1 ),
					LocalDate.of( 2021, 3, 14 ),
					LocalDate.of( 2024, 2, 29 ),
					LocalDate.of( 9999, 12, 31 )
			} ) {
				assertStoredAsWritten( scope, WithLocalDate.class, "tp_local_date", value, dateLiteral( value ) );
			}
			if ( first.isBefore( StandardTemporalPreservations.GREGORIAN_START ) ) {
				// dates which exist only in the proleptic Gregorian calendar,
				// or which differ between it and the Julian calendar
				for ( var value : new LocalDate[] {
						LocalDate.of( 1582, 10, 10 ),
						LocalDate.of( 1500, 3, 1 ),
						LocalDate.of( 1500, 2, 28 ),
						LocalDate.of( 1000, 6, 15 )
				} ) {
					assertStoredAsWritten( scope, WithLocalDate.class, "tp_local_date", value, dateLiteral( value ) );
				}
			}
		}

		@Test
		void localDateOutsideRangeIsRejected(SessionFactoryScope scope) {
			final LocalDate first = firstDay( scope );
			assertRejected( scope, WithLocalDate.class, first.minusDays( 1 ) );
			assertRejected( scope, WithLocalDate.class, LocalDate.of( 10000, 1, 1 ) );
			assertRejected( scope, WithLocalDate.class, LocalDate.of( -1, 6, 15 ) );
			assertReadRejected( scope, "tp_local_date", outsideRangeDateLiteral( scope ) );
		}

		@Test
		void localDateTime(SessionFactoryScope scope) {
			final LocalDate first = firstDay( scope );
			for ( var value : new LocalDateTime[] {
					first.atStartOfDay(),
					LocalDateTime.of( 1970, 1, 1, 0, 0, 0 ),
					LocalDateTime.of( 2021, 3, 14, 2, 30, 0, 123_456_000 ),
					LocalDateTime.of( 2021, 11, 7, 1, 30, 0 ),
					LocalDateTime.of( 9999, 12, 31, 23, 59, 59, 999_999_000 )
			} ) {
				assertStoredAsWritten(
						scope,
						WithLocalDateTime.class,
						"tp_local_date_time",
						value,
						"timestamp '" + DATE_TIME.format( value ) + "'"
				);
			}
		}

		/// 02:30 on 2021-03-14 does not exist in New York, and 01:30 on
		/// 2021-11-07 occurs twice. Neither may be changed by the default
		/// time zone of the JVM.
		@Test
		void localDateTimeIgnoresTheDefaultTimeZone(SessionFactoryScope scope) {
			final TimeZone original = TimeZone.getDefault();
			TimeZone.setDefault( TimeZone.getTimeZone( "America/New_York" ) );
			try {
				for ( var value : new LocalDateTime[] {
						LocalDateTime.of( 2021, 3, 14, 2, 30, 0 ),
						LocalDateTime.of( 2021, 11, 7, 1, 30, 0 )
				} ) {
					assertStoredAsWritten(
							scope,
							WithLocalDateTime.class,
							"tp_local_date_time",
							value,
							"timestamp '" + DATE_TIME.format( value ) + "'"
					);
				}
				final LocalDate date = LocalDate.of( 2021, 3, 14 );
				assertStoredAsWritten( scope, WithLocalDate.class, "tp_local_date", date, dateLiteral( date ) );
			}
			finally {
				TimeZone.setDefault( original );
			}
		}

		@Test
		void localDateTimeOutsideRangeIsRejected(SessionFactoryScope scope) {
			final LocalDate first = firstDay( scope );
			assertRejected( scope, WithLocalDateTime.class, first.atStartOfDay().minusSeconds( 1 ) );
			assertRejected( scope, WithLocalDateTime.class, LocalDateTime.of( 10000, 1, 1, 0, 0 ) );
			assertReadRejected(
					scope,
					"tp_local_date_time",
					"cast(" + outsideRangeDateLiteral( scope ) + " as "
						+ ( dialect( scope ) instanceof MySQLDialect ? "datetime" : "timestamp" ) + ")"
			);
		}

		@Test
		void instant(SessionFactoryScope scope) {
			for ( var value : new Instant[] {
					firstInstant( scope ),
					Instant.parse( "1583-01-01T00:00:00Z" ),
					Instant.parse( "1900-01-01T00:00:00Z" ),
					Instant.EPOCH,
					Instant.parse( "2021-03-14T07:30:00.123456Z" ),
					Instant.parse( "9999-12-31T23:59:59.999999Z" )
			} ) {
				assertStoredAsWritten( scope, WithInstant.class, "tp_instant", value, instantLiteral( scope, value ) );
				assertThat( epochSecondOfLastRow( scope ) ).isEqualTo( value.getEpochSecond() );
			}
		}

		@Test
		void instantIgnoresTheDefaultTimeZone(SessionFactoryScope scope) {
			final TimeZone original = TimeZone.getDefault();
			TimeZone.setDefault( TimeZone.getTimeZone( "America/New_York" ) );
			try {
				for ( var value : new Instant[] {
						Instant.parse( "2021-03-14T07:30:00Z" ),
						Instant.parse( "2021-03-14T02:30:00Z" ),
						Instant.parse( "2021-11-07T05:30:00Z" ),
						Instant.parse( "2021-11-07T06:30:00Z" )
				} ) {
					assertStoredAsWritten(
							scope,
							WithInstant.class,
							"tp_instant",
							value,
							instantLiteral( scope, value )
					);
					assertThat( epochSecondOfLastRow( scope ) ).isEqualTo( value.getEpochSecond() );
				}
			}
			finally {
				TimeZone.setDefault( original );
			}
		}

		@Test
		void instantOutsideRangeIsRejected(SessionFactoryScope scope) {
			final Instant first = firstInstant( scope );
			assertRejected( scope, WithInstant.class, first.minusSeconds( 1 ) );
			assertRejected( scope, WithInstant.class, Instant.parse( "+10000-01-01T00:00:00Z" ) );
			assertRejected( scope, WithInstant.class, Instant.parse( "-0001-06-15T00:00:00Z" ) );
		}

		/// Rows from before the first supported instant, as another client or
		/// an earlier configuration could have written them, are rejected and
		/// never returned as a different instant.
		@Test
		void instantBeforeRangeIsRejectedOnRead(SessionFactoryScope scope) {
			final Instant first = firstInstant( scope );
			final long day = 86_400;
			// The ten days before 1582-10-15 do not exist in Oracle's calendar,
			// and Oracle itself stores such a literal as 1582-10-15.
			final long[] secondsEarlier = dialect( scope ) instanceof OracleDialect
					? new long[] { 11 * day, 12 * day, 400 * day, 30_000 * day }
					: new long[] { 1, day, 5 * day, 10 * day, 11 * day, 20 * day, 400 * day, 30_000 * day };
			for ( long seconds : secondsEarlier ) {
				assertReadRejected( scope, "tp_instant", instantLiteral( scope, first.minusSeconds( seconds ) ) );
			}
		}
	}

	@Nested
	@ServiceRegistry(settings = {
			@Setting(name = MappingSettings.TEMPORAL_PRESERVATION, value = "true"),
			@Setting(name = QuerySettings.CRITERIA_VALUE_HANDLING_MODE, value = "inline")
	})
	@RequiresDialect(H2Dialect.class)
	@RequiresDialect(value = PostgreSQLDialect.class, matchSubTypes = false)
	@RequiresDialect(MySQLDialect.class)
	@DomainModel(annotatedClasses = WithLocalTime.class)
	@SessionFactory
	class Times {
		@AfterEach
		void cleanup(SessionFactoryScope scope) {
			scope.dropData();
		}

		@Test
		void localTime(SessionFactoryScope scope) {
			final TimeZone original = TimeZone.getDefault();
			TimeZone.setDefault( TimeZone.getTimeZone( "America/New_York" ) );
			try {
				for ( var value : new LocalTime[] {
						LocalTime.MIDNIGHT,
						LocalTime.of( 2, 30, 15 ),
						LocalTime.NOON,
						LocalTime.of( 23, 59, 59 )
				} ) {
					assertStoredAsWritten(
							scope,
							WithLocalTime.class,
							"tp_local_time",
							value,
							"time '" + TIME.format( value ) + "'"
					);
				}
			}
			finally {
				TimeZone.setDefault( original );
			}
		}
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Assertions

	/// @param literal the value as a literal in the database's own syntax
	private static <T> void assertStoredAsWritten(
			SessionFactoryScope scope,
			Class<?> entityClass,
			String table,
			T value,
			String literal) {
		final int id = IDS.incrementAndGet();
		final String entityName = entityClass.getSimpleName();

		// written through a bound parameter
		scope.inTransaction( session -> session.persist( newEntity( entityClass, id, value ) ) );

		scope.inTransaction( session -> {
			// the database agrees that the stored value is the one written
			assertThat( count( session.createNativeQuery(
					"select count(*) from " + table + " where id = " + id + " and theValue = " + literal,
					Object.class
			).getSingleResult() ) ).as( "%s stored for %s", literal, value ).isEqualTo( 1 );

			// read back
			assertThat( session.createQuery(
					"select e.theValue from " + entityName + " e where e.id = :id",
					Object.class
			).setParameter( "id", id ).getSingleResult() ).isEqualTo( value );

			// compared to a bound parameter
			assertThat( session.createQuery(
					"select count(e) from " + entityName + " e where e.id = :id and e.theValue = :theValue",
					Long.class
			).setParameter( "id", id ).setParameter( "theValue", value ).getSingleResult() ).isEqualTo( 1L );

			// compared to a literal rendered by Hibernate
			final var builder = session.getCriteriaBuilder();
			final var criteria = builder.createQuery( Long.class );
			final var root = criteria.from( entityClass );
			criteria.select( builder.count( root ) ).where(
					builder.equal( root.get( "id" ), id ),
					builder.equal( root.get( "theValue" ), builder.literal( value ) )
			);
			assertThat( session.createQuery( criteria ).getSingleResult() )
					.as( "literal rendered for %s", value )
					.isEqualTo( 1L );
		} );

		// written by the database itself and read through Hibernate
		final int seededId = IDS.incrementAndGet();
		scope.inTransaction( session -> session.createNativeMutationQuery(
				"insert into " + table + " (id, theValue) values (" + seededId + ", " + literal + ")"
		).executeUpdate() );
		scope.inTransaction( session -> assertThat( session.createQuery(
				"select e.theValue from " + entityName + " e where e.id = :id",
				Object.class
		).setParameter( "id", seededId ).getSingleResult() ).as( "read of %s", literal ).isEqualTo( value ) );

		lastId = id;
	}

	private static int lastId;

	private static void assertRejected(SessionFactoryScope scope, Class<?> entityClass, Object value) {
		final int id = IDS.incrementAndGet();
		final String entityName = entityClass.getSimpleName();

		// bound for an insert
		assertThatThrownBy( () -> scope.inTransaction(
				session -> session.persist( newEntity( entityClass, id, value ) )
		) ).as( "insert of %s", value ).isInstanceOf( TemporalPreservationException.class );

		// bound as a query parameter
		assertThatThrownBy( () -> scope.inTransaction( session -> session.createQuery(
				"select count(e) from " + entityName + " e where e.theValue = :theValue",
				Long.class
		).setParameter( "theValue", value ).getSingleResult() ) )
				.as( "parameter %s", value )
				.isInstanceOf( TemporalPreservationException.class );

		// rendered as a literal
		assertThatThrownBy( () -> scope.inTransaction( session -> {
			final var builder = session.getCriteriaBuilder();
			final var criteria = builder.createQuery( Long.class );
			final var root = criteria.from( entityClass );
			criteria.select( builder.count( root ) )
					.where( builder.equal( root.get( "theValue" ), builder.literal( value ) ) );
			session.createQuery( criteria ).getSingleResult();
		} ) ).as( "literal %s", value ).isInstanceOf( TemporalPreservationException.class );

		scope.inTransaction( session -> assertThat( count( session.createNativeQuery(
				"select count(*) from " + tableOf( entityClass ) + " where id = " + id,
				Object.class
		).getSingleResult() ) ).as( "rows written for %s", value ).isZero() );
	}

	private static String tableOf(Class<?> entityClass) {
		return entityClass.getAnnotation( jakarta.persistence.Table.class ).name();
	}

	private static void assertReadRejected(SessionFactoryScope scope, String table, String literal) {
		final int id = IDS.incrementAndGet();
		final String entityName = switch ( table ) {
			case "tp_local_date" -> "WithLocalDate";
			case "tp_local_date_time" -> "WithLocalDateTime";
			case "tp_instant" -> "WithInstant";
			default -> throw new IllegalArgumentException( table );
		};
		scope.inTransaction( session -> session.createNativeMutationQuery(
				"insert into " + table + " (id, theValue) values (" + id + ", " + literal + ")"
		).executeUpdate() );
		assertThatThrownBy( () -> scope.inTransaction( session -> {
			final Object read = session.createQuery(
					"select e.theValue from " + entityName + " e where e.id = :id",
					Object.class
			).setParameter( "id", id ).getSingleResult();
			throw new AssertionError( "Read " + read + " for " + literal );
		} ) ).as( "read of %s", literal ).isInstanceOf( TemporalPreservationException.class );
	}

	private static int count(Object result) {
		return ( (Number) result ).intValue();
	}

	private static long epochSecondOfLastRow(SessionFactoryScope scope) {
		final String expression;
		if ( dialect( scope ) instanceof OracleDialect ) {
			expression = "round((cast(sys_extract_utc(theValue) as date) - date '1970-01-01') * 86400)";
		}
		else if ( dialect( scope ) instanceof MySQLDialect ) {
			// the column holds the date and time at UTC
			expression = "timestampdiff(second, timestamp '1970-01-01 00:00:00', theValue)"
					+ " - (case when theValue < timestamp '1970-01-01 00:00:00' and microsecond(theValue) <> 0 then 1 else 0 end)";
		}
		else {
			expression = "extract(epoch from theValue)";
		}
		return scope.fromTransaction( session -> new BigDecimal( session.createNativeQuery(
				"select " + expression + " from tp_instant where id = " + lastId,
				Object.class
		).getSingleResult().toString() ).setScale( 0, RoundingMode.FLOOR ).longValueExact() );
	}

	private static Object newEntity(Class<?> entityClass, int id, Object value) {
		if ( entityClass == WithLocalDate.class ) {
			final var entity = new WithLocalDate();
			entity.id = id;
			entity.theValue = (LocalDate) value;
			return entity;
		}
		else if ( entityClass == WithLocalTime.class ) {
			final var entity = new WithLocalTime();
			entity.id = id;
			entity.theValue = (LocalTime) value;
			return entity;
		}
		else if ( entityClass == WithLocalDateTime.class ) {
			final var entity = new WithLocalDateTime();
			entity.id = id;
			entity.theValue = (LocalDateTime) value;
			return entity;
		}
		else {
			final var entity = new WithInstant();
			entity.id = id;
			entity.theValue = (Instant) value;
			return entity;
		}
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// The database's own syntax

	private static Dialect dialect(SessionFactoryScope scope) {
		return scope.getSessionFactory().getJdbcServices().getDialect();
	}

	private static LocalDate firstDay(SessionFactoryScope scope) {
		if ( dialect( scope ) instanceof OracleDialect ) {
			return StandardTemporalPreservations.GREGORIAN_START;
		}
		else if ( dialect( scope ) instanceof MySQLDialect ) {
			return StandardTemporalPreservations.MYSQL_SUPPORTED_START;
		}
		else {
			return StandardTemporalPreservations.COMMON_ERA_START;
		}
	}

	private static Instant firstInstant(SessionFactoryScope scope) {
		final LocalDate firstDay;
		if ( dialect( scope ) instanceof H2Dialect ) {
			firstDay = StandardTemporalPreservations.COMMON_ERA_START;
		}
		else if ( dialect( scope ) instanceof PostgreSQLDialect || dialect( scope ) instanceof MySQLDialect ) {
			firstDay = StandardTemporalPreservations.TIMESTAMP_READ_SAFE_START;
		}
		else {
			firstDay = StandardTemporalPreservations.GREGORIAN_START;
		}
		return firstDay.atStartOfDay().toInstant( ZoneOffset.UTC );
	}

	private static String dateLiteral(LocalDate value) {
		return "date '" + DATE.format( value ) + "'";
	}

	/// A date outside the supported range which the database can store.
	private static String outsideRangeDateLiteral(SessionFactoryScope scope) {
		if ( dialect( scope ) instanceof OracleDialect ) {
			return "date '1582-10-04'";
		}
		else if ( dialect( scope ) instanceof MySQLDialect ) {
			// MySQL and MariaDB do not store a date after the year 9999, and do store one before the year 1000
			return "date '0999-12-31'";
		}
		else {
			return "date '10000-01-01'";
		}
	}

	private static String instantLiteral(SessionFactoryScope scope, Instant value) {
		final String fields = DATE_TIME.format( value.atOffset( ZoneOffset.UTC ) );
		if ( dialect( scope ) instanceof OracleDialect ) {
			return "timestamp '" + fields + " +00:00'";
		}
		else if ( dialect( scope ) instanceof MySQLDialect ) {
			// the column holds the date and time at UTC
			return "timestamp '" + fields + "'";
		}
		else {
			return "timestamp with time zone '" + fields + "+00:00'";
		}
	}
}
