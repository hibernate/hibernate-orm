/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.subselect;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.type.StringType;

/**
 * @author Sharath Reddy
 */
public class SubselectTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testSubselectWithSynchronize() {

		Session s = openSession();
		Transaction tx = s.beginTransaction();

		//We don't use auto-generated ids because these seem to cause the session to flush.
		//We want to test that the session flushes because of the 'synchronize' annotation
		long itemId = 1;
		Item item = new Item();
		item.setName("widget");
		item.setId(itemId);
		s.save(item);
		
		Bid bid1 = new Bid();
		bid1.setAmount(100.0);
		bid1.setItemId(itemId);
		bid1.setId(1);
		s.save(bid1);
		
		Bid bid2 = new Bid();
		bid2.setAmount(200.0);
		bid2.setItemId(itemId);
		bid2.setId(2);
		s.save(bid2);
		
		//Because we use 'synchronize' annotation, this query should trigger session flush
		Query query = s.createQuery("from HighestBid b where b.name = :name");
		query.setParameter( "name", "widget", StringType.INSTANCE );
		HighestBid highestBid = (HighestBid) query.list().iterator().next();
		
		Assert.assertEquals( 200.0, highestBid.getAmount(), 0.01 );
		tx.rollback();		
		s.close();	
		
		
	}
	
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{
				Item.class,
				Bid.class,
				HighestBid.class
		};
	}

}
