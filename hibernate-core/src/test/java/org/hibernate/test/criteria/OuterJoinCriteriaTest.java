/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.criteria;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.sql.JoinFragment;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Mattias Jiderhamn
 * @author Gail Badner
 */
public class OuterJoinCriteriaTest extends BaseCoreFunctionalTestCase {
	private Order order1;
	private Order order2;
	private Order order3;

	@Override
	public String[] getMappings() {
		return new String[] { "criteria/Order.hbm.xml" };
	}

	@Test
	public void testSubcriteriaWithNonNullRestrictions() {
		Session s = openSession();
		s.getTransaction().begin();

		Criteria rootCriteria = s.createCriteria( Order.class );
		Criteria subCriteria = rootCriteria.createCriteria( "orderLines", JoinFragment.LEFT_OUTER_JOIN );
		assertNotSame( rootCriteria, subCriteria );

		// add restrictions to subCriteria, ensuring we stay on subCriteria
		assertSame( subCriteria, subCriteria.add( Restrictions.eq( "articleId", "3000" ) ) );

		List orders = rootCriteria.list();

		// order1 and order3 should be returned because each has articleId == "3000"
		// both should have their full collection
		assertEquals( 2, orders.size() );
		for ( Iterator it = orders.iterator(); it.hasNext(); ) {
			Order o = (Order) it.next();
			if ( order1.getOrderId() == o.getOrderId() ) {
				assertEquals( order1.getLines().size(), o.getLines().size() );
			}
			else if ( order3.getOrderId() == o.getOrderId() ) {
				assertEquals( order3.getLines().size(), o.getLines().size() );
			}
			else {
				fail( "unknown order" );
			}
		}
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testSubcriteriaWithNonNullRestrictionsAliasToEntityMap() {
		Session s = openSession();
		s.getTransaction().begin();

		Criteria rootCriteria = s.createCriteria( Order.class, "o" );
		Criteria subCriteria = rootCriteria.createCriteria( "orderLines", "ol", JoinFragment.LEFT_OUTER_JOIN );
		assertNotSame( rootCriteria, subCriteria );

		// add restriction to subCriteria, ensuring we stay on subCriteria
		assertSame( subCriteria, subCriteria.add( Restrictions.eq( "articleId", "3000" ) ) );

		List orders = rootCriteria.setResultTransformer( Criteria.ALIAS_TO_ENTITY_MAP ).list();

		// order1 and order3 should be returned because each has articleId == "3000";
		// the orders should both should have their full collection;
		assertEquals( 2, orders.size() );
		for ( Iterator it = orders.iterator(); it.hasNext(); ) {
			Map map = (Map) it.next();
			Order o = ( Order ) map.get( "o" );
		    // the orderLine returned from the map should have articleId = "3000"
			OrderLine ol = ( OrderLine ) map.get( "ol" );
			if ( order1.getOrderId() == o.getOrderId() ) {
				assertEquals( order1.getLines().size(), o.getLines().size() );
				assertEquals( "3000", ol.getArticleId() );
			}
			else if ( order3.getOrderId() == o.getOrderId() ) {
				assertEquals( order3.getLines().size(), o.getLines().size() );
				assertEquals( "3000", ol.getArticleId() );
			}
			else {
				fail( "unknown order" );
			}
		}
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testSubcriteriaWithNullOrNonNullRestrictions() {
		Session s = openSession();
		s.getTransaction().begin();

		Criteria rootCriteria = s.createCriteria( Order.class );
		Criteria subCriteria = rootCriteria.createCriteria( "orderLines", JoinFragment.LEFT_OUTER_JOIN );
		assertNotSame( rootCriteria, subCriteria );

		// add restrictions to subCriteria, ensuring we stay on subCriteria
		// add restriction to subCriteria, ensuring we stay on subCriteria
		assertSame(
				subCriteria,
				subCriteria.add(
					Restrictions.or(
							Restrictions.isNull( "articleId" ),		  // Allow null
							Restrictions.eq( "articleId", "1000" )
					)
				)
		);

		List orders = rootCriteria.list();

		// order1 should be returned because it has an orderline with articleId == "1000";
		// order2 should be returned because it has no orderlines
		assertEquals( 2, orders.size() );
		for ( Iterator it = orders.iterator(); it.hasNext(); ) {
			Order o = ( Order ) it.next();
			if ( order1.getOrderId() == o.getOrderId() ) {
				// o.getLines() should contain all of its orderLines
				assertEquals( order1.getLines().size(), o.getLines().size() );
			}
			else if ( order2.getOrderId() == o.getOrderId() ) {
				assertEquals( order2.getLines() , o.getLines() );
				assertTrue( o.getLines().isEmpty() );
			}
			else {
				fail( "unknown order" );
			}
		}
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testSubcriteriaWithNullOrNonNullRestrictionsAliasToEntityMap() {
		Session s = openSession();
		s.getTransaction().begin();

		Criteria rootCriteria = s.createCriteria( Order.class, "o" );
		Criteria subCriteria = rootCriteria.createCriteria( "orderLines", "ol", JoinFragment.LEFT_OUTER_JOIN );
		assertNotSame( rootCriteria, subCriteria );

		// add restriction to subCriteria, ensuring we stay on subCriteria
		assertSame(
				subCriteria,
				subCriteria.add(
					Restrictions.or(
							Restrictions.isNull( "ol.articleId" ),		  // Allow null
							Restrictions.eq( "ol.articleId", "1000" )
					)
				)
		);

		List orders = rootCriteria.setResultTransformer( Criteria.ALIAS_TO_ENTITY_MAP ).list();

		// order1 should be returned because it has an orderline with articleId == "1000";
		// order2 should be returned because it has no orderlines
		assertEquals( 2, orders.size() );
		for ( Iterator it = orders.iterator(); it.hasNext(); ) {
			Map map = (Map) it.next();
			Order o = ( Order ) map.get( "o" );
		    // the orderLine returned from the map should either be null or have articleId = "1000"
			OrderLine ol = ( OrderLine ) map.get( "ol" );
			if ( order1.getOrderId() == o.getOrderId() ) {
				// o.getLines() should contain all of its orderLines
				assertEquals( order1.getLines().size(), o.getLines().size() );
				assertNotNull( ol );
				assertEquals( "1000", ol.getArticleId() );
			}
			else if ( order2.getOrderId() == o.getOrderId() ) {
				assertEquals( order2.getLines() , o.getLines() );
				assertTrue( o.getLines().isEmpty() );
				assertNull( ol );
			}
			else {
				fail( "unknown order" );
			}
		}
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testSubcriteriaWithClauseAliasToEntityMap() {
		Session s = openSession();
		s.getTransaction().begin();

		Criteria rootCriteria = s.createCriteria( Order.class, "o" );
		Criteria subCriteria = rootCriteria.createCriteria(
				"orderLines",
				"ol", JoinFragment.LEFT_OUTER_JOIN,
				Restrictions.or(
						Restrictions.isNull( "ol.articleId" ),		  // Allow null
						Restrictions.eq( "ol.articleId", "1000" )
				)
		);
		assertNotSame( rootCriteria, subCriteria );

		List orders = rootCriteria.setResultTransformer( Criteria.ALIAS_TO_ENTITY_MAP ).list();

		// all orders should be returned (via map.get( "o" )) with their full collections;
		assertEquals( 3, orders.size() );
		for ( Iterator it = orders.iterator(); it.hasNext(); ) {
			Map map = ( Map ) it.next();
			Order o = ( Order ) map.get( "o" );
		    // the orderLine returned from the map should either be null or have articleId = "1000"
			OrderLine ol = ( OrderLine ) map.get( "ol" );
			if ( order1.getOrderId() == o.getOrderId() ) {
				// o.getLines() should contain all of its orderLines
				assertEquals( order1.getLines().size(), o.getLines().size() );
				assertNotNull( ol );
				assertEquals( "1000", ol.getArticleId() );
			}
			else if ( order2.getOrderId() == o.getOrderId() ) {
				assertTrue( o.getLines().isEmpty() );
				assertNull( ol );
			}
			else if ( order3.getOrderId() == o.getOrderId() ) {
				assertEquals( order3.getLines().size(), o.getLines().size() );
				assertNull( ol);
			}
			else {
				fail( "unknown order" );
			}
		}
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testAliasWithNonNullRestrictions() {
		Session s = openSession();
		s.getTransaction().begin();

		Criteria rootCriteria = s.createCriteria( Order.class );
		// create alias, ensuring we stay on the root criteria
		assertSame( rootCriteria, rootCriteria.createAlias( "orderLines", "ol", JoinFragment.LEFT_OUTER_JOIN ) );

		// add restrictions to rootCriteria
		assertSame( rootCriteria, rootCriteria.add( Restrictions.eq( "ol.articleId", "3000" ) ) );

		List orders = rootCriteria.list();

		// order1 and order3 should be returned because each has articleId == "3000"
		// the contained collections should only have the orderLine with articleId == "3000"
		assertEquals( 2, orders.size() );
		for ( Iterator it = orders.iterator(); it.hasNext(); ) {
			Order o = (Order) it.next();
			if ( order1.getOrderId() == o.getOrderId() ) {
				assertEquals( 1, o.getLines().size() );
				assertEquals( "3000", o.getLines().iterator().next().getArticleId() );
			}
			else if ( order3.getOrderId() == o.getOrderId() ) {
				assertEquals( 1, o.getLines().size() );
				assertEquals( "3000", o.getLines().iterator().next().getArticleId() );
			}
			else {
				fail( "unknown order" );
			}
		}
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testAliasWithNullOrNonNullRestrictions() {
		Session s = openSession();
		s.getTransaction().begin();

		Criteria rootCriteria = s.createCriteria( Order.class );
		// create alias, ensuring we stay on the root criteria
		assertSame( rootCriteria, rootCriteria.createAlias( "orderLines", "ol", JoinFragment.LEFT_OUTER_JOIN ) );

		// add restrictions to rootCriteria
		assertSame(
				rootCriteria,
				rootCriteria.add(
						Restrictions.or(
								Restrictions.isNull( "ol.articleId" ),		  // Allow null
								Restrictions.eq( "ol.articleId", "1000" )
						)
				)
		);

		List orders = rootCriteria.list();

		// order1 should be returned because it has an orderline with articleId == "1000";
		// the contained collection for order1 should only have the orderLine with articleId == "1000";
		// order2 should be returned because it has no orderlines
		assertEquals( 2, orders.size() );
		for ( Object order : orders ) {
			Order o = (Order) order;
			if ( order1.getOrderId() == o.getOrderId() ) {
				assertEquals( "1000", o.getLines().iterator().next().getArticleId() );
			}
			else if ( order2.getOrderId() == o.getOrderId() ) {
				assertEquals( 0, o.getLines().size() );
			}
			else {
				fail( "unknown order" );
			}
		}
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testNonNullSubcriteriaRestrictionsOnRootCriteria() {
		Session s = openSession();
		s.getTransaction().begin();

		Criteria rootCriteria = s.createCriteria( Order.class );
		Criteria subCriteria = rootCriteria.createCriteria( "orderLines", "ol", JoinFragment.LEFT_OUTER_JOIN );
		assertNotSame( rootCriteria, subCriteria );

		// add restriction to rootCriteria (NOT subcriteria)
		assertSame( rootCriteria, rootCriteria.add( Restrictions.eq( "ol.articleId", "3000" ) ) );

		List orders = rootCriteria.list();

		// results should be the same as testAliasWithNonNullRestrictions() (using Criteria.createAlias())
		// order1 and order3 should be returned because each has articleId == "3000"
		// the contained collections should only have the orderLine with articleId == "3000"
		assertEquals( 2, orders.size() );
		for ( Iterator it = orders.iterator(); it.hasNext(); ) {
			Order o = (Order) it.next();
			if ( order1.getOrderId() == o.getOrderId() ) {
				assertEquals( 1, o.getLines().size() );
				assertEquals( "3000", o.getLines().iterator().next().getArticleId() );
			}
			else if ( order3.getOrderId() == o.getOrderId() ) {
				assertEquals( 1, o.getLines().size() );
				assertEquals( "3000", o.getLines().iterator().next().getArticleId() );
			}
			else {
				fail( "unknown order" );
			}
		}
		s.getTransaction().commit();
		s.close();
	}

	protected void prepareTest() {
		Session s = openSession();
		s.getTransaction().begin();

		// Order with one mathing line
		order1 = new Order();
		OrderLine line = new OrderLine();
		line.setArticleId( "1000" );
		order1.addLine( line );
		line = new OrderLine();
		line.setArticleId( "3000" );
		order1.addLine( line );
		s.persist( order1 );

		// Order with no lines
		order2 = new Order();
		s.persist( order2 );

		// Order with non-matching line
		order3 = new Order();
		line = new OrderLine();
		line.setArticleId( "3000" );
		order3.addLine( line );
		s.persist( order3 );

		s.getTransaction().commit();
		s.close();
	}

	protected void cleanupTest() {
		Session s = openSession();
		s.getTransaction().begin();

		s.createQuery( "delete from OrderLine" ).executeUpdate();

		s.createQuery( "delete from Order" ).executeUpdate();

		s.getTransaction().commit();
		s.close();
	}

	private static boolean isBlank(String s) {
		return s == null || s.trim().length() == 0;
	}
}
