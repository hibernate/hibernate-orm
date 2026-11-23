package org.hibernate.orm.test.query.criteria;

import java.util.List;
import java.util.function.BiFunction;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Negating a negated comparison predicate must give back the affirmative predicate.
 */
@DomainModel(annotatedClasses = RepeatedComparisonNegationTest.Thing.class)
@SessionFactory
@JiraKey("HHH-21156")
public class RepeatedComparisonNegationTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Thing( 1L, "a" ) );
			session.persist( new Thing( 2L, "b" ) );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void notTwice(SessionFactoryScope scope) {
		assertIds( scope, List.of( 1L ), (cb, thing) -> cb.equal( thing.get( "name" ), "a" ).not().not() );
	}

	@Test
	public void criteriaBuilderNotTwice(SessionFactoryScope scope) {
		assertIds( scope, List.of( 1L ), (cb, thing) -> cb.not( cb.not( cb.equal( thing.get( "name" ), "a" ) ) ) );
	}

	@Test
	public void inequalityNotTwice(SessionFactoryScope scope) {
		assertIds( scope, List.of( 1L ), (cb, thing) -> cb.lt( thing.<Long>get( "id" ), 2L ).not().not() );
	}

	@Test
	public void notOnce(SessionFactoryScope scope) {
		assertIds( scope, List.of( 2L ), (cb, thing) -> cb.equal( thing.get( "name" ), "a" ).not() );
	}

	@Test
	public void notThreeTimes(SessionFactoryScope scope) {
		assertIds( scope, List.of( 2L ), (cb, thing) -> cb.equal( thing.get( "name" ), "a" ).not().not().not() );
	}

	@Test
	public void isNegated(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final Root<Thing> thing = cb.createQuery( Long.class ).from( Thing.class );
			final Predicate predicate = cb.equal( thing.get( "name" ), "a" );
			assertFalse( predicate.isNegated() );
			assertTrue( predicate.not().isNegated() );
			assertFalse( predicate.not().not().isNegated() );
		} );
	}

	private static void assertIds(
			SessionFactoryScope scope,
			List<Long> expected,
			BiFunction<CriteriaBuilder, Root<Thing>, Predicate> restriction) {
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<Long> query = cb.createQuery( Long.class );
			final Root<Thing> thing = query.from( Thing.class );
			query.select( thing.get( "id" ) )
					.where( restriction.apply( cb, thing ) )
					.orderBy( cb.asc( thing.get( "id" ) ) );
			assertEquals( expected, session.createQuery( query ).getResultList() );
		} );
	}

	@Entity(name = "Thing")
	public static class Thing {
		@Id
		Long id;
		String name;

		public Thing() {
		}

		public Thing(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
