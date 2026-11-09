package org.hibernate.orm.test.mapping.basic;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.TimeZoneRepresentation;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.*;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Executable user-guide example of explicitly selected offset-only COLUMN storage.
/// Full regional zone identity remains a separate native-pair guarantee.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
@DomainModel(annotatedClasses = ZonedDateTimeColumnMappingTests.Event.class)
@SessionFactory
class ZonedDateTimeColumnMappingTests {
	@Test
	void preservesDocumentedLocalFieldsAndOffset(SessionFactoryScope scope) {
		final var value = ZonedDateTime.of( 2024, 2, 29, 12, 34, 56, 0, ZoneOffset.ofHoursMinutes( 5, 30 ) );
		assertThat( scope.getSessionFactory().getSessionFactoryOptions().isTemporalPreservationEnabled() ).isTrue();
		try {
			scope.inTransaction( session -> { final var row = new Event(); row.id = 1; row.zonedDateTime = value; session.persist( row ); } );
			scope.inTransaction( session -> {
				final var actual = session.find( Event.class, 1 ).zonedDateTime;
				assertThat( actual.toLocalDateTime() ).isEqualTo( value.toLocalDateTime() );
				assertThat( actual.getOffset() ).isEqualTo( value.getOffset() );
			} );
		}
		finally { scope.dropData(); }
	}

	@Entity(name = "ZonedColumnExample")
	static class Event {
		@Id int id;
		//tag::basic-ZonedDateTime-example[]
		// Explicit offset-only storage; regional zone identity is not guaranteed.
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		ZonedDateTime zonedDateTime;
		//end::basic-ZonedDateTime-example[]
	}
}
