package org.hibernate.orm.test.mapping.javatime.redesign;

import java.time.OffsetTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;

import org.hibernate.annotations.TimeZoneAdjustment;
import org.hibernate.annotations.TimeZoneRepresentation;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Retains COLUMN OffsetTime list reconstruction and mutation without blocking date-time collections.
///
/// @author Steve Ebersole
@ServiceRegistry(settings = @Setting(name = MappingSettings.JAVA_TIME_USE_DIRECT_JDBC, value = "true"))
@RequiresDialectFeature(feature = TemporalEligibilityChecks.UtcOffsetTime.class)
@DomainModel(annotatedClasses = TemporalOffsetTimeCollectionTests.Event.class)
@SessionFactory
class TemporalOffsetTimeCollectionTests {
	@Test
	void listsRoundTrip(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var event = new Event();
			event.id = 1L;
			event.times.add( OffsetTime.parse( "01:00:00+05:30:45" ) );
			session.persist( event );
		} );
		scope.inTransaction( session -> {
			final var event = session.find( Event.class, 1L );
			assertThat( event.times ).containsExactly( OffsetTime.parse( "13:29:15+18:00" ) );
			event.times.add( OffsetTime.parse( "12:00+18:00" ) );
		} );
		scope.inTransaction( session -> {
			final var event = session.find( Event.class, 1L );
			assertThat( event.times ).hasSize( 2 );
			session.remove( event );
		} );
	}

	@Entity(name = "TemporalTimeCollectionEvent")
	static class Event {
		@Id Long id;
		@ElementCollection
		@OrderColumn
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		@TimeZoneAdjustment("+18:00")
		List<OffsetTime> times = new ArrayList<>();
	}
}
