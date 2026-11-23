package org.hibernate.orm.test.query.sqm;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Tuple;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaCteCriteria;
import org.hibernate.query.criteria.JpaDerivedJoin;
import org.hibernate.query.criteria.JpaFunctionJoin;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.criteria.JpaSubQuery;
import org.hibernate.query.sqm.tree.spi.SqmVisitableNode;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A join of a correlated root which is not an attribute join must be
 * rendered as HQL which means the same thing as the criteria query.
 */
@DomainModel(annotatedClasses = {CorrelatedJoinRenderingTest.Parent.class, CorrelatedJoinRenderingTest.Item.class})
@SessionFactory
@JiraKey("HHH-21174")
public class CorrelatedJoinRenderingTest {

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
	public void entityJoin(SessionFactoryScope scope) {
		assertRendered( scope, List.of( 1 ), (cb, query, subquery, correlated) -> {
			final var item = correlated.join( Item.class );
			item.on( cb.equal( item.get( "parent" ), correlated ), cb.equal( item.get( "name" ), "x" ) );
		} );
	}

	@Test
	public void entityJoinWithNestedJoin(SessionFactoryScope scope) {
		assertRendered( scope, List.of( 2 ), (cb, query, subquery, correlated) -> {
			final var item = correlated.join( Item.class );
			item.on( cb.equal( item.get( "parent" ), correlated ) );
			final var owner = item.join( "parent" );
			owner.on( cb.equal( owner.get( "name" ), "A" ) );
		} );
	}

	@Test
	public void entityJoinAfterAttributeJoin(SessionFactoryScope scope) {
		assertRendered( scope, List.of( 1 ), (cb, query, subquery, correlated) -> {
			final var item = correlated.join( "items" );
			item.on( cb.equal( item.get( "name" ), "y" ) );
			final var other = correlated.join( Item.class );
			other.on( cb.equal( other.get( "parent" ), correlated ), cb.equal( other.get( "name" ), "x" ) );
		} );
	}

	@Test
	public void derivedJoin(SessionFactoryScope scope) {
		assertRendered( scope, List.of( 1 ), (cb, query, subquery, correlated) -> {
			final JpaSubQuery<Tuple> items = subquery.subquery( Tuple.class );
			final JpaRoot<Item> item = items.from( Item.class );
			items.multiselect( item.get( "parent" ).get( "id" ).alias( "pid" ), item.get( "name" ).alias( "itemName" ) );
			final JpaDerivedJoin<Tuple> derived = correlated.join( items );
			derived.on( cb.equal( derived.get( "pid" ), correlated.get( "id" ) ), cb.equal( derived.get( "itemName" ), "x" ) );
		} );
	}

	@Test
	@RequiresDialect(H2Dialect.class)
	public void functionJoin(SessionFactoryScope scope) {
		assertRendered( scope, List.of( 1 ), (cb, query, subquery, correlated) -> {
			final JpaFunctionJoin<Integer> series = correlated.join( cb.generateSeries( 1, 1 ) );
			series.on( cb.equal( series, correlated.get( "id" ) ) );
		} );
	}

	@Test
	@RequiresDialect(H2Dialect.class)
	public void lateralJoinAfterAttributeJoin(SessionFactoryScope scope) {
		// HQL has no lateral root, so the lateral join is joined to the root before it
		assertRendered( scope, List.of( 1, 2 ), (cb, query, subquery, correlated) -> {
			final var item = correlated.join( "items" );
			final JpaSubQuery<Tuple> others = subquery.subquery( Tuple.class );
			final JpaRoot<Item> other = others.from( Item.class );
			others.multiselect( other.get( "id" ).alias( "id" ) );
			others.where(
					cb.equal( other.get( "name" ), item.get( "name" ) ),
					cb.notEqual( other.get( "id" ), item.get( "id" ) )
			);
			correlated.joinLateral( others );
		} );
	}

	@Test
	@RequiresDialect(H2Dialect.class)
	public void firstLateralJoin(SessionFactoryScope scope) {
		assertFirstLateralJoin( scope, false );
	}

	@Test
	@RequiresDialect(H2Dialect.class)
	public void firstLateralJoinAfterIndependentRoot(SessionFactoryScope scope) {
		assertFirstLateralJoin( scope, true );
	}

	private static void assertFirstLateralJoin(SessionFactoryScope scope, boolean hasIndependentRoot) {
		scope.inSession( session -> {
			final var cb = session.getCriteriaBuilder();
			final var query = cb.createQuery( Integer.class );
			final var parent = query.from( Parent.class );
			final var subquery = query.subquery( Long.class );
			if ( hasIndependentRoot ) {
				subquery.from( Item.class );
			}
			final var correlated = subquery.correlate( parent );
			final var matchingItems = subquery.subquery( Tuple.class );
			final var item = matchingItems.from( Item.class );
			matchingItems.multiselect( item.get( "id" ).alias( "id" ) );
			matchingItems.where( cb.equal( item.get( "parent" ), parent ), cb.equal( item.get( "name" ), "x" ) );
			final var joined = correlated.joinLateral( matchingItems );
			subquery.select( cb.count( joined.get( "id" ) ) );
			query.select( parent.get( "id" ) ).where( cb.equal( subquery, hasIndependentRoot ? 3L : 1L ) );
			assertEquals( List.of( 1 ), session.createQuery( query ).getResultList() );
			final String rendered = ( (SqmVisitableNode) query ).toHqlString();
			assertEquals( List.of( 1 ), session.createSelectionQuery( rendered, Integer.class ).getResultList(), rendered );
		} );
	}

	@Test
	public void cteJoin(SessionFactoryScope scope) {
		assertRendered( scope, List.of( 1 ), (cb, query, subquery, correlated) -> {
			final JpaCriteriaQuery<Tuple> items = cb.createTupleQuery();
			final JpaRoot<Item> item = items.from( Item.class );
			items.multiselect( item.get( "parent" ).get( "id" ).alias( "pid" ), item.get( "name" ).alias( "itemName" ) );
			final JpaCteCriteria<Tuple> cte = query.with( items );
			final var joined = correlated.join( cte );
			joined.on( cb.equal( joined.get( "pid" ), correlated.get( "id" ) ), cb.equal( joined.get( "itemName" ), "x" ) );
		} );
	}

	private interface Joins {
		void apply(
				HibernateCriteriaBuilder cb,
				JpaCriteriaQuery<Integer> query,
				JpaSubQuery<Integer> subquery,
				JpaRoot<Parent> correlated);
	}

	/**
	 * The parents for which a subquery with the given joins of the correlated parent has a row.
	 * The rendered HQL must return the same parents as the criteria query.
	 */
	private static void assertRendered(SessionFactoryScope scope, List<Integer> expected, Joins joins) {
		scope.inSession( session -> {
			final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			final JpaCriteriaQuery<Integer> query = cb.createQuery( Integer.class );
			final JpaRoot<Parent> parent = query.from( Parent.class );
			final JpaSubQuery<Integer> subquery = query.subquery( Integer.class );
			joins.apply( cb, query, subquery, subquery.correlate( parent ) );
			subquery.select( cb.literal( 1 ) );
			query.select( parent.get( "id" ) ).where( cb.exists( subquery ) ).orderBy( cb.asc( parent.get( "id" ) ) );
			assertEquals( expected, session.createQuery( query ).getResultList() );
			final String rendered = ( (SqmVisitableNode) query ).toHqlString();
			assertEquals( expected, session.createSelectionQuery( rendered, Integer.class ).getResultList(), rendered );
		} );
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
