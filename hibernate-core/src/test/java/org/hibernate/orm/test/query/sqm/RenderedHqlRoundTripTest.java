package org.hibernate.orm.test.query.sqm;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import org.hibernate.Session;
import org.hibernate.query.Order;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.criteria.JpaSubQuery;
import org.hibernate.query.sqm.internal.SqmUtil;
import org.hibernate.query.sqm.spi.SqmStatementAccess;
import org.hibernate.query.sqm.tree.spi.SqmVisitableNode;
import org.hibernate.query.sqm.tree.spi.select.SqmSelectStatement;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The HQL string rendered for an SQM tree must be accepted by the HQL
 * translator, and must mean the same thing as the tree it was rendered from.
 */
@DomainModel(annotatedClasses = {RenderedHqlRoundTripTest.Parent.class, RenderedHqlRoundTripTest.Item.class})
@SessionFactory
@JiraKey("HHH-21128")
public class RenderedHqlRoundTripTest {

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
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsStringAggregation.class)
	public void distinctArgumentFollowedByAnotherArgument(SessionFactoryScope scope) {
		scope.inSession( session -> assertRendersItself( session,
				"select listagg(distinct i.name, ',') within group (order by i.name) from Item i" ) );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonTable.class)
	public void jsonTablePassingAndExistsColumn(SessionFactoryScope scope) {
		scope.inSession( session -> assertRendersItself( session,
				"select t.v from json_table('[1,2]', '$[*]' passing 1 as x"
						+ " columns(v Integer path '$', e exists path '$.a', i for ordinality)) t" ) );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonValue.class)
	public void jsonValuePassing(SessionFactoryScope scope) {
		scope.inSession( session -> assertRendersItself( session,
				"select json_value('{\"a\":1}', '$.a' passing 1 as x, 2 as y) from Parent p" ) );
	}

	@Test
	public void correlatedJoinWithRestriction(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			final JpaCriteriaQuery<Integer> query = cb.createQuery( Integer.class );
			final JpaRoot<Parent> parent = query.from( Parent.class );
			final JpaSubQuery<Integer> subquery = query.subquery( Integer.class );
			final var item = subquery.correlate( parent ).join( "items" );
			item.on( cb.equal( item.get( "name" ), "x" ) );
			subquery.select( cb.literal( 1 ) );
			query.select( parent.get( "id" ) ).where( cb.exists( subquery ) );

			// HQL has no 'on' clause for a correlated root, so the restriction is rendered in the 'where' clause
			final String rendered = ( (SqmVisitableNode) query ).toHqlString();
			assertEquals( List.of( 1 ), session.createSelectionQuery( rendered, Integer.class ).getResultList(), rendered );
		} );
	}

	@Test
	public void caseInsensitiveOrderByPosition(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			final JpaCriteriaQuery<Object[]> query = cb.createQuery( Object[].class );
			final JpaRoot<Parent> parent = query.from( Parent.class );
			query.multiselect( parent.get( "name" ), parent.get( "id" ) );
			final SqmSelectStatement<Object[]> statement = (SqmSelectStatement<Object[]>) query;
			statement.orderBy( SqmUtil.sortSpecification( statement, Order.asc( 1 ).ignoringCase() ) );

			// HQL cannot ignore case when ordering by position, so the select item itself is rendered
			final String rendered = statement.toHqlString();
			assertEquals( List.of( "A", "b" ),
					names( session.createSelectionQuery( rendered, Object[].class ).getResultList() ), rendered );
		} );
	}

	private static List<Object> names(List<Object[]> rows) {
		final List<Object> names = new ArrayList<>();
		for ( Object[] row : rows ) {
			names.add( row[0] );
		}
		return names;
	}

	/**
	 * The rendering of the given query must be accepted by the HQL
	 * translator and must itself be rendered the same way.
	 */
	private static void assertRendersItself(Session session, String hql) {
		final String rendered = render( session, hql );
		assertEquals( rendered, render( session, rendered ) );
	}

	private static String render(Session session, String hql) {
		return session.createSelectionQuery( hql, Object.class )
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
