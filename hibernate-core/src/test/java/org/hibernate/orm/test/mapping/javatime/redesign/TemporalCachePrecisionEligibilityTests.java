package org.hibernate.orm.test.mapping.javatime.redesign;

import java.sql.Connection;
import java.util.stream.Stream;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.junit.jupiter.api.Test;
import org.hibernate.dialect.SpannerPostgreSQLDialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

/// Offline controls distinguish precision-zero COLUMN storage from fixed-precision
/// Spanner storage without acquiring a JDBC connection or skipping broad cache coverage.
///
/// @author Steve Ebersole
class TemporalCachePrecisionEligibilityTests {
	static Stream<Arguments> dialects() {
		return Stream.of(
				Arguments.of( H2Dialect.class, true ),
				Arguments.of( HSQLDialect.class, true ),
				Arguments.of( SybaseASEDialect.class, false ),
				Arguments.of( SpannerDialect.class, false ),
				Arguments.of( SpannerPostgreSQLDialect.class, false )
		);
	}

	@Test
	void changingFixedTypeNamesDoesNotImplementFractionalPrecision() {
		assertThat( TemporalCachePrecisionSupport.rendersFractionalPrecision( "bigdatetime", "datetime" ) ).isFalse();
		assertThat( TemporalCachePrecisionSupport.rendersFractionalPrecision( "timestamp(0)", "datetime(3)" ) ).isFalse();
		assertThat( TemporalCachePrecisionSupport.rendersFractionalPrecision( "timestamp(0)", "timestamp(3)" ) ).isTrue();
	}

	@ParameterizedTest
	@MethodSource("dialects")
	void resolvedPrimaryPrecision(Class<? extends Dialect> dialect, boolean expected) {
		final var guard = new NoConnections();
		try ( var registry = new StandardServiceRegistryBuilder()
				.applySetting( "hibernate.dialect", dialect.getName() )
				.applySetting( "hibernate.boot.allow_jdbc_metadata_access", false )
				.applySetting( MappingSettings.TEMPORAL_PRESERVATION, true )
				.applySetting( "hibernate.cache.use_second_level_cache", false )
				.addService( ConnectionProvider.class, guard )
				.build() ) {
			final var metadata = (MetadataImplementor) new MetadataSources( registry )
					.addAnnotatedClass( TemporalCachePrecisionTests.Event.class )
					.buildMetadata();
			assertThat( TemporalCachePrecisionSupport.inspect( metadata, TemporalCachePrecisionTests.Event.class )
					.supported() ).isEqualTo( expected );
		}
		finally {
			assertThat( guard.attempts ).as( "offline eligibility connection acquisitions" ).isZero();
		}
	}

	private static final class NoConnections implements ConnectionProvider {
		private int attempts;

		@Override
		public Connection getConnection() {
			attempts++;
			throw new AssertionError( "Precision eligibility must not acquire a connection" );
		}

		@Override
		public void closeConnection(Connection connection) {
			throw new AssertionError( "No connection should exist" );
		}

		@Override
		public boolean supportsAggressiveRelease() {
			return false;
		}

		@Override
		public boolean isUnwrappableAs(Class<?> type) {
			return type.isInstance( this );
		}

		@Override
		public <T> T unwrap(Class<T> type) {
			return type.cast( this );
		}
	}
}
