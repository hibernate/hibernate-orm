package org.hibernate.orm.test.mapping.javatime.redesign;

import java.sql.SQLException;
import java.time.Instant;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.dialect.jdbc.spi.SybaseDriverKind;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.type.descriptor.java.InstantJavaType;
import org.hibernate.type.descriptor.java.LocalDateTimeJavaType;
import org.hibernate.type.descriptor.jdbc.TimestampUtcAsJdbcTimestampJdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampUtcAsPlainJdbcTimestampJdbcType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Test expectation eligibility resolves actual contributions without boot JDBC.
///
/// @author Steve Ebersole
class SybaseCachePrecisionSupportTests {
	private static SybaseASEDialect dialect(SybaseDriverKind kind) {
		return new SybaseASEDialect() {
			@Override public SybaseDriverKind getDriverKind() { return kind; }
		};
	}

	@Test
	void rejectsOtherDriversStoragePrecisionAndDescriptors() {
		final var java = InstantJavaType.INSTANCE;
		final var jdbc = TimestampUtcAsPlainJdbcTimestampJdbcType.INSTANCE;
		final var jconnect = dialect( SybaseDriverKind.JCONNECT );
		assertThat( SybaseCachePrecisionSupport.observedPath( jconnect, java, jdbc, "bigdatetime", 6 ) ).isTrue();
		for ( Dialect other : new Dialect[] { new H2Dialect(), dialect( SybaseDriverKind.JTDS ), dialect( SybaseDriverKind.OTHER ) } ) {
			assertThat( SybaseCachePrecisionSupport.observedPath( other, java, jdbc, "bigdatetime", 6 ) ).isFalse();
		}
		assertThat( SybaseCachePrecisionSupport.observedPath( jconnect, java, jdbc, "datetime", 6 ) ).isFalse();
		assertThat( SybaseCachePrecisionSupport.observedPath( jconnect, java, jdbc, "bigdatetime", 3 ) ).isFalse();
		assertThat( SybaseCachePrecisionSupport.observedPath( jconnect, LocalDateTimeJavaType.INSTANCE, jdbc, "bigdatetime", 6 ) ).isFalse();
		assertThat( SybaseCachePrecisionSupport.observedPath( jconnect, java, TimestampUtcAsJdbcTimestampJdbcType.INSTANCE, "bigdatetime", 6 ) ).isFalse();
		assertThat( SybaseCachePrecisionSupport.observedPath( jconnect, new InstantJavaType() {}, jdbc, "bigdatetime", 6 ) ).isFalse();
	}

	@Test
	void bothPrimaryMappingsResolveOffline() throws SQLException {
		final var guard = mock( ConnectionProvider.class );
		when( guard.getConnection() ).thenThrow( new AssertionError( "Metadata inspection attempted JDBC" ) );
		try ( var registry = new StandardServiceRegistryBuilder()
				.applySetting( JdbcSettings.DIALECT, dialect( SybaseDriverKind.JCONNECT ) )
				.applySetting( JdbcSettings.ALLOW_METADATA_ON_BOOT, false )
				.applySetting( MappingSettings.TEMPORAL_PRESERVATION, true )
				.applySetting( "hibernate.cache.use_second_level_cache", false )
				.addService( ConnectionProvider.class, guard ).build() ) {
			final var metadata = (MetadataImplementor) new MetadataSources( registry )
					.addAnnotatedClass( TemporalCacheTests.Event.class ).buildMetadata();
			assertThat( SybaseCachePrecisionSupport.databaseValue( metadata, TemporalCacheTests.Event.class,
					Instant.parse( "2026-03-08T06:59:59.999999900Z" ) ) )
					.isEqualTo( Instant.parse( "2026-03-08T06:59:59.999999Z" ) );
		}
		finally { verify( guard, never() ).getConnection(); }
	}
}
