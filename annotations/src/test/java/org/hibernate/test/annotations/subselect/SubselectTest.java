/*
  * Hibernate, Relational Persistence for Idiomatic Java
  *
  * Copyright (c) 2009, Red Hat, Inc. and/or its affiliates or third-
  * party contributors as indicated by the @author tags or express 
  * copyright attribution statements applied by the authors.  
  * All third-party contributions are distributed under license by 
  * Red Hat, Inc.
  *
  * This copyrighted material is made available to anyone wishing to 
  * use, modify, copy, or redistribute it subject to the terms and 
  * conditions of the GNU Lesser General Public License, as published 
  * by the Free Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of 
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU 
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public 
  * License along with this distribution; if not, write to:
  * 
  * Free Software Foundation, Inc.
  * 51 Franklin Street, Fifth Floor
  * Boston, MA  02110-1301  USA
  */

package org.hibernate.test.annotations.subselect;

import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.annotations.TestCase;


/**
 * @author Sharath Reddy
 */
public class SubselectTest extends TestCase {

	public void testSubselectWithSynchronize() {

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		tx.begin();
		
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
		query.setParameter("name", "widget", Hibernate.STRING);
		HighestBid highestBid = (HighestBid) query.list().iterator().next();
		
		assertEquals(200.0, highestBid.getAmount());
		
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
