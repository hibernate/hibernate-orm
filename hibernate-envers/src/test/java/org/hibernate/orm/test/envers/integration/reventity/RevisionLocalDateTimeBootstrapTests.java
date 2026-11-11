package org.hibernate.orm.test.envers.integration.reventity;

import java.sql.SQLException;
import java.util.stream.Stream;

import org.hibernate.MappingException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.strategy.internal.DefaultAuditStrategy;
import org.hibernate.envers.strategy.internal.ValidityAuditStrategy;
import org.hibernate.envers.test.entities.reventity.CustomLocalDateTimeRevEntity;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableStrategy;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Real revision mappings exercise enabled rejection and legacy/eligible bootstrap
/// without JDBC metadata. Synthetic profiles are not live provider evidence.
///
/// @author Steve Ebersole
class RevisionLocalDateTimeBootstrapTests {
	static Stream<Arguments> profiles() {
		return Stream.of( H2Dialect.class, DB2Dialect.class, SybaseASEDialect.class ).flatMap( dialect ->
				Stream.of( false, true ).flatMap( preservation ->
						Stream.of( DefaultAuditStrategy.class, ValidityAuditStrategy.class )
								.map( strategy -> Arguments.of( dialect, preservation, strategy.getName() ) ) ) );
	}

	@ParameterizedTest
	@MethodSource("profiles")
	void bootstrapWithoutJdbc(Class<? extends Dialect> dialectClass, boolean preservation, String strategy) throws Exception {
		final var dialect = dialectClass.getConstructor().newInstance();
		final boolean eligible = dialectClass == H2Dialect.class;
		assertThat( new RevisionLocalDateTimeEligibility().apply( dialect ) ).isEqualTo( eligible );
		final var guard = mock( ConnectionProvider.class );
		when( guard.getConnection() ).thenThrow( new AssertionError( "No bootstrap JDBC" ) );
		try ( var registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.addService( ConnectionProvider.class, guard )
				.applySetting( AvailableSettings.DIALECT, dialect )
				.applySetting( AvailableSettings.ALLOW_METADATA_ON_BOOT, false )
				.applySetting( AvailableSettings.TEMPORAL_PRESERVATION, preservation )
				.applySetting( AvailableSettings.HBM2DDL_AUTO, "none" )
				// Auxiliary bulk-operation DDL is independent of metadata access.
				.applySetting( GlobalTemporaryTableStrategy.CREATE_ID_TABLES, false )
				.applySetting( GlobalTemporaryTableStrategy.DROP_ID_TABLES, false )
				.applySetting( AvailableSettings.USE_SECOND_LEVEL_CACHE, false )
				.applySetting( EnversSettings.AUDIT_STRATEGY, strategy ).build() ) {
			final var sources = new MetadataSources( registry ).addAnnotatedClass( StrTestEntity.class )
					.addAnnotatedClass( CustomLocalDateTimeRevEntity.class );
			if ( preservation && !eligible ) {
				assertThatThrownBy( sources::buildMetadata ).isInstanceOf( MappingException.class )
						.hasMessageContaining( "CustomLocalDateTimeRevEntity.localDateTimestamp" )
						.hasMessageContaining( "LocalDateTimeJavaType" ).hasMessageContaining( "TimestampJdbcType" )
						.hasMessageContaining( "DATE" ).hasMessageContaining( "UNKNOWN" );
			}
			else {
				try ( var factory = sources.buildMetadata().buildSessionFactory() ) {
					assertThat( factory.getSessionFactoryOptions().isTemporalPreservationEnabled() ).isEqualTo( preservation );
				}
			}
		}
		finally {
			verifyNoJdbc( guard );
		}
	}

	private static void verifyNoJdbc(ConnectionProvider guard) throws SQLException {
		verify( guard, never() ).getConnection();
	}
}
