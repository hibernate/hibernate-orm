package org.hibernate.orm.test.query.restriction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.query.range.Range;
import org.hibernate.query.restriction.Restriction;
import org.hibernate.query.specification.SelectionSpecification;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.metamodel.SingularAttribute;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(annotatedClasses = TemporalRestrictionTest.CalendarEvent.class)
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-20939")
class TemporalRestrictionTest {

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( int i = 1; i <= 5; i++ ) {
				session.persist( new CalendarEvent( i,
						LocalDate.of( 2026, 2, i ), LocalDateTime.of( 2026, 2, 1, 9 + i, 0 ) ) );
			}
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void testLocalDate(SessionFactoryScope scope) {
		final var attribute = scope.getSessionFactory().getJpaMetamodel().entity( CalendarEvent.class )
				.getDeclaredSingularAttribute( "eventDate", LocalDate.class );
		assertOrdering( scope, attribute, LocalDate.of( 2026, 2, 2 ), LocalDate.of( 2026, 2, 4 ) );
	}

	@Test
	void testLocalDateTime(SessionFactoryScope scope) {
		final var attribute = scope.getSessionFactory().getJpaMetamodel().entity( CalendarEvent.class )
				.getDeclaredSingularAttribute( "eventTime", LocalDateTime.class );
		assertOrdering( scope, attribute,
				LocalDateTime.of( 2026, 2, 1, 11, 0 ), LocalDateTime.of( 2026, 2, 1, 13, 0 ) );
	}

	private <U extends Comparable<? super U>> void assertOrdering(
			SessionFactoryScope scope, SingularAttribute<CalendarEvent, U> attribute, U lower, U upper) {
		assertMatches( scope, Restriction.greaterThan( attribute, lower ), 3, 4, 5 );
		assertMatches( scope, Restriction.greaterThanOrEqual( attribute, lower ), 2, 3, 4, 5 );
		assertMatches( scope, Restriction.lessThan( attribute, upper ), 1, 2, 3 );
		assertMatches( scope, Restriction.lessThanOrEqual( attribute, upper ), 1, 2, 3, 4 );
		assertMatches( scope, Restriction.between( attribute, lower, upper ), 2, 3, 4 );
		assertMatches( scope, Restriction.notBetween( attribute, lower, upper ), 1, 5 );
		assertMatches( scope, Restriction.restrict( attribute, Range.greaterThan( lower ) ), 3, 4, 5 );
		assertMatches( scope, Restriction.restrict( attribute, Range.greaterThanOrEqualTo( lower ) ), 2, 3, 4, 5 );
		assertMatches( scope, Restriction.restrict( attribute, Range.lessThan( upper ) ), 1, 2, 3 );
		assertMatches( scope, Restriction.restrict( attribute, Range.lessThanOrEqualTo( upper ) ), 1, 2, 3, 4 );
		assertMatches( scope, Restriction.restrict( attribute, Range.open( lower, upper ) ), 3 );
		assertMatches( scope, Restriction.restrict( attribute, Range.closed( lower, upper ) ), 2, 3, 4 );
	}

	private void assertMatches(SessionFactoryScope scope, Restriction<CalendarEvent> restriction, Integer... ids) {
		final var actual = scope.fromSession( session ->
				SelectionSpecification.create( CalendarEvent.class, "from CalendarEvent order by id" )
						.restrict( restriction )
						.createQuery( session )
						.getResultList() );
		assertEquals( List.of( ids ), actual.stream().map( event -> event.id ).toList() );
	}

	@Entity(name = "CalendarEvent")
	static class CalendarEvent {
		@Id
		int id;
		LocalDate eventDate;
		LocalDateTime eventTime;

		CalendarEvent() {
		}

		CalendarEvent(int id, LocalDate eventDate, LocalDateTime eventTime) {
			this.id = id;
			this.eventDate = eventDate;
			this.eventTime = eventTime;
		}
	}
}
