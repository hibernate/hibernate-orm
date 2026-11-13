package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.util.*;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.*;
import org.hibernate.engine.config.internal.ConfigurationServiceImpl;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.*;
import org.hibernate.type.spi.TypeConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/// Null standalone context uses the declared default; supplied registry values
/// remain authoritative across contribution order and metadata-disabled
/// bootstrap. SQL Server checks here inspect descriptors only, never live
/// execution.
///
/// @author Steve Ebersole
class TemporalRegistryContextTests {
	@Test
	void standaloneDefaultAndRegistryValuesRemainIndependent() {
		for ( Dialect dialect : List.of( new Dialect( DatabaseVersion.make( 1 ) ) {
		}, new H2Dialect(), new MySQLDialect(), new SQLServerDialect() ) ) {
			final Class<?> enabled = dialect instanceof H2Dialect ? TimestampUtcAsOffsetDateTimeJdbcType.class
					: TimestampUtcAsPlainJdbcTimestampJdbcType.class;
			final Class<?> disabled = dialect instanceof H2Dialect ? TimestampUtcAsInstantJdbcType.class
					: TimestampUtcAsJdbcTimestampJdbcType.class;
			assertThat( MappingSettings.JAVA_TIME_USE_DIRECT_JDBC_DEFAULT ).isTrue();
			assertThat( contribute( dialect, null ) ).isExactlyInstanceOf( enabled );
			assertThat( contribute( dialect, registry( Map.of() ) ) ).isExactlyInstanceOf( enabled );
			for ( boolean first : List.of( true, false ) )
				for ( boolean direct : List.of( first, !first ) ) {
					final var registry = registry( Map.of( MappingSettings.JAVA_TIME_USE_DIRECT_JDBC, direct ) );
					assertThat( contribute( dialect, registry ) ).isExactlyInstanceOf( direct ? enabled : disabled );
					verify( registry ).requireService( ConfigurationService.class );
					verifyNoMoreInteractions( registry );
				}
		}
	}

	@Test
	void suppliedBrokenContextDoesNotBecomeAStandaloneDefault() {
		final var broken = mock( ServiceRegistry.class );
		when( broken.requireService( ConfigurationService.class ) )
				.thenThrow( new IllegalStateException( "missing configuration service" ) );
		assertThatThrownBy( () -> MetadataBuildingContext.isPreferJavaTimeJdbcTypesEnabled( broken ) )
				.isInstanceOf( IllegalStateException.class ).hasMessageContaining( "missing configuration" );
		// Retain the existing setting parser's behavior; absence of a registry is the
		// only new defaulting rule, not a catch-all around conversion failures.
		final var invalid = registry( Map.of( MappingSettings.JAVA_TIME_USE_DIRECT_JDBC, new Object() ) );
		assertThatThrownBy( () -> MetadataBuildingContext.isPreferJavaTimeJdbcTypesEnabled( invalid ) )
				.isInstanceOf( org.hibernate.HibernateException.class );
	}

	@ParameterizedTest
	@ValueSource( booleans = { true, false } )
	@org.hibernate.testing.orm.junit.RequiresDialect( H2Dialect.class )
	void managedBootstrapUsesItsOwnModeWithoutMetadata( boolean metadata ) {
		for ( boolean direct : List.of( true, false, false, true ) )
			try ( var scope = ContextScope.boot( metadata, direct, Plain.class ) ) {
				assertThat( scope.factory.getSessionFactoryOptions().isPreferJavaTimeJdbcTypesEnabled() )
						.isEqualTo( direct );
				assertThat( scope.factory.getTypeConfiguration().getJdbcTypeRegistry()
						.getDescriptor( SqlTypes.TIMESTAMP_UTC ) )
						.isExactlyInstanceOf( direct ? TimestampUtcAsPlainJdbcTimestampJdbcType.class
								: TimestampUtcAsInstantJdbcType.class );
			}
	}

	private JdbcType contribute( Dialect dialect, ServiceRegistry registry ) {
		final var types = new TypeConfiguration();
		dialect.contributeTypes( () -> types, registry );
		return types.getJdbcTypeRegistry().getDescriptor( SqlTypes.TIMESTAMP_UTC );
	}

	private ServiceRegistry registry( Map<String, Object> settings ) {
		final var registry = mock( ServiceRegistry.class );
		when( registry.requireService( ConfigurationService.class ) )
				.thenReturn( new ConfigurationServiceImpl( settings ) );
		return registry;
	}

	@Entity( name = "RegistryPlain" )
	static class Plain {
		@Id
		int id;
		LocalDateTime stamp;
	}
}
