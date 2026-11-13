package org.hibernate.orm.test.mapping.javatime;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.hibernate.MappingException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.type.descriptor.jdbc.ZonedDateTimeJdbcType;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

/// Verifies Oracle storage using direct `ZonedDateTime` JDBC binding and extraction.
///
/// @author Steve Ebersole
@RequiresDialect(OracleDialect.class)
// The explicit driver descriptor is a legacy compatibility control, not an assessed preserving pair.
@ServiceRegistry(settings = @Setting(name = MappingSettings.TEMPORAL_PRESERVATION, value = "false"))
@DomainModel(annotatedClasses = OracleZonedDateTimeRoundTripTest.ZonedValue.class)
@SessionFactory
class OracleZonedDateTimeRoundTripTest {

	@Test
	void preservesRegionOffsetAndOverlap(SessionFactoryScope scope) {
		final var region = ZoneId.of( "America/New_York" );
		final var overlap = LocalDateTime.of( 2026, 11, 1, 1, 30 );
		final var values = new ZonedDateTime[] {
				ZonedDateTime.of( 2026, 1, 15, 12, 0, 0, 123456000, region ),
				ZonedDateTime.of( 2026, 1, 15, 12, 0, 0, 123456000, ZoneOffset.ofHoursMinutes( 5, 30 ) ),
				ZonedDateTime.ofStrict( overlap, ZoneOffset.ofHours( -4 ), region ),
				ZonedDateTime.ofStrict( overlap, ZoneOffset.ofHours( -5 ), region )
		};
		try {
			scope.inTransaction( session -> {
				for ( int i = 0; i < values.length; i++ ) {
					final var entity = new ZonedValue();
					entity.id = i;
					entity.value = values[i];
					session.persist( entity );
				}
			} );
			scope.inTransaction( session -> {
				for ( int i = 0; i < values.length; i++ ) {
					final var actual = session.get( ZonedValue.class, i ).value;
					assertThat( actual.getZone() ).isEqualTo( values[i].getZone() );
					assertThat( actual.getOffset() ).isEqualTo( values[i].getOffset() );
					assertThat( actual.toLocalDateTime() ).isEqualTo( values[i].toLocalDateTime() );
					assertThat( actual.toInstant() ).isEqualTo( values[i].toInstant() );
				}
			} );
		}
		finally {
			scope.inTransaction( session -> session.createMutationQuery( "delete from OracleZonedValue" ).execute() );
		}
	}

	@Test
	void enabledModeRejectsUnassessedExplicitDescriptor() {
		try ( var registry = new StandardServiceRegistryBuilder()
				.applySetting( MappingSettings.TEMPORAL_PRESERVATION, true )
				.applySetting( "hibernate.hbm2ddl.auto", "none" ).build() ) {
			assertThatThrownBy( () -> {
				try ( var ignored = new MetadataSources( registry ).addAnnotatedClass( ZonedValue.class )
						.buildMetadata().buildSessionFactory() ) {}
			} )
					.isInstanceOf( MappingException.class ).hasMessageContaining( "preservation" )
					.hasMessageContaining( "ZonedDateTimeJdbcType" );
		}
	}

	@Entity(name = "OracleZonedValue")
	@Table(name = "oracle_zoned_value")
	static class ZonedValue {
		@Id
		Integer id;

		@JdbcType(ZonedDateTimeJdbcType.class)
		ZonedDateTime value;
	}
}
