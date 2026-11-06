package org.hibernate.orm.test.mapping.javatime.redesign;

import java.sql.Connection;
import java.time.*;

import jakarta.persistence.*;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.TimeZoneRepresentation;
import org.hibernate.boot.MetadataSources;
import org.hibernate.dialect.*;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.type.descriptor.jdbc.TimestampUtcAsPlainJdbcTimestampJdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampUtcAsZonedJdbcTimestampJdbcType;
import org.hibernate.dialect.type.internal.PostgreSQLTimestampUtcAsJdbcTimestampJdbcType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Regression coverage for temporal values written and read through distinct
/// physical database sessions, including the two occurrences of a DST overlap.
/// The normal JUnit run executes both phases with two connections held open together.
///
/// Separate-JVM coverage uses the `temporalCrossEnvironment` Gradle task with
/// `hibernate.test.temporal.cross_phase=write` followed by `read` against the same
/// database. Those phases opt in to sharing committed rows across launches; the
/// standalone entry point avoids the JUnit launcher's schema cleanup between them.
/// The reader checks persisted process/JVM identity before verifying and removing
/// the rows. See README.adoc for the launch options and timezone axes.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
@RequiresDialect(PostgreSQLDialect.class)
@RequiresDialect(OracleDialect.class)
class TemporalCrossEnvironmentTests {
	public static void main(String[] args) throws Exception {
		new TemporalCrossEnvironmentTests().preservesAcrossEnvironments();
	}
	@Test
	void preservesAcrossEnvironments() throws Exception {
		final String phase = System.getProperty( "hibernate.test.temporal.cross_phase", "distinct" );
		assertThat( phase ).isIn( "write", "read", "distinct" );
		final var builder = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( "hibernate.type.java_time_use_direct_jdbc", true )
				.applySetting( "hibernate.cache.use_second_level_cache", false )
				.applySetting( "hibernate.hbm2ddl.halt_on_error", true )
				.applySetting( "hibernate.hbm2ddl.auto", phase.equals( "read" ) ? "validate" : phase.equals( "write" ) ? "create" : "create-drop" );
		final String url = System.getProperty( "hibernate.test.temporal.cross_url" );
		if ( url != null ) {
			builder.applySetting( "hibernate.connection.url", url );
		}
		try ( var registry = builder.build() ) {
			final var dialect = registry.requireService( JdbcServices.class ).getDialect();
			final Class<? extends ZonedCarrier> carrier = dialect instanceof PostgreSQLDialect ? PgCarrier.class : dialect instanceof OracleDialect ? OracleCarrier.class : StandardCarrier.class;
			try ( var factory = new MetadataSources( registry ).addAnnotatedClass( Sample.class )
					.addAnnotatedClass( carrier ).getMetadataBuilder()
					.applyTypes( (contributions, services) -> {
						if ( dialect instanceof OracleDialect ) contributions.getTypeConfiguration().getJdbcTypeRegistry().addDescriptor( 881203, new org.hibernate.dialect.type.internal.OracleTimestampUtcAsJdbcTimestampJdbcType( services.requireService( org.hibernate.boot.registry.classloading.spi.ClassLoaderService.class ) ) );
					} ).build().buildSessionFactory() ) {
				final var provider = registry.requireService( ConnectionProvider.class );
				final Connection writer = provider.getConnection();
				final Connection reader = provider.getConnection();
				try {
					final String writerId = identity( writer, dialect );
					final String readerId = identity( reader, dialect );
					assertThat( readerId ).isNotEqualTo( writerId );
					System.out.println( "PHYSICAL_SESSIONS writer=" + writerId + " reader=" + readerId
							+ " process=" + ProcessHandle.current().pid() + " jvm=" + ZoneId.systemDefault() + " phase=" + phase );
					if ( !phase.equals( "read" ) ) {
						write( factory, writer, carrier );
					}
					if ( !phase.equals( "write" ) ) {
						read( factory, reader, carrier, phase.equals( "read" ) );
					}
				}
				finally {
					provider.closeConnection( reader );
					provider.closeConnection( writer );
				}
				if ( phase.equals( "read" ) ) {
					factory.getSchemaManager().drop( false );
				}
			}
		}
	}

	private static String identity(Connection connection, Dialect dialect) throws Exception {
		final String sql = dialect instanceof PostgreSQLDialect ? "select pg_backend_pid()"
				: dialect instanceof OracleDialect ? "select sys_context('USERENV','SID') from dual" : "select session_id()";
		try ( var statement = connection.createStatement(); var result = statement.executeQuery( sql ) ) {
			assertThat( result.next() ).isTrue();
			return result.getString( 1 );
		}
	}

	private static void write(SessionFactory factory, Connection connection, Class<? extends ZonedCarrier> carrier) throws Exception {
		try ( var session = factory.withOptions().connection( connection ).openSession() ) {
			final var tx = session.beginTransaction();
			TemporalMatrixEnvironment.prepareSession( session, true );
			for ( int id = 1; id <= 2; id++ ) {
				final var sample = new Sample();
				sample.id = id;
				sample.process = ProcessHandle.current().pid();
				sample.jvm = ZoneId.systemDefault().getId();
				sample.localDate = LocalDate.of( 2024, 2, 29 );
				sample.localTime = LocalTime.parse( "23:59:58.123456" );
				sample.localDateTime = LocalDateTime.of( sample.localDate, sample.localTime );
				sample.zoned = ZonedDateTime.ofLocal( LocalDateTime.parse( "2024-11-03T01:30:00.123456" ), ZoneId.of( "America/New_York" ), ZoneOffset.ofHours( id == 1 ? -4 : -5 ) );
				sample.offset = sample.zoned.toOffsetDateTime();
				sample.instant = sample.zoned.toInstant();
				sample.plainFallback = sample.instant;
				session.persist( sample );
				final var zoned = carrier.getDeclaredConstructor().newInstance();
				zoned.id = id;
				zoned.value( sample.instant );
				session.persist( zoned );
			}
			tx.commit();
		}
	}

	private static void read(SessionFactory factory, Connection connection, Class<? extends ZonedCarrier> carrier, boolean crossProcess) {
		try ( var session = factory.withOptions().connection( connection ).openSession() ) {
			final var tx = session.beginTransaction();
			TemporalMatrixEnvironment.prepareSession( session, false );
			for ( int id = 1; id <= 2; id++ ) {
				final var actual = session.find( Sample.class, id );
				assertThat( actual ).isNotNull();
				if ( crossProcess ) {
					assertThat( actual.process ).isNotEqualTo( ProcessHandle.current().pid() );
					assertThat( actual.jvm ).isNotEqualTo( ZoneId.systemDefault().getId() );
				}
				final var expected = ZonedDateTime.ofLocal( LocalDateTime.parse( "2024-11-03T01:30:00.123456" ), ZoneId.of( "America/New_York" ), ZoneOffset.ofHours( id == 1 ? -4 : -5 ) );
				assertThat( actual.localDate ).isEqualTo( LocalDate.of( 2024, 2, 29 ) );
				assertThat( actual.localTime ).isEqualTo( LocalTime.parse( "23:59:58.123456" ) );
				assertThat( actual.localDateTime ).isEqualTo( LocalDateTime.parse( "2024-02-29T23:59:58.123456" ) );
				assertThat( actual.offset ).isEqualTo( expected.toOffsetDateTime() );
				assertThat( actual.zoned ).isEqualTo( expected.toOffsetDateTime().toZonedDateTime() );
				assertThat( actual.offset.getOffset() ).isEqualTo( expected.getOffset() );
				assertThat( actual.offset.toLocalDateTime() ).isEqualTo( expected.toLocalDateTime() );
				assertThat( actual.zoned.getOffset() ).isEqualTo( expected.getOffset() );
				assertThat( actual.zoned.getZone() ).isEqualTo( expected.getOffset() );
				assertThat( actual.zoned.toLocalDateTime() ).isEqualTo( expected.toLocalDateTime() );
				assertThat( actual.instant ).isEqualTo( expected.toInstant() );
				assertThat( actual.plainFallback ).isEqualTo( expected.toInstant() );
				final var zoned = session.find( carrier, id );
				assertThat( zoned.value() ).isEqualTo( expected.toInstant() );
				session.remove( zoned );
				session.remove( actual );
			}
			tx.commit();
		}
	}

	@Entity(name = "CrossEnvironmentSample")
	@Table(name = "temporal_cross_env")
	static class Sample {
		@Id int id;
		long process;
		String jvm;
		@Column(name = "local_date_value") LocalDate localDate;
		@Column(name = "local_time_value", secondPrecision = 6) LocalTime localTime;
		@Column(name = "local_datetime_value", secondPrecision = 6) LocalDateTime localDateTime;
		@Column(secondPrecision = 6) Instant instant;
		@JdbcType(TimestampUtcAsPlainJdbcTimestampJdbcType.class)
		@Column(secondPrecision = 6) Instant plainFallback;
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		@Column(name = "offset_value", secondPrecision = 6) OffsetDateTime offset;
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		@Column(secondPrecision = 6) ZonedDateTime zoned;
	}

	@MappedSuperclass
	static abstract class ZonedCarrier {
		@Id int id;
		abstract Instant value();
		abstract void value(Instant value);
	}

	@Entity(name = "CrossEnvironmentStandardCarrier")
	@Table(name = "temporal_cross_zoned")
	static class StandardCarrier extends ZonedCarrier {
		@JdbcType(TimestampUtcAsZonedJdbcTimestampJdbcType.class)
		@Column(secondPrecision = 6) Instant instantValue;
		Instant value() { return instantValue; }
		void value(Instant value) { instantValue = value; }
	}

	@Entity(name = "CrossEnvironmentOracleCarrier")
	@Table(name = "temporal_cross_zoned")
	static class OracleCarrier extends ZonedCarrier {
		@org.hibernate.annotations.JdbcTypeCode(881203)
		@Column(secondPrecision = 6) Instant instantValue;
		Instant value() { return instantValue; }
		void value(Instant value) { instantValue = value; }
	}

	@Entity(name = "CrossEnvironmentPgCarrier")
	@Table(name = "temporal_cross_zoned")
	static class PgCarrier extends ZonedCarrier {
		@JdbcType(PostgreSQLTimestampUtcAsJdbcTimestampJdbcType.class)
		@Column(secondPrecision = 6) Instant instantValue;
		Instant value() { return instantValue; }
		void value(Instant value) { instantValue = value; }
	}
}
