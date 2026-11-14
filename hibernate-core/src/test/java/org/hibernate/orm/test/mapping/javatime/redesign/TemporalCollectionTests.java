package org.hibernate.orm.test.mapping.javatime.redesign;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

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

/// Collection values use the same whole-value adjustment and COLUMN reconstruction as attributes.
///
/// @author Steve Ebersole
@ServiceRegistry(settings = {
		@Setting(name = MappingSettings.JAVA_TIME_USE_DIRECT_JDBC, value = "true"),
		// Oracle prohibits TIMESTAMP WITH TIME ZONE in the set table's primary key.
		@Setting(name = MappingSettings.PREFERRED_INSTANT_JDBC_TYPE, value = "TIMESTAMP")
})
@RequiresDialectFeature(feature = TemporalEligibilityChecks.AdjustedOffsetDateTime.class)
@DomainModel(annotatedClasses = TemporalCollectionTests.Event.class)
@SessionFactory
class TemporalCollectionTests {
	@Test
	void setsAndBagsRoundTrip(SessionFactoryScope scope) {
		final var timestamp = OffsetDateTime.parse( "2026-07-01T12:00:00+02:00" );
		scope.inTransaction( session -> {
			final var event = new Event();
			event.id = 1L;
			event.timestamps.add( timestamp );
			event.bag.add( timestamp );
			session.persist( event );
		} );
		scope.inTransaction( session -> {
			final var event = session.find( Event.class, 1L );
			final var adjusted = timestamp.atZoneSameInstant( ZoneId.of( "America/New_York" ) ).toOffsetDateTime();
			assertThat( event.timestamps ).containsExactly( adjusted );
			assertThat( event.bag ).containsExactly( adjusted );
		} );
		scope.inTransaction( session -> {
			final var event = session.find( Event.class, 1L );
			session.remove( event );
		} );
	}

	@Entity(name = "TemporalCollectionEvent")
	@TimeZoneAdjustment("America/New_York")
	static class Event {
		@Id Long id;

		@ElementCollection
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		Set<OffsetDateTime> timestamps = new HashSet<>();

		@ElementCollection
		List<OffsetDateTime> bag = new ArrayList<>();
	}
}
