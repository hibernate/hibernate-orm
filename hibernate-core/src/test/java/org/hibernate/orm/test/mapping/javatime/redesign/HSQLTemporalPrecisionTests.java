package org.hibernate.orm.test.mapping.javatime.redesign;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZoneId;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.BasicValue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.type.descriptor.java.InstantJavaType;
import org.hibernate.type.descriptor.java.LocalDateTimeJavaType;
import org.hibernate.type.descriptor.jdbc.LocalDateTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampUtcAsPlainJdbcTimestampJdbcType;
import org.hibernate.type.internal.TemporalPrecisionBasicType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Independent fixed-point evidence for HSQL's typed local and UTC Calendar routes.
/// Run with the hsqldb profile; JVM timezone is selected before driver initialization
/// using temporalTestJvmTimeZone. Session offsets are deliberately asymmetric.
/// Expected values use integer decimal grids, never the production quantizer.
///
/// @author Steve Ebersole
@RequiresDialect(HSQLDialect.class)
@ServiceRegistry(settings = {
		@Setting(name = MappingSettings.TEMPORAL_PRESERVATION, value = "true"),
		@Setting(name = "hibernate.cache.use_second_level_cache", value = "true"),
		@Setting(name = "hibernate.jdbc.time_zone", value = "Asia/Kathmandu")
})
@DomainModel(annotatedClasses = HSQLTemporalPrecisionTests.Event.class)
@SessionFactory(generateStatistics = true)
class HSQLTemporalPrecisionTests {
	private static final LocalDateTime INPUT = LocalDateTime.parse( "1969-12-31T23:59:59.123456789" );
	private static final List<String> PROPERTIES = List.of( "automatic", "local0", "local3", "local6", "local9",
			"instant0", "instant3", "instant6", "instant9" );

	@Test
	void metadataDisabledSelectsTheSamePlans(SessionFactoryScope scope) {
		final var guard = new TemporalPrecisionBootstrapTests.NoConnections();
		final var live = scope.getMetadataImplementor();
		try ( var registry = new StandardServiceRegistryBuilder()
				.applySetting( "hibernate.dialect", new HSQLDialect( DatabaseVersion.make( 2, 7 ) ) )
				.applySetting( "hibernate.boot.allow_jdbc_metadata_access", false )
				.applySetting( MappingSettings.TEMPORAL_PRESERVATION, true )
				.applySetting( "hibernate.cache.use_second_level_cache", false )
				.addService( ConnectionProvider.class, guard ).build() ) {
			final var offline = new MetadataSources( registry ).addAnnotatedClass( Event.class ).buildMetadata();
			for ( String name : PROPERTIES ) {
				final var first = (BasicValue) live.getEntityBinding( Event.class.getName() ).getProperty( name ).getValue();
				final var second = (BasicValue) offline.getEntityBinding( Event.class.getName() ).getProperty( name ).getValue();
				assertThat( TemporalPrecisionBasicType.find( first.resolve().getJdbcMapping() ).getPrecisionPlan().getSignature() )
						.isEqualTo( TemporalPrecisionBasicType.find( second.resolve().getJdbcMapping() ).getPrecisionPlan().getSignature() );
			}
			try ( var factory = offline.buildSessionFactory() ) {
				assertThat( factory.getSessionFactoryOptions().isTemporalPreservationEnabled() ).isTrue();
			}
			assertThat( guard.attempts ).hasValue( 0 );
		}
	}

	@Test
	void profileRejectsUnestablishedCalendarRange(SessionFactoryScope scope) {
		final var entity = scope.getMetadataImplementor().getEntityBinding( Event.class.getName() );
		for ( String property : List.of( "local3", "instant3" ) ) {
			final var value = (BasicValue) entity.getProperty( property ).getValue();
			final var plan = TemporalPrecisionBasicType.find( value.resolve().getJdbcMapping() ).getPrecisionPlan();
			for ( LocalDateTime outside : List.of( LocalDateTime.of( 1, 1, 1, 0, 0 ),
					LocalDateTime.of( 1582, 10, 14, 23, 59 ),
					LocalDateTime.of( 10000, 1, 1, 0, 0 ) ) ) {
				final Object input = property.startsWith( "local" ) ? outside : outside.toInstant( ZoneOffset.UTC );
				assertThatThrownBy( () -> plan.prepare( input ) ).hasMessageContaining( "1582-10-15..9999-12-31" );
				assertThatThrownBy( () -> plan.validateExtracted( input ) ).hasMessageContaining( "1582-10-15..9999-12-31" );
			}
		}
	}

	@Test
	void mappedJdbcPreparedValuesAreFixedPoints(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.doWork( connection -> {
			final var options = (SharedSessionContractImplementor) session;
			final var local = LocalDateTimeJdbcType.INSTANCE;
			final var utc = TimestampUtcAsPlainJdbcTimestampJdbcType.INSTANCE;
			System.out.println( "HSQL_PRECISION server=" + connection.getMetaData().getDatabaseProductVersion()
					+ " driver=" + connection.getMetaData().getDriverVersion() + " JVM=" + ZoneId.systemDefault()
					+ " JDBC=" + options.getJdbcTimeZone() );
			for ( int p : new int[] { 0, 3, 6, 9 } ) {
				try ( var statement = connection.createStatement() ) {
					statement.execute( "create table precision_probe (local_value timestamp(" + p + "), instant_value timestamp(" + p + "))" );
				}
				try ( var insert = connection.prepareStatement( "insert into precision_probe values (?, ?)" );
						var statement = connection.createStatement() ) {
					for ( String zone : List.of( "+00:00", "-08:00", "+05:45" ) ) {
						statement.execute( "set time zone interval '" + zone + "' hour to minute" );
						for ( String text : List.of( "1969-12-31T23:59:59.999999500", "2026-12-31T23:59:59.999499999",
								"2026-12-31T23:59:59.999500000", "2026-12-31T23:59:59.999500001",
								"2026-12-31T23:59:59.123456789", "1582-10-15T00:00:00", "9999-12-31T23:59:59.999999999" ) ) {
							final var source = LocalDateTime.parse( text );
							final var expected = truncate( source, p );
							// Raw typed insertion characterizes provider reduction separately.
							local.getBinder( LocalDateTimeJavaType.INSTANCE ).bind( insert, source, 1, options );
							utc.getBinder( InstantJavaType.INSTANCE ).bind( insert, source.toInstant( ZoneOffset.UTC ), 2, options );
							insert.executeUpdate();
							try ( var rows = statement.executeQuery( "select * from precision_probe" ) ) {
								assertThat( rows.next() ).isTrue();
								assertThat( local.getExtractor( LocalDateTimeJavaType.INSTANCE ).extract( rows, 1, options ) ).isEqualTo( expected );
								assertThat( utc.getExtractor( InstantJavaType.INSTANCE ).extract( rows, 2, options ) ).isEqualTo( expected.toInstant( ZoneOffset.UTC ) );
							}
							statement.executeUpdate( "delete from precision_probe" );
							local.getBinder( LocalDateTimeJavaType.INSTANCE ).bind( insert, expected, 1, options );
							utc.getBinder( InstantJavaType.INSTANCE ).bind( insert, expected.toInstant( ZoneOffset.UTC ), 2, options );
							insert.executeUpdate();
							// Extract in a different session zone; UTC Calendar must own Instant interpretation.
							statement.execute( "set time zone interval '+09:00' hour to minute" );
							try ( var rows = statement.executeQuery( "select * from precision_probe" ) ) {
								assertThat( rows.next() ).isTrue();
								assertThat( local.getExtractor( LocalDateTimeJavaType.INSTANCE ).extract( rows, 1, options ) ).isEqualTo( expected );
								assertThat( utc.getExtractor( InstantJavaType.INSTANCE ).extract( rows, 2, options ) ).isEqualTo( expected.toInstant( ZoneOffset.UTC ) );
							}
							statement.executeUpdate( "delete from precision_probe" );
							statement.execute( "set time zone interval '" + zone + "' hour to minute" );
						}
						local.getBinder( LocalDateTimeJavaType.INSTANCE ).bind( insert, null, 1, options );
						utc.getBinder( InstantJavaType.INSTANCE ).bind( insert, null, 2, options );
						insert.executeUpdate();
						try ( var rows = statement.executeQuery( "select * from precision_probe" ) ) {
							assertThat( rows.next() ).isTrue();
							assertThat( local.getExtractor( LocalDateTimeJavaType.INSTANCE ).extract( rows, 1, options ) ).isNull();
							assertThat( utc.getExtractor( InstantJavaType.INSTANCE ).extract( rows, 2, options ) ).isNull();
						}
						statement.executeUpdate( "delete from precision_probe" );
					}
				}
				finally {
					try ( var statement = connection.createStatement() ) { statement.execute( "drop table precision_probe" ); }
				}
			}
		} ) );
	}

	@Test
	void cacheDatabaseAndDirtyCheckingAgree(SessionFactoryScope scope) {
		final var row = new Event(); row.id = 1;
		row.automatic = row.local0 = row.local3 = row.local6 = row.local9 = INPUT;
		row.instant0 = row.instant3 = row.instant6 = row.instant9 = INPUT.toInstant( ZoneOffset.UTC );
		final var factory = scope.getSessionFactory();
		try {
			scope.inTransaction( session -> {
				session.persist( row ); session.flush();
				assertThat( row.local3 ).isEqualTo( INPUT );
				assertThat( row.instant3 ).isEqualTo( INPUT.toInstant( ZoneOffset.UTC ) );
				factory.getStatistics().clear();
				row.local3 = INPUT.withNano( 123_999_999 );
				row.instant3 = row.local3.toInstant( ZoneOffset.UTC );
				session.flush();
				assertThat( factory.getStatistics().getEntityUpdateCount() ).isZero();
			} );
			factory.getStatistics().clear();
			scope.inTransaction( session -> assertCanonical( session.find( Event.class, 1 ) ) );
			assertThat( factory.getStatistics().getSecondLevelCacheHitCount() ).isEqualTo( 1 );
			factory.getCache().evictEntityData( Event.class );
			factory.getStatistics().clear();
			scope.inTransaction( session -> assertCanonical( session.find( Event.class, 1 ) ) );
			assertThat( factory.getStatistics().getEntityLoadCount() ).isEqualTo( 1 );
			scope.inTransaction( session -> {
				final var loaded = session.find( Event.class, 1 );
				loaded.local3 = INPUT.withNano( 124_000_000 );
				factory.getStatistics().clear(); session.flush();
				assertThat( factory.getStatistics().getEntityUpdateCount() ).isEqualTo( 1 );
			} );
		}
		finally { scope.inTransaction( session -> session.remove( session.find( Event.class, 1 ) ) ); }
	}

	private static void assertCanonical(Event row) {
		assertThat( row.automatic ).isEqualTo( truncate( INPUT, 6 ) );
		assertThat( row.local0 ).isEqualTo( truncate( INPUT, 0 ) );
		assertThat( row.local3 ).isEqualTo( truncate( INPUT, 3 ) );
		assertThat( row.local6 ).isEqualTo( truncate( INPUT, 6 ) );
		assertThat( row.local9 ).isEqualTo( INPUT );
		assertThat( row.instant0 ).isEqualTo( truncate( INPUT, 0 ).toInstant( ZoneOffset.UTC ) );
		assertThat( row.instant3 ).isEqualTo( truncate( INPUT, 3 ).toInstant( ZoneOffset.UTC ) );
		assertThat( row.instant6 ).isEqualTo( truncate( INPUT, 6 ).toInstant( ZoneOffset.UTC ) );
		assertThat( row.instant9 ).isEqualTo( INPUT.toInstant( ZoneOffset.UTC ) );
	}

	private static LocalDateTime truncate(LocalDateTime value, int p) {
		int quantum = 1;
		for ( int i = p; i < 9; i++ ) { quantum *= 10; }
		return value.withNano( value.getNano() / quantum * quantum );
	}

	@Entity(name = "HsqlPrecisionEvent")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	static class Event {
		@Id int id;
		LocalDateTime automatic;
		@Column(secondPrecision = 0) LocalDateTime local0;
		@Column(secondPrecision = 3) LocalDateTime local3;
		@Column(secondPrecision = 6) LocalDateTime local6;
		@Column(secondPrecision = 9) LocalDateTime local9;
		@Column(secondPrecision = 0) Instant instant0;
		@Column(secondPrecision = 3) Instant instant3;
		@Column(secondPrecision = 6) Instant instant6;
		@Column(secondPrecision = 9) Instant instant9;
	}
}
