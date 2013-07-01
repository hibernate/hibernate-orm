/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
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
