package org.hibernate.orm.test.polymorphic;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.hibernate.jpa.HibernateHints;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Tuple;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import static jakarta.persistence.FetchType.LAZY;
import static org.assertj.core.api.Assertions.assertThat;

@DomainModel( annotatedClasses = {
		PolymorphicPaginationTest.Left.class,
		PolymorphicPaginationTest.Right.class,
		PolymorphicPaginationTest.Empty.class,
		PolymorphicPaginationTest.Parent.class
} )
@SessionFactory( generateStatistics = true )
@Jira( "https://hibernate.atlassian.net/browse/HHH-17986" )
class PolymorphicPaginationTest {
	private static final String QUERY = "from " + Base.class.getName() + " e";

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var parents = new Parent[3];
			for ( int i = 0; i < parents.length; i++ ) {
				parents[i] = new Parent();
				parents[i].id = i + 1;
				session.persist( parents[i] );
			}
			for ( int i = 1; i <= 4; i++ ) {
				final var entity = new Left();
				entity.id = i;
				entity.parent = parents[i <= 3 ? 0 : 1];
				session.persist( entity );
			}
			for ( int i = 5; i <= 6; i++ ) {
				final var entity = new Right();
				entity.id = i;
				entity.parent = parents[2];
				session.persist( entity );
			}
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void apiPagination(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var query = session.createSelectionQuery( QUERY, Base.class );
			for ( int first = 0; first <= 7; first++ ) {
				for ( int max = 0; max <= 7; max++ ) {
					assertPage( query.setFirstResult( first ).setMaxResults( max ).list(), first, max );
				}
			}
			assertPage( query.setFirstResult( 1 ).setMaxResults( Integer.MAX_VALUE ).list(), 1, 6 );
			assertPage( query.setFirstResult( Integer.MAX_VALUE ).setMaxResults( 2 ).list(), 7, 2 );
		} );
	}

	@Test
	void hqlPagination(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertPage( session.createSelectionQuery( QUERY + " limit 10 offset 1", Base.class ).list(), 1, 10 );
		} );
	}

	@Test
	void parameterizedHqlPagination(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var query = session.createSelectionQuery( QUERY + " limit :max offset :first", Base.class );
			for ( int first = 0; first <= 7; first++ ) {
				for ( int max = 0; max <= 7; max++ ) {
					assertPage( query.setParameter( "first", first ).setParameter( "max", max ).list(), first, max );
				}
			}
		} );
	}

	@ParameterizedTest
	@CsvSource( { "false,false", "false,true", "true,false", "true,true" } )
	void apiOverridesHqlPagination(boolean collectionFetch, boolean inMemory, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final String hql = QUERY + (collectionFetch ? " left join fetch e.tags" : "");
			final var query = session.createSelectionQuery( hql + " limit 1 offset 5", Base.class )
					.setHint( HibernateHints.HINT_LIMIT_IN_MEMORY, inMemory );
			assertPage( query.setFirstResult( 1 ).setMaxResults( 4 ).list(), 1, 4 );
			assertPage( session.createSelectionQuery( hql + " limit 2 offset 5", Base.class )
					.setHint( HibernateHints.HINT_LIMIT_IN_MEMORY, inMemory )
					.setFirstResult( 1 ).list(), 1, 6 );
			assertPage( session.createSelectionQuery( hql + " limit 1 offset 5", Base.class )
					.setHint( HibernateHints.HINT_LIMIT_IN_MEMORY, inMemory )
					.setMaxResults( 4 ).list(), 0, 4 );
			assertPage( session.createSelectionQuery( hql + " limit 0 offset 5", Base.class )
					.setHint( HibernateHints.HINT_LIMIT_IN_MEMORY, inMemory )
					.setMaxResults( 4 ).list(), 0, 4 );
			assertPage( session.createSelectionQuery( hql + " limit 2 offset 5", Base.class )
					.setHint( HibernateHints.HINT_LIMIT_IN_MEMORY, inMemory )
					.setFirstResult( 0 ).list(), 0, 6 );
			assertPage( session.createSelectionQuery( hql + " limit 1 offset 5", Base.class )
					.setHint( HibernateHints.HINT_LIMIT_IN_MEMORY, inMemory )
					.setMaxResults( 0 ).list(), 0, 0 );
		} );
	}

	@Test
	void offsetOrLimitOnly(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertPage( session.createSelectionQuery( QUERY, Base.class ).list(), 0, 6 );
			assertPage( session.createSelectionQuery( QUERY, Base.class ).setFirstResult( 1 ).list(), 1, 6 );
			assertPage( session.createSelectionQuery( QUERY, Base.class ).setMaxResults( 3 ).list(), 0, 3 );
			assertPage( session.createSelectionQuery( QUERY + " offset 1", Base.class ).list(), 1, 6 );
			assertPage( session.createSelectionQuery( QUERY + " limit 3", Base.class ).list(), 0, 3 );
		} );
	}

	@Test
	void loadsOnlyRequiredPrefix(SessionFactoryScope scope) {
		final var statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction( session -> {
			assertPage( session.createSelectionQuery( QUERY, Base.class )
					.setFirstResult( 1 ).setMaxResults( 1 ).list(), 1, 1 );
		} );
		assertThat( statistics.getEntityLoadCount() ).isEqualTo( 2 );
	}

	@Test
	void hqlLoadsOnlyRequiredPrefix(SessionFactoryScope scope) {
		final var statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction( session -> {
			assertPage( session.createSelectionQuery( QUERY + " limit 1 offset 1", Base.class ).list(), 1, 1 );
		} );
		assertThat( statistics.getEntityLoadCount() ).isEqualTo( 2 );
	}

	@ParameterizedTest
	@MethodSource( "projections" )
	void projectionLoadsOnlyRequiredPrefix(String hql, Class<?> resultType, SessionFactoryScope scope) {
		final var statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction( session -> {
			assertThat( session.createSelectionQuery( hql, resultType )
					.setFirstResult( 1 ).setMaxResults( 1 ).list() ).hasSize( 1 );
		} );
		assertThat( statistics.getQueryStatistics( hql ).getExecutionRowCount() ).isEqualTo( 2 );
	}

	static Stream<Arguments> projections() {
		return Stream.of(
				Arguments.of( "select e.id " + QUERY, Integer.class ),
				Arguments.of( "select e.id, e.id " + QUERY, Object[].class ),
				Arguments.of( "select e.id, e.id " + QUERY, Tuple.class ),
				Arguments.of( "select new " + IdView.class.getName() + "(e.id) " + QUERY, IdView.class ),
				Arguments.of( "select e.id, e.id " + QUERY, PairView.class ),
				Arguments.of( "select e " + QUERY + " join e.tags t", Object[].class ),
				Arguments.of( "select e " + QUERY + " join e.tags t", Tuple.class ),
				Arguments.of( "select new " + EntityView.class.getName() + "(e) " + QUERY, EntityView.class )
		);
	}

	@Test
	void entityAssociationProjection(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var page = session.createSelectionQuery(
					"select e.parent " + QUERY + " order by e.id", Parent.class )
					.setFirstResult( 1 ).setMaxResults( 1 ).list();
			assertThat( page ).extracting( parent -> parent.id ).containsExactly( 2 );
		} );
	}

	@Test
	void collectionJoin(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertPage( session.createSelectionQuery( "select e " + QUERY + " join e.tags t", Base.class )
					.setFirstResult( 1 ).setMaxResults( 5 ).list(), 1, 5 );
		} );
	}

	@Test
	void explicitInMemoryPagination(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertPage( session.createSelectionQuery( QUERY + " limit :max offset :first", Base.class )
					.setParameter( "max", 3 ).setParameter( "first", 1 )
					.setHint( HibernateHints.HINT_LIMIT_IN_MEMORY, true ).list(), 1, 3 );
			assertPage( session.createSelectionQuery( QUERY, Base.class )
					.setFirstResult( 1 ).setMaxResults( 3 )
					.setHint( HibernateHints.HINT_LIMIT_IN_MEMORY, true ).list(), 1, 3 );
		} );
	}

	@Test
	void parameterUsedInPredicateAndLimit(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var page = session.createSelectionQuery(
					QUERY + " where e.id <= :max limit :max offset :first", Base.class )
					.setParameter( "max", 3 ).setParameter( "first", 1 ).list();
			assertThat( page ).hasSize( 2 ).extracting( entity -> entity.id )
					.doesNotHaveDuplicates().isSubsetOf( 1, 2, 3 );
		} );
	}

	@Test
	void listTransformer(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var page = session.createQuery( QUERY, Base.class )
					.setResultListTransformer( list -> list.stream().filter( e -> e.id % 2 == 0 ).toList() )
					.setFirstResult( 1 ).setMaxResults( 2 ).list();
			assertThat( page ).hasSize( 2 ).extracting( entity -> entity.id )
					.doesNotHaveDuplicates().isSubsetOf( 2, 4, 6 );
		} );
	}

	@Test
	void collectionFetchWithApiPagination(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var page = session.createSelectionQuery( QUERY + " left join fetch e.tags", Base.class )
					.setFirstResult( 1 ).setMaxResults( 4 ).list();
			assertPage( page, 1, 4 );
			page.forEach( entity -> assertThat( entity.tags ).containsExactlyInAnyOrder( "a", "b", "c" ) );
		} );
	}

	@Test
	void collectionFetchWithHqlPagination(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var page = session.createSelectionQuery(
					QUERY + " left join fetch e.tags limit 4 offset 1", Base.class ).list();
			assertPage( page, 1, 4 );
			page.forEach( entity -> assertThat( entity.tags ).containsExactlyInAnyOrder( "a", "b", "c" ) );
		} );
	}

	@Test
	void emptyPageWithCollectionFetch(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertPage( session.createSelectionQuery( QUERY + " left join fetch e.tags", Base.class )
					.setFirstResult( 1 ).setMaxResults( 0 ).list(), 1, 0 );
			assertPage( session.createSelectionQuery(
					QUERY + " left join fetch e.tags limit 0 offset 1", Base.class ).list(), 1, 0 );
		} );
	}

	private static void assertPage(List<Base> page, int first, int max) {
		assertThat( page ).hasSize( Math.min( max, Math.max( 0, 6 - first ) ) );
		assertThat( page ).extracting( entity -> entity.id )
				.doesNotHaveDuplicates().isSubsetOf( 1, 2, 3, 4, 5, 6 );
	}

	@MappedSuperclass
	public static abstract class Base {
		@Id
		int id;

		@ManyToOne( fetch = LAZY )
		Parent parent;

		@ElementCollection
		Set<String> tags = new HashSet<>( Set.of( "a", "b", "c" ) );
	}

	@Entity( name = "PaginationLeft" )
	public static class Left extends Base {
	}

	@Entity( name = "PaginationRight" )
	public static class Right extends Base {
	}

	@Entity( name = "PaginationEmpty" )
	public static class Empty extends Base {
	}

	@Entity( name = "PaginationParent" )
	public static class Parent {
		@Id
		int id;
	}

	public record IdView(int id) {
	}

	public record EntityView(Base entity) {
	}

	public record PairView(int id, int copy) {
	}
}
