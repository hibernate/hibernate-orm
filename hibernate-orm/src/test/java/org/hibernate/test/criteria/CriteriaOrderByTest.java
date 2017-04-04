/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.criteria;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Projections;
import org.hibernate.sql.JoinType;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.transform.ResultTransformer;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * @author tknowlton at iamhisfriend dot org
 */
public class CriteriaOrderByTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Bid.class, Item.class };
	}

	@Test
	@TestForIssue(jiraKey = "HHH-7116")
	public void testCriteriaOrderBy() {
		final Session s = openSession();
		final Transaction tx = s.beginTransaction();

		Item item;
		Bid bid;

		item = new Item();
		item.name = "ZZZZ";
		s.persist( item );

		bid = new Bid();
		bid.amount = 444.44f;
		bid.item = item;
		s.persist( bid );

		item = new Item();
		item.name = "AAAA";
		s.persist( item );

		bid = new Bid();
		bid.amount = 222.22f;
		bid.item = item;
		s.persist( bid );

		item = new Item();
		item.name = "MMMM";
		s.persist( item );

		bid = new Bid();
		bid.amount = 999.99f;
		bid.item = item;
		s.persist( bid );

		s.flush();

		// For each item, ordered by name, show all bids made by bidders on this item.
		// The joined collections item.bids and bidder.bids have orderings specified on the mappings.
		// For some reason, the association mappings' ordering specifications are not honored if default (INNER) join
		// type is used.
		final Criteria criteria = s
				.createCriteria( Item.class )
				.addOrder( org.hibernate.criterion.Order.asc( "this.name" ) )
				.createAlias( "this.bids", "i_bid", JoinType.LEFT_OUTER_JOIN )
				.setProjection(
						Projections.projectionList().add( Projections.property( "this.name" ), "item_name" )
								.add( Projections.property( "i_bid.amount" ), "bid_amount" ) )
				.setResultTransformer( new ResultTransformer() {
					boolean first = true;
					Object[] previous;

					@Override
					public Object transformTuple(Object[] tuple, String[] aliases) {
						if ( first ) {
							first = false;
							previous = tuple;
						}
						else {
							final String previousName = (String) previous[0];
							final String name = (String) tuple[0];

							Assert.assertTrue(
									"The resultset tuples should be ordered by item name, as specified on the Criteria",
									previousName.compareTo( name ) < 1 );

							previous = tuple;
						}

						return tuple;
					}

					@Override
					public List transformList(List collection) {
						return collection;
					}
				} );

		criteria.list();

		tx.rollback();
		s.close();
	}
}
