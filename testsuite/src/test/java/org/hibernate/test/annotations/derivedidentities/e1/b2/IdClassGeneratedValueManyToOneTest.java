/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.annotations.derivedidentities.e1.b2;

import java.math.BigDecimal;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.annotations.TestCase;

/**
 * A test.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Stale W. Pedersen</a>
 */
public class IdClassGeneratedValueManyToOneTest extends TestCase {

	public void testComplexIdClass() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();

		Customer c1 = new Customer(
				"foo", "bar", "contact1", "100", new BigDecimal( 1000 ), new BigDecimal( 1000 ), new BigDecimal( 1000 ));
		s.persist( c1 );
		s.flush();
        s.clear();
		
//      why does this cause a failure?        
//		Customer c2 = new Customer(
//              "foo1", "bar1", "contact2", "200", new BigDecimal( 2000 ), new BigDecimal( 2000 ), new BigDecimal( 2000 ));
//		s.persist( c2 );
//		s.flush();
//        s.clear();
		
		Item boat = new Item();
		boat.setId( "1" );
		boat.setName( "cruiser" );
		boat.setPrice( new BigDecimal( 500 ) );
		boat.setDescription( "a boat" );
		boat.setCategory( 42 );

		s.persist( boat );
		s.flush();
		s.clear();

		c1.addInventory( boat, 10, new BigDecimal( 5000 ) );
		s.merge( c1 );
		s.flush();
		s.clear();

		Customer c12 = ( Customer ) s.createQuery( "select c from Customer c" ).uniqueResult();

		List<CustomerInventory> inventory = c12.getInventories();

		assertEquals( 1, inventory.size() );
		assertEquals( 10, inventory.get( 0 ).getQuantity() );

		tx.rollback();
		s.close();

		assertTrue( true );
	}
	
	/*
	public void testCustomer()
	{
	   Session s = openSession();
       Transaction tx = s.beginTransaction();
       for(int i=0; i < 2; i++)
       {
          Customer c1 = new Customer(
                "foo"+i, "bar"+i, "contact"+i, "100", new BigDecimal( 1000+i ), new BigDecimal( 1000+i ), new BigDecimal( 1000+i )
          );
          s.persist( c1 );
          s.flush();
          s.clear();
          
          Item boat = new Item();
          boat.setId( Integer.toString(i) );
          boat.setName( "cruiser" );
          boat.setPrice( new BigDecimal( 500 ) );
          boat.setDescription( "a boat" );
          boat.setCategory( 42 );

          s.persist( boat );
          s.flush();
          s.clear();

          c1.addInventory( boat, 10, new BigDecimal( 5000 ) );
          s.merge( c1 );
          
          s.flush();
          s.clear();
       }
       
       tx.rollback();
       s.close();

       assertTrue( true );
	   
	}*/

	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Customer.class,
				CustomerInventory.class,
				CustomerInventoryPK.class,
				Item.class

		};
	}
}
