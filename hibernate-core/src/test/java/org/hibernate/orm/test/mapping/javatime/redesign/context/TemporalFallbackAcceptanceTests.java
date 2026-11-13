package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Map;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.type.spi.DirectJavaTimeJdbcSupport;
import org.hibernate.dialect.type.spi.DirectJavaTimeJdbcSupports;
import org.hibernate.mapping.BasicValue;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.LocalDateJdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampUtcAsPlainJdbcTimestampJdbcType;

import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Selection is distinct from acceptance: unknown or lossy pairs reject even when
/// a recommendation is available. The positive fallback is an explicit, established
/// plain UTC protocol, not a claim about automatic selection by every Dialect.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
class TemporalFallbackAcceptanceTests {
	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void supportedDirectPairAndIndependentFactories(boolean metadata) {
		try ( var scope = ContextScope.boot( metadata, true,
				Map.of( MappingSettings.TEMPORAL_PRESERVATION, true ), DirectDate.class ) ) {
			assertThat( scope.factory.getSessionFactoryOptions().isTemporalPreservationEnabled() ).isTrue();
			final var value = (BasicValue) scope.metadata.getEntityBinding( DirectDate.class.getName() )
					.getProperty( "stamp" ).getValue();
			assertThat( value.resolve().getJdbcType() ).isInstanceOf( LocalDateJdbcType.class );
		}
		try ( var scope = ContextScope.boot( metadata, false,
				Map.of( MappingSettings.TEMPORAL_PRESERVATION, false ), DirectDate.class ) ) {
			assertThat( scope.factory.getSessionFactoryOptions().isTemporalPreservationEnabled() ).isFalse();
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void unsupportedExplicitAndInferredMappingsReject(boolean metadata) {
		for ( Class<?> entity : new Class<?>[] { ExplicitDate.class, ExplicitInstant.class, DirectDate.class, NativeZoned.class, NativeOffset.class, NativeOffsetTime.class } ) {
			assertThatThrownBy( () -> {
				try ( var ignored = ContextScope.boot( metadata, true, Map.of(
						MappingSettings.TEMPORAL_PRESERVATION, true,
						"hibernate.dialect", NoDirectDialect.class ), entity ) ) { }
			} ).isInstanceOf( org.hibernate.MappingException.class )
					.hasMessageContaining( "stamp" ).hasMessageContaining( "UNKNOWN" );
		}
		assertThatThrownBy( () -> {
			try ( var ignored = ContextScope.boot( metadata, true, Map.of(
					MappingSettings.TEMPORAL_PRESERVATION, true ), NativeZoned.class ) ) { }
		} ).isInstanceOf( org.hibernate.MappingException.class )
				.hasMessageContaining( "ZONE_ID" ).hasMessageContaining( "UNSUPPORTED" );
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void contributedFallbackExecutesWithJdbcTimezoneMismatch(boolean metadata) {
		final var input = Instant.parse( "2024-11-03T06:30:00.123456Z" );
		try ( var scope = ContextScope.open( metadata, Map.of(
				MappingSettings.TEMPORAL_PRESERVATION, true,
				"hibernate.dialect", NoDirectDialect.class,
				"hibernate.jdbc.time_zone", "Asia/Tokyo" ), FallbackInstant.class ) ) {
			final var value = (BasicValue) scope.metadata.getEntityBinding( FallbackInstant.class.getName() )
					.getProperty( "stamp" ).getValue();
			assertThat( value.resolve().getJdbcType() ).isExactlyInstanceOf( TimestampUtcAsPlainJdbcTimestampJdbcType.class );
			scope.tx( session -> {
				final var row = new FallbackInstant(); row.id = 1; row.stamp = input; session.persist( row );
				final var empty = new FallbackInstant(); empty.id = 2; session.persist( empty );
			} );
			scope.tx( session -> {
				assertThat( session.find( FallbackInstant.class, 1 ).stamp ).isEqualTo( input );
				assertThat( session.find( FallbackInstant.class, 2 ).stamp ).isNull();
				assertThat( session.createSelectionQuery( "from FallbackInstant where stamp = :stamp", FallbackInstant.class )
						.setParameter( "stamp", input ).getSingleResult().id ).isEqualTo( 1 );
			} );
		}
	}

	public static class NoDirectDialect extends H2Dialect {
		@Override
		public DirectJavaTimeJdbcSupport getDirectJavaTimeJdbcSupport() { return DirectJavaTimeJdbcSupports.none(); }
	}

	@Entity(name = "FallbackDirectDate")
	static class DirectDate { @Id int id; LocalDate stamp; }
	@Entity(name = "FallbackExplicitDate")
	static class ExplicitDate { @Id int id; @JdbcTypeCode(SqlTypes.LOCAL_DATE) LocalDate stamp; }
	@Entity(name = "FallbackExplicitInstant")
	static class ExplicitInstant { @Id int id; @JdbcTypeCode(SqlTypes.INSTANT) Instant stamp; }
	@Entity(name = "FallbackNativeZoned")
	static class NativeZoned { @Id int id; ZonedDateTime stamp; }
	@Entity(name = "FallbackNativeOffset")
	static class NativeOffset { @Id int id; java.time.OffsetDateTime stamp; }
	@Entity(name = "FallbackNativeOffsetTime")
	static class NativeOffsetTime { @Id int id; java.time.OffsetTime stamp; }
	@Entity(name = "FallbackInstant")
	static class FallbackInstant {
		@Id int id;
		@JdbcType(TimestampUtcAsPlainJdbcTimestampJdbcType.class) Instant stamp;
	}
}
