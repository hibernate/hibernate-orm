package org.hibernate.orm.test.type.temporal.preservation;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.config.ConfigurationException;
import org.hibernate.type.internal.TemporalPreservationSettings;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Tests how the temporal preservation setting and the direct JDBC setting
/// resolve, and that the result reaches both the boot model and the factory.
///
/// The two settings are independent. Only the former enables validation and
/// rejection. Either one makes direct JDBC access preferred. Enabling the
/// former while explicitly disabling the latter is contradictory.
///
/// @author Steve Ebersole
@BaseUnitTest
class TemporalPreservationSettingsTests {
	private static final String PRESERVATION = MappingSettings.TEMPORAL_PRESERVATION;
	private static final String DIRECT = MappingSettings.JAVA_TIME_USE_DIRECT_JDBC;

	@Test
	void namesAndDefaults() {
		assertThat( PRESERVATION ).isEqualTo( "hibernate.type.temporal_preservation" );
		assertThat( DIRECT ).isEqualTo( "hibernate.type.java_time_use_direct_jdbc" );
		assertThat( MappingSettings.TEMPORAL_PRESERVATION_DEFAULT ).isFalse();
		assertThat( MappingSettings.JAVA_TIME_USE_DIRECT_JDBC_DEFAULT ).isFalse();
	}

	@Test
	void unsetIsDisabled() {
		assertResolved( Map.of(), false, false );
	}

	@Test
	void preservationImpliesDirectJdbc() {
		assertResolved( Map.of( PRESERVATION, true ), true, true );
		assertResolved( Map.of( PRESERVATION, "true" ), true, true );
		assertResolved( Map.of( PRESERVATION, true, DIRECT, true ), true, true );
		assertResolved( Map.of( PRESERVATION, false ), false, false );
		assertResolved( Map.of( PRESERVATION, "false" ), false, false );
	}

	@Test
	void directJdbcDoesNotEnablePreservation() {
		assertResolved( Map.of( DIRECT, true ), false, true );
		assertResolved( Map.of( DIRECT, "true" ), false, true );
		assertResolved( Map.of( DIRECT, false ), false, false );
		assertResolved( Map.of( PRESERVATION, false, DIRECT, true ), false, true );
		assertResolved( Map.of( PRESERVATION, false, DIRECT, false ), false, false );
	}

	@Test
	void preservationWithDirectJdbcDisabledIsContradictory() {
		for ( var settings : new Map[] {
				Map.of( PRESERVATION, true, DIRECT, false ),
				Map.of( PRESERVATION, "true", DIRECT, "false" )
		} ) {
			assertThatThrownBy( () -> TemporalPreservationSettings.resolve( settings ) )
					.isInstanceOf( ConfigurationException.class )
					.hasMessageContaining( PRESERVATION )
					.hasMessageContaining( DIRECT );
		}
		assertThatThrownBy( () -> assertFactory( Map.of( PRESERVATION, true, DIRECT, false ), true, true ) )
				.hasStackTraceContaining( PRESERVATION )
				.hasStackTraceContaining( DIRECT );
	}

	@Test
	@DisabledIfSystemProperty(
			named = "hibernate.type.temporal_preservation",
			matches = "true",
			disabledReason = "Verifies the behavior with the setting left unset"
	)
	void factoriesAreIndependent() {
		assertFactory( Map.of( PRESERVATION, true ), true, true );
		assertFactory( Map.of(), false, false );
		assertFactory( Map.of( DIRECT, true ), false, true );
		assertFactory( Map.of( PRESERVATION, false, DIRECT, true ), false, true );
		assertFactory( Map.of(), false, false );
	}

	private static void assertResolved(Map<String, Object> settings, boolean preservation, boolean direct) {
		final var resolved = TemporalPreservationSettings.resolve( new HashMap<>( settings ) );
		assertThat( resolved.preservationEnabled() ).isEqualTo( preservation );
		assertThat( resolved.directJdbcPreferred() ).isEqualTo( direct );
	}

	@SuppressWarnings("deprecation")
	private static void assertFactory(Map<String, Object> settings, boolean preservation, boolean direct) {
		final var registryBuilder = ServiceRegistryUtil.serviceRegistryBuilder();
		settings.forEach( registryBuilder::applySetting );
		try ( var registry = registryBuilder.build() ) {
			final var metadata = new MetadataSources( registry ).buildMetadata();
			assertThat( metadata.getDatabase()
					.getTypeConfiguration()
					.getCurrentBaseSqlTypeIndicators()
					.isPreferJavaTimeJdbcTypesEnabled() ).isEqualTo( direct );
			try ( var sessionFactory = (SessionFactoryImplementor) metadata.buildSessionFactory() ) {
				final var options = sessionFactory.getSessionFactoryOptions();
				assertThat( options.isTemporalPreservationEnabled() ).isEqualTo( preservation );
				assertThat( options.isPreferJavaTimeJdbcTypesEnabled() ).isEqualTo( direct );
			}
		}
	}
}
