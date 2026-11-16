package org.hibernate.orm.test.type.temporal.preservation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;

import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// Verifies that with the temporal preservation setting left unset, Java Time
/// values are handled exactly as they were before the setting existed, both
/// with and without `hibernate.type.java_time_use_direct_jdbc`.
///
/// For each Java Time class the test records the JDBC type and column type
/// which are selected and, for a list of values chosen to be sensitive to how
/// they are converted, what the database stores, what is read back, and
/// whether a literal rendered by Hibernate matches. The record is compared
/// with `disabled-mode-baseline-<Dialect>.txt` or `direct-jdbc-baseline-<Dialect>.txt`, which was produced by running
/// this same class, unchanged, against the `main` branch. Because the handling
/// being verified depends on the default time zone of the JVM, the comparison
/// is made only when that is the zone the baseline was recorded in, which is
/// UTC, the zone this build runs its tests in. Another default time zone can
/// be requested with `-Dhibernate.test.temporal_preservation.zone=<id>`, which
/// is reliable only when this class is the only one run.
///
/// This class deliberately uses nothing introduced with temporal
/// preservation, so that it compiles and runs against `main`. To record a
/// baseline, run it with `-Dhibernate.test.temporal_preservation.record=true`
/// and copy the file written to `target/temporal-preservation`.
///
/// @author Steve Ebersole
@DisabledIfSystemProperty(
	named = "hibernate.type.temporal_preservation",
	matches = "true",
	disabledReason = "Verifies the behavior with the setting left unset"
)
class DisabledModeBaselineTests {
	private static final String RECORD = "hibernate.test.temporal_preservation.record";
	private static final String ZONE = "hibernate.test.temporal_preservation.zone";

	/// Neither setting is given.
	@Test
	void unsetBehavesAsBefore() throws IOException {
		verify( false, "disabled-mode-baseline-" );
	}

	/// Only `hibernate.type.java_time_use_direct_jdbc` is enabled, which
	/// existed before temporal preservation and must keep its meaning.
	@Test
	void directJdbcAloneBehavesAsBefore() throws IOException {
		verify( true, "direct-jdbc-baseline-" );
	}

	private static void verify(boolean directJdbc, String filePrefix) throws IOException {
		// conversion through the java.sql types depends on the default time zone
		final TimeZone original = TimeZone.getDefault();
		final String requestedZone = System.getProperty( ZONE );
		if ( requestedZone != null ) {
			TimeZone.setDefault( TimeZone.getTimeZone( requestedZone ) );
		}
		final String zone = "zone=" + TimeZone.getDefault().getID();
		final List<String> lines = new ArrayList<>();
		lines.add( zone );
		final String dialect;
		try {
			dialect = record( lines, directJdbc );
		}
		finally {
			TimeZone.setDefault( original );
		}

		final String fileName = filePrefix + dialect + ".txt";
		final String recorded = String.join( "\n", lines ) + "\n";
		if ( Boolean.getBoolean( RECORD ) ) {
			final Path directory = Path.of( "target", "temporal-preservation" );
			Files.createDirectories( directory );
			Files.writeString( directory.resolve( fileName ), recorded );
			return;
		}

		try ( InputStream stream = DisabledModeBaselineTests.class.getResourceAsStream( fileName ) ) {
			assumeTrue( stream != null, "No baseline recorded for " + dialect );
			final String baseline = new String( stream.readAllBytes(), StandardCharsets.UTF_8 );
			assumeTrue(
					baseline.startsWith( zone + "\n" ),
					"The baseline was recorded in a different default time zone than " + zone
			);
			assertThat( recorded ).isEqualTo( baseline );
		}
	}

	private static String record(List<String> lines, boolean directJdbc) {
		final Class<?>[] entityClasses = {
				BaselineLocalDate.class,
				BaselineLocalTime.class,
				BaselineLocalDateTime.class,
				BaselineInstant.class,
				BaselineOffsetTime.class,
				BaselineOffsetDateTime.class,
				BaselineZonedDateTime.class
		};
		final var registryBuilder = ServiceRegistryUtil.serviceRegistryBuilder();
		registryBuilder.applySetting( "hibernate.hbm2ddl.auto", "create-drop" );
		registryBuilder.applySetting( "hibernate.criteria.value_handling_mode", "inline" );
		if ( directJdbc ) {
			registryBuilder.applySetting( "hibernate.type.java_time_use_direct_jdbc", true );
		}
		try ( var registry = registryBuilder.build() ) {
			final var sources = new MetadataSources( registry );
			for ( var entityClass : entityClasses ) {
				sources.addAnnotatedClass( entityClass );
			}
			final var metadata = (MetadataImplementor) sources.buildMetadata();
			final Dialect dialect = metadata.getDatabase().getDialect();
			try ( var factory = (SessionFactoryImplementor) metadata.buildSessionFactory() ) {
				int id = 0;
				for ( var entityClass : entityClasses ) {
					final var value = (BasicValue) metadata.getEntityBinding( entityClass.getName() )
							.getProperty( "theValue" )
							.getValue();
					lines.add( entityClass.getSimpleName()
							+ " jdbcType=" + value.resolve().getJdbcType().getClass().getSimpleName()
							+ " column=" + ( (Column) value.getColumn() ).getSqlType( metadata ) );
					for ( var sample : samples( entityClass ) ) {
						lines.add( "  " + sample + " -> " + roundTrip( factory, dialect, entityClass, ++id, sample ) );
					}
				}
			}
			return dialect.getClass().getSimpleName();
		}
	}

	private static String roundTrip(
			SessionFactoryImplementor factory,
			Dialect dialect,
			Class<?> entityClass,
			int id,
			Object sample) {
		final String table = entityClass.getAnnotation( Table.class ).name();
		final String entityName = entityClass.getSimpleName();
		try {
			factory.inTransaction( session -> {
				try {
					final Object entity = entityClass.getDeclaredConstructor().newInstance();
					entityClass.getDeclaredField( "id" ).set( entity, id );
					entityClass.getDeclaredField( "theValue" ).set( entity, sample );
					session.persist( entity );
				}
				catch (ReflectiveOperationException e) {
					throw new IllegalStateException( e );
				}
			} );
			return factory.fromTransaction( session -> {
				final Object stored = session.createNativeQuery(
						"select " + asText( dialect, entityClass ) + " from " + table + " where id = " + id,
						Object.class
				).getSingleResult();
				final Object read = session.createQuery(
						"select e.theValue from " + entityName + " e where e.id = :id",
						Object.class
				).setParameter( "id", id ).getSingleResult();
				final Long matchedByParameter = session.createQuery(
						"select count(e) from " + entityName + " e where e.id = :id and e.theValue = :theValue",
						Long.class
				).setParameter( "id", id ).setParameter( "theValue", sample ).getSingleResult();
				final var builder = session.getCriteriaBuilder();
				final var criteria = builder.createQuery( Long.class );
				final var root = criteria.from( entityClass );
				criteria.select( builder.count( root ) ).where(
						builder.equal( root.get( "id" ), id ),
						builder.equal( root.get( "theValue" ), builder.literal( sample ) )
				);
				String matchedByLiteral;
				try {
					matchedByLiteral = String.valueOf( session.createQuery( criteria ).getSingleResult() );
				}
				catch (RuntimeException e) {
					matchedByLiteral = e.getClass().getSimpleName();
				}
				return "stored=" + stored + " read=" + read
					+ " parameter=" + matchedByParameter + " literal=" + matchedByLiteral;
			} );
		}
		catch (RuntimeException e) {
			return e.getClass().getSimpleName();
		}
	}

	/// The stored value as text, as the database itself renders it.
	private static String asText(Dialect dialect, Class<?> entityClass) {
		if ( dialect.getClass().getSimpleName().startsWith( "Oracle" ) ) {
			if ( entityClass == BaselineLocalDate.class ) {
				return "to_char(theValue, 'SYYYY-MM-DD HH24:MI:SS')";
			}
			else if ( entityClass == BaselineLocalTime.class || entityClass == BaselineLocalDateTime.class ) {
				return "to_char(theValue, 'SYYYY-MM-DD HH24:MI:SS.FF9')";
			}
			else {
				return "to_char(sys_extract_utc(theValue), 'SYYYY-MM-DD HH24:MI:SS.FF9')"
					+ " || ' ' || to_char(theValue, 'TZH:TZM')";
			}
		}
		else if ( entityClass == BaselineInstant.class
				|| entityClass == BaselineOffsetDateTime.class
				|| entityClass == BaselineZonedDateTime.class ) {
			// independent of the time zone of the database session
			return "cast(extract(epoch from theValue) as varchar(60))";
		}
		else {
			return "cast(theValue as varchar(60))";
		}
	}

	private static List<Object> samples(Class<?> entityClass) {
		final List<LocalDateTime> dateTimes = List.of(
				LocalDateTime.of( 1, 1, 1, 0, 0, 0 ),
				LocalDateTime.of( 1500, 3, 1, 12, 0, 0 ),
				LocalDateTime.of( 1582, 10, 10, 12, 0, 0 ),
				LocalDateTime.of( 1582, 10, 15, 0, 0, 0 ),
				LocalDateTime.of( 1883, 11, 18, 12, 0, 0 ),
				LocalDateTime.of( 1900, 1, 1, 0, 0, 0 ),
				LocalDateTime.of( 1970, 1, 1, 0, 0, 0 ),
				LocalDateTime.of( 2021, 3, 14, 2, 30, 0 ),
				LocalDateTime.of( 2021, 3, 14, 7, 30, 0, 123_456_000 ),
				LocalDateTime.of( 2021, 11, 7, 1, 30, 0 ),
				LocalDateTime.of( 2021, 11, 7, 6, 30, 0 ),
				LocalDateTime.of( 9999, 12, 31, 23, 59, 59 )
		);
		final List<Object> samples = new ArrayList<>();
		for ( var dateTime : dateTimes ) {
			if ( entityClass == BaselineLocalDate.class ) {
				samples.add( dateTime.toLocalDate() );
			}
			else if ( entityClass == BaselineLocalTime.class ) {
				samples.add( dateTime.toLocalTime().withNano( 0 ) );
			}
			else if ( entityClass == BaselineLocalDateTime.class ) {
				samples.add( dateTime );
			}
			else if ( entityClass == BaselineInstant.class ) {
				samples.add( dateTime.toInstant( ZoneOffset.UTC ) );
			}
			else if ( entityClass == BaselineOffsetTime.class ) {
				samples.add( dateTime.toLocalTime().withNano( 0 ).atOffset( ZoneOffset.ofHours( 2 ) ) );
			}
			else if ( entityClass == BaselineOffsetDateTime.class ) {
				samples.add( dateTime.atOffset( ZoneOffset.ofHours( 2 ) ) );
			}
			else {
				samples.add( dateTime.atZone( ZoneId.of( "Europe/Paris" ) ) );
			}
		}
		return samples.stream().distinct().toList();
	}

	@Entity(name = "BaselineLocalDate")
	@Table(name = "tpb_local_date")
	public static class BaselineLocalDate {
		@Id
		public Integer id;
		public LocalDate theValue;
	}

	@Entity(name = "BaselineLocalTime")
	@Table(name = "tpb_local_time")
	public static class BaselineLocalTime {
		@Id
		public Integer id;
		public LocalTime theValue;
	}

	@Entity(name = "BaselineLocalDateTime")
	@Table(name = "tpb_local_date_time")
	public static class BaselineLocalDateTime {
		@Id
		public Integer id;
		public LocalDateTime theValue;
	}

	@Entity(name = "BaselineInstant")
	@Table(name = "tpb_instant")
	public static class BaselineInstant {
		@Id
		public Integer id;
		public Instant theValue;
	}

	@Entity(name = "BaselineOffsetTime")
	@Table(name = "tpb_offset_time")
	public static class BaselineOffsetTime {
		@Id
		public Integer id;
		public OffsetTime theValue;
	}

	@Entity(name = "BaselineOffsetDateTime")
	@Table(name = "tpb_offset_date_time")
	public static class BaselineOffsetDateTime {
		@Id
		public Integer id;
		public OffsetDateTime theValue;
	}

	@Entity(name = "BaselineZonedDateTime")
	@Table(name = "tpb_zoned_date_time")
	public static class BaselineZonedDateTime {
		@Id
		public Integer id;
		public ZonedDateTime theValue;
	}
}
