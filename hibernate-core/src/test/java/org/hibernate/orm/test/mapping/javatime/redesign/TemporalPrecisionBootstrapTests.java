package org.hibernate.orm.test.mapping.javatime.redesign;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.boot.MetadataSources;
import org.hibernate.MappingException;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.mapping.BasicValue;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.type.internal.TemporalPrecisionBasicType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// SQL sizing and precision resolution work without boot-time JDBC metadata or
/// schema queries. Defaults and explicit zero remain distinct mapping inputs.
///
/// @author Steve Ebersole
class TemporalPrecisionBootstrapTests {
	@Test
	void resolvedSizesNeedNoConnectionAndLegacyModeHasNoAdapter() {
		for ( boolean enabled : new boolean[] { true, false } ) {
			final var connections = new NoConnections();
			try ( var registry = ServiceRegistryUtil.serviceRegistryBuilder()
					.applySetting( "hibernate.dialect", H2Dialect.class )
					.applySetting( "hibernate.boot.allow_jdbc_metadata_access", false )
					.applySetting( "hibernate.hbm2ddl.auto", "none" )
					.applySetting( MappingSettings.TEMPORAL_PRESERVATION, enabled )
					.addService( ConnectionProvider.class, connections ).build() ) {
				final var metadata = new MetadataSources( registry ).addAnnotatedClass( Sized.class ).buildMetadata();
				final var entity = metadata.getEntityBinding( Sized.class.getName() );
				for ( String name : List.of( "automatic", "seconds", "millis", "micros", "nanos", "time" ) ) {
					final var value = (BasicValue) entity.getProperty( name ).getValue();
					final var temporal = TemporalPrecisionBasicType.find( value.resolve().getJdbcMapping() );
					if ( enabled ) {
						assertThat( temporal ).isNotNull();
						final var context = temporal.getPrecisionPlan().getContext();
						assertThat( context.precision() ).isEqualTo( value.getColumns().get( 0 )
								.getColumnSize( metadata.getDatabase().getDialect(), metadata ).getPrecision() );
						assertThat( context.ddlTypeName() ).isEqualTo( value.getColumns().get( 0 ).getSqlType( metadata ) );
						if ( name.equals( "automatic" ) ) {
							assertThat( context.declaredPrecision() ).isNull();
							assertThat( context.precision() ).isEqualTo( 6 );
						}
						else if ( name.equals( "seconds" ) ) {
							assertThat( context.declaredPrecision() ).isZero();
							assertThat( context.precision() ).isZero();
						}
					}
					else {
						assertThat( temporal ).isNull();
					}
				}
				try ( var factory = metadata.buildSessionFactory() ) {
					assertThat( factory.isOpen() ).isTrue();
				}
				assertThat( connections.attempts ).hasValue( 0 );
			}
		}
	}

	@Test
	void physicalDefinitionControlsAutomaticPrecisionAndRejectsContradictions() {
		final var connections = new NoConnections();
		try ( var registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( "hibernate.dialect", H2Dialect.class )
				.applySetting( "hibernate.boot.allow_jdbc_metadata_access", false )
				.applySetting( "hibernate.hbm2ddl.auto", "none" )
				.applySetting( MappingSettings.TEMPORAL_PRESERVATION, true )
				.addService( ConnectionProvider.class, connections ).build() ) {
			final var metadata = new MetadataSources( registry ).addAnnotatedClass( PhysicalMillis.class ).buildMetadata();
			final var value = (BasicValue) metadata.getEntityBinding( PhysicalMillis.class.getName() ).getProperty( "stamp" ).getValue();
			final var temporal = TemporalPrecisionBasicType.find( value.resolve().getJdbcMapping() );
			final var input = LocalDateTime.parse( "2026-01-02T12:34:56.123456789" );
			assertThat( temporal.prepareRelational( input ) ).isEqualTo( input.withNano( 123_000_000 ) );
			assertThatThrownBy( () -> new MetadataSources( registry ).addAnnotatedClass( Contradictory.class ).buildMetadata() )
					.isInstanceOf( MappingException.class ).hasMessageContaining( "physical column precision 3" );
			assertThatThrownBy( () -> new MetadataSources( registry ).addAnnotatedClass( Opaque.class ).buildMetadata() )
					.isInstanceOf( MappingException.class ).hasMessageContaining( "Unresolved physical" );
			assertThat( connections.attempts ).hasValue( 0 );
		}
	}

	@Entity
	static class PhysicalMillis {
		@Id int id;
		@Column(columnDefinition = "timestamp(3)") LocalDateTime stamp;
	}

	@Entity
	static class Contradictory {
		@Id int id;
		@Column(columnDefinition = "timestamp(3)", secondPrecision = 6) LocalDateTime stamp;
	}

	@Entity
	static class Opaque {
		@Id int id;
		@Column(columnDefinition = "application_timestamp") LocalDateTime stamp;
	}

	@Entity
	static class Sized {
		@Id int id;
		LocalDateTime automatic;
		@Column(secondPrecision = 0) LocalDateTime seconds;
		@Column(secondPrecision = 3) LocalDateTime millis;
		@Column(secondPrecision = 6) LocalDateTime micros;
		@Column(secondPrecision = 9) LocalDateTime nanos;
		@Column(secondPrecision = 3) LocalTime time;
	}

	/// Any connection attempt fails the bootstrap control, even if caught by boot.
	///
	/// @author Steve Ebersole
	static class NoConnections extends UserSuppliedConnectionProviderImpl {
		final AtomicInteger attempts = new AtomicInteger();

		@Override
		public Connection getConnection() {
			attempts.incrementAndGet();
			throw new AssertionError( "Precision resolution must not acquire a connection" );
		}
	}
}
