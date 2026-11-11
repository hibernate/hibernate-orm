package org.hibernate.orm.test.mapping.javatime.redesign;

import java.time.OffsetTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.TimeZoneAdjustment;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// OffsetTime adjustment and query encoding, isolated from independent date-time mappings.
///
/// @author Steve Ebersole
@ServiceRegistry(settings = @Setting(name = MappingSettings.JAVA_TIME_USE_DIRECT_JDBC, value = "true"))
@RequiresDialectFeature(feature = TemporalEligibilityChecks.UtcOffsetTime.class)
@DomainModel(annotatedClasses = TemporalOffsetTimeMappingTests.Event.class)
@SessionFactory
class TemporalOffsetTimeMappingTests {
	@Test
	void targetSurvivesJdbcReconstruction(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var event = new Event();
			event.id = 1L;
			event.adjustedTime = OffsetTime.parse( "01:00+05:30:45" );
			session.persist( event );
		} );
		scope.inTransaction( session -> {
			final var event = session.find( Event.class, 1L );
			assertThat( event.adjustedTime ).isEqualTo( OffsetTime.parse( "13:29:15+18:00" ) );
			assertThat( session.createSelectionQuery( "from TemporalTimeEvent e where e.adjustedTime = :time", Event.class )
					.setParameter( "time", OffsetTime.parse( "01:00+05:30:45" ) ).getResultList() ).hasSize( 1 );
			session.remove( event );
		} );
	}

	@Entity(name = "TemporalTimeEvent")
	static class Event {
		@Id Long id;
		@TimeZoneAdjustment("+18:00") OffsetTime adjustedTime;
	}
}
