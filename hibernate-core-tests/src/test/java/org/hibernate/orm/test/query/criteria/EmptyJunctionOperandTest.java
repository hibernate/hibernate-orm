package org.hibernate.orm.test.query.criteria;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.tree.spi.predicate.SqmJunctionPredicate;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * An empty conjunction is true and an empty disjunction is false,
 * including when the junction is an operand of another predicate.
 */
@DomainModel(annotatedClasses = EmptyJunctionOperandTest.Thing.class)
@SessionFactory
@JiraKey("HHH-21157")
public class EmptyJunctionOperandTest {

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
	public void notEmptyConjunction(SessionFactoryScope scope) {
		assertIds( scope, List.of(), (cb, thing) -> cb.not( emptyConjunction( cb ) ) );
	}

	@Test
	public void emptyConjunctionOrPredicate(SessionFactoryScope scope) {
		assertIds( scope, List.of( 1L, 2L ),
				(cb, thing) -> cb.or( emptyConjunction( cb ), cb.equal( thing.get( "name" ), "a" ) ) );
	}

	@Test
	public void predicateOrEmptyConjunction(SessionFactoryScope scope) {
		assertIds( scope, List.of( 1L, 2L ),
				(cb, thing) -> cb.or( cb.equal( thing.get( "name" ), "a" ), emptyConjunction( cb ) ) );
	}

	@Test
	public void emptyConjunctionAndPredicate(SessionFactoryScope scope) {
		assertIds( scope, List.of( 1L ),
				(cb, thing) -> cb.and( emptyConjunction( cb ), cb.equal( thing.get( "name" ), "a" ) ) );
	}

	@Test
	public void emptyDisjunction(SessionFactoryScope scope) {
		assertIds( scope, List.of(),
				(cb, thing) -> new SqmJunctionPredicate( Predicate.BooleanOperator.OR, new ArrayList<>(), (NodeBuilder) cb ) );
	}

	@Test
	public void emptyConjunctionAsRestriction(SessionFactoryScope scope) {
		assertIds( scope, List.of( 1L, 2L ), (cb, thing) -> emptyConjunction( cb ) );
	}

	private static Predicate emptyConjunction(HibernateCriteriaBuilder cb) {
		return cb.wrap( new Predicate[0] );
	}

	private static void assertIds(
			SessionFactoryScope scope,
			List<Long> expected,
			BiFunction<HibernateCriteriaBuilder, Root<Thing>, Predicate> restriction) {
		scope.inTransaction( session -> {
			final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
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
