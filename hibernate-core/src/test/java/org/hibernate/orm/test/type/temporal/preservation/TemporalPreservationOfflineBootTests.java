package org.hibernate.orm.test.type.temporal.preservation;

import org.hibernate.orm.test.type.temporal.preservation.TemporalPreservationBootMatrix.WithLocalTime;
import org.hibernate.type.descriptor.jdbc.LocalTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.sql.spi.StringBuilderSqlAppender;
import org.hibernate.dialect.type.internal.OracleInstantAtUtcJdbcType;
import org.hibernate.dialect.type.internal.OracleLocalTimeJdbcType;
import java.time.LocalTime;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.MappingException;
import org.hibernate.TemporalPreservationException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.PostgresPlusDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.descriptor.jdbc.DateJdbcType;
import org.hibernate.type.descriptor.jdbc.internal.TemporalPreservationGuard;

import org.hibernate.testing.orm.junit.DialectContext;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Tests that mappings are accepted and rejected at boot without a
/// connection to the database: what is supported is a matter of the Dialect
/// alone, and nothing is asked of the driver or the database. Every attempt
/// to open a connection is counted, and each test ends with none, so that an
/// attempt whose failure was caught along the way does not go unnoticed.
///
/// @author Steve Ebersole
class TemporalPreservationOfflineBootTests {
	@BeforeEach
	void forgetConnectionAttempts() {
		NoConnections.ATTEMPTS.set( 0 );
	}

	@AfterEach
	void noConnectionWasAttempted() {
		assertThat( NoConnections.ATTEMPTS.get() ).as( "attempts to open a connection" ).isZero();
	}

	@Test
	@RequiresDialect(H2Dialect.class)
	@RequiresDialect(HSQLDialect.class)
	@RequiresDialect(value = PostgreSQLDialect.class, matchSubTypes = false)
	@RequiresDialect(PostgresPlusDialect.class)
	@RequiresDialect(CockroachDialect.class)
	@RequiresDialect(MySQLDialect.class)
	@RequiresDialect(SQLServerDialect.class)
	@RequiresDialect(OracleDialect.class)
	@RequiresDialect(value = DB2Dialect.class, matchSubTypes = false)
	void supportedMappingIsAcceptedWithoutAConnection() {
		assertThatCode( () -> boot( WithLocalDate.class ) ).doesNotThrowAnyException();
	}

	@Test
	void unsupportedMappingIsRejectedWithoutAConnection() {
		assertThatThrownBy( () -> boot( WithArray.class ) )
				.isInstanceOf( MappingException.class )
				.hasMessageContaining( "WithArray.theValues" )
				.hasMessageContaining( MappingSettings.TEMPORAL_PRESERVATION );
	}

	/// Oracle has no type for a time of day. With temporal preservation
	/// enabled a `LocalTime` is exchanged as a date and time on the first day
	/// of 1970, and a literal is rendered as one; without it nothing changes.
	/// This needs no database, so it is verified whatever the database under test.
	@Test
	@SuppressWarnings("unchecked")
	void localTimeOnOracleIsExchangedAsADateTime() {
		for ( boolean enabled : new boolean[] { true, false } ) {
			final var registryBuilder = ServiceRegistryUtil.serviceRegistryBuilder();
			registryBuilder.applySetting( MappingSettings.TEMPORAL_PRESERVATION, enabled );
			registryBuilder.applySetting( MappingSettings.JAVA_TIME_USE_DIRECT_JDBC, true );
			registryBuilder.applySetting( JdbcSettings.ALLOW_METADATA_ON_BOOT, false );
			registryBuilder.applySetting( JdbcSettings.DIALECT, OracleDialect.class.getName() );
			registryBuilder.applySetting( JdbcSettings.CONNECTION_PROVIDER, new NoConnections() );
			registryBuilder.applySetting( "hibernate.hbm2ddl.auto", "none" );
			try ( var registry = registryBuilder.build();
					var factory = (SessionFactoryImplementor) new MetadataSources( registry )
							.addAnnotatedClass( WithLocalTime.class )
							.buildMetadata()
							.buildSessionFactory() ) {
				final var mapping = factory.getMappingMetamodel()
						.getEntityDescriptor( WithLocalTime.class )
						.findAttributeMapping( "theValue" )
						.getSingleJdbcMapping();
				final var literal = new StringBuilder();
				( (JdbcLiteralFormatter<Object>) mapping.getJdbcLiteralFormatter() ).appendJdbcLiteral(
						new StringBuilderSqlAppender( literal ),
						LocalTime.of( 12, 34, 56 ),
						factory.getJdbcServices().getDialect(),
						factory.getWrapperOptions()
				);
				if ( enabled ) {
					assertThat( mapping.getJdbcType() ).isInstanceOf( OracleLocalTimeJdbcType.class );
					assertThat( literal.toString() ).contains( "1970-01-01 12:34:56" );
				}
				else {
					assertThat( mapping.getJdbcType() ).isExactlyInstanceOf( LocalTimeJdbcType.class );
					assertThat( literal.toString() ).isEqualTo( "{t '12:34:56'}" );
				}
			}
		}
	}

	/// The timestamp of a soft delete is always computed by the database. Its
	/// column on DB2 holds the date and time at UTC, for which the Dialect has
	/// an expression. A Dialect which has none rejects the mapping.
	@Test
	void timestampOfSoftDeleteNeedsAnExpressionForTheTimeAtUtc() {
		assertThat( new DB2Dialect().getCurrentTemporalSupport().currentUtcTimestamp() )
				.isEqualTo( "(current timestamp - current timezone)" );
		assertThat( new OracleDialect().getCurrentTemporalSupport().currentUtcTimestamp() )
				.isEqualTo( "sys_extract_utc(systimestamp)" );
		for ( Class<? extends DB2Dialect> dialectClass : List.of( DB2Dialect.class, Db2WithoutTheTimeAtUtc.class ) ) {
			final var registryBuilder = ServiceRegistryUtil.serviceRegistryBuilder();
			registryBuilder.applySetting( MappingSettings.TEMPORAL_PRESERVATION, true );
			registryBuilder.applySetting( JdbcSettings.ALLOW_METADATA_ON_BOOT, false );
			registryBuilder.applySetting( JdbcSettings.DIALECT, dialectClass.getName() );
			registryBuilder.applySetting( JdbcSettings.CONNECTION_PROVIDER, new NoConnections() );
			registryBuilder.applySetting( "hibernate.hbm2ddl.auto", "none" );
			try ( var registry = registryBuilder.build() ) {
				final var boot = assertThatCode( () -> new MetadataSources( registry )
						.addAnnotatedClass( SoftDeleted.class )
						.buildMetadata()
						.buildSessionFactory()
						.close() );
				if ( dialectClass == DB2Dialect.class ) {
					boot.doesNotThrowAnyException();
				}
				else {
					boot.hasStackTraceContaining( MappingSettings.TEMPORAL_PRESERVATION )
							.hasStackTraceContaining( "has no expression for the current date and time at UTC" );
				}
			}
		}
	}

	/// Compared with an instant, `current_timestamp` stands for the current
	/// instant. The column of an `Instant` on DB2 holds the date and time at
	/// UTC, so a Dialect which has no expression for that rejects the
	/// comparison, and DB2 rejects one with the current date, which cannot be
	/// given such a meaning. This is decided when the query is translated,
	/// before any connection is needed.
	@Test
	void comparisonWithTheCurrentTimestampNeedsAnExpressionForTheTimeAtUtc() {
		final var registryBuilder = ServiceRegistryUtil.serviceRegistryBuilder();
		registryBuilder.applySetting( MappingSettings.TEMPORAL_PRESERVATION, true );
		registryBuilder.applySetting( JdbcSettings.ALLOW_METADATA_ON_BOOT, false );
		registryBuilder.applySetting( JdbcSettings.DIALECT, Db2WithoutTheTimeAtUtc.class.getName() );
		registryBuilder.applySetting( JdbcSettings.CONNECTION_PROVIDER, new NoConnections() );
		registryBuilder.applySetting( "hibernate.hbm2ddl.auto", "none" );
		try ( var registry = registryBuilder.build();
				var factory = (SessionFactoryImplementor) new MetadataSources( registry )
						.addAnnotatedClass( WithInstant.class )
						.buildMetadata()
						.buildSessionFactory();
				var session = factory.openSession() ) {
			for ( String current : new String[] { "current_timestamp", "local datetime", "local datetime - 1 day" } ) {
				assertThatThrownBy( () -> session.createQuery(
						"select e.id from WithInstant e where e.theValue < " + current,
						Integer.class
				).getResultList() )
						.as( current )
						.hasStackTraceContaining( MappingSettings.TEMPORAL_PRESERVATION )
						.hasStackTraceContaining( "has no expression for the current date and time at UTC" );
			}
			assertThatThrownBy( () -> session.createQuery(
					"select e.id from WithInstant e where e.theValue < current_date",
					Integer.class
			).getResultList() )
					.hasStackTraceContaining( "the result would depend on that time zone" );
		}
	}

	/// Oracle does not allow a column with a time zone in a primary key, which
	/// the row start of a temporal table is part of. It is held there as its
	/// date and time at UTC in a plain timestamp, by a descriptor of its own,
	/// while an ordinary `Instant` keeps its column with a time zone.
	@Test
	void rowStartOfATemporalTableOnOracleIsHeldAtUtc() {
		final var registryBuilder = ServiceRegistryUtil.serviceRegistryBuilder();
		registryBuilder.applySetting( MappingSettings.TEMPORAL_PRESERVATION, true );
		registryBuilder.applySetting( JdbcSettings.ALLOW_METADATA_ON_BOOT, false );
		registryBuilder.applySetting( JdbcSettings.DIALECT, OracleDialect.class.getName() );
		registryBuilder.applySetting( JdbcSettings.CONNECTION_PROVIDER, new NoConnections() );
		registryBuilder.applySetting( org.hibernate.cfg.StateManagementSettings.TEMPORAL_TABLE_STRATEGY, "SINGLE_TABLE" );
		registryBuilder.applySetting( "hibernate.hbm2ddl.auto", "none" );
		try ( var registry = registryBuilder.build() ) {
			final var metadata = (org.hibernate.boot.spi.MetadataImplementor) new MetadataSources( registry )
					.addAnnotatedClass( WithHistory.class )
					.buildMetadata();
			try ( var factory = (SessionFactoryImplementor) metadata.buildSessionFactory() ) {
				final var table = metadata.getEntityBinding( WithHistory.class.getName() ).getTable();
				final var rowStart = table.getColumn( org.hibernate.boot.model.naming.Identifier.toIdentifier( "effective_from" ) );
				assertThat( rowStart ).isNotNull();
				assertThat( rowStart.getSqlType( metadata ) ).startsWith( "timestamp" ).doesNotContain( "time zone" );
				final var rowStartValue = (org.hibernate.mapping.BasicValue) rowStart.getValue();
				assertThat( rowStartValue.resolve().getJdbcType() ).isInstanceOf( OracleInstantAtUtcJdbcType.class );
				final var ordinary = factory.getMappingMetamodel()
						.getEntityDescriptor( WithHistory.class )
						.findAttributeMapping( "recordedAt" )
						.getSingleJdbcMapping();
				assertThat( ordinary.getJdbcType() ).isNotInstanceOf( OracleInstantAtUtcJdbcType.class );
			}
		}
	}

	/// On DB2 a `LocalDate` is exchanged as a `java.sql.Date`, which passes
	/// through the default time zone of the JVM. A day which that zone
	/// skipped cannot be written, and the day after it cannot be read. This
	/// needs no database, so it is verified whatever the database under test.
	@Test
	void daySkippedByTheZoneOfTheJvmIsRejectedOnDb2() {
		final var original = TimeZone.getDefault();
		final var registryBuilder = ServiceRegistryUtil.serviceRegistryBuilder();
		registryBuilder.applySetting( MappingSettings.TEMPORAL_PRESERVATION, true );
		registryBuilder.applySetting( JdbcSettings.ALLOW_METADATA_ON_BOOT, false );
		registryBuilder.applySetting( JdbcSettings.DIALECT, DB2Dialect.class.getName() );
		registryBuilder.applySetting( JdbcSettings.CONNECTION_PROVIDER, new NoConnections() );
		registryBuilder.applySetting( "hibernate.hbm2ddl.auto", "none" );
		try ( var registry = registryBuilder.build();
				var factory = (SessionFactoryImplementor) new MetadataSources( registry )
						.addAnnotatedClass( WithLocalDate.class )
						.buildMetadata()
						.buildSessionFactory() ) {
			final var mapping = factory.getMappingMetamodel()
					.getEntityDescriptor( WithLocalDate.class )
					.findAttributeMapping( "theValue" )
					.getSingleJdbcMapping();
			assertThat( mapping.getJdbcType() ).isInstanceOf( DateJdbcType.class );
			final var options = factory.getWrapperOptions();
			final LocalDate skipped = LocalDate.of( 2011, 12, 30 );
			final LocalDate next = skipped.plusDays( 1 );

			// Samoa moved across the date line at the end of 29 December 2011
			TimeZone.setDefault( TimeZone.getTimeZone( "Pacific/Apia" ) );
			assertThatThrownBy( () -> TemporalPreservationGuard.checkBind(
					skipped, mapping.getJdbcJavaType(), mapping.getJdbcType(), options ) )
					.isInstanceOf( TemporalPreservationException.class )
					.hasMessageContaining( "does not exist" )
					.hasMessageContaining( "Pacific/Apia" );
			assertThatThrownBy( () -> TemporalPreservationGuard.checkExtracted(
					next, mapping.getJdbcJavaType(), mapping.getJdbcType(), options ) )
					.isInstanceOf( TemporalPreservationException.class )
					.hasMessageContaining( "cannot be told" );
			// the day after can be written, and the days around them written and read
			assertThatCode( () -> {
				TemporalPreservationGuard.checkBind( next, mapping.getJdbcJavaType(), mapping.getJdbcType(), options );
				TemporalPreservationGuard.checkBind( skipped.minusDays( 1 ), mapping.getJdbcJavaType(), mapping.getJdbcType(), options );
				TemporalPreservationGuard.checkExtracted( skipped.minusDays( 1 ), mapping.getJdbcJavaType(), mapping.getJdbcType(), options );
				TemporalPreservationGuard.checkExtracted( next.plusDays( 1 ), mapping.getJdbcJavaType(), mapping.getJdbcType(), options );
			} ).doesNotThrowAnyException();

			// a zone which skipped no day
			TimeZone.setDefault( TimeZone.getTimeZone( "America/New_York" ) );
			assertThatCode( () -> {
				TemporalPreservationGuard.checkBind( skipped, mapping.getJdbcJavaType(), mapping.getJdbcType(), options );
				TemporalPreservationGuard.checkExtracted( next, mapping.getJdbcJavaType(), mapping.getJdbcType(), options );
			} ).doesNotThrowAnyException();
		}
		finally {
			TimeZone.setDefault( original );
		}
	}

	private static void boot(Class<?> entityClass) {
		final var registryBuilder = ServiceRegistryUtil.serviceRegistryBuilder();
		registryBuilder.applySetting( MappingSettings.TEMPORAL_PRESERVATION, true );
		registryBuilder.applySetting( JdbcSettings.ALLOW_METADATA_ON_BOOT, false );
		registryBuilder.applySetting( JdbcSettings.DIALECT, DialectContext.getDialect().getClass().getName() );
		registryBuilder.applySetting( JdbcSettings.CONNECTION_PROVIDER, new NoConnections() );
		registryBuilder.applySetting( "hibernate.hbm2ddl.auto", "none" );
		try ( var registry = registryBuilder.build() ) {
			final var metadata = new MetadataSources( registry ).addAnnotatedClass( entityClass ).buildMetadata();
			metadata.buildSessionFactory().close();
		}
	}

	@Entity(name = "WithLocalDate")
	static class WithLocalDate {
		@Id
		Integer id;
		LocalDate theValue;
	}

	@Entity(name = "WithInstant")
	static class WithInstant {
		@Id
		Integer id;
		java.time.Instant theValue;
	}

	@org.hibernate.annotations.Temporal(rowStart = "effective_from", rowEnd = "effective_to")
	@Entity(name = "WithHistory")
	static class WithHistory {
		@Id
		Integer id;
		java.time.Instant recordedAt;
	}

	/// DB2Dialect as it was before it had an expression for the current date and time at UTC.
	public static class Db2WithoutTheTimeAtUtc extends DB2Dialect {
		@Override
		public String currentUtcTimestamp() {
			return null;
		}
	}

	@Entity(name = "WithArray")
	static class WithArray {
		@Id
		Integer id;
		LocalDate[] theValues;
	}

	public static class NoConnections implements ConnectionProvider {
		/// How often a connection was asked for, whatever became of the failure.
		static final AtomicInteger ATTEMPTS = new AtomicInteger();

		@Override
		public Connection getConnection() throws SQLException {
			ATTEMPTS.incrementAndGet();
			throw new SQLException( "No connection may be opened by this test" );
		}

		@Override
		public void closeConnection(Connection connection) {
		}

		@Override
		public boolean supportsAggressiveRelease() {
			return false;
		}

		@Override
		public boolean isUnwrappableAs(Class<?> unwrapType) {
			return false;
		}

		@Override
		public <T> T unwrap(Class<T> unwrapType) {
			throw new UnsupportedOperationException();
		}
	}

	@Entity(name = "SoftDeleted")
	@org.hibernate.annotations.SoftDelete(strategy = org.hibernate.annotations.SoftDeleteType.TIMESTAMP)
	static class SoftDeleted {
		@Id
		Integer id;
	}
}
