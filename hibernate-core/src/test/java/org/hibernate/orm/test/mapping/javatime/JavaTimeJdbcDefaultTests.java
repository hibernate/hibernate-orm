package org.hibernate.orm.test.mapping.javatime;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.mapping.BasicValue;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.jdbc.DateJdbcType;
import org.hibernate.type.descriptor.jdbc.LocalDateJdbcType;
import org.hibernate.type.descriptor.jdbc.LocalDateTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.LocalTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.TimeJdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampJdbcType;
import org.hibernate.type.spi.TypeConfiguration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// The default retains legacy conversions; explicit opt-in works with or without
/// JDBC metadata access during bootstrap.
///
/// @author Steve Ebersole
class JavaTimeJdbcDefaultTests {
	@ParameterizedTest
	@CsvSource({ "unset,true", "false,true", "true,true", "unset,false", "false,false", "true,false" })
	void bootstrapPreference(String setting, boolean metadataAccess) throws SQLException {
		final boolean direct = "true".equals( setting );
		final var guard = mock( ConnectionProvider.class );
		when( guard.getConnection() ).thenThrow( new AssertionError( "Unexpected boot JDBC access" ) );
		final var builder = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.DIALECT, H2Dialect.class.getName() )
				.applySetting( AvailableSettings.ALLOW_METADATA_ON_BOOT, metadataAccess )
				.applySetting( AvailableSettings.DRIVER, "org.h2.Driver" )
				.applySetting( AvailableSettings.URL, "jdbc:h2:mem:javaTimeDefault" )
				.applySetting( AvailableSettings.USER, "sa" )
				.applySetting( AvailableSettings.PASS, "" )
				.applySetting( AvailableSettings.HBM2DDL_AUTO, "none" )
				.applySetting( AvailableSettings.USE_SECOND_LEVEL_CACHE, false );
		builder.getSettings().remove( AvailableSettings.JAVA_TIME_USE_DIRECT_JDBC );
		if ( !"unset".equals( setting ) ) {
			builder.applySetting( AvailableSettings.JAVA_TIME_USE_DIRECT_JDBC, setting );
		}
		if ( !metadataAccess ) {
			builder.addService( ConnectionProvider.class, guard );
		}
		try ( var registry = builder.build() ) {
			final var metadata = new MetadataSources( registry ).addAnnotatedClass( TemporalEntity.class ).buildMetadata();
			final var entity = metadata.getEntityBinding( TemporalEntity.class.getName() );
			assertThat( ((BasicValue) entity.getProperty( "date" ).getValue()).resolve().getJdbcType() )
					.isInstanceOf( direct ? LocalDateJdbcType.class : DateJdbcType.class );
			assertThat( ((BasicValue) entity.getProperty( "time" ).getValue()).resolve().getJdbcType() )
					.isInstanceOf( direct ? LocalTimeJdbcType.class : TimeJdbcType.class );
			assertThat( ((BasicValue) entity.getProperty( "timestamp" ).getValue()).resolve().getJdbcType() )
					.isInstanceOf( direct ? LocalDateTimeJdbcType.class : TimestampJdbcType.class );
			try ( var factory = metadata.buildSessionFactory() ) {
				assertThat( factory.getSessionFactoryOptions().isPreferJavaTimeJdbcTypesEnabled() ).isEqualTo( direct );
			}
		}
		finally {
			verify( guard, never() ).getConnection();
		}
	}

	@Test
	void standaloneTypeConfigurationRetainsLegacyDefaults() {
		final var types = new TypeConfiguration().getBasicTypeRegistry();
		assertThat( types.resolve( StandardBasicTypes.LOCAL_DATE ).getJdbcType() ).isInstanceOf( DateJdbcType.class );
		assertThat( types.resolve( StandardBasicTypes.LOCAL_TIME ).getJdbcType() ).isInstanceOf( TimeJdbcType.class );
		assertThat( types.resolve( StandardBasicTypes.LOCAL_DATE_TIME ).getJdbcType() ).isInstanceOf( TimestampJdbcType.class );
	}

	@Entity
	static class TemporalEntity {
		@Id
		int id;
		LocalDate date;
		LocalTime time;
		LocalDateTime timestamp;
	}
}
