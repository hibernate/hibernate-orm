package org.hibernate.orm.test.mapping.javatime.redesign;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Enabled counterparts to temporal query operations in the legacy gambit fixture.
/// A small model keeps unsupported regional-zone storage out of unrelated queries.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
@DomainModel(annotatedClasses = TemporalEnabledQueryTests.Event.class)
@SessionFactory
class TemporalEnabledQueryTests {
	@AfterEach
	void clean(SessionFactoryScope scope) { scope.dropData(); }

	@Test
	void projectionLiteralsParametersAndFunctions(SessionFactoryScope scope) {
		assertThat( scope.getSessionFactory().getSessionFactoryOptions().isTemporalPreservationEnabled() ).isTrue();
		scope.inTransaction( session -> session.persist( new Event() ) );
		scope.inTransaction( session -> {
			final var dto = session.createSelectionQuery( "select new " + Values.class.getName()
					+ "(e.day, e.clock, e.stamp) from EnabledTemporalEvent e", Values.class ).getSingleResult();
			assertThat( dto ).isEqualTo( new Values( LocalDate.of( 2024, 2, 29 ), LocalTime.of( 12, 34, 56 ),
					LocalDateTime.of( 2024, 2, 29, 12, 34, 56 ) ) );
			assertThat( session.createSelectionQuery( "from EnabledTemporalEvent e where e.day = {2024-02-29}"
					+ " and e.clock = {12:34:56} and e.stamp = {2024-02-29 12:34:56}", Event.class ).getResultList() ).hasSize( 1 );
			assertThat( session.createSelectionQuery( "select timestampdiff(year, e.day, :day) from EnabledTemporalEvent e"
					+ " where e.day <= :day", Long.class ).setParameter( "day", dto.day ).getSingleResult() ).isZero();
			assertThat( session.createSelectionQuery( "select max(e.stamp) from EnabledTemporalEvent e where e.stamp = :stamp",
					LocalDateTime.class ).setParameter( "stamp", dto.stamp ).getSingleResult() ).isEqualTo( dto.stamp );
			assertThat( session.createSelectionQuery( "select extract(year from e.stamp) from EnabledTemporalEvent e",
					Integer.class ).getSingleResult() ).isEqualTo( 2024 );
			assertThat( session.createSelectionQuery( "select truncate(e.stamp, day) from EnabledTemporalEvent e",
					LocalDateTime.class ).getSingleResult() ).isEqualTo( dto.day.atStartOfDay() );
			assertThat( session.createSelectionQuery( "select cast(e.stamp as LocalDate) from EnabledTemporalEvent e",
					LocalDate.class ).getSingleResult() ).isEqualTo( dto.day );
			final var builder = session.getCriteriaBuilder();
			final var query = builder.createQuery( String.class );
			final var root = query.from( Event.class );
			query.select( builder.format( root.get( "stamp" ), "yyyy-MM-dd" ) );
			assertThat( session.createQuery( query ).getSingleResult() ).isEqualTo( "2024-02-29" );
			assertThat( session.createSelectionQuery( "select local date", LocalDate.class ).getSingleResult() ).isNotNull();
			assertThat( session.createSelectionQuery( "select local time", LocalTime.class ).getSingleResult() ).isNotNull();
			assertThat( session.createSelectionQuery( "select local datetime", LocalDateTime.class ).getSingleResult() ).isNotNull();
			assertThat( session.createMutationQuery( "update EnabledTemporalEvent set day = local date" ).executeUpdate() ).isEqualTo( 1 );
			session.clear();
			assertThat( session.find( Event.class, 1 ).day )
					.isEqualTo( session.createSelectionQuery( "select local date", LocalDate.class ).getSingleResult() );
		} );
	}

	public record Values(LocalDate day, LocalTime clock, LocalDateTime stamp) { }

	@Entity(name = "EnabledTemporalEvent")
	static class Event {
		@Id int id = 1;
		@jakarta.persistence.Column(name = "event_day")
		LocalDate day = LocalDate.of( 2024, 2, 29 );
		LocalTime clock = LocalTime.of( 12, 34, 56 );
		LocalDateTime stamp = LocalDateTime.of( 2024, 2, 29, 12, 34, 56 );
	}
}
