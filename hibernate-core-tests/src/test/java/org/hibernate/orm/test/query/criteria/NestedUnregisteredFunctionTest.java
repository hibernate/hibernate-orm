package org.hibernate.orm.test.query.criteria;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;

import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies nesting an unregistered function does not discard the outer function's arguments.
///
/// @author Steve Ebersole
@JiraKey("HHH-12984")
@RequiresDialect(H2Dialect.class)
@DomainModel(annotatedClasses = NestedUnregisteredFunctionTest.TestEntity.class)
@SessionFactory
public class NestedUnregisteredFunctionTest {
	private static final String FUNCTION_NAME = "hhh12984_replace";

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		assertThat( scope.getSessionFactory().getQueryEngine().getSqmFunctionRegistry()
				.findFunctionDescriptor( FUNCTION_NAME ) ).isNull();
		scope.inTransaction( session -> {
			// Define the function in the database without registering it with Hibernate.
			session.createNativeMutationQuery(
					"create alias " + FUNCTION_NAME + " for \"" + getClass().getName() + ".replace\""
			).executeUpdate();
			final var first = new TestEntity();
			first.id = 1L;
			first.text = "aaba";
			final var second = new TestEntity();
			second.id = 2L;
			second.text = "zzzz";
			session.persist( first );
			session.persist( second );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session ->
				session.createNativeMutationQuery( "drop alias " + FUNCTION_NAME ).executeUpdate()
		);
	}

	@Test
	public void testSelection(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder builder = session.getCriteriaBuilder();
			final var query = builder.createQuery( String.class );
			final var root = query.from( TestEntity.class );
			query.select( substringOfUnregisteredFunction( builder, root ) )
					.orderBy( builder.asc( root.get( "id" ) ) );
			assertThat( session.createQuery( query ).getResultList() ).containsExactly( "xbx", "zzz" );
		} );
	}

	@Test
	public void testPredicate(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder builder = session.getCriteriaBuilder();
			final var query = builder.createQuery( TestEntity.class );
			final var root = query.from( TestEntity.class );
			query.select( root ).where(
					builder.equal( substringOfUnregisteredFunction( builder, root ), "xbx" )
			);
			assertThat( session.createQuery( query ).getResultList() )
					.extracting( entity -> entity.id )
					.containsExactly( 1L );
		} );
	}

	private static Expression<String> substringOfUnregisteredFunction(CriteriaBuilder builder, Root<TestEntity> root) {
		return builder.substring(
				builder.function( FUNCTION_NAME, String.class, root.get( "text" ),
						builder.literal( 'a' ), builder.literal( 'x' ) ),
				2,
				3
		);
	}

	public static String replace(String value, String target, String replacement) {
		return value == null ? null : value.replace( target, replacement );
	}

	@Entity(name = "NestedFunctionEntity")
	@Table(name = "nested_function_entity")
	public static class TestEntity {
		@Id
		Long id;

		String text;
	}
}
