package org.hibernate.orm.test.mapping.javatime.redesign;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.dialect.SpannerPostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.annotations.Temporal;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.cfg.StateManagementSettings;
import java.util.Map;
import java.time.OffsetDateTime;
import java.time.OffsetTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

import org.hibernate.annotations.CurrentTimestamp;
import org.hibernate.annotations.TemporalPreservationPolicy;
import org.hibernate.annotations.TimeZoneAdjustment;
import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.PostgresPlusDialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.testing.orm.junit.DialectContext;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Unsupported positive fixtures retain explicit, isolated bootstrap rejection coverage.
/// Metadata-disabled controls fail on any attempted connection, including failed bootstraps.
///
/// @author Steve Ebersole
class TemporalEligibilityRejectionTests {
	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void localVersionProducer(boolean metadata) throws SQLException {
		final var dialect = DialectContext.getDialect();
		assumeFalse( new TemporalEligibilityChecks.LocalDateTimeProducer().apply( dialect ) );
		reject( dialect, metadata, LocalVersion.class, "LocalVersion.stamp", "LocalDateTime", "preserv" );
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void normalizedOffsetVersionProducer(boolean metadata) throws SQLException {
		final var dialect = DialectContext.getDialect();
		assumeFalse( new TemporalEligibilityChecks.NormalizedOffsetDateTimeProducer().apply( dialect ) );
		reject( dialect, metadata, OffsetVersion.class, "OffsetVersion.stamp", "OffsetDateTime", "preserv" );
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void inlineOffsetTime(boolean metadata) throws SQLException {
		final var dialect = DialectContext.getDialect();
		assumeFalse( new TemporalEligibilityChecks.InlineOffsetTime().apply( dialect ) );
		reject( dialect, metadata, InlineTime.class, "stamp", "OffsetTimeJavaType", "preservation" );
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void adjustedOffsetTime(boolean metadata) throws SQLException {
		final var dialect = DialectContext.getDialect();
		assumeFalse( new TemporalEligibilityChecks.UtcOffsetTime().apply( dialect ) );
		reject( dialect, metadata, AdjustedTime.class, "stamp", "UtcTimeJdbcType", "preservation" );
		reject( dialect, metadata, TemporalOffsetTimeCollectionTests.Event.class, "OffsetTimeEmbeddable.utcTime", "UtcTimeJdbcType", "preservation" );
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void adjustedDateTime(boolean metadata) throws SQLException {
		final var dialect = DialectContext.getDialect();
		assumeFalse( new TemporalEligibilityChecks.AdjustedOffsetDateTime().apply( dialect ) );
		reject( dialect, metadata, AdjustedDateTime.class, "stamp", "OffsetDateTimeJavaType", "preservation" );
	}

	@Test
	void specificMissingProducersRejectWithoutJdbc() throws SQLException {
		reject( new HSQLDialect(), false, LocalVersion.class,
				"LocalVersion.stamp", "LocalDateTimeJdbcType", "no preserving DB producer" );
		reject( new PostgresPlusDialect(), false, OffsetVersion.class,
				"OffsetVersion.stamp", "PostgreSQLOffsetDateTimeJdbcType", "no preserving DB producer" );
	}

	@Test
	void unknownLocalPairsRejectWithoutMetadata() throws SQLException {
		for ( Dialect dialect : List.of( new DB2Dialect(), new SybaseASEDialect(),
				new SpannerDialect(), new SpannerPostgreSQLDialect() ) ) {
			if ( dialect instanceof DB2Dialect || dialect instanceof SybaseASEDialect ) {
				reject( dialect, false, DateRow.class, "DateRow.stamp", "LocalDateJavaType", "UNKNOWN" );
			}
			else {
				assertThat( new TemporalEligibilityChecks.LocalDateMapping().apply( dialect ) ).isTrue();
			}
			reject( dialect, false, TimeRow.class, "TimeRow.stamp", "LocalTimeJavaType", "UNKNOWN" );
			reject( dialect, false, DateTimeRow.class, "DateTimeRow.stamp", "LocalDateTimeJavaType", "UNKNOWN" );
			reject( dialect, false, ExplicitDateTimeRow.class, "ExplicitDateTimeRow.stamp", "LocalDateTimeJavaType", "UNKNOWN" );
			reject( dialect, false, ExplicitTimeRow.class, "ExplicitTimeRow.stamp", "LocalTimeJavaType", "UNKNOWN" );
		}
	}

	@Test
	void sqlServerNativePeriodRejectsWithoutMetadata() throws SQLException {
		reject( new SQLServerDialect(), false, NativeHistory.class,
				Map.of( StateManagementSettings.TEMPORAL_TABLE_STRATEGY, "NATIVE" ),
				"No preserving system-period profile", "NATIVE" );
	}

	private static void reject(Dialect dialect, boolean metadata, Class<?> entity, String... diagnostic) throws SQLException {
		reject( dialect, metadata, entity, Map.of(), diagnostic );
	}

	private static void reject(Dialect dialect, boolean metadata, Class<?> entity, Map<String, Object> settings, String... diagnostic) throws SQLException {
		final var provider = mock( ConnectionProvider.class );
		when( provider.getConnection() ).thenThrow( new AssertionError( "No bootstrap JDBC access" ) );
		final var builder = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( JdbcSettings.DIALECT, dialect )
				.applySetting( JdbcSettings.ALLOW_METADATA_ON_BOOT, metadata )
				.applySetting( MappingSettings.TEMPORAL_PRESERVATION, true )
				.applySetting( "hibernate.hbm2ddl.auto", "none" )
				.applySettings( settings );
		if ( !metadata ) {
			builder.addService( ConnectionProvider.class, provider );
		}
		try ( var registry = builder.build() ) {
			final var assertion = assertThatThrownBy( () -> {
				try ( var ignored = new MetadataSources( registry ).addAnnotatedClass( entity )
						.buildMetadata().buildSessionFactory() ) {
				}
			} );
			for ( String fragment : diagnostic ) {
				assertion.hasStackTraceContaining( fragment );
			}
		}
		if ( !metadata ) {
			verify( provider, never() ).getConnection();
		}
	}

	@Entity(name = "EligibilityDateRow")
	static class DateRow { @Id int id; LocalDate stamp; }
	@Entity(name = "EligibilityTimeRow")
	static class TimeRow { @Id int id; LocalTime stamp; }
	@Entity(name = "EligibilityDateTimeRow")
	static class DateTimeRow { @Id int id; LocalDateTime stamp; }
	@Entity(name = "EligibilityExplicitTime")
	static class ExplicitTimeRow { @Id int id; @JdbcTypeCode(SqlTypes.LOCAL_TIME) LocalTime stamp; }
	@Entity(name = "EligibilityExplicitDateTime")
	static class ExplicitDateTimeRow { @Id int id; @JdbcTypeCode(SqlTypes.LOCAL_DATE_TIME) LocalDateTime stamp; }
	@Entity(name = "EligibilityNativeHistory")
	@Temporal
	static class NativeHistory { @Id int id; String value; }

	@Entity(name = "EligibilityLocalVersion")
	static class LocalVersion {
		@Id int id;
		@Version @CurrentTimestamp LocalDateTime stamp;
	}

	@Entity(name = "EligibilityOffsetVersion")
	static class OffsetVersion {
		@Id int id;
		@Version @CurrentTimestamp
		@TemporalPreservationPolicy(TemporalPreservationPolicy.Type.NORMALIZED)
		OffsetDateTime stamp;
	}

	@Entity(name = "EligibilityInlineTime")
	static class InlineTime {
		@Id int id;
		OffsetTime stamp;
	}

	@Entity(name = "EligibilityAdjustedTime")
	static class AdjustedTime {
		@Id int id;
		@TimeZoneAdjustment("+18:00") OffsetTime stamp;
	}
	@Entity(name = "EligibilityAdjustedDateTime")
	static class AdjustedDateTime {
		@Id int id;
		@TimeZoneAdjustment("America/New_York") OffsetDateTime stamp;
	}

}
