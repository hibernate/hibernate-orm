package org.hibernate.orm.test.type.temporal.preservation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.Session;
import org.hibernate.TemporalPreservationException;
import org.hibernate.annotations.TimeZoneColumn;
import org.hibernate.annotations.TimeZoneRepresentation;
import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.cfg.QuerySettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.orm.test.type.temporal.preservation.TemporalPreservationBootMatrix.WithOffsetDateTime;
import org.hibernate.orm.test.type.temporal.preservation.TemporalPreservationBootMatrix.WithZonedDateTime;
import org.hibernate.type.descriptor.java.OffsetDateTimeJavaType;

import org.hibernate.testing.orm.junit.DialectContext;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// Verifies against a live database what temporal preservation stores for an
/// `OffsetDateTime` and a `ZonedDateTime`, under each policy.
///
/// With the `NORMALIZED` policy the stored value must denote the same instant
/// as the Java value, by the database's own arithmetic and by comparison with
/// a literal written by hand, and must read back as that instant. With the
/// `VALUE` policy, where it is supported, the stored value must also
/// carry the same offset, and the value read back must equal the one written.
///
/// With a second column, the whole value must be preserved on every
/// database, including the zone of a `ZonedDateTime`.
///
/// As in [TemporalPreservationTimeZoneTests], the default time zone of the
/// JVM, the JDBC time zone of the session and the time zone of the database
/// session are varied, and must make no difference.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
@RequiresDialect(value = PostgreSQLDialect.class, matchSubTypes = false)
@RequiresDialect(MySQLDialect.class)
@RequiresDialect(OracleDialect.class)
class TemporalPreservationOffsetTests {
	private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern( "uuuu-MM-dd HH:mm:ss.SSSSSS" );
	private static final String[] DATABASE_SESSION_ZONES = { null, "Asia/Kathmandu", "America/New_York", "Asia/Manila" };
	private static final String[] SESSION_JDBC_ZONES = { null, "Pacific/Chatham" };

	private final AtomicInteger ids = new AtomicInteger();

	@Test
	void normalizedPolicyPreservesTheInstant() {
		inEveryEnvironment( "normalized", (factory, environment) -> {
			final Dialect dialect = factory.getJdbcServices().getDialect();
			final Instant first = firstInstant( dialect );
			for ( var value : samples( first ) ) {
				for ( var entityClass : new Class<?>[] { WithOffsetDateTime.class, WithZonedDateTime.class } ) {
					final Object typed = entityClass == WithOffsetDateTime.class ? value : value.toZonedDateTime();
					environment.accept( session -> checkStored( session, dialect, entityClass, typed, false ) );
				}
			}
			for ( var zoned : zonedSamples() ) {
				environment.accept( session -> checkStored( session, dialect, WithZonedDateTime.class, zoned, false ) );
			}
		} );
	}

	/// Only H2 keeps the offset to the second, so only there is every part
	/// of an `OffsetDateTime` preserved.
	@Test
	void valuePolicyPreservesTheOffsetInASingleColumn() {
		assumeTrue( DialectContext.getDialect() instanceof H2Dialect, "Only H2 keeps the offset to the second" );
		inEveryEnvironment( "value", (factory, environment) -> {
			final Dialect dialect = factory.getJdbcServices().getDialect();
			for ( var value : samples( firstInstant( dialect ) ) ) {
				environment.accept( session -> {
					final int id = checkStored( session, dialect, WithOffsetDateTime.class, value, true );
					// the database itself reports the offset which was written
					final String text = (String) session.createNativeQuery(
							"select cast(theValue as varchar(80)) from tp_offset_date_time where id = " + id,
							Object.class
					).getSingleResult();
					assertThat( OffsetDateTime.parse(
							text.replace( ' ', 'T' ).replaceFirst( "([+-]\\d\\d)$", "$1:00" )
					).getOffset() ).as( "offset stored for %s", value ).isEqualTo( value.getOffset() );
				} );
			}
		} );
	}

	/// With a second column, every database preserves the whole value: the
	/// instant and the offset of an `OffsetDateTime`, to the second, and the
	/// instant and the zone of a `ZonedDateTime`.
	@Test
	void secondColumnPreservesTheWholeValue() {
		inEveryEnvironment( "value", List.of( WithOffsetColumn.class, WithZoneColumn.class ), (factory, environment) -> {
			final Dialect dialect = factory.getJdbcServices().getDialect();
			final List<Object> values = new ArrayList<>( samples( firstInstant( dialect ) ) );
			values.addAll( zonedSamples() );
			for ( var sample : samples( firstInstant( dialect ) ) ) {
				// a ZonedDateTime may have a fixed offset for its zone
				values.add( sample.toZonedDateTime() );
			}
			for ( var value : values ) {
				final boolean zoned = value instanceof ZonedDateTime;
				final Instant instant = zoned
						? ( (ZonedDateTime) value ).toInstant()
						: ( (OffsetDateTime) value ).toInstant();
				final Class<?> entityClass = zoned ? WithZoneColumn.class : WithOffsetColumn.class;
				final String table = zoned ? "tp_zone_column" : "tp_offset_column";
				// what the second column holds, as the database would write it
				final String companion = zoned
						? "'" + ( (ZonedDateTime) value ).getZone().getId() + "'"
						: String.valueOf( ( (OffsetDateTime) value ).getOffset().getTotalSeconds() );
				environment.accept( session -> {
					final int id = ids.incrementAndGet();
					session.persist( newEntity( entityClass, id, value ) );
					session.flush();
					session.clear();

					// the database holds the instant in one column and the offset or zone in the other
					final String literal = instantLiteral( dialect, instant );
					assertThat( ( (Number) session.createNativeQuery(
							"select count(*) from " + table + " where id = " + id + " and theValue = " + literal
								+ " and theCompanion = " + companion,
							Object.class
					).getSingleResult() ).intValue() ).as( "%s and %s stored for %s", literal, companion, value )
							.isEqualTo( 1 );

					assertThat( read( session, entityClass, id ) ).as( "read of %s", value ).isEqualTo( value );

					// written by the database itself and read through Hibernate
					final int seededId = ids.incrementAndGet();
					session.createNativeMutationQuery(
							"insert into " + table + " (id, theValue, theCompanion) values (" + seededId + ", "
								+ literal + ", " + companion + ")"
					).executeUpdate();
					assertThat( read( session, entityClass, seededId ) ).as( "read of %s and %s", literal, companion )
							.isEqualTo( value );
				} );
			}
		} );
	}

	private static Object read(Session session, Class<?> entityClass, int id) {
		return session.createQuery(
				"select e.theValue from " + entityClass.getSimpleName() + " e where e.id = :id",
				Object.class
		).setParameter( "id", id ).getSingleResult();
	}

	@Test
	void valuesOutsideTheRangeAreRejected() {
		inEveryEnvironment( "normalized", (factory, environment) -> {
			final Dialect dialect = factory.getJdbcServices().getDialect();
			final Instant first = firstInstant( dialect );
			for ( var value : new OffsetDateTime[] {
					first.minusSeconds( 1 ).atOffset( ZoneOffset.UTC ),
					// the date in its own offset is within the range, but the instant is not
					first.minusSeconds( 3600 ).atOffset( ZoneOffset.ofHours( 5 ) ),
					OffsetDateTime.parse( "+10000-01-01T00:00:00Z" )
			} ) {
				for ( var entityClass : new Class<?>[] { WithOffsetDateTime.class, WithZonedDateTime.class } ) {
					final Object typed = entityClass == WithOffsetDateTime.class ? value : value.toZonedDateTime();
					environment.accept( session -> assertThatThrownBy( () -> {
						session.persist( newEntity( entityClass, ids.incrementAndGet(), typed ) );
						session.flush();
					} ).as( "insert of %s", typed ).isInstanceOf( TemporalPreservationException.class ) );
				}
			}

			final long day = 86_400;
			// The ten days before 1582-10-15 do not exist in Oracle's calendar.
			final long[] secondsEarlier = dialect instanceof OracleDialect
					? new long[] { 90 * day, 400 * day }
					: new long[] { 1, 16 * 3600, day, 10 * day, 78 * day, 79 * day, 90 * day, 400 * day };
			for ( long seconds : secondsEarlier ) {
				final Instant earlier = first.minusSeconds( seconds );
				for ( var table : new String[] { "tp_offset_date_time", "tp_zoned_date_time" } ) {
					final int id = ids.incrementAndGet();
					final String entityName = table.equals( "tp_offset_date_time" )
							? "WithOffsetDateTime"
							: "WithZonedDateTime";
					environment.accept( session -> {
						session.createNativeMutationQuery(
								"insert into " + table + " (id, theValue) values (" + id + ", "
									+ instantLiteral( dialect, earlier ) + ")"
						).executeUpdate();
						assertThatThrownBy( () -> {
							final Object read = session.createQuery(
									"select e.theValue from " + entityName + " e where e.id = :id",
									Object.class
							).setParameter( "id", id ).getSingleResult();
							throw new AssertionError( "Read " + read + " for " + earlier );
						} ).as( "read of %s from %s", earlier, table )
								.isInstanceOf( TemporalPreservationException.class );
					} );
				}
			}
		} );
	}

	/// @return the identifier of the row written
	private int checkStored(Session session, Dialect dialect, Class<?> entityClass, Object value, boolean exact) {
		final String table = entityClass.getAnnotation( jakarta.persistence.Table.class ).name();
		final String entityName = entityClass.getSimpleName();
		final Instant instant = value instanceof OffsetDateTime offsetDateTime
				? offsetDateTime.toInstant()
				: ( (ZonedDateTime) value ).toInstant();
		final int offsetSeconds = value instanceof OffsetDateTime offsetDateTime
				? offsetDateTime.getOffset().getTotalSeconds()
				: ( (ZonedDateTime) value ).getOffset().getTotalSeconds();
		final int id = ids.incrementAndGet();

		session.persist( newEntity( entityClass, id, value ) );
		session.flush();
		session.clear();

		// the database agrees that the stored value denotes the instant written
		final String literal = instantLiteral( dialect, instant );
		assertThat( ( (Number) session.createNativeQuery(
				"select count(*) from " + table + " where id = " + id + " and theValue = " + literal,
				Object.class
		).getSingleResult() ).intValue() ).as( "%s stored for %s", literal, value ).isEqualTo( 1 );
		final String epoch;
		if ( dialect instanceof OracleDialect ) {
			epoch = "round((cast(sys_extract_utc(theValue) as date) - date '1970-01-01') * 86400)";
		}
		else if ( dialect instanceof MySQLDialect ) {
			// the column holds the date and time at UTC
			epoch = "timestampdiff(second, timestamp '1970-01-01 00:00:00', theValue)"
					+ " - (case when theValue < timestamp '1970-01-01 00:00:00' and microsecond(theValue) <> 0 then 1 else 0 end)";
		}
		else {
			epoch = "extract(epoch from theValue)";
		}
		assertThat( new BigDecimal( session.createNativeQuery(
				"select " + epoch + " from " + table + " where id = " + id,
				Object.class
		).getSingleResult().toString() ).setScale( 0, RoundingMode.FLOOR ).longValueExact() )
				.as( "epoch second stored for %s", value )
				.isEqualTo( instant.getEpochSecond() );

		// read back
		final Object read = session.createQuery(
				"select e.theValue from " + entityName + " e where e.id = :id",
				Object.class
		).setParameter( "id", id ).getSingleResult();
		if ( exact ) {
			assertThat( read ).as( "read of %s", value ).isEqualTo( value );
		}
		else {
			assertThat( read instanceof OffsetDateTime offsetDateTime
					? offsetDateTime.toInstant()
					: ( (ZonedDateTime) read ).toInstant() ).as( "instant read for %s", value ).isEqualTo( instant );
		}

		// compared to a bound parameter
		assertThat( session.createQuery(
				"select count(e) from " + entityName + " e where e.id = :id and e.theValue = :theValue",
				Long.class
		).setParameter( "id", id ).setParameter( "theValue", value ).getSingleResult() )
				.as( "parameter %s", value )
				.isEqualTo( 1L );

		// compared to a literal rendered by Hibernate, which has no way to write
		// an offset with seconds, or a year before 1 or after 9999, and so declines to
		final var builder = session.getCriteriaBuilder();
		final var criteria = builder.createQuery( Long.class );
		final var root = criteria.from( entityClass );
		criteria.select( builder.count( root ) ).where(
				builder.equal( root.get( "id" ), id ),
				builder.equal( root.get( "theValue" ), builder.literal( value ) )
		);
		final int localYear = value instanceof OffsetDateTime offsetDateTime
				? offsetDateTime.getYear()
				: ( (ZonedDateTime) value ).getYear();
		if ( !( dialect instanceof H2Dialect ) && Math.abs( offsetSeconds ) >= 16 * 3600 ) {
			// PostgreSQL and Oracle refuse a literal with an offset of sixteen hours
			// or more outright, which would end the transaction this check runs in
		}
		else if ( offsetSeconds % 60 == 0 && localYear >= 1 && localYear <= 9999 ) {
			assertThat( session.createQuery( criteria ).getSingleResult() )
					.as( "literal rendered for %s", value )
					.isEqualTo( 1L );
		}
		else {
			assertThatThrownBy( () -> session.createQuery( criteria ).getSingleResult() )
					.as( "literal rendered for %s", value )
					.isInstanceOf( TemporalPreservationException.class );
		}

		// written by the database itself and read through Hibernate
		final int seededId = ids.incrementAndGet();
		session.createNativeMutationQuery(
				"insert into " + table + " (id, theValue) values (" + seededId + ", " + literal + ")"
		).executeUpdate();
		final Object seeded = session.createQuery(
				"select e.theValue from " + entityName + " e where e.id = :id",
				Object.class
		).setParameter( "id", seededId ).getSingleResult();
		assertThat( seeded instanceof OffsetDateTime offsetDateTime
				? offsetDateTime.toInstant()
				: ( (ZonedDateTime) seeded ).toInstant() ).as( "read of %s", literal ).isEqualTo( instant );
		return id;
	}

	/// Offsets of whole hours, of a quarter hour, with seconds, and the
	/// extremes; the first and last supported instants; and values whose
	/// date in their own offset differs from the date in UTC.
	private static List<OffsetDateTime> samples(Instant first) {
		final List<OffsetDateTime> samples = new ArrayList<>();
		final var local = LocalDateTime.of( 2021, 3, 14, 12, 0, 0, 123_456_000 );
		for ( var offset : new String[] { "Z", "+05:45", "-04:00", "+05:41:16", "+18:00", "-18:00" } ) {
			samples.add( local.atOffset( ZoneOffset.of( offset ) ) );
		}
		samples.add( first.atOffset( ZoneOffset.UTC ) );
		samples.add( first.atOffset( ZoneOffset.ofHoursMinutes( 5, 45 ) ) );
		samples.add( first.atOffset( ZoneOffset.ofHours( -16 ) ) );
		samples.add( first.plusSeconds( 20 * 3600 ).atOffset( ZoneOffset.ofHours( -5 ) ) );
		samples.add( OffsetDateTime.parse( "9999-12-31T23:59:59.999999Z" ) );
		samples.add( OffsetDateTime.parse( "9999-12-31T23:59:59-18:00" ).minusHours( 18 ) );
		return samples;
	}

	/// Values in region-based zones, including both occurrences of a time
	/// which is repeated when clocks are set back.
	private static List<ZonedDateTime> zonedSamples() {
		final var newYork = ZoneId.of( "America/New_York" );
		return List.of(
				LocalDateTime.of( 2021, 3, 14, 12, 0 ).atZone( ZoneId.of( "Europe/Paris" ) ),
				LocalDateTime.of( 2021, 11, 7, 1, 30 ).atZone( newYork ).withEarlierOffsetAtOverlap(),
				LocalDateTime.of( 2021, 11, 7, 1, 30 ).atZone( newYork ).withLaterOffsetAtOverlap(),
				LocalDateTime.of( 1985, 12, 31, 23, 59 ).atZone( ZoneId.of( "Asia/Kathmandu" ) )
		);
	}

	private static Instant firstInstant(Dialect dialect) {
		return (Instant) dialect.getTemporalPreservation(
				OffsetDateTimeJavaType.INSTANCE,
				TemporalPreservationOffsetTests.offsetDateTimeJdbcType
		).minimum();
	}

	private static org.hibernate.type.descriptor.jdbc.JdbcType offsetDateTimeJdbcType;

	private static String instantLiteral(Dialect dialect, Instant value) {
		final String fields = DATE_TIME.format( value.atOffset( ZoneOffset.UTC ) );
		if ( dialect instanceof OracleDialect ) {
			return "timestamp '" + fields + " +00:00'";
		}
		else if ( dialect instanceof MySQLDialect ) {
			// the column holds the date and time at UTC
			return "timestamp '" + fields + "'";
		}
		else {
			return "timestamp with time zone '" + fields + "+00:00'";
		}
	}

	private static Object newEntity(Class<?> entityClass, int id, Object value) {
		try {
			final Object entity = entityClass.getDeclaredConstructor().newInstance();
			entityClass.getDeclaredField( "id" ).set( entity, id );
			entityClass.getDeclaredField( "theValue" ).set( entity, value );
			return entity;
		}
		catch (ReflectiveOperationException e) {
			throw new IllegalStateException( e );
		}
	}

	@Entity(name = "WithOffsetColumn")
	@Table(name = "tp_offset_column")
	public static class WithOffsetColumn {
		@Id
		public Integer id;
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		@TimeZoneColumn(name = "theCompanion")
		public OffsetDateTime theValue;
	}

	@Entity(name = "WithZoneColumn")
	@Table(name = "tp_zone_column")
	public static class WithZoneColumn {
		@Id
		public Integer id;
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		@TimeZoneColumn(name = "theCompanion")
		public ZonedDateTime theValue;
	}

	/// A value stored in two columns is compared by its instant with values
	/// which occupy a single column, in every predicate which compares:
	/// `between` and `in` as well as the comparison operators.
	@Test
	void twoColumnsAreComparedWithSingleValuesByInstant() {
		inEveryEnvironment( "value", List.of( WithOffsetColumn.class, WithZoneColumn.class ), (factory, environment) -> {
			final var offset = new WithOffsetColumn();
			offset.id = ids.incrementAndGet();
			offset.theValue = OffsetDateTime.parse( "2021-03-14T12:00:00+01:00" );
			final var zoned = new WithZoneColumn();
			zoned.id = ids.incrementAndGet();
			zoned.theValue = ZonedDateTime.parse( "2021-03-14T12:00:00+01:00[Europe/Paris]" );
			factory.inTransaction( session -> {
				session.persist( offset );
				session.persist( zoned );
			} );
			environment.accept( session -> {
				for ( var target : List.of( "WithOffsetColumn e where e.id = " + offset.id, "WithZoneColumn e where e.id = " + zoned.id ) ) {
					final String select = "select count(e) from " + target + " and e.theValue ";
					assertThat( count( session, select + "= {2021-03-14 11:00:00 +00:00}" ) ).isEqualTo( 1L );
					assertThat( count( session, select
							+ "between {2021-03-14 10:00:00 +00:00} and {2021-03-14 12:00:00 +00:00}" ) ).isEqualTo( 1L );
					assertThat( count( session, select
							+ "not between {2021-03-14 10:00:00 +00:00} and {2021-03-14 12:00:00 +00:00}" ) ).isEqualTo( 0L );
					assertThat( count( session, select
							+ "between {2021-03-14 11:30:00 +00:00} and {2021-03-14 12:00:00 +00:00}" ) ).isEqualTo( 0L );
					assertThat( count( session, select
							+ "in ({2000-01-01 00:00:00 +00:00}, {2021-03-14 11:00:00 +00:00})" ) ).isEqualTo( 1L );
					assertThat( count( session, select
							+ "not in ({2000-01-01 00:00:00 +00:00}, {2021-03-14 11:00:00 +00:00})" ) ).isEqualTo( 0L );
					assertThat( count( session, select
							+ "in ({2000-01-01 00:00:00 +00:00}, {2021-03-14 11:00:01 +00:00})" ) ).isEqualTo( 0L );
				}
				// a parameter stands for both columns, and cannot be mixed with a value which occupies one
				assertThatThrownBy( () -> session.createQuery( "select count(e) from WithOffsetColumn e"
						+ " where e.theValue between :low and {2021-03-14 12:00:00 +00:00}", Long.class )
						.setParameter( "low", offset.theValue ).getSingleResult() )
						.hasStackTraceContaining( "in the same predicate" );
				assertThatThrownBy( () -> session.createQuery( "select count(e) from WithOffsetColumn e"
						+ " where e.theValue in (:one, {2021-03-14 11:00:00 +00:00})", Long.class )
						.setParameter( "one", offset.theValue ).getSingleResult() )
						.hasStackTraceContaining( "in the same predicate" );
				// parameters alone compare both columns
				assertThat( session.createQuery( "select count(e) from WithOffsetColumn e where e.id = :id"
						+ " and e.theValue in (:one, :other)", Long.class )
						.setParameter( "id", offset.id )
						.setParameter( "one", offset.theValue )
						.setParameter( "other", offset.theValue.plusDays( 1 ) ).getSingleResult() ).isEqualTo( 1L );
				// a subquery which selects a single column cannot stand in for both
				assertThatThrownBy( () -> count( session, "select count(e) from WithZoneColumn e"
						+ " where e.theValue in (select local datetime from WithZoneColumn f)" ) )
						.hasStackTraceContaining( "with the result of a subquery" );
			} );
		} );
	}

	private static Long count(Session session, String hql) {
		return session.createQuery( hql, Long.class ).getSingleResult();
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private interface Body {
		void run(SessionFactoryImplementor factory, Consumer<Consumer<Session>> environment);
	}

	/// Run the body with a factory using the given policy, under two default
	/// time zones of the JVM. The body is handed a function which runs an
	/// action in a session of its own, in each combination of JDBC time zone
	/// of the session and time zone of the database session.
	private static void inEveryEnvironment(String policy, Body body) {
		inEveryEnvironment(
				policy,
				policy.equals( "normalized" )
						? List.of( WithOffsetDateTime.class, WithZonedDateTime.class )
						: List.of( WithOffsetDateTime.class ),
				body
		);
	}

	private static void inEveryEnvironment(String policy, List<Class<?>> entityClasses, Body body) {
		final TimeZone original = TimeZone.getDefault();
		try {
			for ( var jvmZone : new String[] { "UTC", "America/New_York" } ) {
				TimeZone.setDefault( TimeZone.getTimeZone( jvmZone ) );
				final var registryBuilder = ServiceRegistryUtil.serviceRegistryBuilder();
				registryBuilder.applySetting( "hibernate.hbm2ddl.auto", "create-drop" );
				registryBuilder.applySetting( MappingSettings.TEMPORAL_PRESERVATION, true );
				registryBuilder.applySetting( MappingSettings.TEMPORAL_PRESERVATION_POLICY, policy );
				registryBuilder.applySetting( QuerySettings.CRITERIA_VALUE_HANDLING_MODE, "inline" );
				try ( var registry = registryBuilder.build() ) {
					final var sources = new MetadataSources( registry );
					entityClasses.forEach( sources::addAnnotatedClass );
					try ( var factory = (SessionFactoryImplementor) sources.buildMetadata().buildSessionFactory() ) {
						offsetDateTimeJdbcType = factory.getTypeConfiguration()
								.getBasicTypeForJavaType( OffsetDateTime.class )
								.getJdbcType();
						body.run( factory, action -> {
							for ( var sessionJdbcZone : SESSION_JDBC_ZONES ) {
								for ( var databaseSessionZone : DATABASE_SESSION_ZONES ) {
									inSession( factory, sessionJdbcZone, databaseSessionZone, action );
								}
							}
						} );
					}
				}
			}
		}
		finally {
			TimeZone.setDefault( original );
		}
	}

	static void inSession(
			SessionFactoryImplementor factory,
			String sessionJdbcZone,
			String databaseSessionZone,
			Consumer<Session> action) {
		final Dialect dialect = factory.getJdbcServices().getDialect();
		final var sessionBuilder = factory.withOptions();
		if ( sessionJdbcZone != null ) {
			sessionBuilder.jdbcTimeZone( TimeZone.getTimeZone( sessionJdbcZone ) );
		}
		try ( Session session = sessionBuilder.openSession() ) {
			session.beginTransaction();
			final String[] restore = new String[1];
			try {
				if ( databaseSessionZone != null ) {
					session.doWork( connection -> restore[0] = setDatabaseSessionZone(
							connection,
							dialect,
							databaseSessionZone
					) );
				}
				action.accept( session );
			}
			catch (AssertionError | RuntimeException e) {
				throw new AssertionError(
						"With sessionJdbc=" + sessionJdbcZone + " databaseSession=" + databaseSessionZone
							+ " jvm=" + TimeZone.getDefault().getID() + ": " + e.getMessage(),
						e
				);
			}
			finally {
				if ( restore[0] != null ) {
					session.doWork( connection -> execute( connection, restore[0] ) );
				}
				session.getTransaction().rollback();
			}
		}
	}

	private static String setDatabaseSessionZone(Connection connection, Dialect dialect, String zone)
			throws SQLException {
		if ( dialect instanceof OracleDialect ) {
			final String current = queryString( connection, "select sessiontimezone from dual" );
			execute( connection, "alter session set time_zone = '" + zone + "'" );
			return "alter session set time_zone = '" + current + "'";
		}
		else if ( dialect instanceof PostgreSQLDialect ) {
			final String current = queryString( connection, "show time zone" );
			execute( connection, "set time zone '" + zone + "'" );
			return "set time zone '" + current + "'";
		}
		else if ( dialect instanceof MySQLDialect ) {
			// MySQL and MariaDB
			execute( connection, "set time_zone = '" + zone + "'" );
			return "set time_zone = @@global.time_zone";
		}
		else {
			execute( connection, "set time zone '" + zone + "'" );
			return "set time zone local";
		}
	}

	private static String queryString(Connection connection, String sql) throws SQLException {
		try ( var statement = connection.createStatement(); var results = statement.executeQuery( sql ) ) {
			results.next();
			return results.getString( 1 );
		}
	}

	private static void execute(Connection connection, String sql) throws SQLException {
		try ( var statement = connection.createStatement() ) {
			statement.execute( sql );
		}
	}
}
