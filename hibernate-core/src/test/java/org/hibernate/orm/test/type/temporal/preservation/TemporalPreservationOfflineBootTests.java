package org.hibernate.orm.test.type.temporal.preservation;

import org.hibernate.orm.test.type.temporal.preservation.TemporalPreservationBootMatrix.WithLocalTime;
import org.hibernate.type.descriptor.jdbc.LocalTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.sql.spi.StringBuilderSqlAppender;
import org.hibernate.dialect.type.internal.OracleInstantAtUtcJdbcType;
import org.hibernate.dialect.type.internal.OracleLocalTimeJdbcType;
import java.time.LocalTime;
import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import org.hibernate.dialect.DB2iDialect;
import org.hibernate.dialect.DB2zDialect;
import org.hibernate.dialect.type.internal.DB2LocalDateTimeAsTextJdbcType;
import org.hibernate.mapping.BasicValue;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.orm.test.type.temporal.preservation.TemporalPreservationBootMatrix.WithLocalDateTime;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.jdbc.TimestampJdbcType;
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

	/// On DB2 a `LocalDateTime` is exchanged as text which the database
	/// converts, since the driver passes one through the default time zone of
	/// the JVM however it is given. The SQL which does so, and the text itself,
	/// need no database, so they are verified whatever the database under test.
	@Test
	void localDateTimeIsExchangedAsTextOnDb2() throws SQLException {
		final List<String> statements = new ArrayList<>();
		final var registryBuilder = ServiceRegistryUtil.serviceRegistryBuilder();
		registryBuilder.applySetting( MappingSettings.TEMPORAL_PRESERVATION, true );
		registryBuilder.applySetting( JdbcSettings.ALLOW_METADATA_ON_BOOT, false );
		registryBuilder.applySetting( JdbcSettings.DIALECT, DB2Dialect.class.getName() );
		registryBuilder.applySetting( JdbcSettings.CONNECTION_PROVIDER, new NoStatements( statements ) );
		registryBuilder.applySetting( "hibernate.hbm2ddl.auto", "none" );
		try ( var registry = registryBuilder.build();
				var factory = (SessionFactoryImplementor) new MetadataSources( registry )
						.addAnnotatedClass( WithLocalDateTime.class )
						.buildMetadata()
						.buildSessionFactory() ) {
			final var mapping = factory.getMappingMetamodel()
					.getEntityDescriptor( WithLocalDateTime.class )
					.findAttributeMapping( "theValue" )
					.getSingleJdbcMapping();
			assertThat( mapping.getJdbcType() ).isSameAs( DB2LocalDateTimeAsTextJdbcType.INSTANCE );
			assertThat( mapping.getJdbcType().getDdlTypeCode() ).isEqualTo( Types.TIMESTAMP );
			// a value which is not that of a column, such as a parameter of a function, is exchanged the same way
			assertThat( factory.getTypeConfiguration().getBasicTypeForJavaType( LocalDateTime.class ).getJdbcType() )
					.isSameAs( DB2LocalDateTimeAsTextJdbcType.INSTANCE );

			final String written = "cast(cast(? as varchar(40)) as timestamp(12))";
			final String read = "varchar_format(wldt1_0.theValue,'YYYY-MM-DD HH24:MI:SS.FF9')";

			// a query reads the column as text, and compares it with a parameter given as text
			try ( var session = factory.openSession() ) {
				assertThatThrownBy( () -> session.createQuery(
						"select e.theValue from WithLocalDateTime e where e.theValue > :theValue",
						LocalDateTime.class
				).setParameter( "theValue", LocalDateTime.of( 2021, 3, 14, 2, 30 ) ).getResultList() )
						.hasStackTraceContaining( "No statement may be prepared by this test" );
			}
			assertThat( statements ).last().asString()
					.startsWith( "select " + read + " from " )
					.endsWith( "wldt1_0.theValue>" + written );

			// so does loading the entity
			statements.clear();
			try ( var session = factory.openSession() ) {
				assertThatThrownBy( () -> session.find( WithLocalDateTime.class, 1 ) )
						.hasStackTraceContaining( "No statement may be prepared by this test" );
			}
			assertThat( statements ).last().asString().contains( read );

			// and it is written as text
			statements.clear();
			try ( var session = factory.openSession() ) {
				assertThatThrownBy( () -> {
					session.beginTransaction();
					final var entity = new WithLocalDateTime();
					entity.id = 1;
					entity.theValue = LocalDateTime.of( 2021, 3, 14, 2, 30 );
					session.persist( entity );
					session.flush();
				} ).hasStackTraceContaining( "No statement may be prepared by this test" );
			}

			// the text is the same whatever the default time zone of the JVM
			final var original = TimeZone.getDefault();
			try {
				for ( String zone : new String[] { "UTC", "America/New_York", "Asia/Kathmandu" } ) {
					TimeZone.setDefault( TimeZone.getTimeZone( zone ) );
					assertThat( boundText( mapping, LocalDateTime.of( 2021, 3, 14, 2, 30 ), factory ) )
							.as( zone )
							.isEqualTo( "2021-03-14 02:30:00.000000000" );
					assertThat( boundText( mapping, LocalDateTime.of( 1, 1, 1, 0, 0, 0, 1 ), factory ) )
							.as( zone )
							.isEqualTo( "0001-01-01 00:00:00.000000001" );
					assertThat( boundText( mapping, LocalDateTime.of( 1582, 10, 10, 12, 34, 56, 123_456_000 ), factory ) )
							.as( zone )
							.isEqualTo( "1582-10-10 12:34:56.123456000" );
					assertThat( DB2LocalDateTimeAsTextJdbcType.parse( "2021-03-14 02:30:00.000000000" ) )
							.as( zone )
							.isEqualTo( LocalDateTime.of( 2021, 3, 14, 2, 30 ) );
					assertThat( DB2LocalDateTimeAsTextJdbcType.parse( "0001-01-01 00:00:00.000000001" ) )
							.as( zone )
							.isEqualTo( LocalDateTime.of( 1, 1, 1, 0, 0, 0, 1 ) );
				}
			}
			finally {
				TimeZone.setDefault( original );
			}
			// DB2 holds the end of a day as 24:00:00 of that day
			assertThat( DB2LocalDateTimeAsTextJdbcType.parse( "2021-12-31 24:00:00.000000000" ) )
					.isEqualTo( LocalDateTime.of( 2022, 1, 1, 0, 0 ) );
			assertThatThrownBy( () -> DB2LocalDateTimeAsTextJdbcType.parse( "2021-03-14-02.30.00.000000" ) )
					.isInstanceOf( TemporalPreservationException.class );

			// values outside the supported range are rejected as they are bound
			assertThatThrownBy( () -> boundText( mapping, LocalDateTime.of( 10000, 1, 1, 0, 0 ), factory ) )
					.isInstanceOf( TemporalPreservationException.class );
			assertThatThrownBy( () -> boundText( mapping, LocalDateTime.of( 0, 12, 31, 23, 59 ), factory ) )
					.isInstanceOf( TemporalPreservationException.class );
		}
		assertThat( statements ).last().asString()
				.startsWith( "insert into tp_local_date_time " )
				.contains( "cast(cast(? as varchar(40)) as timestamp(12))" );

		// nothing of this applies with temporal preservation disabled, nor on the other DB2 databases
		for ( Class<? extends DB2Dialect> dialectClass : List.of( DB2Dialect.class, DB2zDialect.class, DB2iDialect.class ) ) {
			final var disabled = ServiceRegistryUtil.serviceRegistryBuilder();
			disabled.applySetting( MappingSettings.TEMPORAL_PRESERVATION, dialectClass != DB2Dialect.class );
			disabled.applySetting( JdbcSettings.ALLOW_METADATA_ON_BOOT, false );
			disabled.applySetting( JdbcSettings.DIALECT, dialectClass.getName() );
			disabled.applySetting( JdbcSettings.CONNECTION_PROVIDER, new NoConnections() );
			disabled.applySetting( "hibernate.hbm2ddl.auto", "none" );
			try ( var registry = disabled.build() ) {
				final var metadata = new MetadataSources( registry )
						.addAnnotatedClass( WithLocalDateTime.class )
						.buildMetadata();
				final var value = (BasicValue) metadata.getEntityBinding( WithLocalDateTime.class.getName() )
						.getProperty( "theValue" )
						.getValue();
				assertThat( value.resolve().getJdbcType() ).as( dialectClass.getSimpleName() )
						.isSameAs( TimestampJdbcType.INSTANCE );
			}
		}
	}

	/// The text which the binder of the mapping hands to the JDBC driver for the value.
	private static String boundText(JdbcMapping mapping, LocalDateTime value, SessionFactoryImplementor factory)
			throws SQLException {
		final String[] text = new String[1];
		final var statement = (PreparedStatement) Proxy.newProxyInstance(
				TemporalPreservationOfflineBootTests.class.getClassLoader(),
				new Class<?>[] { PreparedStatement.class },
				(proxy, method, arguments) -> {
					if ( method.getName().equals( "setString" ) ) {
						text[0] = (String) arguments[1];
						return null;
					}
					throw new UnsupportedOperationException( method.getName() );
				}
		);
		@SuppressWarnings("unchecked")
		final var binder = (ValueBinder<Object>) mapping.getJdbcValueBinder();
		binder.bind( statement, value, 1, factory.getWrapperOptions() );
		return text[0];
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

	/// Hands out connections which record the SQL they are asked to prepare, and prepare nothing.
	public static class NoStatements implements ConnectionProvider {
		private final List<String> statements;

		NoStatements(List<String> statements) {
			this.statements = statements;
		}

		@Override
		public Connection getConnection() {
			return (Connection) Proxy.newProxyInstance(
					NoStatements.class.getClassLoader(),
					new Class<?>[] { Connection.class },
					(proxy, method, arguments) -> {
						if ( method.getName().startsWith( "prepare" ) ) {
							statements.add( (String) arguments[0] );
							throw new SQLException( "No statement may be prepared by this test" );
						}
						final Class<?> type = method.getReturnType();
						if ( type == boolean.class ) {
							return method.getName().equals( "getAutoCommit" );
						}
						else if ( type == int.class ) {
							return 0;
						}
						else {
							return null;
						}
					}
			);
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
