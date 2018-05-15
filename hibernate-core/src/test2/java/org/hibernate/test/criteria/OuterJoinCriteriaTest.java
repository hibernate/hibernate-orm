/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.criteria;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Session;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Restrictions;
import org.hibernate.sql.JoinFragment;
import org.hibernate.sql.JoinType;
import org.hibernate.testing.TestForIssue;
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

	@Test
	@TestForIssue( jiraKey = "HHH-9597")
	public void testMultipleSubCriteriaRestrictionsOnCollections() {
		Session s = openSession();
		s.getTransaction().begin();

		Criteria rootCriteria = s.createCriteria(Order.class, "order");
		rootCriteria.createCriteria( "order.orderLines", "line", JoinType.LEFT_OUTER_JOIN )
				.add(Restrictions.eq("line.articleId", "3000"));
		rootCriteria.createCriteria( "order.orderContacts", "contact", JoinType.LEFT_OUTER_JOIN )
				.add(Restrictions.eq("contact.contact", "Contact1"));
		// result should be order1, because that's the only Order with:
		// 1) orderLines containing an OrderLine with articleId == "3000"
		// and 2) orderContacts containing an OrderContact with contact == "Contact1"
		// Since both restrictions are in subcriteria, both collections should be non-filtered
		// (i.e. includes all elements in both collections)
		Order result = (Order) rootCriteria.uniqueResult();
		assertEquals( order1.getOrderId(), result.getOrderId() );
		assertEquals( 2, result.getContacts().size() );
		assertEquals( 2, result.getLines().size() );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9597")
	public void testMultipleRootCriteriaRestrictionsOnCollections() {
		Session s = openSession();
		s.getTransaction().begin();

		Criteria rootCriteria = s.createCriteria(Order.class, "order");
		rootCriteria.createAlias( "order.orderLines", "line", JoinType.LEFT_OUTER_JOIN )
				.add(Restrictions.eq("line.articleId", "3000"));
		rootCriteria.createAlias("order.orderContacts", "contact", JoinType.LEFT_OUTER_JOIN)
				.add( Restrictions.eq( "contact.contact", "Contact1" ) );
		// result should be order1, because that's the only Order with:
		// 1) orderLines containing an OrderLine with articleId == "3000"
		// and 2) orderContacts containing an OrderContact with contact == "Contact1"
		// Since both restrictions are in root criteria, both collections should be filtered
		// to contain only the elements that satisfy the restrictions.
		Order result = (Order) rootCriteria.uniqueResult();
		assertEquals( order1.getOrderId(), result.getOrderId() );
		assertEquals( 1, result.getLines().size() );
		assertEquals( "3000", result.getLines().iterator().next().getArticleId() );
		assertEquals( 1, result.getContacts().size() );
		assertEquals( "Contact1", result.getContacts().iterator().next().getContact() );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9597")
	public void testRootAndSubCriteriaRestrictionsOnCollections() {
		// the result of all Criteria in this test will be order1, because that's the only Order with:
		// 1) orderLines containing an OrderLine with articleId == "3000"
		// and 2) orderContacts containing an OrderContact with contact == "Contact1"

		Session s = openSession();
		s.getTransaction().begin();
		Criteria rootCriteria = s.createCriteria(Order.class, "order");
		rootCriteria.createCriteria("order.orderContacts", "contact", JoinType.LEFT_OUTER_JOIN)
				.add( Restrictions.eq( "contact.contact", "Contact1" ) );
		rootCriteria.createAlias("order.orderLines", "line", JoinType.LEFT_OUTER_JOIN)
				.add(Restrictions.eq("line.articleId", "3000"));
		// Since restriction on orderLines is on root criteria, that collection should be filtered.
		// Since restriction on orderContacts is on a subcriteria, that collection should be
		// non-filtered (contain all its elements)
		Order result = (Order) rootCriteria.uniqueResult();
		assertEquals( order1.getOrderId(), result.getOrderId() );
		assertEquals( 1, result.getLines().size() );
		assertEquals( "3000", result.getLines().iterator().next().getArticleId() );
		assertEquals( 2, result.getContacts().size() );
		s.getTransaction().commit();
		s.close();

		// The following should have the same result as the previous, since it has the same
		// restrictions applied in reverse order.

		s = openSession();
		s.getTransaction().begin();
		rootCriteria = s.createCriteria(Order.class, "order");
		rootCriteria.createCriteria("order.orderContacts", "contact", JoinType.LEFT_OUTER_JOIN)
				.add(Restrictions.eq("contact.contact", "Contact1"));
		rootCriteria.createAlias( "order.orderLines", "line", JoinType.LEFT_OUTER_JOIN )
				.add(Restrictions.eq("line.articleId", "3000"));
		// Since restriction on orderLines is on root criteria, that collection should be filtered.
		// Since restriction on orderContacts is on a subcriteria, that collection should be
		// non-filtered (contain all its elements)
		result = (Order) rootCriteria.uniqueResult();
		assertEquals( order1.getOrderId(), result.getOrderId() );
		assertEquals( 1, result.getLines().size() );
		assertEquals( "3000", result.getLines().iterator().next().getArticleId() );
		assertEquals( 2, result.getContacts().size() );
		s.getTransaction().commit();
		s.close();

		// Even though the following seem redundant, there was a failure due to HHH-9597
		// that reproduced when filtering Order.orderContacts, but not order.orderLines.

		s = openSession();
		s.getTransaction().begin();
		rootCriteria = s.createCriteria(Order.class, "order");
		rootCriteria.createAlias( "order.orderContacts", "contact", JoinType.LEFT_OUTER_JOIN )
				.add(Restrictions.eq("contact.contact", "Contact1"));
		rootCriteria.createCriteria( "order.orderLines", "line", JoinType.LEFT_OUTER_JOIN )
				.add(Restrictions.eq("line.articleId", "3000"));
		// Since restriction on orderContacts is on root criteria, that collection should be filtered.
		// Since restriction on orderLines is on a subcriteria, that collection should be
		// non-filtered (contain all its elements)
		result = (Order) rootCriteria.uniqueResult();
		assertEquals( order1.getOrderId(), result.getOrderId() );
		assertEquals( 2, result.getLines().size() );
		assertEquals( 1, result.getContacts().size() );
		assertEquals( "Contact1", result.getContacts().iterator().next().getContact() );
		s.getTransaction().commit();
		s.close();

		// The following should have the same result as the previous, since it has the same
		// restrictions applied in reverse order.

		s = openSession();
		s.getTransaction().begin();
		rootCriteria = s.createCriteria(Order.class, "order");
		rootCriteria.createCriteria( "order.orderLines", "line", JoinType.LEFT_OUTER_JOIN )
				.add(Restrictions.eq("line.articleId", "3000"));
		rootCriteria.createAlias( "order.orderContacts", "contact", JoinType.LEFT_OUTER_JOIN )
				.add(Restrictions.eq("contact.contact", "Contact1"));
		result = (Order) rootCriteria.uniqueResult();
		assertEquals( order1.getOrderId(), result.getOrderId() );
		assertEquals( 2, result.getLines().size() );
		assertEquals( 1, result.getContacts().size() );
		assertEquals( "Contact1", result.getContacts().iterator().next().getContact() );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9597")
	public void testSubCriteriaRestrictionsOnCollectionsNestedInManyToOne() {
		// the result of all Criteria in this test will be order1, because that's the only Order with:
		// 1) orderContacts containing an OrderContact with contact == "Contact1"
		// and 2) orderAddress.notifiedAddresses containing an Address with addressText == "over the rainbow"

		Session s = openSession();
		s.getTransaction().begin();
		Criteria rootCriteria = s.createCriteria(Order.class, "order");
		rootCriteria.createCriteria("order.orderContacts", "contact", JoinType.LEFT_OUTER_JOIN)
				.add( Restrictions.eq( "contact.contact", "Contact1" ) );
		rootCriteria.createCriteria( "order.orderAddress", "orderAddress", JoinType.LEFT_OUTER_JOIN )
				.createCriteria( "orderAddress.notifiedAddresses", "notifiedAddress", JoinType.LEFT_OUTER_JOIN )
				.add( Restrictions.eq( "notifiedAddress.addressText", "over the rainbow" ) );

		// Since restrictions are on subcriteria, the collections should n on orderLines is on root criteria, that collection should be filtered.
		// Since restriction on orderContacts is on a subcriteria, that collection should be
		// non-filtered (contain all its elements)
		Order result = (Order) rootCriteria.uniqueResult();
		assertEquals( order1.getOrderId(), result.getOrderId() );
		assertEquals( 2, result.getContacts().size() );
		assertEquals( 2, result.getOrderAddress().getNotifiedAddresses().size() );
		s.getTransaction().commit();
		s.close();

		// The following should have the same result as the previous, since it has the same
		// restrictions applied in reverse order.

		s = openSession();
		s.getTransaction().begin();
		rootCriteria = s.createCriteria(Order.class, "order");
		rootCriteria.createCriteria( "order.orderAddress", "orderAddress", JoinType.LEFT_OUTER_JOIN )
				.createCriteria( "orderAddress.notifiedAddresses", "notifiedAddress", JoinType.LEFT_OUTER_JOIN )
				.add( Restrictions.eq( "notifiedAddress.addressText", "over the rainbow" ) );
		rootCriteria.createCriteria("order.orderContacts", "contact", JoinType.LEFT_OUTER_JOIN)
				.add( Restrictions.eq( "contact.contact", "Contact1" ) );
		// Since restrictions are on subcriteria, the collections should n on orderLines is on root criteria, that collection should be filtered.
		// Since restriction on orderContacts is on a subcriteria, that collection should be
		// non-filtered (contain all its elements)
		result = (Order) rootCriteria.uniqueResult();
		assertEquals( order1.getOrderId(), result.getOrderId() );
		assertEquals( 2, result.getContacts().size() );
		assertEquals( 2, result.getOrderAddress().getNotifiedAddresses().size() );
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

		OrderContact contact = new OrderContact();
		contact.setContact( "Contact1" );
		order1.addContact( contact );
		contact = new OrderContact();
		contact.setContact( "Contact2" );
		order1.addContact( contact );

		OrderAddress orderAddress = new OrderAddress();
		Address address = new Address();
		address.setAddressText( "over the rainbow" );
		orderAddress.setDeliveryAddress( address );
		Address otherAddress = new Address();
		otherAddress.setAddressText( "other place" );
		orderAddress.getNotifiedAddresses().add( address );
		orderAddress.getNotifiedAddresses().add( otherAddress );
		order1.setOrderAddress( orderAddress );

		s.persist( order1 );

		// Order with no lines
		order2 = new Order();
		contact = new OrderContact();
		contact.setContact( "Contact1" );
		order2.addContact( contact );
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

		List orders = s.createQuery( "from Order" ).list();

		for( Object order : orders ) {
			s.delete( order );
		}
		s.createQuery( "delete from OrderContact" ).executeUpdate();

		s.createQuery( "delete from OrderLine" ).executeUpdate();

		s.createQuery( "delete from Order" ).executeUpdate();

		s.getTransaction().commit();
		s.close();
	}

	private static boolean isBlank(String s) {
		return s == null || s.trim().length() == 0;
	}
}
