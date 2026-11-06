package org.hibernate.orm.test.mapping.javatime.redesign;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.TimeZoneAdjustment;
import org.hibernate.annotations.TimeZoneRepresentation;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Integration coverage for mapping-owned targets and scoped COLUMN selection.
///
/// @author Steve Ebersole
@ServiceRegistry(settings = @Setting(name = MappingSettings.JAVA_TIME_USE_DIRECT_JDBC, value = "true"))
@DomainModel(annotatedClasses = TemporalMappingTests.Event.class)
@SessionFactory
class TemporalMappingTests {
	@Test
	void targetsSurviveJdbcReconstruction(SessionFactoryScope scope) {
		final var input = OffsetDateTime.parse( "2026-07-01T12:00:00+02:00" );
		scope.inTransaction( session -> {
			final var event = new Event();
			event.id = 1L;
			event.adjustedTime = java.time.OffsetTime.parse( "01:00+05:30:45" );
			event.newYork = input;
			event.india = input;
			event.columnValue = input.atZoneSameInstant( ZoneId.of( "Europe/Paris" ) );
			session.persist( event );
			session.flush();
			assertThat( event.newYork ).isSameAs( input );
		} );
		scope.inTransaction( session -> {
			final var event = session.find( Event.class, 1L );
			assertThat( event.adjustedTime ).isEqualTo( java.time.OffsetTime.parse( "13:29:15+18:00" ) );
			assertThat( session.createSelectionQuery( "from TemporalEvent e where e.adjustedTime = :time", Event.class )
					.setParameter( "time", java.time.OffsetTime.parse( "01:00+05:30:45" ) ).getResultList() ).hasSize( 1 );

			assertThat( event.newYork.getOffset() ).isEqualTo( ZoneOffset.ofHours( -4 ) );
			assertThat( event.india.getOffset() ).isEqualTo( ZoneOffset.ofHoursMinutes( 5, 30 ) );
			assertThat( event.newYork.toInstant() ).isEqualTo( input.toInstant() );
			assertThat( session.createSelectionQuery( "select e.newYork from TemporalEvent e where e.id = 1", OffsetDateTime.class )
					.getSingleResult() ).isEqualTo( event.newYork );
			assertThat( session.createSelectionQuery( "select e.columnValue from TemporalEvent e where e.id = 1", ZonedDateTime.class )
					.getSingleResult() ).isEqualTo( event.columnValue );
			assertThat( session.createSelectionQuery( "from TemporalEvent e where e.newYork in :values", Event.class )
					.setParameterList( "values", java.util.List.of( input ) ).getResultList() ).hasSize( 1 );
			assertThat( session.createSelectionQuery( "from TemporalEvent e where e.newYork = offset datetime 2026-07-01 12:00+02:00", Event.class )
					.getResultList() ).hasSize( 1 );
			final var mapping = (org.hibernate.type.BasicType<OffsetDateTime>) scope.getSessionFactory().getMappingMetamodel()
					.getEntityDescriptor( Event.class ).getPropertyType( "newYork" );
			assertThat( session.createNativeQuery( "select newYork as adjusted_value from TemporalEvent where newYork = :value" )
					.addScalar( "adjusted_value", mapping ).setParameter( "value", input, mapping ).getSingleResult() )
					.isEqualTo( event.newYork );

			assertThat( event.columnValue.getZone() ).isEqualTo( ZoneId.of( "America/New_York" ) );
			assertThat( event.columnValue.toInstant() ).isEqualTo( input.toInstant() );
			assertThat( session.createSelectionQuery( "from TemporalEvent e where e.newYork = :value", Event.class )
					.setParameter( "value", input ).getResultList() ).hasSize( 1 );
			assertThatThrownBy( () -> session.createSelectionQuery(
					"from TemporalEvent e where e.newYork = :value or e.india = :value", Event.class )
					.setParameter( "value", input ).getResultList() )
					.hasMessageContaining( "Query parameter ':value'" )
					.hasMessageContaining( "newYork" ).hasMessageContaining( "india" )
					.hasMessageContaining( "America/New_York" ).hasMessageContaining( "+05:30" )
					.hasMessageContaining( "Use a separate parameter name or position" );

		} );
		final var updated = input.plusDays( 1 );
		scope.inTransaction( session -> assertThat( session.createMutationQuery( "update TemporalEvent e set e.india = :value where e.id = 1" )
				.setParameter( "value", updated ).executeUpdate() ).isEqualTo( 1 ) );
		scope.inTransaction( session -> assertThat( session.find( Event.class, 1L ).india )
				.isEqualTo( updated.atZoneSameInstant( ZoneOffset.ofHoursMinutes( 5, 30 ) ).toOffsetDateTime() ) );
		scope.inTransaction( session -> session.createMutationQuery( "delete from TemporalEvent" ).executeUpdate() );
	}

	@Entity(name = "TemporalEvent")
	@TimeZoneAdjustment("America/New_York")
	static class Event {
		@Id
		Long id;

		OffsetDateTime newYork;

		@TimeZoneAdjustment("+18:00")
		java.time.OffsetTime adjustedTime;


		@TimeZoneAdjustment("+05:30")
		OffsetDateTime india;

		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		ZonedDateTime columnValue;
	}
}
