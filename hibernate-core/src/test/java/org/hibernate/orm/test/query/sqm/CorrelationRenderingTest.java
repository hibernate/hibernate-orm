package org.hibernate.orm.test.query.sqm;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.criteria.JoinType;

import org.hibernate.Session;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.query.SemanticException;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.criteria.JpaSubQuery;
import org.hibernate.query.sqm.spi.SqmStatementAccess;
import org.hibernate.query.sqm.tree.spi.SqmVisitableNode;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hibernate.query.sqm.tree.spi.jpa.ParameterCollector.collectParameters;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A correlation only refers to the outer query. It must not be rendered as an
 * element of the from clause of the subquery, though each of its joins is one.
 */
@DomainModel(annotatedClasses = {
		CorrelationRenderingTest.Parent.class,
		CorrelationRenderingTest.Item.class,
		CorrelationRenderingTest.Tag.class
})
@SessionFactory
@JiraKey("HHH-21176")
public class CorrelationRenderingTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Parent first = new Parent( 1, "b" );
			final Parent second = new Parent( 2, "A" );
			session.persist( first );
			session.persist( second );
			session.persist( new Item( 1, "x", first ) );
			session.persist( new Item( 2, "y", first ) );
			session.persist( new Item( 3, "y", second ) );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void correlatedRootAndAnotherRoot(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			final JpaCriteriaQuery<Integer> query = cb.createQuery( Integer.class );
			final JpaRoot<Parent> parent = query.from( Parent.class );
			final JpaSubQuery<Integer> subquery = query.subquery( Integer.class );
			final JpaRoot<Parent> correlated = subquery.correlate( parent );
			final JpaRoot<Item> item = subquery.from( Item.class );
			subquery.select( cb.literal( 1 ) )
					.where( cb.equal( item.get( "parent" ), correlated ), cb.equal( item.get( "name" ), "x" ) );
			query.select( parent.get( "id" ) ).where( cb.exists( subquery ) ).orderBy( cb.asc( parent.get( "id" ) ) );
			assertRendered( session, query, List.of( 1 ) );
		} );
	}

	@Test
	public void correlatedRootAlone(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			final JpaCriteriaQuery<Integer> query = cb.createQuery( Integer.class );
			final JpaRoot<Parent> parent = query.from( Parent.class );
			final JpaSubQuery<Integer> subquery = query.subquery( Integer.class );
			final JpaRoot<Parent> correlated = subquery.correlate( parent );
			subquery.select( cb.literal( 1 ) ).where( cb.equal( correlated.get( "name" ), "A" ) );
			query.select( parent.get( "id" ) ).where( cb.exists( subquery ) ).orderBy( cb.asc( parent.get( "id" ) ) );
			// the subquery has no from clause at all
			assertRendered( session, query, List.of( 2 ) );
		} );
	}

	@Test
	public void correlatedJoinAndAnotherRoot(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			final JpaCriteriaQuery<Integer> query = cb.createQuery( Integer.class );
			final JpaRoot<Parent> parent = query.from( Parent.class );
			final var item = parent.join( "items" );
			final JpaSubQuery<Integer> subquery = query.subquery( Integer.class );
			final var correlated = subquery.correlate( item );
			final JpaRoot<Item> other = subquery.from( Item.class );
			subquery.select( cb.literal( 1 ) ).where(
					cb.equal( other.get( "name" ), correlated.get( "name" ) ),
					cb.notEqual( other.get( "id" ), correlated.get( "id" ) )
			);
			query.select( item.get( "id" ) ).where( cb.exists( subquery ) ).orderBy( cb.asc( item.get( "id" ) ) );
			assertRendered( session, query, List.of( 2, 3 ) );
		} );
	}

	@Test
	public void joinOfCorrelatedJoin(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			final JpaCriteriaQuery<Integer> query = cb.createQuery( Integer.class );
			final JpaRoot<Parent> parent = query.from( Parent.class );
			final var item = parent.join( "items" );
			final JpaSubQuery<Integer> subquery = query.subquery( Integer.class );
			final var owner = subquery.correlate( item ).join( "parent" );
			owner.on( cb.equal( owner.get( "name" ), "A" ) );
			subquery.select( cb.literal( 1 ) );
			query.select( item.get( "id" ) ).where( cb.exists( subquery ) ).orderBy( cb.asc( item.get( "id" ) ) );
			// the join is a root of the subquery, relative to the alias of the join of the outer query
			assertRendered( session, query, List.of( 3 ) );
		} );
	}

	@Test
	public void leftJoinOfCorrelatedJoin(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final var cb = session.getCriteriaBuilder();
			final var query = cb.createQuery( Integer.class );
			final var item = query.from( Parent.class ).join( "items" );
			final var subquery = query.subquery( Integer.class );
			final var owner = subquery.correlate( item ).join( "parent", JoinType.LEFT );
			owner.on( cb.equal( owner.get( "name" ), "A" ) );
			subquery.select( cb.literal( 1 ) ).where( owner.get( "id" ).isNull() );
			query.select( item.get( "id" ) ).where( cb.exists( subquery ) ).orderBy( cb.asc( item.get( "id" ) ) );
			final var expected = List.of( 1, 2 );
			assertEquals( expected, session.createQuery( query ).getResultList() );
			assertRendersItself( session, expected, ( (SqmVisitableNode) query ).toHqlString() );
		} );
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"Item i",
			"Parent p join p.items i",
			"Parent p join Item i on i.parent = p",
			"Parent p cross join Item i"
	})
	public void entityJoinAfterCorrelatedPathRoot(String outerFrom, SessionFactoryScope scope) {
		scope.inSession( session -> {
			assertRendersItself( session, List.of( 3 ), """
					select distinct i.id from %s
					where exists (select 1 from i.parent q left join Item x on x.parent = q and x.name = 'x'
								where x.id is null)
					order by i.id
					""".formatted( outerFrom ) );
			assertRendersItself( session, List.of( 1 ), """
					select distinct i.id from %s
					where exists (select 1 from i.parent q join Item x on x.id = i.id and x.name = 'x')
					order by i.id
					""".formatted( outerFrom ) );
		} );
	}

	private static final String CORRELATED_JOIN = """
			select p.id from Parent p where exists (
				select 1 from Item x join p.items i join Item y on y.id = i.id
				where x.id = i.id and y.name = 'x'
			)
			""";

	private static final String LOCAL_AND_CORRELATED_JOINS = """
			select p.id from Parent p where exists (
				select 1 from Item x join x.parent q join p.items i
				join Item y on y.parent = q and y.id = i.id
				where x.id = i.id and y.name = 'x'
			)
			""";

	@ParameterizedTest
	@ValueSource(strings = { CORRELATED_JOIN, LOCAL_AND_CORRELATED_JOINS })
	public void entityJoinReferringToCorrelatedJoin(String hql, SessionFactoryScope scope) {
		scope.inSession( session -> {
			final String rendered = render( session, hql );
			assertTrue( rendered.contains( "y.id = i.id" ), rendered );
			assertEquals( rendered, render( session, rendered ) );
		} );
	}

	@ParameterizedTest
	@ValueSource(strings = { CORRELATED_JOIN, LOCAL_AND_CORRELATED_JOINS })
	public void executeEntityJoinReferringToCorrelatedJoin(String hql, SessionFactoryScope scope) {
		scope.inSession( session -> assertRendersItself( session, List.of( 1 ), hql ) );
	}

	@ParameterizedTest
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSubqueryInSelect.class)
	@ValueSource(strings = { "join Tag u on u.id = t.id", "left join Tag u on u.id = t.id and u.name = 'red'" })
	public void countWithEntityJoinAfterTwoCorrelatedCollections(String entityJoin, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var red = new Tag( 1, "red" );
			final var blue = new Tag( 2, "blue" );
			session.persist( red );
			session.persist( blue );
			session.find( Parent.class, 1 ).tags.addAll( List.of( red, blue ) );
			session.find( Parent.class, 2 ).tags.add( blue );
			session.persist( new Parent( 3, "empty" ) );
			assertRendersItself( session, List.of( 4, 1, 0 ), """
					select (select cast(count(*) as Integer) from p.items i join p.tags t %s)
					from Parent p order by p.id
					""".formatted( entityJoin ) );
		} );
	}

	@Test
	public void entityJoinReferringToSecondCorrelatedRoot(SessionFactoryScope scope) {
		scope.inSession( session -> assertRendersItself( session, List.of( 2 ), """
				select p.id from Parent p, Parent q where p.id <> q.id and exists (
					select 1 from p.items i join q.items j join Item y on y.id = j.id
					where i.name = 'y' and y.name = 'x'
				)
				""" ) );
	}

	@Test
	public void correlatedJoinInCriteriaCopy(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final var criteria = session.getCriteriaBuilder().createQuery( CORRELATED_JOIN, Integer.class );
			assertEquals( 1L, session.createQuery( criteria.createCountQuery() ).getSingleResult() );
		} );
	}

	@Test
	public void joinOnOuterJoinAlias(SessionFactoryScope scope) {
		scope.inSession( session -> assertRendersItself( session, List.of( 3 ), """
				select i.id from Parent p join p.items i where exists (
					select 1 from Item x join i.parent q join q.items j
					join Parent y on y.id = q.id and y.name = 'A'
					where j.id = x.id
				)
				""" ) );
	}

	@Test
	public void sameOuterPathInSeparateJoinChains(SessionFactoryScope scope) {
		scope.inSession( session -> assertRendersItself( session, List.of( 3 ), """
				select i.id from Item i where exists (
					select 1 from Item x join i.parent.items a,
						Item y join i.parent.items b join Parent z on z.id = b.parent.id
					where x.id = a.id and y.id = b.id and z.name = 'A'
				)
				""" ) );
	}

	@Test
	public void leftJoinReferringToCorrelatedJoin(SessionFactoryScope scope) {
		scope.inSession( session -> assertRendersItself( session, List.of( 2 ), """
				select p.id from Parent p where exists (
					select 1 from Item x join p.items i
					left join Item y on y.id = i.id and y.name = 'x'
					where x.id = i.id and i.id = 3 and y.id is null
				)
				""" ) );
	}

	@Test
	public void leftJoinOnOuterPathWithLocalPredicate(SessionFactoryScope scope) {
		scope.inSession( session -> assertRendersItself( session, List.of( 2 ), """
				select p.id from Parent p where exists (
					select 1 from Item x left join p.items i on i.id = x.id
					where x.id = 1 and i.id is null
				)
				""" ) );
	}

	@Test
	public void rightJoinPreservesUnmatchedItem(SessionFactoryScope scope) {
		assertPreservesUnmatchedItem( scope, "right" );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsFullJoin.class)
	@SkipForDialect(dialectClass = MariaDBDialect.class,
			reason = "MariaDB cannot resolve outer references in ON clauses inside full join emulation")
	@SkipForDialect(dialectClass = SybaseASEDialect.class,
			reason = "Sybase ASE does not support the UNION needed for full join emulation in a subquery")
	public void fullJoinPreservesUnmatchedItem(SessionFactoryScope scope) {
		assertPreservesUnmatchedItem( scope, "full" );
	}

	private static void assertPreservesUnmatchedItem(SessionFactoryScope scope, String joinType) {
		scope.inTransaction( session -> {
			session.persist( new Parent( 3, "empty" ) );
			assertRendersItself( session, List.of( 2, 3 ), """
					select p.id from Parent p where exists (
						select 1 from Item x join p.items i %s join Item y on y.id = i.id
						where y.name = 'x' and x.id is null
					) order by p.id
					""".formatted( joinType ) );
		} );
	}

	@Test
	public void correlatedRootIndependentOfRightJoin(SessionFactoryScope scope) {
		scope.inSession( session -> assertRendersItself( session, List.of( 1 ), """
				select p.id from Parent p where exists (
					select 1 from p.items i, Item x right join Item y on x.id = y.id and x.name = 'missing'
					where x.id is null and i.id = y.id and y.name = 'x'
				)
				""" ) );
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"right join p.items i on x.id = i.id",
			"full join p.items i on x.id = i.id",
			"right join p.items i",
			"full join p.items i",
			"right join treat(p as Parent).items i",
			"full join items i"
	})
	public void rejectsRightAndFullJoinsToOuterPath(String join, SessionFactoryScope scope) {
		scope.inSession( session -> {
			final String hql = "select p.id from Parent p where exists (select 1 from Item x " + join + ")";
			final var exception = assertThrows( SemanticException.class,
					() -> session.createSelectionQuery( hql, Integer.class ) );
			assertTrue( exception.getMessage().contains( "join to an outer query path" ), exception.getMessage() );
		} );
	}

	@Test
	public void rightJoinToLocalPathInSubquery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Parent( 3, "empty" ) );
			assertRendersItself( session, List.of( 2, 3 ), """
					select p.id from Parent p where exists (
						select 1 from Item x right join x.parent q on x.name = 'x'
						where q.id = p.id and x.id is null
					) order by p.id
					""" );
		} );
	}

	@Test
	public void parameterOnCorrelatedJoinIsVisitedOnce(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final var query = session.createSelectionQuery( """
					select p.id from Parent p where exists (
						select 1 from Item x join p.items i on i.name = :name where x.id = i.id
					)
					""", Integer.class );
			final var occurrences = new ArrayList<String>();
			collectParameters( query.unwrap( SqmStatementAccess.class ).getSqmStatement(),
					parameter -> occurrences.add( parameter.getName() ) );
			assertEquals( List.of( "name" ), occurrences );
			assertEquals( List.of( 1 ), query.setParameter( "name", "x" ).getResultList() );
		} );
	}

	@Test
	public void subqueryReferringToRoot(SessionFactoryScope scope) {
		scope.inSession( session -> {
			assertRendersItself( session, List.of( 1 ),
					"select p.id from Parent p where exists (select 1 from Item i where i.parent = p and i.name = 'x') order by p.id" );
			assertRendersItself( session, List.of( 2 ),
					"select p.id from Parent p where exists (select 1 where p.name = 'A') order by p.id" );
		} );
	}

	@Test
	public void subqueryReferringToJoin(SessionFactoryScope scope) {
		scope.inSession( session -> {
			assertRendersItself( session, List.of( 2, 3 ),
					"select i.id from Parent p join p.items i"
							+ " where exists (select 1 from Item o where o.name = i.name and o.id <> i.id) order by i.id" );
			assertRendersItself( session, List.of( 3 ),
					"select i.id from Parent p join p.items i"
							+ " where exists (select 1 from i.parent q where q.name = 'A') order by i.id" );
		} );
	}

	/**
	 * The rendering of the criteria query must return the same results as the query.
	 */
	private static void assertRendered(Session session, JpaCriteriaQuery<Integer> query, List<Integer> expected) {
		assertEquals( expected, session.createQuery( query ).getResultList() );
		final String rendered = ( (SqmVisitableNode) query ).toHqlString();
		assertEquals( expected, session.createSelectionQuery( rendered, Integer.class ).getResultList(), rendered );
	}

	/**
	 * The rendering of the HQL query must return the same results
	 * as the query, and must itself be rendered the same way.
	 */
	private static void assertRendersItself(Session session, List<Integer> expected, String hql) {
		assertEquals( expected, session.createSelectionQuery( hql, Integer.class ).getResultList() );
		final String rendered = render( session, hql );
		assertEquals( expected, session.createSelectionQuery( rendered, Integer.class ).getResultList(), rendered );
		assertEquals( rendered, render( session, rendered ) );
	}

	private static String render(Session session, String hql) {
		return session.createSelectionQuery( hql, Integer.class )
				.unwrap( SqmStatementAccess.class ).getSqmStatement().toHqlString();
	}

	@Entity(name = "Parent")
	public static class Parent {
		@Id
		Integer id;
		String name;
		@OneToMany(mappedBy = "parent")
		List<Item> items = new ArrayList<>();
		@ManyToMany
		List<Tag> tags = new ArrayList<>();

		public Parent() {
		}

		public Parent(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity(name = "Item")
	public static class Item {
		@Id
		Integer id;
		String name;
		@ManyToOne
		Parent parent;

		public Item() {
		}

		public Item(Integer id, String name, Parent parent) {
			this.id = id;
			this.name = name;
			this.parent = parent;
		}
	}

	@Entity(name = "Tag")
	public static class Tag {
		@Id
		Integer id;
		String name;

		public Tag() {
		}

		public Tag(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
