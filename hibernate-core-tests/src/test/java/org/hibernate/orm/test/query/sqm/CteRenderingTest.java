package org.hibernate.orm.test.query.sqm;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Tuple;

import org.hibernate.Session;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.PostgresPlusDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaCteCriteria;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.sqm.spi.SqmStatementAccess;
import org.hibernate.query.sqm.tree.spi.SqmVisitableNode;
import org.hibernate.query.sqm.tree.spi.cte.SqmCteStatement;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The rendering of a {@code with} clause must be accepted by the HQL
 * translator, and must mean the same thing as the tree it was rendered from.
 */
@DomainModel(annotatedClasses = CteRenderingTest.Item.class)
@SessionFactory
@JiraKey("HHH-21177")
public class CteRenderingTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Item( 1, "x" ) );
			session.persist( new Item( 2, "y" ) );
			session.persist( new Item( 3, "y" ) );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void namedCte(SessionFactoryScope scope) {
		scope.inSession( session -> assertRendered( session, "items" ) );
	}

	@Test
	public void cteWithGeneratedName(SessionFactoryScope scope) {
		// the generated name is not a legal name of a CTE in HQL
		scope.inSession( session -> assertRendered( session, null ) );
	}

	@Test
	public void cteNamedAfterEntity(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final var cb = session.getCriteriaBuilder();
			final var definition = cb.createTupleQuery();
			final var item = definition.from( Item.class );
			definition.multiselect( item.get( "id" ).alias( "id" ) ).where( cb.equal( item.get( "name" ), "x" ) );
			final var query = cb.createQuery( Integer.class );
			final var cte = query.with( "Item", definition );
			query.select( query.from( cte ).get( "id" ) );
			assertEquals( List.of( 1 ), session.createQuery( query ).getResultList() );
			final String rendered = ( (SqmVisitableNode) query ).toHqlString();
			assertEquals( List.of( 1 ), session.createSelectionQuery( rendered, Integer.class ).getResultList() );
			assertEquals( rendered, render( session, rendered ) );
		} );
	}

	@Test
	public void generatedNameMatchesExplicitName(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final var cb = session.getCriteriaBuilder();
			final var query = cb.createQuery( Integer.class );
			final var generated = query.with( itemWithId( cb, 1 ) );
			final String name = "generated_" + ( (SqmCteStatement<?>) generated ).getCteTable().getCteName();
			final var explicit = query.with( name, itemWithId( cb, 2 ) );
			final var first = query.from( generated ).<Integer>get( "id" );
			final var second = query.from( explicit ).<Integer>get( "id" );
			query.select( cb.sum( first, second ) );
			final String rendered = ( (SqmVisitableNode) query ).toHqlString();
			assertEquals( List.of( 3 ), session.createSelectionQuery( rendered, Integer.class ).getResultList() );
			assertEquals( rendered, render( session, rendered ) );
		} );
	}

	private static JpaCriteriaQuery<Tuple> itemWithId(HibernateCriteriaBuilder cb, int id) {
		final var definition = cb.createTupleQuery();
		final var item = definition.from( Item.class );
		definition.multiselect( item.get( "id" ).alias( "id" ) ).where( cb.equal( item.get( "id" ), id ) );
		return definition;
	}

	@Test
	public void outerCteReferencedInsideAnotherWithClause(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final var cb = session.getCriteriaBuilder();
			final var query = cb.createQuery( Integer.class );
			final var outer = query.with( "Item", itemWithId( cb, 1 ) );
			final var subquery = query.subquery( Integer.class );
			final var definition = query.subquery( Tuple.class );
			final var item = definition.from( Item.class );
			definition.multiselect( item.get( "id" ).alias( "id" ) ).where( cb.equal( item.get( "id" ), 2 ) );
			final var inner = subquery.with( "Item_1", definition );
			final var first = subquery.from( outer ).<Integer>get( "id" );
			final var second = subquery.from( inner ).<Integer>get( "id" );
			subquery.select( cb.sum( first, second ) );
			query.select( query.from( outer ).get( "id" ) ).where( cb.equal( subquery, 3 ) );
			final String rendered = ( (SqmVisitableNode) query ).toHqlString();
			assertEquals( List.of( 1 ), session.createSelectionQuery( rendered, Integer.class ).getResultList() );
			assertEquals( rendered, render( session, rendered ) );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsRecursiveCtes.class)
	public void recursiveReferenceToCteNamedAfterEntity(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final var cb = session.getCriteriaBuilder();
			final var query = cb.createQuery( Integer.class );
			final var start = cb.createTupleQuery();
			start.multiselect( cb.literal( 1 ).alias( "id" ) );
			final var numbers = query.withRecursiveUnionAll( "Item", start, self -> {
				final var next = cb.createTupleQuery();
				final var id = next.from( self ).<Integer>get( "id" );
				next.multiselect( cb.sum( id, 1 ).alias( "id" ) ).where( cb.lt( id, 2 ) );
				return next;
			} );
			final var id = query.from( numbers ).<Integer>get( "id" );
			query.select( id ).orderBy( cb.asc( id ) );
			final String rendered = ( (SqmVisitableNode) query ).toHqlString();
			assertEquals( List.of( 1, 2 ), session.createSelectionQuery( rendered, Integer.class ).getResultList() );
			assertEquals( rendered, render( session, rendered ) );
		} );
	}

	private static final String INSERT_SELECT_WITH_CTE = "insert into Item (id, name) with picked as"
			+ " (select i.id as id, i.name as name from Item i where i.name = 'y')"
			+ " select p.id + 10, p.name from picked p";

	@Test
	public void insertSelectWithCte(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final String rendered = renderMutation( session, INSERT_SELECT_WITH_CTE );
			assertTrue( rendered.contains( ") with picked as (" ), rendered );
			assertEquals( rendered, renderMutation( session, rendered ) );
		} );
	}

	// These dialects accept WITH before INSERT or inline the CTE in the source query.
	@Test
	@RequiresDialect(H2Dialect.class)
	@RequiresDialect(value = PostgreSQLDialect.class, matchSubTypes = false)
	@RequiresDialect(PostgresPlusDialect.class)
	@RequiresDialect(SQLServerDialect.class)
	@RequiresDialect(SybaseASEDialect.class)
	public void executeInsertSelectWithCte(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final String rendered = renderMutation( session, INSERT_SELECT_WITH_CTE );
			assertEquals( 2, session.createMutationQuery( rendered ).executeUpdate() );
			assertEquals( List.of( 12, 13 ), session.createSelectionQuery(
					"select i.id from Item i where i.id > 10 and i.name = 'y' order by i.id", Integer.class ).getResultList() );
		} );
	}

	@Test
	public void cteOfSubquery(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final String hql = "select i.id from Item i where exists ("
					+ "with others as (select o.id as id, o.name as itemName from Item o)"
					+ " select 1 from others c where c.itemName = i.name and c.id <> i.id) order by i.id";
			assertEquals( List.of( 2, 3 ), session.createSelectionQuery( hql, Integer.class ).getResultList() );
			final String rendered = render( session, hql );
			assertEquals( List.of( 2, 3 ), session.createSelectionQuery( rendered, Integer.class ).getResultList(), rendered );
			assertEquals( rendered, render( session, rendered ) );
		} );
	}

	@Test
	public void materialization(SessionFactoryScope scope) {
		scope.inSession( session -> {
			assertRendersItself( session, " as materialized (",
					"with items as materialized (select i.id as id from Item i) select c.id from items c" );
			assertRendersItself( session, " as not materialized (",
					"with items as not materialized (select i.id as id from Item i) select c.id from items c" );
		} );
	}

	@Test
	public void searchAndCycleClauses(SessionFactoryScope scope) {
		final String tree = "with tree as (select i.id as id from Item i where i.id = 1"
				+ " union all select i.id as id from Item i join tree t on i.id = t.id + 1)";
		scope.inSession( session -> {
			assertRendersItself( session,
					" search depth first by id desc nulls last set ord cycle id set isCycle to true default false using path ",
					tree + " search depth first by id desc nulls last set ord"
							+ " cycle id set isCycle to true default false using path select t.id from tree t" );
			assertRendersItself( session, " search breadth first by id asc nulls first set ord ",
					tree + " search breadth first by id asc nulls first set ord select t.id from tree t" );
		} );
	}

	/**
	 * The rendering of a criteria query over a CTE with the given
	 * name must return the same results as the query.
	 */
	private static void assertRendered(Session session, String cteName) {
		final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
		final JpaCriteriaQuery<Tuple> items = cb.createTupleQuery();
		final JpaRoot<Item> item = items.from( Item.class );
		items.multiselect( item.get( "id" ).alias( "id" ), item.get( "name" ).alias( "itemName" ) );
		final JpaCriteriaQuery<Integer> query = cb.createQuery( Integer.class );
		final JpaCteCriteria<Tuple> cte = cteName == null ? query.with( items ) : query.with( cteName, items );
		final var root = query.from( cte );
		query.select( root.get( "id" ) ).where( cb.equal( root.get( "itemName" ), "y" ) ).orderBy( cb.asc( root.get( "id" ) ) );

		assertEquals( List.of( 2, 3 ), session.createQuery( query ).getResultList() );
		final String rendered = ( (SqmVisitableNode) query ).toHqlString();
		assertEquals( List.of( 2, 3 ), session.createSelectionQuery( rendered, Integer.class ).getResultList(), rendered );
	}

	/**
	 * The rendering of the given query must contain the given fragment, must be
	 * accepted by the HQL translator, and must itself be rendered the same way.
	 */
	private static void assertRendersItself(Session session, String fragment, String hql) {
		final String rendered = render( session, hql );
		assertTrue( rendered.contains( fragment ), rendered );
		assertEquals( rendered, render( session, rendered ) );
	}

	private static String render(Session session, String hql) {
		return session.createSelectionQuery( hql, Integer.class )
				.unwrap( SqmStatementAccess.class ).getSqmStatement().toHqlString();
	}

	private static String renderMutation(Session session, String hql) {
		return ( (SqmStatementAccess<?>) session.createMutationQuery( hql ) ).getSqmStatement().toHqlString();
	}

	@Entity(name = "Item")
	@Table(name = "cte_item")
	public static class Item {
		@Id
		Integer id;
		String name;

		public Item() {
		}

		public Item(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
