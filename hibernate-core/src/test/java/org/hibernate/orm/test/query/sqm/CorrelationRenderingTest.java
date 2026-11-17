package org.hibernate.orm.test.query.sqm;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.criteria.JoinType;

import org.hibernate.Session;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.criteria.JpaSubQuery;
import org.hibernate.query.sqm.spi.SqmStatementAccess;
import org.hibernate.query.sqm.tree.spi.SqmVisitableNode;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A correlation only refers to the outer query. It must not be rendered as an
 * element of the from clause of the subquery, though each of its joins is one.
 */
@DomainModel(annotatedClasses = {CorrelationRenderingTest.Parent.class, CorrelationRenderingTest.Item.class})
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
}
