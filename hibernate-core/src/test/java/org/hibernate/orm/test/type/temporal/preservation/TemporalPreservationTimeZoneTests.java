package org.hibernate.orm.test.type.temporal.preservation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.Session;
import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.cfg.QuerySettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.orm.test.type.temporal.preservation.TemporalPreservationBootMatrix.WithInstant;
import org.hibernate.orm.test.type.temporal.preservation.TemporalPreservationBootMatrix.WithLocalDate;
import org.hibernate.orm.test.type.temporal.preservation.TemporalPreservationBootMatrix.WithLocalDateTime;
import org.hibernate.orm.test.type.temporal.preservation.TemporalPreservationBootMatrix.WithLocalTime;

import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies against a live database that what temporal preservation stores
/// and reads does not depend on any time zone in the environment.
///
/// Four zones are varied independently: the default time zone of the JVM,
/// the JDBC time zone configured for the factory, the JDBC time zone given
/// to an individual session, and the time zone of the database session. For
/// every combination, the stored value must equal a literal written by hand
/// in the database's own syntax, an instant must be the same number of
/// seconds from the epoch by the database's own arithmetic, and the value
/// must read back, compare to a bound parameter, and compare to a literal
/// rendered by Hibernate.
///
/// This covers both kinds of access. A `LocalDate`, `LocalTime` or
/// `LocalDateTime` is passed to the JDBC driver directly, with no time zone
/// involved. An `Instant` is converted on PostgreSQL to a `java.sql.Timestamp`
/// with a fixed UTC calendar, and on Oracle to an `OffsetDateTime` at UTC;
/// neither may be swayed by a configured JDBC time zone.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
@RequiresDialect(value = PostgreSQLDialect.class, matchSubTypes = false)
@RequiresDialect(OracleDialect.class)
class TemporalPreservationTimeZoneTests {
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern( "uuuu-MM-dd" );
	private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern( "uuuu-MM-dd HH:mm:ss.SSSSSS" );
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern( "HH:mm:ss" );

	/// `null` leaves the zone as it is.
	private static final String[] JDBC_ZONES = { null, "UTC", "America/New_York", "Asia/Kathmandu" };
	private static final String[] SESSION_JDBC_ZONES = { null, "Pacific/Chatham" };
	private static final String[] DATABASE_SESSION_ZONES = { null, "Asia/Kathmandu", "America/New_York" };

	private final AtomicInteger ids = new AtomicInteger();

	@Test
	void storageIsIndependentOfEveryTimeZone() {
		final TimeZone original = TimeZone.getDefault();
		final List<String> combinations = new ArrayList<>();
		try {
			for ( var jvmZone : new String[] { "UTC", "America/New_York" } ) {
				TimeZone.setDefault( TimeZone.getTimeZone( jvmZone ) );
				for ( var jdbcZone : JDBC_ZONES ) {
					inFactory( jdbcZone, factory -> {
						for ( var sessionJdbcZone : SESSION_JDBC_ZONES ) {
							for ( var databaseSessionZone : DATABASE_SESSION_ZONES ) {
								final String combination = "jvm=" + jvmZone + " jdbc=" + jdbcZone
									+ " sessionJdbc=" + sessionJdbcZone + " databaseSession=" + databaseSessionZone;
								check( factory, sessionJdbcZone, databaseSessionZone, combination );
								combinations.add( combination );
							}
						}
					} );
				}
			}
		}
		finally {
			TimeZone.setDefault( original );
		}
		assertThat( combinations ).hasSize(
				2 * JDBC_ZONES.length * SESSION_JDBC_ZONES.length * DATABASE_SESSION_ZONES.length
		);
	}

	private void check(
			SessionFactoryImplementor factory,
			String sessionJdbcZone,
			String databaseSessionZone,
			String combination) {
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
				for ( var sample : samples( dialect ).entrySet() ) {
					checkSample( session, dialect, sample.getKey(), sample.getValue(), combination );
				}
			}
			finally {
				if ( restore[0] != null ) {
					session.doWork( connection -> execute( connection, restore[0] ) );
				}
				session.getTransaction().rollback();
			}
		}
	}

	private void checkSample(Session session, Dialect dialect, Object value, String literal, String combination) {
		final Class<?> entityClass = entityClassFor( value );
		final String table = entityClass.getAnnotation( jakarta.persistence.Table.class ).name();
		final String entityName = entityClass.getSimpleName();
		final int id = ids.incrementAndGet();
		final String description = value + " with " + combination;

		session.persist( newEntity( entityClass, id, value ) );
		session.flush();
		session.clear();

		// the database agrees that the stored value is the one written
		assertThat( ( (Number) session.createNativeQuery(
				"select count(*) from " + table + " where id = " + id + " and theValue = " + literal,
				Object.class
		).getSingleResult() ).intValue() ).as( "%s stored for %s", literal, description ).isEqualTo( 1 );

		if ( value instanceof Instant instant ) {
			final String epoch = dialect instanceof OracleDialect
					? "round((cast(sys_extract_utc(theValue) as date) - date '1970-01-01') * 86400)"
					: "extract(epoch from theValue)";
			assertThat( new BigDecimal( session.createNativeQuery(
					"select " + epoch + " from " + table + " where id = " + id,
					Object.class
			).getSingleResult().toString() ).setScale( 0, RoundingMode.FLOOR ).longValueExact() )
					.as( "epoch second stored for %s", description )
					.isEqualTo( instant.getEpochSecond() );
		}

		// read back
		assertThat( session.createQuery(
				"select e.theValue from " + entityName + " e where e.id = :id",
				Object.class
		).setParameter( "id", id ).getSingleResult() ).as( "read of %s", description ).isEqualTo( value );

		// compared to a bound parameter
		assertThat( session.createQuery(
				"select count(e) from " + entityName + " e where e.id = :id and e.theValue = :theValue",
				Long.class
		).setParameter( "id", id ).setParameter( "theValue", value ).getSingleResult() )
				.as( "parameter %s", description )
				.isEqualTo( 1L );

		// compared to a literal rendered by Hibernate
		final var builder = session.getCriteriaBuilder();
		final var criteria = builder.createQuery( Long.class );
		final var root = criteria.from( entityClass );
		criteria.select( builder.count( root ) ).where(
				builder.equal( root.get( "id" ), id ),
				builder.equal( root.get( "theValue" ), builder.literal( value ) )
		);
		assertThat( session.createQuery( criteria ).getSingleResult() )
				.as( "literal rendered for %s", description )
				.isEqualTo( 1L );

		// written by the database itself and read through Hibernate
		final int seededId = ids.incrementAndGet();
		session.createNativeMutationQuery(
				"insert into " + table + " (id, theValue) values (" + seededId + ", " + literal + ")"
		).executeUpdate();
		assertThat( session.createQuery(
				"select e.theValue from " + entityName + " e where e.id = :id",
				Object.class
		).setParameter( "id", seededId ).getSingleResult() ).as( "read of %s with %s", literal, combination )
				.isEqualTo( value );
	}

	/// Values paired with their literal in the database's own syntax. The
	/// local values include a time skipped and a time repeated in New York;
	/// the instants include both occurrences of that repeated time, and one
	/// from before Kathmandu changed its offset.
	private static Map<Object, String> samples(Dialect dialect) {
		final Map<Object, String> samples = new LinkedHashMap<>();
		for ( var value : new LocalDate[] { LocalDate.of( 2021, 3, 14 ), LocalDate.of( 1985, 12, 31 ) } ) {
			samples.put( value, "date '" + DATE.format( value ) + "'" );
		}
		for ( var value : new LocalDateTime[] {
				LocalDateTime.of( 2021, 3, 14, 2, 30, 0 ),
				LocalDateTime.of( 2021, 11, 7, 1, 30, 0 ),
				LocalDateTime.of( 1985, 12, 31, 23, 59, 59, 123_456_000 )
		} ) {
			samples.put( value, "timestamp '" + DATE_TIME.format( value ) + "'" );
		}
		if ( !( dialect instanceof OracleDialect ) ) {
			for ( var value : new LocalTime[] { LocalTime.of( 2, 30, 15 ), LocalTime.MIDNIGHT } ) {
				samples.put( value, "time '" + TIME.format( value ) + "'" );
			}
		}
		for ( var value : new Instant[] {
				Instant.parse( "2021-03-14T07:30:00.123456Z" ),
				Instant.parse( "2021-11-07T05:30:00Z" ),
				Instant.parse( "2021-11-07T06:30:00Z" ),
				Instant.parse( "1985-12-31T18:15:00Z" ),
				Instant.EPOCH
		} ) {
			final String fields = DATE_TIME.format( value.atOffset( ZoneOffset.UTC ) );
			samples.put(
					value,
					dialect instanceof OracleDialect
							? "timestamp '" + fields + " +00:00'"
							: "timestamp with time zone '" + fields + "+00:00'"
			);
		}
		return samples;
	}

	private static Class<?> entityClassFor(Object value) {
		if ( value instanceof LocalDate ) {
			return WithLocalDate.class;
		}
		else if ( value instanceof LocalTime ) {
			return WithLocalTime.class;
		}
		else if ( value instanceof LocalDateTime ) {
			return WithLocalDateTime.class;
		}
		else {
			return WithInstant.class;
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

	/// Change the time zone of the database session, and answer the statement
	/// which changes it back.
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

	private static void inFactory(String jdbcZone, java.util.function.Consumer<SessionFactoryImplementor> action) {
		final var registryBuilder = ServiceRegistryUtil.serviceRegistryBuilder();
		registryBuilder.applySetting( "hibernate.hbm2ddl.auto", "create-drop" );
		registryBuilder.applySetting( MappingSettings.TEMPORAL_PRESERVATION, true );
		registryBuilder.applySetting( QuerySettings.CRITERIA_VALUE_HANDLING_MODE, "inline" );
		if ( jdbcZone != null ) {
			registryBuilder.applySetting( JdbcSettings.JDBC_TIME_ZONE, jdbcZone );
		}
		try ( var registry = registryBuilder.build() ) {
			final var sources = new MetadataSources( registry )
					.addAnnotatedClass( WithLocalDate.class )
					.addAnnotatedClass( WithLocalDateTime.class )
					.addAnnotatedClass( WithInstant.class );
			final var metadata = sources.buildMetadata();
			if ( !( metadata.getDatabase().getDialect() instanceof OracleDialect ) ) {
				// a LocalTime is not supported on Oracle
				try ( var withTime = ServiceRegistryUtil.serviceRegistryBuilder()
						.applySettings( registry.getService( org.hibernate.engine.config.spi.ConfigurationService.class ).getSettings() )
						.build() ) {
					try ( var factory = (SessionFactoryImplementor) new MetadataSources( withTime )
							.addAnnotatedClass( WithLocalDate.class )
							.addAnnotatedClass( WithLocalDateTime.class )
							.addAnnotatedClass( WithInstant.class )
							.addAnnotatedClass( WithLocalTime.class )
							.buildMetadata()
							.buildSessionFactory() ) {
						action.accept( factory );
					}
				}
			}
			else {
				try ( var factory = (SessionFactoryImplementor) metadata.buildSessionFactory() ) {
					action.accept( factory );
				}
			}
		}
	}
}
