package org.hibernate.orm.test.hhh16425;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collector;

import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Tuple;

import static java.util.Comparator.naturalOrder;
import static java.util.stream.Collectors.averagingLong;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.maxBy;
import static java.util.stream.Collectors.minBy;
import static java.util.stream.Collectors.summingLong;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestForIssue(jiraKey = "HHH-16425")
@RequiresDialect(H2Dialect.class)
@DomainModel(
		annotatedClasses = {
				EntityB.class
		}
)
@SessionFactory
public class JiraIssue16425Test {
	private static final String COUNT_QUERY = "select a.name, count(*) from (select name as name from EntityB) as a group by a.name";
	private static final String MIN_QUERY = "select a.name, min(a.amount) from (select name as name, amount as amount from EntityB) as a group by a.name";
	private static final String MAX_QUERY = "select a.name, max(a.amount) from (select name as name, amount as amount from EntityB) as a group by a.name";
	private static final String SUM_QUERY = "select a.name, sum(a.amount) from (select name as name, amount as amount from EntityB) as a group by a.name";
	private static final String AVERAGE_QUERY = "select a.name, avg(a.amount) from (select name as name, amount as amount from EntityB) as a group by a.name";

	@BeforeEach
	void generateDataBeforeEach(SessionFactoryScope scope) {
		final var RND = new Random();
		final var STR = "ABCDEFGHIJ";
		scope.inTransaction( session -> {
			session.createNativeQuery( "truncate table entityb" ).executeUpdate();
			for ( var n = 0; n < 10; ++n ) {
				final var a = new EntityB(
						n + 1,
						Character.toString( STR.charAt( RND.nextInt( STR.length() ) ) ),
						RND.nextInt( 10000 )
				);
				session.persist( a );
			}
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-16425")
	@DisplayName("Count aggregate is working properly")
	void count(SessionFactoryScope scope) {
		final List<Tuple> actual = scope.fromSession( session -> session.createQuery( COUNT_QUERY, Tuple.class )
				.getResultList() );
		final Map<String, Long> expected = expectedResults( scope, counting() );

		assertEquals( expected.size(), actual.size() );
		actual.forEach( t -> assertEquals(
				expected.get( t.get( 0, String.class ) ),
				t.get( 1, Number.class ).longValue()
		) );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-16425")
	@DisplayName("Min aggregate is not working in 6.1")
	void min(SessionFactoryScope scope) {
		final List<Tuple> actual = scope.fromSession( session -> session.createQuery( MIN_QUERY, Tuple.class )
				.getResultList() );
		final var expected = expectedResults( scope, mapping( Number::longValue, minBy( naturalOrder() ) ) );

		assertEquals( expected.size(), actual.size() );
		actual.forEach( t -> assertEquals(
				expected.get( t.get( 0, String.class ) ).get(),
				t.get( 1, Number.class ).longValue()
		) );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-16425")
	@DisplayName("Max aggregate is not working in 6.1")
	void max(SessionFactoryScope scope) {
		final List<Tuple> actual = scope.fromSession( session -> session.createQuery( MAX_QUERY, Tuple.class )
				.getResultList() );
		final var expected = expectedResults( scope, mapping( Number::longValue, maxBy( naturalOrder() ) ) );

		assertEquals( expected.size(), actual.size() );
		actual.forEach( t -> assertEquals(
				expected.get( t.get( 0, String.class ) ).get(),
				t.get( 1, Number.class ).longValue()
		) );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-16425")
	@DisplayName("Sum aggregate is not working")
	void sum(SessionFactoryScope scope) {
		final List<Tuple> actual = scope.fromSession( session -> session.createQuery( SUM_QUERY, Tuple.class )
				.getResultList() );
		final Map<String, Long> expected = expectedResults( scope, summingLong( Number::longValue ) );

		assertEquals( expected.size(), actual.size() );
		actual.forEach( t -> assertEquals(
				expected.get( t.get( 0, String.class ) ),
				t.get( 1, Number.class ).longValue()
		) );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-16425")
	@DisplayName("Average aggregate is working properly")
	void average(SessionFactoryScope scope) {
		final List<Tuple> actual = scope.fromSession( session -> session.createQuery( AVERAGE_QUERY, Tuple.class )
				.getResultList() );
		final var expected = expectedResults( scope, averagingLong( Number::longValue ) );

		assertEquals( expected.size(), actual.size() );
		actual.forEach( t -> assertEquals(
				expected.get( t.get( 0, String.class ) ),
				t.get( 1, Number.class ).doubleValue()
		) );
	}

	private static <A, X> Map<String, A> expectedResults(SessionFactoryScope scope, Collector<Number, ?, A> collector) {
		return scope.fromSession( session -> {
			return session.createNativeQuery(
							"select name, amount from entityb",
							Tuple.class
					)
					.stream()
					.collect( groupingBy( t -> t.get( 0, String.class ), mapping(
							t -> t.get( 1, Number.class ),
							collector
					) ) );
		} );
	}
}
