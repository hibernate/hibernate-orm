package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.cfg.MappingSettings;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/// Enabled counterparts to local temporal query operations in legacy shared fixtures.
///
/// @author Steve Ebersole
class TemporalLocalQueryTests {
	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void parametersProjectionAndBulkUpdate(boolean metadata) {
		try ( var scope = ContextScope.open( metadata,
				Map.of( MappingSettings.TEMPORAL_PRESERVATION, true ), Event.class ) ) {
			final var date = LocalDate.of( 2026, 11, 1 );
			final var stamp = date.atTime( 1, 30 );
			scope.tx( session -> {
				var event = new Event();
				event.id = 1;
				event.day = date;
				event.stamp = stamp;
				session.persist( event );
			} );
			scope.tx( session -> {
				assertThat( session.createSelectionQuery(
						"select stamp from LocalQueryEvent where day=:day", LocalDateTime.class )
						.setParameter( "day", date ).getSingleResult() ).isEqualTo( stamp );
				assertThat( session.createMutationQuery( "update LocalQueryEvent set day=:day, stamp=:stamp where id=1" )
						.setParameter( "day", date.plusDays( 1 ) )
						.setParameter( "stamp", stamp.plusDays( 1 ) ).executeUpdate() ).isOne();
			} );
			scope.tx( session -> {
				var event = session.find( Event.class, 1 );
				assertThat( event.day ).isEqualTo( date.plusDays( 1 ) );
				assertThat( event.stamp ).isEqualTo( stamp.plusDays( 1 ) );
			} );
		}
	}

	@Entity(name = "LocalQueryEvent")
	static class Event {
		@Id Integer id;
		LocalDate day;
		LocalDateTime stamp;
	}
}
