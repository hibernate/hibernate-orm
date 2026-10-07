package org.hibernate.orm.test.query.criteria;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Tuple;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.query.SemanticException;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// Exercises an unregistered database aggregate in constructor, tuple, and array projections.
///
/// @author Steve Ebersole
@JiraKey("HHH-10337")
@RequiresDialect(H2Dialect.class)
@RequiresDialect(HSQLDialect.class)
@DomainModel(annotatedClasses = UnregisteredAggregateProjectionTest.TestEntity.class)
@SessionFactory
class UnregisteredAggregateProjectionTest {
	@BeforeAll
	void setUp(SessionFactoryScope scope) {
		assertThat( scope.getSessionFactory().getQueryEngine().getSqmFunctionRegistry()
				.findFunctionDescriptor( "group_concat" ) ).isNull();
		scope.inTransaction( session -> {
			session.persist( new TestEntity( 1L, "first" ) );
			session.persist( new TestEntity( 2L, "second" ) );
		} );
	}

	@AfterAll
	void cleanUp(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void testUntypedHqlConstructorReportsMissingConstructor(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var exception = assertThrows( SemanticException.class, () ->
					session.createSelectionQuery(
							"select new " + Result.class.getName()
									+ "(function('group_concat', e.text)) from AggregateProjectionEntity e",
							Result.class
					) );
			assertThat( exception ).hasMessageContaining( "Missing constructor" );
		} );
	}

	@Test
	void testTypedHqlConstructor(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertAggregate( session.createSelectionQuery(
				"select new " + Result.class.getName()
						+ "(function(group_concat as String, e.text)) from AggregateProjectionEntity e",
				Result.class
		).getSingleResult().text ) );
	}

	@Test
	void testHqlTuple(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var tuple = session.createSelectionQuery(
					"select function('group_concat', e.text), count(e) from AggregateProjectionEntity e",
					Tuple.class
			).getSingleResult();
			assertAggregate( tuple.get( 0, String.class ) );
			assertThat( tuple.get( 1, Long.class ) ).isEqualTo( 2L );
		} );
	}

	@Test
	void testHqlArray(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var result = session.createSelectionQuery(
					"select function('group_concat', e.text), count(e) from AggregateProjectionEntity e",
					Object[].class
			).getSingleResult();
			assertAggregate( (String) result[0] );
			assertThat( result[1] ).isEqualTo( 2L );
		} );
	}

	@Test
	void testCriteriaConstructor(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var cb = session.getCriteriaBuilder();
			final var query = cb.createQuery( Result.class );
			final var root = query.from( TestEntity.class );
			query.select( cb.construct( Result.class,
					cb.function( "group_concat", String.class, root.get( "text" ) ) ) );
			assertAggregate( session.createQuery( query ).getSingleResult().text );
		} );
	}

	@Test
	void testCriteriaTuple(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var cb = session.getCriteriaBuilder();
			final var query = cb.createTupleQuery();
			final var root = query.from( TestEntity.class );
			query.select( cb.tuple( cb.function( "group_concat", String.class, root.get( "text" ) ),
					cb.count( root ) ) );
			final var result = session.createQuery( query ).getSingleResult();
			assertAggregate( result.get( 0, String.class ) );
			assertThat( result.get( 1, Long.class ) ).isEqualTo( 2L );
		} );
	}

	@Test
	void testCriteriaArray(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var cb = session.getCriteriaBuilder();
			final var query = cb.createQuery( Object[].class );
			final var root = query.from( TestEntity.class );
			query.select( cb.array( cb.function( "group_concat", String.class, root.get( "text" ) ),
					cb.count( root ) ) );
			final var result = session.createQuery( query ).getSingleResult();
			assertAggregate( (String) result[0] );
			assertThat( result[1] ).isEqualTo( 2L );
		} );
	}

	private static void assertAggregate(String value) {
		assertThat( value.split( "," ) ).containsExactlyInAnyOrder( "first", "second" );
	}

	public static class Result {
		final String text;

		public Result(String text) {
			this.text = text;
		}
	}

	@Entity(name = "AggregateProjectionEntity")
	static class TestEntity {
		@Id
		Long id;

		String text;

		TestEntity() {
		}

		TestEntity(Long id, String text) {
			this.id = id;
			this.text = text;
		}
	}
}
